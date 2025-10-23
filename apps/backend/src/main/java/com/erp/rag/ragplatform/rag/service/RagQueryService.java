package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.LatencyMetrics;
import com.erp.rag.ragplatform.rag.dto.QueryRequest;
import com.erp.rag.ragplatform.rag.dto.QueryResponse;
import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.ragplatform.worker.service.embedding.EmbeddingService.EmbeddingGenerationException;
import com.erp.rag.supabase.entity.RagQuery;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main orchestration service for RAG query processing.
 * <p>
 * Story 1.5 – AC1, AC10: Orchestrates query embedding generation, vector
 * search, context window management, and query logging. Returns structured
 * response with queryId, retrieved documents, grounded context, and latency
 * metrics.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class RagQueryService {

    private static final Logger logger = LoggerFactory.getLogger(RagQueryService.class);

    private final QueryEmbeddingService queryEmbeddingService;
    private final VectorSearchService vectorSearchService;
    private final ContextWindowManager contextWindowManager;
    private final QueryLoggerService queryLoggerService;
    private final Counter queryCounter;
    private final Counter errorCounter;
    private final Timer queryLatencyTimer;

    public RagQueryService(QueryEmbeddingService queryEmbeddingService,
            VectorSearchService vectorSearchService,
            ContextWindowManager contextWindowManager,
            QueryLoggerService queryLoggerService,
            MeterRegistry meterRegistry) {
        this.queryEmbeddingService = queryEmbeddingService;
        this.vectorSearchService = vectorSearchService;
        this.contextWindowManager = contextWindowManager;
        this.queryLoggerService = queryLoggerService;

        // Initialize metrics
        this.queryCounter = Counter.builder("rag_query_total")
                .description("Total number of RAG queries processed")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("rag_query_errors_total")
                .description("Total number of RAG query errors")
                .register(meterRegistry);

        this.queryLatencyTimer = Timer.builder("rag_query_latency_seconds")
                .description("RAG query latency distribution")
                .register(meterRegistry);
    }

    /**
     * Process a RAG query end-to-end.
     *
     * @param request query request
     * @param userId  user ID (optional, from JWT)
     * @return query response with results
     */
    public QueryResponse processQuery(QueryRequest request, UUID userId) {
        long startTime = System.currentTimeMillis();

        logger.info("Processing RAG query: companyId={}, language={}, userId={}",
                request.getCompanyId(), request.getLanguage(), userId);

        RagQuery ragQuery = null;

        try {
            // Step 1: Generate query embedding
            long embeddingStart = System.currentTimeMillis();
            float[] embedding = queryEmbeddingService.generateQueryEmbedding(request.getQuery());
            String embeddingStr = queryEmbeddingService.formatEmbeddingForPostgres(embedding);
            int embeddingLatency = (int) (System.currentTimeMillis() - embeddingStart);

            // Step 2: Log query start
            ragQuery = queryLoggerService.logQueryStart(
                    request.getCompanyId(),
                    userId,
                    request.getQuery(),
                    embeddingStr,
                    request.getLanguage());

            // Step 3: Execute vector search
            long searchStart = System.currentTimeMillis();
            List<RetrievedDocumentDTO> retrievedDocuments = vectorSearchService.search(
                    request.getCompanyId(),
                    embeddingStr,
                    request.getFilters());
            int searchLatency = (int) (System.currentTimeMillis() - searchStart);

            // Step 4: Build grounded context
            long contextStart = System.currentTimeMillis();
            List<UUID> documentIds = retrievedDocuments.stream()
                    .map(RetrievedDocumentDTO::getId)
                    .collect(Collectors.toList());

            String groundedContext = contextWindowManager.buildGroundedContext(
                    retrievedDocuments,
                    documentIds,
                    request.getCompanyId());

            List<Integer> tokensPerDoc = contextWindowManager.calculateTokensPerDocument(
                    documentIds,
                    request.getCompanyId());
            int contextLatency = (int) (System.currentTimeMillis() - contextStart);

            // Step 5: Log query completion
            int totalLatency = (int) (System.currentTimeMillis() - startTime);
            queryLoggerService.logQueryComplete(
                    ragQuery.getId(),
                    searchLatency,
                    totalLatency,
                    retrievedDocuments,
                    tokensPerDoc);

            // Step 6: Build response
            LatencyMetrics latencyMetrics = new LatencyMetrics(
                    embeddingLatency,
                    searchLatency,
                    contextLatency,
                    totalLatency);

            QueryResponse response = new QueryResponse(
                    ragQuery.getId(),
                    retrievedDocuments,
                    groundedContext,
                    latencyMetrics);

            // Update metrics
            queryCounter.increment();
            queryLatencyTimer.record(totalLatency, java.util.concurrent.TimeUnit.MILLISECONDS);

            logger.info("RAG query completed successfully: queryId={}, latency={}ms",
                    ragQuery.getId(), totalLatency);

            return response;

        } catch (EmbeddingGenerationException e) {
            logger.error("Embedding generation failed: {}", e.getMessage(), e);
            errorCounter.increment();
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);

        } catch (Exception e) {
            logger.error("RAG query processing failed: {}", e.getMessage(), e);
            errorCounter.increment();
            
            // Log error to query if we have a query ID
            if (ragQuery != null) {
                try {
                    queryLoggerService.logQueryError(ragQuery.getId(), e.getMessage());
                } catch (Exception logError) {
                    logger.error("Failed to log query error: {}", logError.getMessage());
                }
            }
            
            throw new RuntimeException("Failed to process query: " + e.getMessage(), e);
        }
    }
}

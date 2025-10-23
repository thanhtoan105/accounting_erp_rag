package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.supabase.entity.RagQuery;
import com.erp.rag.supabase.entity.RagQueryDocument;
import com.erp.rag.supabase.repository.RagQueryDocumentRepository;
import com.erp.rag.supabase.repository.RagQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for logging RAG queries to audit trail.
 * <p>
 * Story 1.5 – AC7: Persist queries and retrieved documents to immutable
 * audit trail with timestamps, latency metrics (Circular 200 compliance).
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class QueryLoggerService {

    private static final Logger logger = LoggerFactory.getLogger(QueryLoggerService.class);

    private final RagQueryRepository ragQueryRepository;
    private final RagQueryDocumentRepository ragQueryDocumentRepository;

    public QueryLoggerService(RagQueryRepository ragQueryRepository,
            RagQueryDocumentRepository ragQueryDocumentRepository) {
        this.ragQueryRepository = ragQueryRepository;
        this.ragQueryDocumentRepository = ragQueryDocumentRepository;
    }

    /**
     * Create initial query log entry.
     *
     * @param companyId      company UUID
     * @param userId         user UUID (optional)
     * @param queryText      the query text
     * @param queryEmbedding the query embedding string
     * @param language       the language
     * @return the created RagQuery entity
     */
    @Transactional
    public RagQuery logQueryStart(UUID companyId, UUID userId, String queryText,
            String queryEmbedding, String language) {
        logger.debug("Logging query start for company: {}, user: {}", companyId, userId);

        RagQuery query = new RagQuery();
        query.setCompanyId(companyId);
        query.setUserId(userId);
        query.setQueryText(queryText);
        query.setQueryEmbedding(queryEmbedding);
        query.setLanguage(language);
        query.setStatus("pending");
        query.setCreatedAt(OffsetDateTime.now());

        query = ragQueryRepository.save(query);
        logger.info("Query logged with ID: {}", query.getId());

        return query;
    }

    /**
     * Update query with completion details.
     *
     * @param queryId               the query UUID
     * @param retrievalLatencyMs    retrieval latency
     * @param totalLatencyMs        total latency
     * @param retrievedDocuments    list of retrieved documents
     * @param tokensPerDocument     tokens used per document
     */
    @Transactional
    public void logQueryComplete(UUID queryId, Integer retrievalLatencyMs,
            Integer totalLatencyMs, List<RetrievedDocumentDTO> retrievedDocuments,
            List<Integer> tokensPerDocument) {
        logger.debug("Logging query completion for query ID: {}", queryId);

        // Update query status
        RagQuery query = ragQueryRepository.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("Query not found: " + queryId));

        query.setStatus("complete");
        query.setRetrievalLatencyMs(retrievalLatencyMs);
        query.setTotalLatencyMs(totalLatencyMs);
        query.setCompletedAt(OffsetDateTime.now());

        ragQueryRepository.save(query);

        // Log retrieved documents
        for (int i = 0; i < retrievedDocuments.size(); i++) {
            RetrievedDocumentDTO doc = retrievedDocuments.get(i);
            Integer tokens = i < tokensPerDocument.size() ? tokensPerDocument.get(i) : 0;

            RagQueryDocument queryDoc = new RagQueryDocument();
            queryDoc.setQueryId(queryId);
            queryDoc.setDocumentVectorId(doc.getId());
            queryDoc.setRank(i + 1);
            queryDoc.setRelevanceScore(doc.getRelevanceScore());
            queryDoc.setTokensUsed(tokens);
            queryDoc.setExcerpt(doc.getExcerpt());

            ragQueryDocumentRepository.save(queryDoc);
        }

        logger.info("Query completion logged: {} documents", retrievedDocuments.size());
    }

    /**
     * Log query error.
     *
     * @param queryId      the query UUID
     * @param errorMessage the error message
     */
    @Transactional
    public void logQueryError(UUID queryId, String errorMessage) {
        logger.error("Logging query error for query ID: {}, error: {}", queryId, errorMessage);

        RagQuery query = ragQueryRepository.findById(queryId)
                .orElseThrow(() -> new IllegalArgumentException("Query not found: " + queryId));

        query.setStatus("error");
        query.setErrorMessage(errorMessage);
        query.setCompletedAt(OffsetDateTime.now());

        ragQueryRepository.save(query);
    }
}

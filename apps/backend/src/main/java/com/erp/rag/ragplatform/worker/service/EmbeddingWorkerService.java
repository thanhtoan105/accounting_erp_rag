package com.erp.rag.ragplatform.worker.service;

import com.erp.rag.ragplatform.worker.domain.EmbeddingBatch;
import com.erp.rag.ragplatform.worker.domain.ErpDocument;
import com.erp.rag.ragplatform.worker.repository.EmbeddingBatchRepository;
import com.erp.rag.ragplatform.worker.service.embedding.EmbeddingService;
import com.erp.rag.supabase.vector.VectorDocument;
import com.erp.rag.supabase.vector.VectorDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Main service orchestrating the embedding generation pipeline.
 * <p>
 * Story 1.4 – AC1-10: End-to-end pipeline implementation:
 * - Document extraction (AC1)
 * - Text preparation with PII masking (AC2, AC5)
 * - Embedding generation with batching (AC3, AC4)
 * - Metadata extraction and persistence (AC6)
 * - Error handling with retry logic (AC7)
 * - Batch tracking and state machine (AC9)
 * - Progress logging and telemetry (AC10)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class EmbeddingWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingWorkerService.class);
    private static final int BATCH_SIZE = 100; // Max batch size per AC3
    private static final int PROGRESS_LOG_INTERVAL = 1000; // Log progress every 1000 docs per AC4

    private final DocumentExtractor documentExtractor;
    private final TextTemplateRenderer textRenderer;
    private final EmbeddingService embeddingService;
    private final VectorDocumentRepository vectorRepository;
    private final EmbeddingBatchRepository batchRepository;
    private final ObjectMapper objectMapper;

    public EmbeddingWorkerService(
            DocumentExtractor documentExtractor,
            TextTemplateRenderer textRenderer,
            EmbeddingService embeddingService,
            VectorDocumentRepository vectorRepository,
            EmbeddingBatchRepository batchRepository,
            ObjectMapper objectMapper) {
        this.documentExtractor = documentExtractor;
        this.textRenderer = textRenderer;
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.batchRepository = batchRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute embedding generation batch for a company.
     *
     * @param companyId    company UUID
     * @param batchType    full or incremental
     * @param triggeredBy  user/system identifier
     * @param sourceTables optional list of specific tables to process
     * @param updatedAfter optional timestamp for incremental sync
     * @return batch UUID
     */
    @Transactional
    public UUID executeBatch(
            UUID companyId,
            EmbeddingBatch.BatchType batchType,
            String triggeredBy,
            List<String> sourceTables,
            OffsetDateTime updatedAfter) {

        logger.info("Starting embedding batch for company {} (type: {}, triggeredBy: {})",
                companyId, batchType, triggeredBy);

        // Create batch record
        EmbeddingBatch batch = new EmbeddingBatch(companyId, batchType, triggeredBy);

        // Calculate hash to prevent duplicate processing
        String batchHash = calculateBatchHash(companyId, batchType, sourceTables, updatedAfter);
        batch.setBatchHash(batchHash);

        // Check for duplicate batch
        Optional<EmbeddingBatch> existingBatch = batchRepository.findByBatchHash(batchHash);
        if (existingBatch.isPresent() &&
                (existingBatch.get().getStatus() == EmbeddingBatch.Status.RUNNING ||
                        existingBatch.get().getStatus() == EmbeddingBatch.Status.QUEUED)) {
            logger.warn("Duplicate batch detected with hash {}, skipping", batchHash);
            return existingBatch.get().getId();
        }

        batch = batchRepository.save(batch);

        try {
            // Step 1: Extract documents
            List<ErpDocument> documents;
            if (sourceTables != null && !sourceTables.isEmpty()) {
                documents = documentExtractor.extractFrom(companyId, sourceTables, updatedAfter);
            } else {
                documents = documentExtractor.extractAll(companyId, updatedAfter);
            }

            logger.info("Extracted {} documents for batch {}", documents.size(), batch.getId());

            // Start batch
            batch.start(documents.size());
            batch = batchRepository.save(batch);

            // Step 2: Process documents in batches
            int processedCount = 0;
            int failedCount = 0;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, documents.size());
                List<ErpDocument> batchDocs = documents.subList(i, endIndex);

                try {
                    processBatch(batchDocs, companyId);
                    processedCount += batchDocs.size();

                    // Update batch progress
                    batch.setProcessedDocuments(processedCount);
                    batchRepository.save(batch);

                    // Log progress every 1000 docs (AC4)
                    if (processedCount % PROGRESS_LOG_INTERVAL == 0 || endIndex == documents.size()) {
                        long elapsedMs = System.currentTimeMillis() - startTime;
                        double throughput = (processedCount / (elapsedMs / 1000.0)) * 60; // docs/min
                        long estimatedCompletionMs = (long) ((documents.size() - processedCount)
                                / (processedCount / (double) elapsedMs) * 1000);

                        logger.info(
                                "Progress: {}/{} docs processed ({} failed) | Throughput: {:.1f} docs/min | Elapsed: {}s | ETA: {}s",
                                processedCount, documents.size(), failedCount,
                                throughput, elapsedMs / 1000, estimatedCompletionMs / 1000);
                    }

                } catch (Exception e) {
                    logger.error("Failed to process batch of {} documents at index {}: {}",
                            batchDocs.size(), i, e.getMessage(), e);
                    failedCount += batchDocs.size();
                    batch.setFailedDocuments(failedCount);
                }
            }

            // Calculate final metrics
            long totalElapsedMs = System.currentTimeMillis() - startTime;
            double avgThroughput = (processedCount / (totalElapsedMs / 1000.0)) * 60;

            // Store metrics in metadata
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("total_elapsed_ms", totalElapsedMs);
            metadata.put("avg_throughput_docs_per_min", avgThroughput);
            metadata.put("api_cost_usd", calculateCost(processedCount));
            batch.setMetadata(metadata);

            // Complete batch
            batch.complete();
            batch = batchRepository.save(batch);

            logger.info("Batch {} completed: {} processed, {} failed, {:.1f} docs/min, {}s elapsed",
                    batch.getId(), processedCount, failedCount, avgThroughput, totalElapsedMs / 1000);

            // Check error rate alert threshold (AC7)
            double errorRate = failedCount / (double) documents.size();
            if (errorRate > 0.05) {
                logger.error("ERROR RATE ALERT: {:.1f}% failures exceeds 5% threshold for batch {}",
                        errorRate * 100, batch.getId());
                // TODO: Send Slack alert per AC10
            }

            return batch.getId();

        } catch (Exception e) {
            logger.error("Batch {} failed: {}", batch.getId(), e.getMessage(), e);
            batch.fail(e.getMessage());
            batchRepository.save(batch);
            throw new RuntimeException("Embedding batch failed", e);
        }
    }

    /**
     * Process a batch of documents: render text, generate embeddings, persist
     * vectors.
     */
    private void processBatch(List<ErpDocument> documents, UUID companyId) throws Exception {
        List<String> texts = new ArrayList<>();
        List<ErpDocument> validDocs = new ArrayList<>();

        // Step 1: Render all documents to text with PII masking
        for (ErpDocument doc : documents) {
            try {
                String maskedText = textRenderer.renderDocument(doc);
                if (maskedText != null && !maskedText.isBlank()) {
                    texts.add(maskedText);
                    validDocs.add(doc);
                } else {
                    logger.warn("Skipping document with empty text: {} id={}",
                            doc.getDocumentType(), doc.getId());
                }
            } catch (TextTemplateRenderer.TextRenderingException e) {
                // Critical PII masking failure - re-throw to halt batch
                throw e;
            } catch (Exception e) {
                logger.error("Error rendering document {} id={}: {}",
                        doc.getDocumentType(), doc.getId(), e.getMessage());
                // Skip malformed document per AC7
            }
        }

        if (texts.isEmpty()) {
            logger.warn("No valid texts to process in this batch");
            return;
        }

        // Step 2: Generate embeddings
        List<float[]> embeddings = embeddingService.generateEmbeddings(texts);

        if (embeddings.size() != texts.size()) {
            throw new IllegalStateException(
                    "Embedding count mismatch: expected " + texts.size() + ", got " + embeddings.size());
        }

        // Step 3: Persist vectors with metadata (AC6)
        for (int i = 0; i < validDocs.size(); i++) {
            ErpDocument doc = validDocs.get(i);
            float[] embedding = embeddings.get(i);
            String contentText = texts.get(i);

            try {
                persistVectorDocument(doc, embedding, contentText, companyId);
            } catch (Exception e) {
                logger.error("Failed to persist vector for document {} id={}: {}",
                        doc.getDocumentType(), doc.getId(), e.getMessage(), e);
                // Continue processing remaining documents
            }
        }
    }

    /**
     * Persist vector document with metadata to vector_documents table using ON
     * CONFLICT upsert.
     * Story 1.5 dependency: Store content_text in metadata for grounded context generation.
     */
    private void persistVectorDocument(ErpDocument doc, float[] embedding, String contentText, UUID companyId) {
        // Create metadata JSON (AC6)
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("document_type", doc.getDocumentType());
        metadata.put("module", doc.getModule());
        metadata.put("status", doc.getStatus());
        metadata.put("content_text", contentText); // Store rendered text for Story 1.5 query processing
        if (doc.getFiscalPeriod() != null) {
            metadata.put("fiscal_period", doc.getFiscalPeriod());
        }

        // Convert embedding to string format for PostgreSQL vector type
        String embeddingStr = floatArrayToVectorString(embedding);

        // Create VectorDocument entity
        VectorDocument vectorDoc = new VectorDocument(
                companyId,
                doc.getId(),
                doc.getSourceTable(),
                doc.getId(),
                embeddingStr,
                metadata);

        vectorDoc.setFiscalPeriod(doc.getFiscalPeriod());

        // Save (JPA will handle ON CONFLICT via merge)
        vectorRepository.save(vectorDoc);

        logger.debug("Persisted vector for document: {} id={}", doc.getDocumentType(), doc.getId());
    }

    /**
     * Convert float array to PostgreSQL vector string format: "[0.1,0.2,...]".
     */
    private String floatArrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Calculate batch hash for duplicate detection.
     */
    private String calculateBatchHash(UUID companyId, EmbeddingBatch.BatchType batchType,
            List<String> sourceTables, OffsetDateTime updatedAfter) {
        try {
            String input = companyId.toString() + batchType.toString() +
                    (sourceTables != null ? String.join(",", sourceTables) : "") +
                    (updatedAfter != null ? updatedAfter.toString() : "");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            logger.error("Error calculating batch hash: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Calculate API cost estimate (Azure OpenAI text-embedding-3-large pricing).
     * Assumes ~500 tokens per document average, $0.13 per 1M tokens.
     */
    private double calculateCost(int documentCount) {
        int avgTokensPerDoc = 500;
        double costPerMillionTokens = 0.13;
        return (documentCount * avgTokensPerDoc / 1_000_000.0) * costPerMillionTokens;
    }
}

package com.erp.rag.ragplatform.worker.service.embedding;

import java.util.List;

/**
 * Service interface for generating vector embeddings from text.
 * <p>
 * Story 1.4 – AC3: Embedding generation using Azure OpenAI
 * text-embedding-3-large
 * with batched API calls (≤100 docs per batch) to control costs and respect
 * rate limits.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * Generate embeddings for a batch of text documents.
     * <p>
     * Batch size must be ≤100 documents per call.
     * Model: Azure OpenAI text-embedding-3-large (1536 dimensions).
     * Rate limits: Azure OpenAI 3000 RPM → batch size 100 allows ~5 RPS with retry
     * safety.
     * </p>
     *
     * @param texts list of text documents (max 100 items)
     * @return list of embeddings (each embedding is a float array of 1536
     *         dimensions)
     * @throws EmbeddingGenerationException if API call fails after retries
     * @throws IllegalArgumentException     if batch size exceeds 100
     */
    List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingGenerationException;

    /**
     * Generate embedding for a single text document.
     *
     * @param text single text document
     * @return embedding as float array (1536 dimensions)
     * @throws EmbeddingGenerationException if API call fails after retries
     */
    float[] generateEmbedding(String text) throws EmbeddingGenerationException;

    /**
     * Returns the embedding dimension size for this model.
     *
     * @return embedding dimension (1536 for text-embedding-3-large)
     */
    int getEmbeddingDimension();

    /**
     * Exception thrown when embedding generation fails.
     */
    class EmbeddingGenerationException extends Exception {
        public EmbeddingGenerationException(String message) {
            super(message);
        }

        public EmbeddingGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

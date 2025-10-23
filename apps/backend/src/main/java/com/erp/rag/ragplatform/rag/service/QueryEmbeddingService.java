package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.worker.service.embedding.AzureOpenAiEmbeddingService;
import com.erp.rag.ragplatform.worker.service.embedding.EmbeddingService.EmbeddingGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Service for generating query embeddings.
 * <p>
 * Story 1.5 – AC2: Reuses AzureOpenAiEmbeddingService from Story 1.4 for
 * semantic consistency (same model: text-embedding-3-large, 1536 dimensions).
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class QueryEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(QueryEmbeddingService.class);

    private final AzureOpenAiEmbeddingService embeddingService;

    public QueryEmbeddingService(AzureOpenAiEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Generate embedding for query text.
     *
     * @param queryText the query text
     * @return embedding vector as float array (1536 dimensions)
     * @throws EmbeddingGenerationException if embedding generation fails
     */
    public float[] generateQueryEmbedding(String queryText) throws EmbeddingGenerationException {
        logger.debug("Generating query embedding for text: {}", queryText.substring(0, Math.min(50, queryText.length())));
        
        float[] embedding = embeddingService.generateEmbedding(queryText);
        
        if (embedding == null || embedding.length != 1536) {
            throw new EmbeddingGenerationException("Invalid embedding dimension: expected 1536, got " + 
                    (embedding == null ? "null" : embedding.length));
        }
        
        logger.debug("Query embedding generated successfully");
        return embedding;
    }

    /**
     * Convert float array to PostgreSQL vector string format.
     *
     * @param embedding float array
     * @return vector string format "[0.1,0.2,...]"
     */
    public String formatEmbeddingForPostgres(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}

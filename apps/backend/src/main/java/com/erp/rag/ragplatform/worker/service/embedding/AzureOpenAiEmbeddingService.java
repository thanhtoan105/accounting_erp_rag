package com.erp.rag.ragplatform.worker.service.embedding;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Azure OpenAI implementation of embedding service using text-embedding-3-large
 * model.
 * <p>
 * Story 1.4 – AC3: Embedding generation with batched API calls (≤100 docs),
 * retry logic (3x exponential backoff), and cost tracking.
 * </p>
 * <p>
 * NOTE: This is a STUB implementation for development/testing.
 * Production implementation should integrate with actual Azure OpenAI API.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class AzureOpenAiEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(AzureOpenAiEmbeddingService.class);
    private static final int EMBEDDING_DIMENSION = 1536;
    private static final int MAX_BATCH_SIZE = 100;

    private final Counter embeddingsGeneratedCounter;
    private final Counter embeddingErrorsCounter;
    private final Timer embeddingLatencyTimer;

    public AzureOpenAiEmbeddingService(MeterRegistry meterRegistry) {
        this.embeddingsGeneratedCounter = Counter.builder("embeddings_generated_total")
                .description("Total number of embeddings generated")
                .register(meterRegistry);

        this.embeddingErrorsCounter = Counter.builder("embedding_errors_total")
                .description("Total number of embedding generation errors")
                .register(meterRegistry);

        this.embeddingLatencyTimer = Timer.builder("embedding_latency_seconds")
                .description("Embedding generation latency")
                .register(meterRegistry);
    }

    @Override
    @Retryable(retryFor = {
            Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000))
    public List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingGenerationException {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        if (texts.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Batch size " + texts.size() + " exceeds maximum of " + MAX_BATCH_SIZE);
        }

        try {
            return embeddingLatencyTimer.record(() -> {
                logger.debug("Generating embeddings for batch of {} documents", texts.size());

                List<float[]> embeddings = new ArrayList<>(texts.size());

                // STUB: Generate random embeddings for MVP
                // TODO: Replace with actual Azure OpenAI API call
                for (String text : texts) {
                    float[] embedding = generateStubEmbedding(text);
                    embeddings.add(embedding);
                }

                embeddingsGeneratedCounter.increment(texts.size());
                logger.info("Successfully generated {} embeddings", embeddings.size());

                return embeddings;
            });
        } catch (Exception e) {
            embeddingErrorsCounter.increment();
            logger.error("Failed to generate embeddings for batch of {} documents: {}",
                    texts.size(), e.getMessage(), e);
            throw new EmbeddingGenerationException(
                    "Failed to generate embeddings for batch", e);
        }
    }

    @Override
    public float[] generateEmbedding(String text) throws EmbeddingGenerationException {
        List<float[]> embeddings = generateEmbeddings(List.of(text));
        return embeddings.isEmpty() ? null : embeddings.get(0);
    }

    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }

    /**
     * STUB: Generates a deterministic random embedding based on text hash.
     * This is for development/testing only.
     * <p>
     * Production implementation should call Azure OpenAI API:
     * POST
     * https://{resource}.openai.azure.com/openai/deployments/{deployment}/embeddings?api-version=2024-04-01-preview
     * Headers:
     * - api-key: {AZURE_OPENAI_API_KEY}
     * - Content-Type: application/json
     * Body:
     * {
     * "input": ["text1", "text2", ...],
     * "model": "text-embedding-3-large"
     * }
     * </p>
     *
     * @param text input text
     * @return stub embedding (1536 dimensions)
     */
    private float[] generateStubEmbedding(String text) {
        // Use text hash as seed for deterministic embeddings
        long seed = text.hashCode();
        Random rng = new Random(seed);

        float[] embedding = new float[EMBEDDING_DIMENSION];
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = (rng.nextFloat() - 0.5f) * 2.0f; // Range: [-1, 1]
        }

        // Normalize to unit vector
        float norm = 0.0f;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }
}

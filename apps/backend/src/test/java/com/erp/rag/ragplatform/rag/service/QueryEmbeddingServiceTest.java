package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.worker.service.embedding.AzureOpenAiEmbeddingService;
import com.erp.rag.ragplatform.worker.service.embedding.EmbeddingService.EmbeddingGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryEmbeddingService.
 * <p>
 * Story 1.5-UNIT-002 – Query embedding generation with dimension validation.
 * Priority: P0 (Critical - core functionality)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Story 1.5-UNIT-002: Query Embedding Service")
class QueryEmbeddingServiceTest {

    @Mock
    private AzureOpenAiEmbeddingService azureOpenAiEmbeddingService;

    @InjectMocks
    private QueryEmbeddingService queryEmbeddingService;

    private float[] validEmbedding;

    @BeforeEach
    void setUp() {
        // Create valid 1536-dimension embedding
        validEmbedding = new float[1536];
        for (int i = 0; i < 1536; i++) {
            validEmbedding[i] = (float) (Math.random() * 2 - 1); // Range -1 to 1
        }
    }

    @Test
    @DisplayName("Should generate query embedding with correct dimensions")
    void testGenerateQueryEmbedding_ValidDimensions() throws EmbeddingGenerationException {
        // Given
        String queryText = "What is the current AR balance?";
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenReturn(validEmbedding);

        // When
        float[] result = queryEmbeddingService.generateQueryEmbedding(queryText);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1536);
        verify(azureOpenAiEmbeddingService).generateEmbedding(queryText);
    }

    @Test
    @DisplayName("Should format embedding for Postgres vector type")
    void testFormatEmbeddingForPostgres() {
        // Given
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

        // When
        String result = queryEmbeddingService.formatEmbeddingForPostgres(embedding);

        // Then
        assertThat(result).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    @DisplayName("Should format embedding with proper decimal precision")
    void testFormatEmbeddingForPostgres_DecimalPrecision() {
        // Given
        float[] embedding = new float[]{0.123456789f, -0.987654321f, 0.0f};

        // When
        String result = queryEmbeddingService.formatEmbeddingForPostgres(embedding);

        // Then
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result).contains(",");
        assertThat(result.split(",")).hasSize(3);
    }

    @Test
    @DisplayName("Should throw exception when embedding generation fails")
    void testGenerateQueryEmbedding_EmbeddingFailure() throws EmbeddingGenerationException {
        // Given
        String queryText = "Test query";
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenThrow(new EmbeddingGenerationException("Azure OpenAI API error"));

        // When/Then
        assertThatThrownBy(() -> queryEmbeddingService.generateQueryEmbedding(queryText))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Azure OpenAI API error");
    }

    @Test
    @DisplayName("Should throw exception when embedding dimension is invalid")
    void testGenerateQueryEmbedding_InvalidDimension() throws EmbeddingGenerationException {
        // Given
        String queryText = "Test query";
        float[] invalidEmbedding = new float[512]; // Wrong dimension
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenReturn(invalidEmbedding);

        // When/Then
        assertThatThrownBy(() -> queryEmbeddingService.generateQueryEmbedding(queryText))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Invalid embedding dimension");
    }

    @Test
    @DisplayName("Should handle empty query text gracefully")
    void testGenerateQueryEmbedding_EmptyQuery() throws EmbeddingGenerationException {
        // Given
        String queryText = "";
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenReturn(validEmbedding);

        // When
        float[] result = queryEmbeddingService.generateQueryEmbedding(queryText);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1536);
        verify(azureOpenAiEmbeddingService).generateEmbedding("");
    }

    @Test
    @DisplayName("Should handle Vietnamese query text")
    void testGenerateQueryEmbedding_VietnameseText() throws EmbeddingGenerationException {
        // Given
        String queryText = "Khách hàng nào còn nợ?";
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenReturn(validEmbedding);

        // When
        float[] result = queryEmbeddingService.generateQueryEmbedding(queryText);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1536);
        verify(azureOpenAiEmbeddingService).generateEmbedding(queryText);
    }

    @Test
    @DisplayName("Should throw exception when no embeddings returned")
    void testGenerateQueryEmbedding_NoEmbeddingsReturned() throws EmbeddingGenerationException {
        // Given
        String queryText = "Test query";
        when(azureOpenAiEmbeddingService.generateEmbedding(anyString()))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> queryEmbeddingService.generateQueryEmbedding(queryText))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Invalid embedding dimension");
    }

    @Test
    @DisplayName("Should format large embedding array correctly")
    void testFormatEmbeddingForPostgres_FullSize() {
        // Given - full 1536-dimension embedding
        float[] embedding = validEmbedding;

        // When
        String result = queryEmbeddingService.formatEmbeddingForPostgres(embedding);

        // Then
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        String[] values = result.substring(1, result.length() - 1).split(",");
        assertThat(values).hasSize(1536);
        
        // Verify all values are valid floats
        for (String value : values) {
            assertThatCode(() -> Float.parseFloat(value)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should handle null embedding array")
    void testFormatEmbeddingForPostgres_NullArray() {
        // When/Then
        assertThatThrownBy(() -> queryEmbeddingService.formatEmbeddingForPostgres(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle embedding with edge case values")
    void testFormatEmbeddingForPostgres_EdgeCaseValues() {
        // Given
        float[] embedding = new float[]{
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                0.0f,
                -0.0f,
                1.0f,
                -1.0f
        };

        // When
        String result = queryEmbeddingService.formatEmbeddingForPostgres(embedding);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result.split(",")).hasSize(6);
    }
}

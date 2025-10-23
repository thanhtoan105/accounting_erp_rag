package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.QueryRequest;
import com.erp.rag.ragplatform.rag.dto.QueryResponse;
import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.ragplatform.worker.service.embedding.EmbeddingService.EmbeddingGenerationException;
import com.erp.rag.supabase.entity.RagQuery;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RagQueryService.
 * <p>
 * Story 1.5-UNIT-006 – End-to-end RAG query orchestration.
 * Priority: P0 (Critical - main orchestrator)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Story 1.5-UNIT-006: RAG Query Service Orchestration")
class RagQueryServiceTest {

    @Mock
    private QueryEmbeddingService queryEmbeddingService;

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private ContextWindowManager contextWindowManager;

    @Mock
    private QueryLoggerService queryLoggerService;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private RagQueryService ragQueryService;

    private UUID companyId;
    private UUID userId;
    private QueryRequest validRequest;
    private float[] mockEmbedding;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        validRequest = new QueryRequest();
        validRequest.setCompanyId(companyId);
        validRequest.setQuery("What is the current AR balance?");
        validRequest.setLanguage("en");
        
        mockEmbedding = new float[1536];
        for (int i = 0; i < 1536; i++) {
            mockEmbedding[i] = (float) (Math.random() * 2 - 1);
        }
    }

    @Test
    @DisplayName("Should orchestrate complete query pipeline successfully")
    void testProcessQuery_SuccessfulOrchestration() throws EmbeddingGenerationException {
        // Given
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        List<RetrievedDocumentDTO> documents = createMockDocuments(5);
        String groundedContext = "Sample context from 5 documents";
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenReturn(documents);
        when(contextWindowManager.buildGroundedContext(any(), any(), any()))
                .thenReturn(groundedContext);

        // When
        QueryResponse response = ragQueryService.processQuery(validRequest, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getQueryId()).isEqualTo(queryId);
        assertThat(response.getRetrievedDocuments()).hasSize(5);
        assertThat(response.getGroundedContext()).isEqualTo(groundedContext);
        assertThat(response.getLatencyMs()).isNotNull();
        assertThat(response.getLatencyMs().getTotal()).isGreaterThan(0);
        
        // Verify orchestration order
        verify(queryEmbeddingService).generateQueryEmbedding(validRequest.getQuery());
        verify(queryLoggerService).logQueryStart(companyId, userId, validRequest.getQuery(), 
                embeddingStr, "en");
        verify(vectorSearchService).search(eq(companyId), eq(embeddingStr), any());
        verify(contextWindowManager).buildGroundedContext(any(), any(), eq(companyId));
        verify(queryLoggerService).logQueryComplete(eq(queryId), anyInt(), anyInt(), eq(documents), anyList());
    }

    @Test
    @DisplayName("Should handle embedding generation failure")
    void testProcessQuery_EmbeddingFailure() throws EmbeddingGenerationException {
        // Given
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenThrow(new EmbeddingGenerationException("Azure OpenAI API error"));

        // When/Then
        assertThatThrownBy(() -> ragQueryService.processQuery(validRequest, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process query");

        verify(queryEmbeddingService).generateQueryEmbedding(anyString());
        verifyNoInteractions(vectorSearchService);
        verifyNoInteractions(contextWindowManager);
    }

    @Test
    @DisplayName("Should handle vector search failure")
    void testProcessQuery_SearchFailure() throws EmbeddingGenerationException {
        // Given
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenThrow(new RuntimeException("Database timeout"));

        // When/Then
        assertThatThrownBy(() -> ragQueryService.processQuery(validRequest, userId))
                .isInstanceOf(RuntimeException.class);

        verify(queryLoggerService).logQueryError(eq(queryId), contains("Database timeout"));
    }

    @Test
    @DisplayName("Should increment Prometheus metrics on success")
    void testProcessQuery_MetricsIncremented() throws EmbeddingGenerationException {
        // Given
        setupSuccessfulMocks();
        
        Counter queryCounter = meterRegistry.counter("rag_query_total");
        double initialCount = queryCounter.count();

        // When
        ragQueryService.processQuery(validRequest, userId);

        // Then
        assertThat(queryCounter.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should increment error counter on failure")
    void testProcessQuery_ErrorMetricsIncremented() throws EmbeddingGenerationException {
        // Given
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenThrow(new EmbeddingGenerationException("Error"));
        
        Counter errorCounter = meterRegistry.counter("rag_query_errors_total");
        double initialCount = errorCounter.count();

        // When/Then
        assertThatThrownBy(() -> ragQueryService.processQuery(validRequest, userId))
                .isInstanceOf(RuntimeException.class);

        assertThat(errorCounter.count()).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("Should calculate latency metrics correctly")
    void testProcessQuery_LatencyMetrics() throws EmbeddingGenerationException {
        // Given
        setupSuccessfulMocks();

        // When
        QueryResponse response = ragQueryService.processQuery(validRequest, userId);

        // Then
        assertThat(response.getLatencyMs()).isNotNull();
        // Note: With mocks, operations may complete in <1ms, so latency can be 0
        assertThat(response.getLatencyMs().getEmbedding()).isGreaterThanOrEqualTo(0);
        assertThat(response.getLatencyMs().getSearch()).isGreaterThanOrEqualTo(0);
        assertThat(response.getLatencyMs().getContextPrep()).isGreaterThanOrEqualTo(0);
        assertThat(response.getLatencyMs().getTotal()).isGreaterThanOrEqualTo(0);
        
        // Total should be sum of components (approximately, due to timing)
        int sum = response.getLatencyMs().getEmbedding() 
                + response.getLatencyMs().getSearch() 
                + response.getLatencyMs().getContextPrep();
        assertThat(response.getLatencyMs().getTotal()).isGreaterThanOrEqualTo(sum);
    }

    @Test
    @DisplayName("Should handle null userId gracefully")
    void testProcessQuery_NullUserId() throws EmbeddingGenerationException {
        // Given
        setupSuccessfulMocks();

        // When
        QueryResponse response = ragQueryService.processQuery(validRequest, null);

        // Then
        assertThat(response).isNotNull();
        verify(queryLoggerService).logQueryStart(eq(companyId), isNull(), anyString(), 
                anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle Vietnamese query")
    void testProcessQuery_VietnameseQuery() throws EmbeddingGenerationException {
        // Given
        QueryRequest vnRequest = new QueryRequest();
        vnRequest.setCompanyId(companyId);
        vnRequest.setQuery("Khách hàng nào còn nợ?");
        vnRequest.setLanguage("vi");
        
        setupSuccessfulMocks();

        // When
        QueryResponse response = ragQueryService.processQuery(vnRequest, userId);

        // Then
        assertThat(response).isNotNull();
        verify(queryEmbeddingService).generateQueryEmbedding("Khách hàng nào còn nợ?");
        verify(queryLoggerService).logQueryStart(any(), any(), eq("Khách hàng nào còn nợ?"), 
                anyString(), eq("vi"));
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testProcessQuery_EmptyResults() throws EmbeddingGenerationException {
        // Given
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenReturn(List.of());
        when(contextWindowManager.buildGroundedContext(any(), any(), any()))
                .thenReturn("");

        // When
        QueryResponse response = ragQueryService.processQuery(validRequest, userId);

        // Then
        assertThat(response.getRetrievedDocuments()).isEmpty();
        assertThat(response.getGroundedContext()).isEmpty();
        verify(queryLoggerService).logQueryComplete(eq(queryId), anyInt(), anyInt(), eq(List.of()), anyList());
    }

    @Test
    @DisplayName("Should extract document IDs for context building")
    void testProcessQuery_DocumentIdsExtracted() throws EmbeddingGenerationException {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        
        List<RetrievedDocumentDTO> documents = List.of(
                createDTO(doc1Id, 0.95),
                createDTO(doc2Id, 0.90)
        );
        
        setupSuccessfulMocksWithDocuments(documents);

        // When
        QueryResponse response = ragQueryService.processQuery(validRequest, userId);

        // Then
        verify(contextWindowManager).buildGroundedContext(
                eq(documents), 
                argThat(ids -> ids.contains(doc1Id) && ids.contains(doc2Id)),
                eq(companyId));
    }

    @Test
    @DisplayName("Should record query latency histogram")
    void testProcessQuery_LatencyHistogram() throws EmbeddingGenerationException {
        // Given
        setupSuccessfulMocks();
        
        Timer latencyTimer = meterRegistry.timer("rag_query_latency_seconds");
        long initialCount = latencyTimer.count();

        // When
        ragQueryService.processQuery(validRequest, userId);

        // Then
        assertThat(latencyTimer.count()).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("Should handle context building failure gracefully")
    void testProcessQuery_ContextBuildingFailure() throws EmbeddingGenerationException {
        // Given
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenReturn(createMockDocuments(5));
        when(contextWindowManager.buildGroundedContext(any(), any(), any()))
                .thenThrow(new RuntimeException("Context building error"));

        // When/Then
        assertThatThrownBy(() -> ragQueryService.processQuery(validRequest, userId))
                .isInstanceOf(RuntimeException.class);

        verify(queryLoggerService).logQueryError(eq(queryId), contains("Context building error"));
    }

    // Helper methods

    private void setupSuccessfulMocks() throws EmbeddingGenerationException {
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenReturn(createMockDocuments(5));
        when(contextWindowManager.buildGroundedContext(any(), any(), any()))
                .thenReturn("Sample grounded context");
    }

    private void setupSuccessfulMocksWithDocuments(List<RetrievedDocumentDTO> documents) 
            throws EmbeddingGenerationException {
        String embeddingStr = "[0.1,0.2,0.3]";
        UUID queryId = UUID.randomUUID();
        RagQuery ragQuery = new RagQuery();
        ragQuery.setId(queryId);
        
        when(queryEmbeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(mockEmbedding);
        when(queryEmbeddingService.formatEmbeddingForPostgres(any()))
                .thenReturn(embeddingStr);
        when(queryLoggerService.logQueryStart(any(), any(), anyString(), anyString(), anyString()))
                .thenReturn(ragQuery);
        when(vectorSearchService.search(any(), anyString(), any()))
                .thenReturn(documents);
        when(contextWindowManager.buildGroundedContext(any(), any(), any()))
                .thenReturn("Sample grounded context");
    }

    private List<RetrievedDocumentDTO> createMockDocuments(int count) {
        List<RetrievedDocumentDTO> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            documents.add(createDTO(UUID.randomUUID(), 0.9 - (i * 0.05)));
        }
        return documents;
    }

    private RetrievedDocumentDTO createDTO(UUID id, double relevanceScore) {
        RetrievedDocumentDTO dto = new RetrievedDocumentDTO();
        dto.setId(id);
        dto.setDocumentType("invoice");
        dto.setModule("ar");
        dto.setRelevanceScore(relevanceScore);
        dto.setExcerpt("Sample excerpt");
        return dto;
    }
}

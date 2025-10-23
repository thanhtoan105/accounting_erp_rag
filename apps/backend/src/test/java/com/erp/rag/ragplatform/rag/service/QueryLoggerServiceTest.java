package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.supabase.entity.RagQuery;
import com.erp.rag.supabase.entity.RagQueryDocument;
import com.erp.rag.supabase.repository.RagQueryDocumentRepository;
import com.erp.rag.supabase.repository.RagQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryLoggerService.
 * <p>
 * Story 1.5-UNIT-005 – Query logging and audit trail persistence.
 * Priority: P0 (Critical - Circular 200 compliance)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Story 1.5-UNIT-005: Query Logger Service")
class QueryLoggerServiceTest {

    @Mock
    private RagQueryRepository ragQueryRepository;

    @Mock
    private RagQueryDocumentRepository ragQueryDocumentRepository;

    @InjectMocks
    private QueryLoggerService queryLoggerService;

    private UUID companyId;
    private UUID userId;
    private String queryText;
    private String queryEmbedding;
    private String language;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        queryText = "What is the current AR balance?";
        queryEmbedding = "[0.1,0.2,0.3]";
        language = "en";
    }

    @Test
    @DisplayName("Should log query start with status=pending")
    void testLogQueryStart() {
        // Given
        RagQuery savedQuery = new RagQuery();
        savedQuery.setId(UUID.randomUUID());
        savedQuery.setStatus("pending");
        
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(savedQuery);

        // When
        RagQuery result = queryLoggerService.logQueryStart(
                companyId, userId, queryText, queryEmbedding, language);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("pending");
        
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        RagQuery captured = captor.getValue();
        assertThat(captured.getCompanyId()).isEqualTo(companyId);
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getQueryText()).isEqualTo(queryText);
        assertThat(captured.getQueryEmbedding()).isEqualTo(queryEmbedding);
        assertThat(captured.getLanguage()).isEqualTo(language);
        assertThat(captured.getStatus()).isEqualTo("pending");
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should log query start with Vietnamese language")
    void testLogQueryStart_Vietnamese() {
        // Given
        String vnQuery = "Khách hàng nào còn nợ?";
        RagQuery savedQuery = new RagQuery();
        savedQuery.setId(UUID.randomUUID());
        
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(savedQuery);

        // When
        queryLoggerService.logQueryStart(companyId, userId, vnQuery, queryEmbedding, "vi");

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        assertThat(captor.getValue().getQueryText()).isEqualTo(vnQuery);
        assertThat(captor.getValue().getLanguage()).isEqualTo("vi");
    }

    @Test
    @DisplayName("Should log query completion with latency metrics")
    void testLogQueryComplete() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        existingQuery.setStatus("pending");
        
        List<RetrievedDocumentDTO> documents = List.of(
                createDTO(UUID.randomUUID(), 0.95, "Excerpt 1"),
                createDTO(UUID.randomUUID(), 0.90, "Excerpt 2")
        );
        
        List<Integer> tokens = List.of(100, 150);
        int retrievalLatency = 850;
        int totalLatency = 1200;
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, retrievalLatency, totalLatency, documents, tokens);

        // Then
        ArgumentCaptor<RagQuery> queryCaptor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(queryCaptor.capture());
        
        RagQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.getStatus()).isEqualTo("complete");
        assertThat(capturedQuery.getRetrievalLatencyMs()).isEqualTo(retrievalLatency);
        assertThat(capturedQuery.getTotalLatencyMs()).isEqualTo(totalLatency);
        assertThat(capturedQuery.getCompletedAt()).isNotNull();
        
        // Verify RagQueryDocument records saved
        ArgumentCaptor<RagQueryDocument> docCaptor = ArgumentCaptor.forClass(RagQueryDocument.class);
        verify(ragQueryDocumentRepository, times(2)).save(docCaptor.capture());
        
        List<RagQueryDocument> capturedDocs = docCaptor.getAllValues();
        assertThat(capturedDocs).hasSize(2);
        assertThat(capturedDocs.get(0).getRank()).isEqualTo(1);
        assertThat(capturedDocs.get(0).getRelevanceScore()).isEqualTo(0.95);
        assertThat(capturedDocs.get(0).getTokensUsed()).isEqualTo(100);
        assertThat(capturedDocs.get(1).getRank()).isEqualTo(2);
        assertThat(capturedDocs.get(1).getRelevanceScore()).isEqualTo(0.90);
        assertThat(capturedDocs.get(1).getTokensUsed()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should log query completion with documents list")
    void testLogQueryComplete_DocumentsWithTokens() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        existingQuery.setStatus("pending");
        
        List<RetrievedDocumentDTO> documents = List.of(
                createDTO(UUID.randomUUID(), 0.95, "Excerpt 1"));
        List<Integer> tokens = List.of(100);
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, 800, 1100, documents, tokens);

        // Then
        verify(ragQueryRepository).save(any(RagQuery.class));
        verify(ragQueryDocumentRepository, times(1)).save(any(RagQueryDocument.class));
    }

    @Test
    @DisplayName("Should log query error with error message")
    void testLogQueryError() {
        // Given
        UUID queryId = UUID.randomUUID();
        String errorMessage = "Azure OpenAI API rate limit exceeded";
        
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        existingQuery.setStatus("pending");
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryError(queryId, errorMessage);

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        RagQuery captured = captor.getValue();
        assertThat(captured.getStatus()).isEqualTo("error");
        assertThat(captured.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(captured.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should persist audit trail with immutable timestamps")
    void testLogQueryStart_ImmutableTimestamps() {
        // Given
        RagQuery savedQuery = new RagQuery();
        savedQuery.setId(UUID.randomUUID());
        
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(savedQuery);

        // When
        queryLoggerService.logQueryStart(companyId, userId, queryText, queryEmbedding, language);

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        RagQuery captured = captor.getValue();
        assertThat(captured.getCreatedAt()).isNotNull();
        assertThat(captured.getCompletedAt()).isNull(); // Should be null on start
    }

    @Test
    @DisplayName("Should handle empty document list in completion")
    void testLogQueryComplete_EmptyDocuments() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, 500, 600, List.of(), List.of());

        // Then
        verify(ragQueryRepository).save(any(RagQuery.class));
        verify(ragQueryDocumentRepository, never()).save(any(RagQueryDocument.class));
    }

    @Test
    @DisplayName("Should preserve rank order in RagQueryDocument")
    void testLogQueryComplete_PreserveRankOrder() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        
        List<RetrievedDocumentDTO> documents = List.of(
                createDTO(UUID.randomUUID(), 0.95, "Top result"),
                createDTO(UUID.randomUUID(), 0.85, "Second result"),
                createDTO(UUID.randomUUID(), 0.75, "Third result")
        );
        List<Integer> tokens = List.of(100, 150, 200);
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, 800, 1100, documents, tokens);

        // Then
        ArgumentCaptor<RagQueryDocument> captor = ArgumentCaptor.forClass(RagQueryDocument.class);
        verify(ragQueryDocumentRepository, times(3)).save(captor.capture());
        
        List<RagQueryDocument> captured = captor.getAllValues();
        assertThat(captured.get(0).getRank()).isEqualTo(1);
        assertThat(captured.get(1).getRank()).isEqualTo(2);
        assertThat(captured.get(2).getRank()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle null userId (anonymous queries)")
    void testLogQueryStart_NullUserId() {
        // Given
        RagQuery savedQuery = new RagQuery();
        savedQuery.setId(UUID.randomUUID());
        
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(savedQuery);

        // When
        queryLoggerService.logQueryStart(companyId, null, queryText, queryEmbedding, language);

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    @DisplayName("Should capture document excerpts in audit trail")
    void testLogQueryComplete_CaptureExcerpts() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        
        String excerpt1 = "Invoice INV-001 from Customer ABC. Amount: 5000 USD.";
        String excerpt2 = "Invoice INV-002 from Customer XYZ. Amount: 3000 USD.";
        
        List<RetrievedDocumentDTO> documents = List.of(
                createDTO(UUID.randomUUID(), 0.95, excerpt1),
                createDTO(UUID.randomUUID(), 0.90, excerpt2)
        );
        List<Integer> tokens = List.of(100, 150);
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, 800, 1100, documents, tokens);

        // Then
        ArgumentCaptor<RagQueryDocument> captor = ArgumentCaptor.forClass(RagQueryDocument.class);
        verify(ragQueryDocumentRepository, times(2)).save(captor.capture());
        
        List<RagQueryDocument> captured = captor.getAllValues();
        assertThat(captured.get(0).getExcerpt()).isEqualTo(excerpt1);
        assertThat(captured.get(1).getExcerpt()).isEqualTo(excerpt2);
    }

    @Test
    @DisplayName("Should handle long error messages")
    void testLogQueryError_LongMessage() {
        // Given
        UUID queryId = UUID.randomUUID();
        String longError = "Error: " + "A".repeat(1000);
        
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryError(queryId, longError);

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        assertThat(captor.getValue().getErrorMessage()).isEqualTo(longError);
    }

    @Test
    @DisplayName("Should handle completion latency metrics correctly")
    void testLogQueryComplete_LatencyMetrics() {
        // Given
        UUID queryId = UUID.randomUUID();
        RagQuery existingQuery = new RagQuery();
        existingQuery.setId(queryId);
        
        int retrievalLatency = 1234;
        int totalLatency = 5678;
        
        when(ragQueryRepository.findById(queryId)).thenReturn(java.util.Optional.of(existingQuery));
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(existingQuery);

        // When
        queryLoggerService.logQueryComplete(queryId, retrievalLatency, totalLatency, List.of(), List.of());

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        RagQuery captured = captor.getValue();
        assertThat(captured.getRetrievalLatencyMs()).isEqualTo(1234);
        assertThat(captured.getTotalLatencyMs()).isEqualTo(5678);
    }

    @Test
    @DisplayName("Should store query embedding for reuse")
    void testLogQueryStart_StoreEmbedding() {
        // Given
        String embeddingVector = "[" + "0.1,".repeat(1535) + "0.1]";
        RagQuery savedQuery = new RagQuery();
        savedQuery.setId(UUID.randomUUID());
        
        when(ragQueryRepository.save(any(RagQuery.class))).thenReturn(savedQuery);

        // When
        queryLoggerService.logQueryStart(companyId, userId, queryText, embeddingVector, language);

        // Then
        ArgumentCaptor<RagQuery> captor = ArgumentCaptor.forClass(RagQuery.class);
        verify(ragQueryRepository).save(captor.capture());
        
        assertThat(captor.getValue().getQueryEmbedding()).isEqualTo(embeddingVector);
    }

    // Helper methods

    private RetrievedDocumentDTO createDTO(UUID id, double relevanceScore, String excerpt) {
        RetrievedDocumentDTO dto = new RetrievedDocumentDTO();
        dto.setId(id);
        dto.setRelevanceScore(relevanceScore);
        dto.setExcerpt(excerpt);
        dto.setDocumentType("invoice");
        dto.setModule("ar");
        return dto;
    }
}

package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.supabase.vector.VectorDocument;
import com.erp.rag.supabase.vector.VectorDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.assertj.core.data.Percentage.withPercentage;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VectorSearchService.
 * <p>
 * Story 1.5-UNIT-003 – Vector similarity search and relevance scoring.
 * Priority: P0 (Critical - core functionality)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Story 1.5-UNIT-003: Vector Search Service")
class VectorSearchServiceTest {

    @Mock
    private VectorDocumentRepository vectorDocumentRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VectorSearchService vectorSearchService;

    private UUID companyId;
    private String queryEmbedding;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        queryEmbedding = "[0.1,0.2,0.3]"; // Simplified embedding
    }

    @Test
    @DisplayName("Should execute top-10 similarity search")
    void testSearch_Top10Results() {
        // Given
        List<VectorDocument> mockDocuments = createMockDocuments(10);
        when(vectorDocumentRepository.findSimilarVectors(eq(companyId), eq(queryEmbedding), eq(10)))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(10);
        verify(vectorDocumentRepository).findSimilarVectors(companyId, queryEmbedding, 10);
    }

    @Test
    @DisplayName("Should calculate relevance scores in descending order")
    void testSearch_RelevanceScores() {
        // Given
        List<VectorDocument> mockDocuments = createMockDocuments(5);
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(5);
        // Verify scores are in descending order (placeholder algorithm: 0.9 - rank*0.05)
        assertThat(results.get(0).getRelevanceScore()).isCloseTo(0.9, offset(0.01));
        assertThat(results.get(1).getRelevanceScore()).isCloseTo(0.85, offset(0.01));
        assertThat(results.get(2).getRelevanceScore()).isCloseTo(0.80, offset(0.01));
        assertThat(results.get(3).getRelevanceScore()).isCloseTo(0.75, offset(0.01));
        assertThat(results.get(4).getRelevanceScore()).isCloseTo(0.70, offset(0.01));
        // Verify descending order
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).getRelevanceScore())
                    .isGreaterThanOrEqualTo(results.get(i + 1).getRelevanceScore());
        }
    }

    @Test
    @DisplayName("Should extract document type from metadata")
    void testSearch_ExtractDocumentType() {
        // Given
        List<VectorDocument> mockDocuments = List.of(
                createVectorDocument(UUID.randomUUID(), "invoice", "ar", "Invoice content"),
                createVectorDocument(UUID.randomUUID(), "bill", "ap", "Bill content")
        );
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDocumentType()).isEqualTo("invoice");
        assertThat(results.get(1).getDocumentType()).isEqualTo("bill");
    }

    @Test
    @DisplayName("Should extract module from metadata")
    void testSearch_ExtractModule() {
        // Given
        List<VectorDocument> mockDocuments = List.of(
                createVectorDocument(UUID.randomUUID(), "invoice", "ar", "Content"),
                createVectorDocument(UUID.randomUUID(), "bill", "ap", "Content"),
                createVectorDocument(UUID.randomUUID(), "journal", "gl", "Content")
        );
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results.get(0).getModule()).isEqualTo("ar");
        assertThat(results.get(1).getModule()).isEqualTo("ap");
        assertThat(results.get(2).getModule()).isEqualTo("gl");
    }

    @Test
    @DisplayName("Should extract excerpts (first 200 chars)")
    void testSearch_ExtractExcerpts() {
        // Given
        String longText = "a".repeat(300);
        VectorDocument doc = createVectorDocument(UUID.randomUUID(), "invoice", "ar", longText);
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExcerpt()).hasSize(203); // 200 chars + "..."
        assertThat(results.get(0).getExcerpt()).endsWith("...");
    }

    @Test
    @DisplayName("Should not truncate short content")
    void testSearch_ShortExcerpt() {
        // Given
        String shortText = "Short invoice content.";
        VectorDocument doc = createVectorDocument(UUID.randomUUID(), "invoice", "ar", shortText);
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results.get(0).getExcerpt()).isEqualTo(shortText);
        assertThat(results.get(0).getExcerpt()).doesNotEndWith("...");
    }

    @Test
    @DisplayName("Should parse metadata into Map")
    void testSearch_ParseMetadata() {
        // Given
        VectorDocument doc = createVectorDocument(UUID.randomUUID(), "invoice", "ar", "Content");
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results.get(0).getMetadata()).isNotNull();
        assertThat(results.get(0).getMetadata()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) results.get(0).getMetadata();
        assertThat(metadata).containsKeys("content_text", "document_type", "module");
    }

    @Test
    @DisplayName("Should handle empty results")
    void testSearch_EmptyResults() {
        // Given
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of());

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle missing content_text in metadata")
    void testSearch_MissingContentText() {
        // Given
        VectorDocument doc = new VectorDocument();
        doc.setId(UUID.randomUUID());
        JsonNode metadata = objectMapper.createObjectNode()
                .put("document_type", "invoice")
                .put("module", "ar");
        doc.setMetadata(metadata);
        
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExcerpt()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null metadata gracefully")
    void testSearch_NullMetadata() {
        // Given
        VectorDocument doc = new VectorDocument();
        doc.setId(UUID.randomUUID());
        doc.setMetadata(null);
        
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocumentType()).isEqualTo("unknown");
        assertThat(results.get(0).getModule()).isEqualTo("unknown");
        assertThat(results.get(0).getExcerpt()).isEmpty();
    }

    @Test
    @DisplayName("Should handle Vietnamese content")
    void testSearch_VietnameseContent() {
        // Given
        String vietnameseText = "Hóa đơn INV-001 từ khách hàng ABC. Số tiền: 5.000.000 VNĐ.";
        VectorDocument doc = createVectorDocument(UUID.randomUUID(), "invoice", "ar", vietnameseText);
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(List.of(doc));

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExcerpt()).contains("Hóa đơn");
        assertThat(results.get(0).getExcerpt()).contains("VNĐ");
    }

    @Test
    @DisplayName("Should handle filters parameter (basic validation)")
    void testSearch_WithFilters() {
        // Given
        Map<String, Object> filters = Map.of("module", "ar", "fiscal_period", "2024-10");
        List<VectorDocument> mockDocuments = createMockDocuments(5);
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, filters);

        // Then
        assertThat(results).hasSize(5);
        // Note: Metadata filtering not fully implemented in MVP, but should not crash
        verify(vectorDocumentRepository).findSimilarVectors(companyId, queryEmbedding, 10);
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void testSearch_RepositoryException() {
        // Given
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Database connection error"));

        // When/Then
        assertThatThrownBy(() -> vectorSearchService.search(companyId, queryEmbedding, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");
    }

    @Test
    @DisplayName("Should assign correct IDs to results")
    void testSearch_ResultIds() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        List<VectorDocument> mockDocuments = List.of(
                createVectorDocument(doc1Id, "invoice", "ar", "Content 1"),
                createVectorDocument(doc2Id, "bill", "ap", "Content 2")
        );
        when(vectorDocumentRepository.findSimilarVectors(any(), any(), anyInt()))
                .thenReturn(mockDocuments);

        // When
        List<RetrievedDocumentDTO> results = vectorSearchService.search(
                companyId, queryEmbedding, null);

        // Then
        assertThat(results.get(0).getId()).isEqualTo(doc1Id);
        assertThat(results.get(1).getId()).isEqualTo(doc2Id);
    }

    // Helper methods

    private List<VectorDocument> createMockDocuments(int count) {
        List<VectorDocument> documents = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID id = UUID.randomUUID();
            documents.add(createVectorDocument(id, "invoice", "ar", "Content " + i));
        }
        return documents;
    }

    private VectorDocument createVectorDocument(UUID id, String documentType, 
                                                String module, String contentText) {
        VectorDocument doc = new VectorDocument();
        doc.setId(id);
        doc.setCompanyId(companyId);
        
        JsonNode metadata = objectMapper.createObjectNode()
                .put("content_text", contentText)
                .put("document_type", documentType)
                .put("module", module)
                .put("fiscal_period", "2024-10")
                .put("status", "active");
        doc.setMetadata(metadata);
        
        return doc;
    }
}

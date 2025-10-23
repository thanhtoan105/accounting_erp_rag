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
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContextWindowManager.
 * <p>
 * Story 1.5-UNIT-004 – Context window management with 8K token budget.
 * Priority: P0 (Critical - affects LLM input)
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Story 1.5-UNIT-004: Context Window Manager")
class ContextWindowManagerTest {

    @Mock
    private VectorDocumentRepository vectorDocumentRepository;

    @InjectMocks
    private ContextWindowManager contextWindowManager;

    private ObjectMapper objectMapper;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        companyId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should estimate tokens using chars/4 formula")
    void testTokenEstimation() {
        // Given
        String text400Chars = "a".repeat(400);
        UUID docId = UUID.randomUUID();
        VectorDocument doc = createVectorDocument(docId, text400Chars);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(docId, companyId))
                .thenReturn(Optional.of(doc));

        // When
        List<Integer> tokens = contextWindowManager.calculateTokensPerDocument(
                List.of(docId), companyId);

        // Then
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0)).isEqualTo(100); // 400 chars / 4 = 100 tokens
    }

    @Test
    @DisplayName("Should build grounded context from single document")
    void testBuildGroundedContext_SingleDocument() {
        // Given
        UUID docId = UUID.randomUUID();
        String contentText = "Invoice INV-001 from Customer ABC. Amount: 1000 USD.";
        VectorDocument doc = createVectorDocument(docId, contentText);
        
        RetrievedDocumentDTO dto = new RetrievedDocumentDTO();
        dto.setId(docId);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(docId, companyId))
                .thenReturn(Optional.of(doc));

        // When
        String context = contextWindowManager.buildGroundedContext(
                List.of(dto), List.of(docId), companyId);

        // Then
        assertThat(context).isEqualTo(contentText);
        verify(vectorDocumentRepository).findByIdAndCompanyId(docId, companyId);
    }

    @Test
    @DisplayName("Should concatenate multiple documents with separator")
    void testBuildGroundedContext_MultipleDocuments() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        UUID doc3Id = UUID.randomUUID();
        
        VectorDocument doc1 = createVectorDocument(doc1Id, "Document 1 content");
        VectorDocument doc2 = createVectorDocument(doc2Id, "Document 2 content");
        VectorDocument doc3 = createVectorDocument(doc3Id, "Document 3 content");
        
        List<RetrievedDocumentDTO> dtos = List.of(
                createDTO(doc1Id), createDTO(doc2Id), createDTO(doc3Id));
        List<UUID> docIds = List.of(doc1Id, doc2Id, doc3Id);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.of(doc2));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc3Id, companyId))
                .thenReturn(Optional.of(doc3));

        // When
        String context = contextWindowManager.buildGroundedContext(dtos, docIds, companyId);

        // Then
        assertThat(context).contains("Document 1 content");
        assertThat(context).contains("Document 2 content");
        assertThat(context).contains("Document 3 content");
        assertThat(context).contains("\n\n---\n\n"); // Separator
        
        String[] parts = context.split("\n\n---\n\n");
        assertThat(parts).hasSize(3);
    }

    @Test
    @DisplayName("Should prune documents exceeding 8K token budget")
    void testBuildGroundedContext_TokenBudgetExceeded() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        UUID doc3Id = UUID.randomUUID();
        
        // Create documents: 7000 + 1500 + 1500 tokens (total 10000 > 8000 budget)
        String text7000Tokens = "a".repeat(28000); // 7000 tokens
        String text1500Tokens = "b".repeat(6000);  // 1500 tokens
        
        VectorDocument doc1 = createVectorDocument(doc1Id, text7000Tokens);
        VectorDocument doc2 = createVectorDocument(doc2Id, text1500Tokens);
        VectorDocument doc3 = createVectorDocument(doc3Id, text1500Tokens);
        
        List<RetrievedDocumentDTO> dtos = List.of(
                createDTO(doc1Id), createDTO(doc2Id), createDTO(doc3Id));
        List<UUID> docIds = List.of(doc1Id, doc2Id, doc3Id);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        // Lenient: doc2 and doc3 may not be fetched if budget exceeded after doc1
        lenient().when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.of(doc2));
        lenient().when(vectorDocumentRepository.findByIdAndCompanyId(doc3Id, companyId))
                .thenReturn(Optional.of(doc3));

        // When
        String context = contextWindowManager.buildGroundedContext(dtos, docIds, companyId);

        // Then - should only include first document (7000 tokens, adding doc2 would exceed 8K budget)
        assertThat(context).contains(text7000Tokens);
        assertThat(context).doesNotContain(text1500Tokens); // Doc2 and Doc3 should be pruned
        int tokenCount = context.length() / 4;
        assertThat(tokenCount).isEqualTo(7000); // Only doc1 included
    }

    @Test
    @DisplayName("Should handle empty document list")
    void testBuildGroundedContext_EmptyList() {
        // When
        String context = contextWindowManager.buildGroundedContext(
                List.of(), List.of(), companyId);

        // Then
        assertThat(context).isEmpty();
        verifyNoInteractions(vectorDocumentRepository);
    }

    @Test
    @DisplayName("Should handle missing documents gracefully")
    void testBuildGroundedContext_MissingDocuments() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        
        VectorDocument doc1 = createVectorDocument(doc1Id, "Document 1 content");
        
        List<RetrievedDocumentDTO> dtos = List.of(createDTO(doc1Id), createDTO(doc2Id));
        List<UUID> docIds = List.of(doc1Id, doc2Id);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.empty()); // Missing document

        // When
        String context = contextWindowManager.buildGroundedContext(dtos, docIds, companyId);

        // Then
        assertThat(context).isEqualTo("Document 1 content");
        assertThat(context).doesNotContain("\n\n---\n\n"); // No separator for single doc
    }

    @Test
    @DisplayName("Should handle documents with empty content")
    void testBuildGroundedContext_EmptyContent() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        
        VectorDocument doc1 = createVectorDocument(doc1Id, "Valid content");
        VectorDocument doc2 = createVectorDocument(doc2Id, ""); // Empty content
        
        List<RetrievedDocumentDTO> dtos = List.of(createDTO(doc1Id), createDTO(doc2Id));
        List<UUID> docIds = List.of(doc1Id, doc2Id);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.of(doc2));

        // When
        String context = contextWindowManager.buildGroundedContext(dtos, docIds, companyId);

        // Then
        assertThat(context).isEqualTo("Valid content");
    }

    @Test
    @DisplayName("Should calculate tokens for multiple documents")
    void testCalculateTokensPerDocument_MultipleDocuments() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        UUID doc3Id = UUID.randomUUID();
        
        VectorDocument doc1 = createVectorDocument(doc1Id, "a".repeat(400));  // 100 tokens
        VectorDocument doc2 = createVectorDocument(doc2Id, "b".repeat(800));  // 200 tokens
        VectorDocument doc3 = createVectorDocument(doc3Id, "c".repeat(1200)); // 300 tokens
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.of(doc2));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc3Id, companyId))
                .thenReturn(Optional.of(doc3));

        // When
        List<Integer> tokens = contextWindowManager.calculateTokensPerDocument(
                List.of(doc1Id, doc2Id, doc3Id), companyId);

        // Then
        assertThat(tokens).containsExactly(100, 200, 300);
    }

    @Test
    @DisplayName("Should return zero tokens for missing documents")
    void testCalculateTokensPerDocument_MissingDocuments() {
        // Given
        UUID doc1Id = UUID.randomUUID();
        UUID doc2Id = UUID.randomUUID();
        
        VectorDocument doc1 = createVectorDocument(doc1Id, "a".repeat(400));
        
        when(vectorDocumentRepository.findByIdAndCompanyId(doc1Id, companyId))
                .thenReturn(Optional.of(doc1));
        when(vectorDocumentRepository.findByIdAndCompanyId(doc2Id, companyId))
                .thenReturn(Optional.empty());

        // When
        List<Integer> tokens = contextWindowManager.calculateTokensPerDocument(
                List.of(doc1Id, doc2Id), companyId);

        // Then
        assertThat(tokens).containsExactly(100, 0);
    }

    @Test
    @DisplayName("Should handle Vietnamese text in token calculation")
    void testBuildGroundedContext_VietnameseText() {
        // Given
        UUID docId = UUID.randomUUID();
        String vietnameseText = "Khách hàng ABC có số dư nợ là 5.000.000 VNĐ.";
        VectorDocument doc = createVectorDocument(docId, vietnameseText);
        
        RetrievedDocumentDTO dto = createDTO(docId);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(docId, companyId))
                .thenReturn(Optional.of(doc));

        // When
        String context = contextWindowManager.buildGroundedContext(
                List.of(dto), List.of(docId), companyId);

        // Then
        assertThat(context).isEqualTo(vietnameseText);
        
        List<Integer> tokens = contextWindowManager.calculateTokensPerDocument(
                List.of(docId), companyId);
        assertThat(tokens.get(0)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle exactly 8K token budget")
    void testBuildGroundedContext_ExactBudget() {
        // Given - create document with exactly 8000 tokens (32000 chars)
        UUID docId = UUID.randomUUID();
        String text8000Tokens = "a".repeat(32000);
        VectorDocument doc = createVectorDocument(docId, text8000Tokens);
        
        RetrievedDocumentDTO dto = createDTO(docId);
        
        when(vectorDocumentRepository.findByIdAndCompanyId(docId, companyId))
                .thenReturn(Optional.of(doc));

        // When
        String context = contextWindowManager.buildGroundedContext(
                List.of(dto), List.of(docId), companyId);

        // Then
        assertThat(context).hasSize(32000);
        int estimatedTokens = context.length() / 4;
        assertThat(estimatedTokens).isEqualTo(8000);
    }

    // Helper methods

    private VectorDocument createVectorDocument(UUID id, String contentText) {
        VectorDocument doc = new VectorDocument();
        doc.setId(id);
        doc.setCompanyId(companyId);
        
        try {
            JsonNode metadata = objectMapper.createObjectNode()
                    .put("content_text", contentText)
                    .put("document_type", "invoice")
                    .put("module", "ar");
            doc.setMetadata(metadata);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return doc;
    }

    private RetrievedDocumentDTO createDTO(UUID id) {
        RetrievedDocumentDTO dto = new RetrievedDocumentDTO();
        dto.setId(id);
        dto.setRelevanceScore(0.9);
        return dto;
    }
}

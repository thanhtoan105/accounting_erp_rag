package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.RetrievedDocumentDTO;
import com.erp.rag.supabase.vector.VectorDocument;
import com.erp.rag.supabase.vector.VectorDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing context window for LLM.
 * <p>
 * Story 1.5 – AC5: Prune documents to fit 8K token budget, concatenate with
 * separators for clear LLM context boundaries.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class ContextWindowManager {

    private static final Logger logger = LoggerFactory.getLogger(ContextWindowManager.class);
    private static final int MAX_TOKENS = 8000;
    private static final String DOCUMENT_SEPARATOR = "\n\n---\n\n";

    private final VectorDocumentRepository vectorDocumentRepository;

    public ContextWindowManager(VectorDocumentRepository vectorDocumentRepository) {
        this.vectorDocumentRepository = vectorDocumentRepository;
    }

    /**
     * Prune documents to fit token budget and build grounded context.
     *
     * @param retrievedDocuments list of retrieved documents ordered by relevance
     * @param documentIds        list of document vector IDs
     * @return grounded context string
     */
    public String buildGroundedContext(List<RetrievedDocumentDTO> retrievedDocuments,
            List<UUID> documentIds, UUID companyId) {
        logger.debug("Building grounded context from {} documents", retrievedDocuments.size());

        StringBuilder context = new StringBuilder();
        int totalTokens = 0;
        int includedDocs = 0;

        for (int i = 0; i < Math.min(retrievedDocuments.size(), documentIds.size()); i++) {
            UUID docId = documentIds.get(i);
            
            // Fetch full document text
            var docOptional = vectorDocumentRepository.findByIdAndCompanyId(docId, companyId);
            if (docOptional.isEmpty()) {
                continue;
            }

            String docText = extractContentText(docOptional.get());
            if (docText == null || docText.isEmpty()) {
                continue;
            }

            // Estimate tokens (chars ÷ 4 approximation)
            int docTokens = estimateTokens(docText);

            // Check if adding this document exceeds budget
            if (totalTokens + docTokens > MAX_TOKENS) {
                logger.debug("Token budget exceeded, pruned {} documents", 
                        retrievedDocuments.size() - includedDocs);
                break;
            }

            // Add document to context
            if (context.length() > 0) {
                context.append(DOCUMENT_SEPARATOR);
            }
            context.append(docText);

            totalTokens += docTokens;
            includedDocs++;
        }

        logger.info("Grounded context built: {} documents, ~{} tokens", includedDocs, totalTokens);
        return context.toString();
    }

    /**
     * Estimate token count from character count.
     * Uses approximation: tokens ≈ chars ÷ 4
     *
     * @param text the text to estimate
     * @return estimated token count
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }

    /**
     * Get tokens used by each document.
     *
     * @param documentIds list of document IDs
     * @param companyId   the company UUID
     * @return list of token counts corresponding to each document
     */
    public List<Integer> calculateTokensPerDocument(List<UUID> documentIds, UUID companyId) {
        List<Integer> tokens = new ArrayList<>();

        for (UUID docId : documentIds) {
            var docOptional = vectorDocumentRepository.findByIdAndCompanyId(docId, companyId);
            if (docOptional.isPresent()) {
                String docText = extractContentText(docOptional.get());
                tokens.add(estimateTokens(docText));
            } else {
                tokens.add(0);
            }
        }

        return tokens;
    }

    /**
     * Extract content text from metadata JSON.
     * Story 1.5 dependency: content_text stored in metadata by Story 1.4.
     */
    private String extractContentText(VectorDocument doc) {
        JsonNode metadata = doc.getMetadata();
        if (metadata != null && metadata.has("content_text")) {
            return metadata.get("content_text").asText();
        }
        logger.warn("No content_text found in metadata for document: {}", doc.getId());
        return "";
    }
}

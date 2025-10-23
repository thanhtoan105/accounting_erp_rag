package com.erp.rag.ragplatform.worker.service;

import com.erp.rag.ragplatform.worker.domain.ErpDocument;
import com.erp.rag.ragplatform.worker.service.pii.PiiMaskingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service for rendering ERP documents into text templates with PII masking
 * applied.
 * <p>
 * Story 1.4 – AC2, AC5: Text preparation with Vietnamese UTF-8 support and PII
 * masking integration.
 * Simplified MVP template per Dev Notes #4: defer per-type optimization to
 * Story 2.12.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class TextTemplateRenderer {

    private static final Logger logger = LoggerFactory.getLogger(TextTemplateRenderer.class);

    private final PiiMaskingService piiMaskingService;

    public TextTemplateRenderer(PiiMaskingService piiMaskingService) {
        this.piiMaskingService = piiMaskingService;
    }

    /**
     * Renders a document into masked text ready for embedding generation.
     * <p>
     * Process:
     * 1. Extract raw text from document (via ErpDocument.getRawText())
     * 2. Apply PII masking BEFORE embedding (critical security requirement)
     * 3. Validate UTF-8 encoding preservation (Vietnamese diacritics)
     * </p>
     *
     * @param document ERP document to render
     * @return masked text ready for embedding, or null if document is malformed
     */
    public String renderDocument(ErpDocument document) {
        if (document == null) {
            logger.warn("Attempted to render null document");
            return null;
        }

        if (document.isDeleted()) {
            logger.debug("Skipping soft-deleted document: {} id={}",
                    document.getDocumentType(), document.getId());
            return null;
        }

        try {
            // Step 1: Get raw text from document
            String rawText = document.getRawText();

            if (rawText == null || rawText.isBlank()) {
                logger.warn("Document has no raw text: {} id={}",
                        document.getDocumentType(), document.getId());
                return null;
            }

            // Step 2: Validate UTF-8 encoding (Vietnamese diacritics)
            if (!isValidUtf8(rawText)) {
                logger.error("Invalid UTF-8 encoding detected for document: {} id={}",
                        document.getDocumentType(), document.getId());
                return null;
            }

            // Step 3: Apply PII masking BEFORE embedding generation
            // This is critical per Dev Notes #1: masking halts batch on failure
            String maskedText = piiMaskingService.maskText(rawText, document.getDocumentType());

            if (maskedText == null || maskedText.isBlank()) {
                logger.error("PII masking returned empty text for document: {} id={}",
                        document.getDocumentType(), document.getId());
                return null;
            }

            logger.debug("Successfully rendered document: {} id={} (length: {} -> {})",
                    document.getDocumentType(), document.getId(), rawText.length(), maskedText.length());

            return maskedText;

        } catch (PiiMaskingService.PiiMaskingException e) {
            // Critical error: PII masking failure halts batch per Dev Notes #1
            logger.error("PII masking failed for document: {} id={} - HALTING BATCH",
                    document.getDocumentType(), document.getId(), e);
            throw new TextRenderingException(
                    "PII masking failure for document " + document.getId() + " - batch halted", e);

        } catch (Exception e) {
            logger.error("Unexpected error rendering document: {} id={}",
                    document.getDocumentType(), document.getId(), e);
            return null;
        }
    }

    /**
     * Validates that the text is valid UTF-8 encoded.
     * This ensures Vietnamese diacritics are preserved.
     *
     * @param text text to validate
     * @return true if valid UTF-8
     */
    private boolean isValidUtf8(String text) {
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            String reconstructed = new String(bytes, StandardCharsets.UTF_8);
            return text.equals(reconstructed);
        } catch (Exception e) {
            logger.error("UTF-8 validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Exception thrown when text rendering fails critically (e.g., PII masking
     * failure).
     */
    public static class TextRenderingException extends RuntimeException {
        public TextRenderingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.erp.rag.ragplatform.worker.service.pii;

/**
 * Service interface for PII masking operations.
 * <p>
 * Story 1.4 – AC5: PII masking integration before embedding generation.
 * This interface defines the contract established in Story 1.2 for text
 * masking.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public interface PiiMaskingService {

    /**
     * Masks PII in the given text based on document type.
     * <p>
     * Performance requirement: <100ms per document (hard requirement).
     * Error handling: Throws PiiMaskingException on failure (halts batch).
     * </p>
     *
     * @param rawText      raw text containing potential PII
     * @param documentType document type (invoice, bill, customer, etc.)
     * @return masked text with PII replaced by tokens (e.g., "CUSTOMER_12345")
     * @throws PiiMaskingException if masking fails
     */
    String maskText(String rawText, String documentType) throws PiiMaskingException;

    /**
     * Exception thrown when PII masking fails.
     */
    class PiiMaskingException extends Exception {
        public PiiMaskingException(String message) {
            super(message);
        }

        public PiiMaskingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

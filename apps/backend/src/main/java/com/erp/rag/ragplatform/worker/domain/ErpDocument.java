package com.erp.rag.ragplatform.worker.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an extracted ERP document ready for embedding generation.
 * This interface defines the contract for all document types extracted from the ERP system.
 * <p>
 * Story 1.4 – AC1, AC2: Document extraction with normalized structure for text preparation.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public interface ErpDocument {

    /**
     * Returns the unique identifier of this document.
     *
     * @return document UUID
     */
    UUID getId();

    /**
     * Returns the company ID for multi-tenant isolation.
     *
     * @return company UUID
     */
    UUID getCompanyId();

    /**
     * Returns the document type identifier (e.g., "invoice", "bill", "journal_entry").
     *
     * @return document type string
     */
    String getDocumentType();

    /**
     * Returns the source table name in the database.
     *
     * @return source table name
     */
    String getSourceTable();

    /**
     * Returns the fiscal period in format YYYY-MM.
     *
     * @return fiscal period string, or null if not applicable
     */
    String getFiscalPeriod();

    /**
     * Returns the document date.
     *
     * @return document date
     */
    LocalDate getDate();

    /**
     * Returns the document status (e.g., "DRAFT", "POSTED", "PAID").
     *
     * @return status string
     */
    String getStatus();

    /**
     * Returns the module this document belongs to (e.g., "ar", "ap", "gl", "cash_bank").
     *
     * @return module identifier
     */
    String getModule();

    /**
     * Returns the raw text representation of the document for embedding.
     * This method should return the document fields concatenated in a meaningful way,
     * but WITHOUT PII masking applied (masking is handled by TextTemplateRenderer).
     *
     * @return raw text content
     */
    String getRawText();

    /**
     * Checks if this document has been soft-deleted.
     *
     * @return true if deleted_at is not null
     */
    boolean isDeleted();
}




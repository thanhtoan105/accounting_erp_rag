package com.erp.rag.ragplatform.worker.service;

import com.erp.rag.ragplatform.worker.dao.DocumentExtractionDao;
import com.erp.rag.ragplatform.worker.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for extracting ERP documents from all source tables.
 * <p>
 * Story 1.4 – AC1: Orchestrates document extraction across all 7 document types.
 * Supports both full and incremental extraction modes.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class DocumentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DocumentExtractor.class);

    private final DocumentExtractionDao dao;

    public DocumentExtractor(DocumentExtractionDao dao) {
        this.dao = dao;
    }

    /**
     * Extract all documents for a company (full extraction).
     *
     * @param companyId company UUID
     * @return list of all extracted documents
     */
    public List<ErpDocument> extractAll(UUID companyId) {
        return extractAll(companyId, null);
    }

    /**
     * Extract documents for a company, optionally filtering by updated_at (incremental extraction).
     *
     * @param companyId    company UUID
     * @param updatedAfter optional timestamp for incremental sync (null for full extraction)
     * @return list of extracted documents
     */
    public List<ErpDocument> extractAll(UUID companyId, OffsetDateTime updatedAfter) {
        logger.info("Extracting documents for company {} (mode: {}, updatedAfter: {})",
                companyId,
                updatedAfter == null ? "full" : "incremental",
                updatedAfter);

        List<ErpDocument> allDocuments = new ArrayList<>();

        try {
            // Extract from all 7 source tables
            List<InvoiceDocument> invoices = dao.extractInvoices(companyId, updatedAfter);
            logger.debug("Extracted {} invoices", invoices.size());
            allDocuments.addAll(invoices);

            List<BillDocument> bills = dao.extractBills(companyId, updatedAfter);
            logger.debug("Extracted {} bills", bills.size());
            allDocuments.addAll(bills);

            List<JournalEntryDocument> journalEntries = dao.extractJournalEntries(companyId, updatedAfter);
            logger.debug("Extracted {} journal entries", journalEntries.size());
            allDocuments.addAll(journalEntries);

            List<CustomerDocument> customers = dao.extractCustomers(companyId, updatedAfter);
            logger.debug("Extracted {} customers", customers.size());
            allDocuments.addAll(customers);

            List<VendorDocument> vendors = dao.extractVendors(companyId, updatedAfter);
            logger.debug("Extracted {} vendors", vendors.size());
            allDocuments.addAll(vendors);

            List<PaymentDocument> payments = dao.extractPayments(companyId, updatedAfter);
            logger.debug("Extracted {} payments", payments.size());
            allDocuments.addAll(payments);

            List<BankTransactionDocument> bankTransactions = dao.extractBankTransactions(companyId, updatedAfter);
            logger.debug("Extracted {} bank transactions", bankTransactions.size());
            allDocuments.addAll(bankTransactions);

            logger.info("Total documents extracted: {}", allDocuments.size());
            return allDocuments;

        } catch (Exception e) {
            logger.error("Error extracting documents for company {}: {}", companyId, e.getMessage(), e);
            throw new DocumentExtractionException("Failed to extract documents for company " + companyId, e);
        }
    }

    /**
     * Extract documents from specific source tables.
     *
     * @param companyId    company UUID
     * @param sourceTables list of table names to extract from
     * @param updatedAfter optional timestamp for incremental sync
     * @return list of extracted documents
     */
    public List<ErpDocument> extractFrom(UUID companyId, List<String> sourceTables, OffsetDateTime updatedAfter) {
        logger.info("Extracting from specific tables {} for company {}", sourceTables, companyId);

        List<ErpDocument> documents = new ArrayList<>();

        for (String table : sourceTables) {
            switch (table.toLowerCase()) {
                case "invoices" -> documents.addAll(dao.extractInvoices(companyId, updatedAfter));
                case "bills" -> documents.addAll(dao.extractBills(companyId, updatedAfter));
                case "journal_entries" -> documents.addAll(dao.extractJournalEntries(companyId, updatedAfter));
                case "customers" -> documents.addAll(dao.extractCustomers(companyId, updatedAfter));
                case "vendors" -> documents.addAll(dao.extractVendors(companyId, updatedAfter));
                case "payments" -> documents.addAll(dao.extractPayments(companyId, updatedAfter));
                case "cash_transactions" -> documents.addAll(dao.extractBankTransactions(companyId, updatedAfter));
                default -> logger.warn("Unknown source table: {}", table);
            }
        }

        logger.info("Extracted {} documents from specified tables", documents.size());
        return documents;
    }

    /**
     * Exception thrown when document extraction fails.
     */
    public static class DocumentExtractionException extends RuntimeException {
        public DocumentExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}




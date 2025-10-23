package com.erp.rag.ragplatform.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a journal entry document extracted from the accounting.journal_entries table.
 * <p>
 * Story 1.4 – AC1, AC2: Journal entry extraction with descriptions and account codes.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class JournalEntryDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private String entryNumber;
    private LocalDate entryDate;
    private String entryType;
    private String description;
    private String referenceNo;
    private String status;
    private String fiscalPeriod;
    private OffsetDateTime deletedAt;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String accountCodes; // Aggregated account codes from lines

    public JournalEntryDocument() {
    }

    public JournalEntryDocument(UUID id, UUID companyId, String entryNumber, LocalDate entryDate,
            String entryType, String description, String referenceNo, String status,
            String fiscalPeriod, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.entryNumber = entryNumber;
        this.entryDate = entryDate;
        this.entryType = entryType;
        this.description = description;
        this.referenceNo = referenceNo;
        this.status = status;
        this.fiscalPeriod = fiscalPeriod;
        this.deletedAt = deletedAt;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public UUID getCompanyId() {
        return companyId;
    }

    @Override
    public String getDocumentType() {
        return "journal_entry";
    }

    @Override
    public String getSourceTable() {
        return "journal_entries";
    }

    @Override
    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    @Override
    public LocalDate getDate() {
        return entryDate;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getModule() {
        return "gl";
    }

    @Override
    public String getRawText() {
        StringBuilder text = new StringBuilder();
        text.append("journal_entry ").append(entryNumber).append(": ");
        
        if (description != null && !description.isBlank()) {
            text.append(description).append(" | ");
        }
        
        if (totalDebit != null) {
            text.append("Debit: ").append(totalDebit).append(" VND, ");
            text.append("Credit: ").append(totalCredit).append(" VND | ");
        }
        
        text.append("Date: ").append(entryDate);
        
        if (status != null) {
            text.append(" | Status: ").append(status);
        }
        
        if (accountCodes != null && !accountCodes.isBlank()) {
            text.append(" | Accounts: ").append(accountCodes);
        }
        
        return text.toString();
    }

    @Override
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // Getters and Setters

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getEntryNumber() {
        return entryNumber;
    }

    public void setEntryNumber(String entryNumber) {
        this.entryNumber = entryNumber;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getAccountCodes() {
        return accountCodes;
    }

    public void setAccountCodes(String accountCodes) {
        this.accountCodes = accountCodes;
    }
}




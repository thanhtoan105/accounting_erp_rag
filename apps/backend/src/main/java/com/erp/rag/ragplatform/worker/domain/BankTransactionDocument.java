package com.erp.rag.ragplatform.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a bank/cash transaction extracted from the accounting.cash_transactions table.
 * <p>
 * Story 1.4 – AC1, AC2: Bank transaction extraction with account details and descriptions.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class BankTransactionDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private UUID bankAccountId;
    private String bankAccountName;
    private String transactionNumber;
    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal amount;
    private String description;
    private String referenceNo;
    private String fiscalPeriod;
    private OffsetDateTime deletedAt;

    public BankTransactionDocument() {
    }

    public BankTransactionDocument(UUID id, UUID companyId, UUID bankAccountId,
            String bankAccountName, String transactionNumber, LocalDate transactionDate,
            String transactionType, BigDecimal amount, String description,
            String referenceNo, String fiscalPeriod, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.bankAccountId = bankAccountId;
        this.bankAccountName = bankAccountName;
        this.transactionNumber = transactionNumber;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
        this.referenceNo = referenceNo;
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
        return "bank_transaction";
    }

    @Override
    public String getSourceTable() {
        return "cash_transactions";
    }

    @Override
    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    @Override
    public LocalDate getDate() {
        return transactionDate;
    }

    @Override
    public String getStatus() {
        return "POSTED"; // Bank transactions are always posted
    }

    @Override
    public String getModule() {
        return "cash_bank";
    }

    @Override
    public String getRawText() {
        StringBuilder text = new StringBuilder();
        text.append("bank_transaction ").append(transactionNumber).append(": ");
        
        if (description != null && !description.isBlank()) {
            text.append(description).append(" | ");
        }
        
        text.append("Amount: ").append(amount).append(" VND | ");
        text.append("Date: ").append(transactionDate);
        
        if (transactionType != null) {
            text.append(" | Type: ").append(transactionType);
        }
        
        if (bankAccountName != null && !bankAccountName.isBlank()) {
            text.append(" | Account: ").append(bankAccountName);
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

    public UUID getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(UUID bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public void setFiscalPeriod(String fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}




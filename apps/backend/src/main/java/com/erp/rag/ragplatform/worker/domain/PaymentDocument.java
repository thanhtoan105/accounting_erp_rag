package com.erp.rag.ragplatform.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a customer payment document extracted from the accounting.payments table.
 * <p>
 * Story 1.4 – AC1, AC2: Payment extraction with customer details and amounts.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class PaymentDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private UUID customerId;
    private String customerName;
    private String paymentNumber;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNo;
    private String notes;
    private String fiscalPeriod;
    private OffsetDateTime deletedAt;

    public PaymentDocument() {
    }

    public PaymentDocument(UUID id, UUID companyId, UUID customerId, String customerName,
            String paymentNumber, LocalDate paymentDate, BigDecimal amount,
            String paymentMethod, String referenceNo, String notes,
            String fiscalPeriod, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.paymentNumber = paymentNumber;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceNo = referenceNo;
        this.notes = notes;
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
        return "payment";
    }

    @Override
    public String getSourceTable() {
        return "payments";
    }

    @Override
    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    @Override
    public LocalDate getDate() {
        return paymentDate;
    }

    @Override
    public String getStatus() {
        return "POSTED"; // Payments are always posted
    }

    @Override
    public String getModule() {
        return "ar";
    }

    @Override
    public String getRawText() {
        StringBuilder text = new StringBuilder();
        text.append("payment ").append(paymentNumber).append(": ");
        text.append("Amount: ").append(amount).append(" VND | ");
        text.append("Date: ").append(paymentDate);
        
        if (paymentMethod != null) {
            text.append(" | Method: ").append(paymentMethod);
        }
        
        if (customerName != null && !customerName.isBlank()) {
            text.append(" | Customer: ").append(customerName);
        }
        
        if (referenceNo != null && !referenceNo.isBlank()) {
            text.append(" | Ref: ").append(referenceNo);
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

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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




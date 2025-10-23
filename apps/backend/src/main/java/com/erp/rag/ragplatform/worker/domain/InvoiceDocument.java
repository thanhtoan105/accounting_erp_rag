package com.erp.rag.ragplatform.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an invoice document extracted from the accounting.invoices table.
 * <p>
 * Story 1.4 – AC1, AC2: Invoice extraction with customer names, amounts, and descriptions.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class InvoiceDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private UUID customerId;
    private String customerName;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status;
    private String notes;
    private String fiscalPeriod;
    private OffsetDateTime deletedAt;
    private String description; // Aggregated from invoice_lines

    public InvoiceDocument() {
    }

    public InvoiceDocument(UUID id, UUID companyId, UUID customerId, String customerName,
            String invoiceNumber, LocalDate invoiceDate, LocalDate dueDate,
            BigDecimal totalAmount, BigDecimal paidAmount, String status,
            String notes, String fiscalPeriod, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
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
        return "invoice";
    }

    @Override
    public String getSourceTable() {
        return "invoices";
    }

    @Override
    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    @Override
    public LocalDate getDate() {
        return invoiceDate;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getModule() {
        return "ar";
    }

    @Override
    public String getRawText() {
        // Simplified MVP template per Dev Notes #4
        StringBuilder text = new StringBuilder();
        text.append("invoice ").append(invoiceNumber).append(": ");
        
        if (description != null && !description.isBlank()) {
            text.append(description).append(" | ");
        }
        
        text.append("Amount: ").append(totalAmount).append(" VND | ");
        text.append("Date: ").append(invoiceDate);
        
        if (status != null) {
            text.append(" | Status: ").append(status);
        }
        
        if (customerName != null && !customerName.isBlank()) {
            text.append(" | Customer: ").append(customerName);
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}




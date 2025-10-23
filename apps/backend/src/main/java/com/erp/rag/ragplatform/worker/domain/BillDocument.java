package com.erp.rag.ragplatform.worker.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a vendor bill document extracted from the accounting.bills table.
 * <p>
 * Story 1.4 – AC1, AC2: Bill (AP) extraction with vendor names, amounts, and descriptions.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class BillDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private UUID vendorId;
    private String vendorName;
    private String billNumber;
    private LocalDate billDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status;
    private String notes;
    private String fiscalPeriod;
    private OffsetDateTime deletedAt;
    private String description; // Aggregated from bill_lines

    public BillDocument() {
    }

    public BillDocument(UUID id, UUID companyId, UUID vendorId, String vendorName,
            String billNumber, LocalDate billDate, LocalDate dueDate,
            BigDecimal totalAmount, BigDecimal paidAmount, String status,
            String notes, String fiscalPeriod, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.billNumber = billNumber;
        this.billDate = billDate;
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
        return "bill";
    }

    @Override
    public String getSourceTable() {
        return "bills";
    }

    @Override
    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    @Override
    public LocalDate getDate() {
        return billDate;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getModule() {
        return "ap";
    }

    @Override
    public String getRawText() {
        StringBuilder text = new StringBuilder();
        text.append("bill ").append(billNumber).append(": ");
        
        if (description != null && !description.isBlank()) {
            text.append(description).append(" | ");
        }
        
        text.append("Amount: ").append(totalAmount).append(" VND | ");
        text.append("Date: ").append(billDate);
        
        if (status != null) {
            text.append(" | Status: ").append(status);
        }
        
        if (vendorName != null && !vendorName.isBlank()) {
            text.append(" | Vendor: ").append(vendorName);
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

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public LocalDate getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDate billDate) {
        this.billDate = billDate;
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




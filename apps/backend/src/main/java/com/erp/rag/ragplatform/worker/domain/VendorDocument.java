package com.erp.rag.ragplatform.worker.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a vendor master record extracted from the accounting.vendors table.
 * <p>
 * Story 1.4 – AC1, AC2: Vendor extraction with contact details and payment terms.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class VendorDocument implements ErpDocument {

    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private String nameEn;
    private String taxCode;
    private String address;
    private String phone;
    private String email;
    private String contactPerson;
    private Integer paymentTerms;
    private Boolean isActive;
    private OffsetDateTime deletedAt;

    public VendorDocument() {
    }

    public VendorDocument(UUID id, UUID companyId, String code, String name, String nameEn,
            String taxCode, String address, String phone, String email,
            String contactPerson, Integer paymentTerms, Boolean isActive, OffsetDateTime deletedAt) {
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
        this.taxCode = taxCode;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.contactPerson = contactPerson;
        this.paymentTerms = paymentTerms;
        this.isActive = isActive;
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
        return "vendor";
    }

    @Override
    public String getSourceTable() {
        return "vendors";
    }

    @Override
    public String getFiscalPeriod() {
        return null; // Vendors are not tied to a specific fiscal period
    }

    @Override
    public LocalDate getDate() {
        return null; // Vendors don't have a primary date field
    }

    @Override
    public String getStatus() {
        return isActive != null && isActive ? "ACTIVE" : "INACTIVE";
    }

    @Override
    public String getModule() {
        return "ap";
    }

    @Override
    public String getRawText() {
        StringBuilder text = new StringBuilder();
        text.append("vendor ").append(code).append(": ");
        text.append(name);
        
        if (nameEn != null && !nameEn.isBlank()) {
            text.append(" (").append(nameEn).append(")");
        }
        
        if (taxCode != null && !taxCode.isBlank()) {
            text.append(" | Tax Code: ").append(taxCode);
        }
        
        if (address != null && !address.isBlank()) {
            text.append(" | Address: ").append(address);
        }
        
        if (paymentTerms != null) {
            text.append(" | Payment Terms: ").append(paymentTerms).append(" days");
        }
        
        text.append(" | Status: ").append(getStatus());
        
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Integer getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(Integer paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}




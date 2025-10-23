# PII Masking Rules and Field Inventory

**Project:** accounting_erp_rag  
**Date:** 2025-10-18  
**Version:** 1.0  
**Compliance:** Vietnam Circular 200/2014/TT-BTC

## Overview

This document provides a comprehensive inventory of all Personally Identifiable Information (PII) fields across the ERP database schema and defines masking rules for each field type. All PII data is masked before indexing into the RAG vector database to ensure compliance with data protection requirements while maintaining referential integrity for audit trail purposes.

## PII Field Inventory

### 1. Companies Table (`accounting.companies`)

| Column | PII Type | Masking Rule | Example Input | Example Masked Output |
|--------|----------|--------------|---------------|----------------------|
| `name` | Company Name | Deterministic Hash | "Công ty TNHH ABC" | "Company_a7f5d" |
| `name_en` | Company Name (EN) | Deterministic Hash | "ABC Limited Co." | "Company_a7f5d_en" |
| `tax_code` | Tax ID | Partial Masking | "0123456789" | "TAX_*****6789" |
| `address` | Address | City-Only | "123 Nguyễn Huệ, Q1, TPHCM" | "City_TPHCM" |
| `phone` | Phone Number | Deterministic Hash | "+84901234567" | "Phone_b2c3e" |
| `email` | Email | Domain Preservation | "[email protected]" | "user_a7f5d@example.com" |
| `legal_representative` | Person Name | Deterministic Hash | "Nguyễn Văn A" | "Person_c4d8f" |

### 2. User Profiles Table (`accounting.user_profiles`)

| Column | PII Type | Masking Rule | Example Input | Example Masked Output |
|--------|----------|--------------|---------------|----------------------|
| `username` | Username | Deterministic Hash | "admin001" | "User_e5f9a" |
| `full_name` | Person Name | Deterministic Hash | "Trần Thị B" | "Person_d6e2b" |

### 3. Customers Table (`accounting.customers`)

| Column | PII Type | Masking Rule | Example Input | Example Masked Output |
|--------|----------|--------------|---------------|----------------------|
| `code` | Customer Code | Preserve (Non-PII) | "CUST001" | "CUST001" |
| `name` | Customer Name | Deterministic Hash | "Khách hàng XYZ" | "Customer_f3a7c" |
| `tax_code` | Tax ID | Partial Masking | "0987654321" | "TAX_*****4321" |
| `address` | Address | City-Only | "456 Lê Lợi, Q3, TPHCM" | "City_TPHCM" |
| `phone` | Phone Number | Deterministic Hash | "0281234567" | "Phone_g8b4d" |
| `email` | Email | Domain Preservation | "[email protected]" | "contact_f3a7c@client.vn" |
| `contact_person` | Person Name | Deterministic Hash | "Lê Văn C" | "Person_h9c5e" |

### 4. Bank Accounts Table (`accounting.bank_accounts`)

| Column | PII Type | Masking Rule | Example Input | Example Masked Output |
|--------|----------|--------------|---------------|----------------------|
| `account_name` | Account Name | Deterministic Hash | "TK Công ty ABC" | "BankAccount_i2d6f" |
| `bank_name` | Bank Name | Preserve (Non-PII) | "Vietcombank" | "Vietcombank" |
| `account_number` | Bank Account Number | Partial Masking | "1234567890123" | "BANK_*****0123" |

### 5. Audit Logs Table (`accounting.audit_logs`)

| Column | PII Type | Masking Rule | Example Input | Example Masked Output |
|--------|----------|--------------|---------------|----------------------|
| `old_values` (JSONB) | Mixed PII | Recursive Scan & Mask | `{"name": "ABC"}` | `{"name": "Company_a7f5d"}` |
| `new_values` (JSONB) | Mixed PII | Recursive Scan & Mask | `{"email": "[email protected]"}` | `{"email": "user_a7f5d@example.com"}` |
| `ip_address` | IP Address | Preserve for Audit | "192.168.1.1" | "192.168.1.1" |

## Masking Rule Definitions

### Rule 1: Deterministic Hashing (Names, Usernames, Phones)

**Algorithm:** SHA-256(value + salt) → Base64 → Take first 5 characters  
**Purpose:** Enable referential integrity and joins without exposing PII  
**Salt Management:** Retrieved from Supabase Vault (company-specific or global)  
**Implementation:**

```java
public String maskCustomerName(String name, UUID companyId) {
    String salt = getSalt(companyId);
    String hash = SHA256(name + salt).substring(0, 5);
    String masked = "Customer_" + hash;
    
    // Store mapping in pii_mask_map
    storeMaskMapping("customers", customerId, "name", masked, hash, saltVersion);
    
    return masked;
}
```

**Properties:**
- Same input + same salt = Same output (deterministic)
- Allows JOIN operations without reverse lookup
- Irreversible without salt and mapping table

### Rule 2: Partial Masking (Tax IDs, Bank Account Numbers)

**Algorithm:** Preserve last 4 digits, mask prefix with asterisks  
**Purpose:** Maintain auditability while protecting full identifiers  
**Implementation:**

```java
public String maskTaxId(String taxId, UUID companyId) {
    if (taxId == null || taxId.length() < 4) return "TAX_****";
    
    String lastFour = taxId.substring(taxId.length() - 4);
    String masked = "TAX_*****" + lastFour;
    
    // Store full hash for reversibility
    String salt = getSalt(companyId);
    String hash = SHA256(taxId + salt);
    storeMaskMapping("customers", customerId, "tax_code", masked, hash, saltVersion);
    
    return masked;
}
```

**Properties:**
- Last 4 digits preserved for manual validation
- Full reversibility via `pii_mask_map` lookup
- Compliant with Vietnam audit requirements

### Rule 3: Domain Preservation (Emails)

**Algorithm:** Hash local part, preserve domain  
**Purpose:** Maintain email domain for business intelligence while protecting identity  
**Implementation:**

```java
public String maskEmail(String email, UUID companyId) {
    if (email == null || !email.contains("@")) return "masked@unknown.com";
    
    String[] parts = email.split("@");
    String localPart = parts[0];
    String domain = parts[1];
    
    String salt = getSalt(companyId);
    String hash = SHA256(localPart + salt).substring(0, 5);
    String masked = localPart.substring(0, Math.min(4, localPart.length())) + "_" + hash + "@" + domain;
    
    storeMaskMapping("customers", customerId, "email", masked, SHA256(email + salt), saltVersion);
    
    return masked;
}
```

**Properties:**
- Preserves email domain for analytics
- Partially recognizable for debugging
- Fully reversible via mapping table

### Rule 4: City-Only (Addresses)

**Algorithm:** Extract city/province only, discard street details  
**Purpose:** Preserve geographic insights while removing precise location  
**Implementation:**

```java
public String maskAddress(String address, UUID companyId) {
    if (address == null) return "City_Unknown";
    
    // Vietnamese city pattern: "TPHCM", "Hà Nội", "Đà Nẵng"
    String city = extractVietnameseCity(address); // Regex-based extraction
    String masked = "City_" + city;
    
    String salt = getSalt(companyId);
    storeMaskMapping("customers", customerId, "address", masked, SHA256(address + salt), saltVersion);
    
    return masked;
}
```

**Properties:**
- City-level geographic data preserved
- No precise street address exposed
- Full address recoverable for compliance

### Rule 5: Preserve Non-PII

**Fields:** Customer codes, account codes, bank names, IP addresses (for audit)  
**Rule:** No masking applied  
**Rationale:** These fields are not PII or required for audit compliance

## Salt Management Strategy

### Supabase Vault Integration

**Storage:** Cryptographic salts stored in Supabase Vault (PostgreSQL `vault` extension)  
**Encryption:** AES-256 encryption at rest  
**Access Control:** Service role only (authenticated queries via Supabase client)

### Salt Hierarchy

1. **Global Salt (`pii_masking_global_salt`):**  
   - Used for system-wide entities (user_profiles, companies)  
   - Rotated annually (manual process for thesis scope)

2. **Company-Specific Salts (`pii_masking_company_{company_id}`):**  
   - Used for customer, vendor, invoice data  
   - Isolated per company (multi-tenant security)  
   - Rotated on demand (e.g., security incidents)

### Salt Version Tracking

- Each masked value records `salt_version` in `pii_mask_map` table  
- Enables salt rotation without breaking existing mappings  
- Unmask operation checks version and retrieves correct historical salt

## Mapping Table Schema

### `pii_mask_map` Table

Stores original → masked mapping for audit trail and reversibility:

```sql
CREATE TABLE pii_mask_map (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_table TEXT NOT NULL,           -- e.g., "customers", "companies"
    source_id UUID NOT NULL,              -- Record ID in source table
    field TEXT NOT NULL,                  -- e.g., "name", "tax_code"
    masked_value TEXT NOT NULL,           -- e.g., "Customer_a7f5d"
    hash TEXT NOT NULL,                   -- Full SHA-256 hash for reversibility
    salt_version INT NOT NULL,            -- Salt version used for this hash
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_pii_mask_map_source ON pii_mask_map(source_table, source_id);
CREATE INDEX idx_pii_mask_map_masked ON pii_mask_map(masked_value);
```

**Row-Level Security (RLS):**  
- ADMIN role: Full access (SELECT, INSERT)  
- Service role: SELECT only (for unmask operations)  
- Other roles: No access

**Append-Only:** No UPDATE/DELETE allowed (audit compliance)

## Vietnamese-Specific PII Patterns

### Vietnamese Tax ID Format

**Pattern:** 10 or 13 digits (MST - Mã số thuế)  
**Regex:** `^[0-9]{10}(-[0-9]{3})?$`  
**Example:** `0123456789` or `0123456789-001`

### Vietnamese Phone Number Format

**Pattern:** Mobile (+84 9xx xxx xxxx) or Landline (028 xxxx xxxx)  
**Regex:** `^(\+84|0)(9[0-9]{8}|[2-8][0-9]{8})$`  
**Example:** `+84901234567`, `0281234567`

### Vietnamese Name Patterns

**Characteristics:** Unicode diacritics (ă, â, đ, ê, ô, ơ, ư, ạ, ả, ã, á, à)  
**Regex:** `^[A-ZÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ][a-zàáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]+( [A-ZÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ][a-zàáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]+)+$`  
**Example:** `Nguyễn Văn A`, `Trần Thị Bình`

### Vietnamese City/Province Names

**Major Cities:** Hà Nội, TPHCM (TP.HCM), Đà Nẵng, Hải Phòng, Cần Thơ  
**Extraction Pattern:** Match against province list or regex `(TP\.?HCM|Hà Nội|Đà Nẵng|...)`

## Compliance Alignment

### Vietnam Circular 200/2014/TT-BTC Requirements

1. **10-Year Audit Retention:** `pii_mask_map` table preserved for 10 years (enforced via retention policy)
2. **Immutable Audit Trail:** Append-only constraints, no DELETE/UPDATE on mapping table
3. **Reversibility for Compliance Investigations:** `unmask_pii()` function with audit logging (`pii_unmask_audit` table)
4. **Access Control:** ADMIN role only for unmask operations, MFA recommended (out of thesis scope)

### GDPR Principles (Future Enhancement)

- **Data Minimization:** Only PII fields necessary for business operations are stored
- **Purpose Limitation:** Masked data used only for RAG indexing, not marketing/analytics
- **Right to Erasure:** Masked values can be deleted; mapping table retains hash for audit (anonymization)

## Performance Considerations

### Masking Overhead Target

**Requirement:** < 100ms per document (end-to-end masking for all PII fields)

**Optimization Strategies:**
1. **Batch Salt Retrieval:** Fetch salt once per batch (not per field)
2. **Connection Pooling:** HikariCP pool (min=2, max=10) for Supabase queries
3. **Async Mapping Storage:** Store `pii_mask_map` entries asynchronously (fire-and-forget)
4. **SHA-256 Hardware Acceleration:** Use Java NIO `MessageDigest` with native crypto

**Benchmark Plan:**  
- 1000-document batch with 5 PII fields per document  
- Measure P50/P95/P99 latency with/without masking  
- Target: P95 < 100ms per document

## Test Strategy

### Unit Tests (JUnit 5)

- **Deterministic Hashing:** Same input + salt = Same output across 100 invocations
- **Format Compliance:** Verify "Customer_xxxxx", "TAX_*****1234" patterns
- **Edge Cases:** Null values, empty strings, malformed inputs, multi-byte UTF-8

### Integration Tests (Testcontainers)

- **End-to-End Masking:** Ingest 10 sample documents, verify vector_documents table contains zero raw PII
- **Unmask Function:** Service role can reverse lookup, regular role denied, audit log captured
- **Vietnamese Regex:** Test Vietnamese tax IDs, names (with diacritics), phone numbers

### Performance Tests

- **Benchmark:** 1000-document batch, measure masking overhead (< 100ms per document)
- **Load Test:** 10,000 documents with concurrent masking workers (5 threads)

## Future Enhancements (Post-Thesis)

1. **Automated Salt Rotation:** Quarterly rotation with seamless versioning
2. **MFA for Unmask Operations:** Google Authenticator integration, manager approval workflow
3. **Time-Limited Unmask Access:** 1-hour expiry tokens for compliance investigations
4. **HSM-Backed Encryption:** Hardware Security Module for highly sensitive environments
5. **AI-Powered PII Detection:** Machine learning model to detect new PII patterns beyond regex

## References

- [Vietnam Circular 200/2014/TT-BTC](https://thuvienphapluat.vn/van-ban/Tai-chinh-nha-nuoc/Thong-tu-200-2014-TT-BTC-huong-dan-che-do-ke-toan-doanh-nghiep-2015-260273.aspx)
- [Supabase Vault Documentation](https://supabase.com/docs/guides/database/vault)
- [NIST SP 800-63B: Digital Identity Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [GDPR Article 32: Security of Processing](https://gdpr-info.eu/art-32-gdpr/)

---

**Document Owner:** Data Engineering Team  
**Last Updated:** 2025-10-18  
**Next Review:** 2026-01-18

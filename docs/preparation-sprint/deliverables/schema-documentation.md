# Accounting ERP Database Schema Documentation

**Generated:** 2025-10-19
**Database:** PostgreSQL 17.6.1 with pgvector 0.8.0
**Schema:** `accounting`
**Total Tables:** 18
**Total Indexes:** 84
**Total Foreign Keys:** 42
**Database Size:** ~12 MB (including indexes)

---

## Table of Contents

1. [Overview](#overview)
2. [Schema Statistics](#schema-statistics)
3. [Entity Relationship Diagram](#entity-relationship-diagram)
4. [Table Definitions](#table-definitions)
   - [System Management](#system-management-3-tables)
   - [Chart of Accounts](#chart-of-accounts-3-tables)
   - [Accounts Receivable](#accounts-receivable-5-tables)
   - [Cash & Bank](#cash--bank-2-tables)
   - [General Ledger](#general-ledger-3-tables)
   - [RAG Platform](#rag-platform-2-tables)
5. [Multi-Tenancy Architecture](#multi-tenancy-architecture)
6. [Indexes & Performance](#indexes--performance)
7. [Sample Queries](#sample-queries)
8. [Vietnam Circular 200 Compliance](#vietnam-circular-200-compliance)

---

## Overview

This schema implements a **multi-tenant ERP accounting system** compliant with **Vietnam Circular 200/2014/TT-BTC** regulations. It supports:

- **Double-entry bookkeeping** with automated validation
- **4-level chart of accounts** hierarchy (Class → Group → Detail → Sub-detail)
- **Multi-company operations** with full data isolation
- **Fiscal period management** with OPEN/CLOSED/LOCKED states
- **RAG (Retrieval-Augmented Generation)** pipeline integration with pgvector
- **Comprehensive audit logging** for regulatory compliance (10-year retention)

### Key Features

✅ **Multi-tenancy:** All tables partitioned by `company_id`
✅ **Soft deletes:** `deleted_at` timestamp (never hard delete)
✅ **Audit trail:** Automatic logging of all CREATE/UPDATE/DELETE operations
✅ **Row Level Security (RLS):** Enabled on all business tables
✅ **Bilingual support:** Vietnamese (`name_vn`) + English (`name_en`)
✅ **Vector search:** pgvector 0.8.0 with HNSW indexes for semantic retrieval

---

## Schema Statistics

### Table Size Distribution

| Table Name | Total Size | Table Data | Indexes | Row Count | Notes |
|------------|------------|------------|---------|-----------|-------|
| `vector_documents` | 11 MB | 192 KB | 11 MB | 1,000 | RAG embeddings (1536-dim) |
| `customers` | 96 KB | 8 KB | 88 KB | 0 | Customer master data |
| `vector_test` | 88 KB | 8 KB | 80 KB | 3 | pgvector test table |
| `user_profiles` | 80 KB | 8 KB | 72 KB | 5 | User accounts |
| `companies` | 80 KB | 8 KB | 72 KB | 1 | Multi-tenant root |
| `journal_entries` | 56 KB | 0 KB | 56 KB | 0 | General ledger entries |
| `invoices` | 56 KB | 0 KB | 56 KB | 0 | AR invoices |
| `accounts` | 48 KB | 0 KB | 48 KB | 0 | Chart of accounts |
| `cash_transactions` | 40 KB | 0 KB | 40 KB | 0 | Bank/cash movements |
| `payments` | 40 KB | 0 KB | 40 KB | 0 | Customer payments |
| `rag_queries` | 40 KB | 0 KB | 40 KB | 0 | RAG query audit log |
| `audit_logs` | 40 KB | 0 KB | 40 KB | 0 | Change tracking |
| Other tables | < 40 KB | 0 KB | < 40 KB | 0 | Supporting tables |

**Total:** ~12 MB (93% indexes, 7% data)

### Foreign Key Relationships

**Most Connected Tables:**
- `companies` (12 incoming FKs) - Multi-tenant root
- `user_profiles` (9 incoming FKs) - User actions tracking
- `fiscal_periods` (7 incoming FKs) - Period locking
- `journal_entries` (5 incoming FKs) - GL integration
- `accounts` (6 incoming FKs) - Chart of accounts references

---

## Entity Relationship Diagram

```
┌─────────────────┐
│   companies     │──┬─────────────────────────────────────┐
│  (Multi-tenant) │  │                                     │
└─────────────────┘  │                                     │
        │            │                                     │
        ├────────────┼─────────────────┐                  │
        │            │                 │                  │
        ▼            ▼                 ▼                  ▼
┌──────────────┐ ┌─────────┐  ┌──────────────┐  ┌────────────────┐
│user_profiles │ │accounts │  │fiscal_periods│  │vector_documents│
│  (RBAC)      │ │ (CoA)   │  │ (Closing)    │  │  (RAG)         │
└──────────────┘ └─────────┘  └──────────────┘  └────────────────┘
        │            │                 │
        │            │                 │
        ▼            ▼                 ▼
┌─────────────────────────────────────────────┐
│         journal_entries (GL Core)           │
│  ┌─────────────────────────────────┐        │
│  │  journal_entry_lines (Dr/Cr)    │        │
│  └─────────────────────────────────┘        │
└─────────────────────────────────────────────┘
        ▲            ▲                 ▲
        │            │                 │
        │            ├─────────────────┤
        │            │                 │
  ┌─────────┐  ┌─────────┐    ┌──────────────┐
  │invoices │  │payments │    │cash_trans... │
  │  (AR)   │  │  (AR)   │    │  (Bank)      │
  └─────────┘  └─────────┘    └──────────────┘
        │            │
        └────┬───────┘
             ▼
    ┌─────────────────┐
    │invoice_payments │
    │  (Link table)   │
    └─────────────────┘

Legend:
───  One-to-Many FK
═══  Multi-tenant partition
```

---

## Table Definitions

### System Management (3 tables)

#### 1. `companies` - Multi-Tenant Root

**Purpose:** Master table for multi-company operations. All business tables partition by `company_id`.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Company unique ID |
| `name` | VARCHAR(255) | NOT NULL | Company legal name (Vietnamese) |
| `name_en` | VARCHAR(255) | NULL | Company name (English) |
| `tax_code` | VARCHAR(50) | NOT NULL, UNIQUE | Vietnamese tax code (MST) |
| `address` | TEXT | NULL | Company address |
| `phone` | VARCHAR(50) | NULL | Contact phone |
| `email` | VARCHAR(255) | NULL | Contact email |
| `legal_representative` | VARCHAR(255) | NULL | Legal representative name |
| `business_type` | VARCHAR(100) | NULL | Business type/industry |
| `fiscal_year_start` | INTEGER | DEFAULT 1, CHECK(1-12) | Fiscal year start month |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Record creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |
| `deleted_at` | TIMESTAMP | NULL | Soft delete timestamp |

**Indexes:**
- `companies_pkey` (UNIQUE) on `id`
- `companies_tax_code_key` (UNIQUE) on `tax_code`
- `idx_companies_tax_code` on `tax_code`
- `idx_companies_deleted_at` on `deleted_at`

**Outgoing FKs:** None (root table)
**Incoming FKs:** 12 tables (accounts, invoices, customers, etc.)

**Sample Data:**
```sql
-- Example: Vietnamese technology company
INSERT INTO accounting.companies (name, name_en, tax_code, address, fiscal_year_start)
VALUES (
    'Công ty TNHH Công Nghệ ABC',
    'ABC Technology Ltd.',
    '0123456789',
    '123 Nguyễn Huệ, Q.1, TP.HCM',
    1  -- January fiscal year start
);
```

---

#### 2. `user_profiles` - User Accounts & RBAC

**Purpose:** User metadata linked to Supabase `auth.users`. Implements role-based access control (ADMIN, ACCOUNTANT, VIEWER).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `user_id` | UUID | PK, FK→auth.users | Supabase auth user ID |
| `company_id` | BIGINT | FK→companies | User's primary company |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE (lowercase) | Login username |
| `full_name` | VARCHAR(255) | NOT NULL | User display name |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE (lowercase) | Email address |
| `role` | VARCHAR(50) | NOT NULL, CHECK | ADMIN, ACCOUNTANT, VIEWER |
| `is_active` | BOOLEAN | DEFAULT TRUE | Account active status |
| `last_login_at` | TIMESTAMP | NULL | Last login timestamp |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Account creation |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Role Permissions:**
- **ADMIN**: Full system access, user management, company config
- **ACCOUNTANT**: Post transactions, manage AR/AP, run reports
- **VIEWER**: Read-only access to dashboards and reports

**Indexes:**
- `user_profiles_pkey` (UNIQUE) on `user_id`
- `idx_user_profiles_username_lower` (UNIQUE) on `LOWER(username)`
- `idx_user_profiles_email_lower` (UNIQUE) on `LOWER(email)`
- `idx_user_profiles_company_id` on `company_id`

**Sample Query:**
```sql
-- Get all active accountants for a company
SELECT user_id, full_name, email, last_login_at
FROM accounting.user_profiles
WHERE company_id = 1
  AND role = 'ACCOUNTANT'
  AND is_active = TRUE
  AND deleted_at IS NULL;
```

---

#### 3. `audit_logs` - Change Tracking

**Purpose:** Immutable audit trail for regulatory compliance (10-year retention required by Circular 200).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Log entry ID |
| `company_id` | BIGINT | FK→companies | Tenant ID |
| `user_id` | UUID | FK→user_profiles | User who performed action |
| `table_name` | VARCHAR(100) | NOT NULL | Target table name |
| `record_id` | BIGINT | NOT NULL | Target record ID |
| `action` | VARCHAR(20) | NOT NULL, CHECK | INSERT, UPDATE, DELETE |
| `old_values` | JSONB | NULL | Previous field values (UPDATE/DELETE) |
| `new_values` | JSONB | NULL | New field values (INSERT/UPDATE) |
| `ip_address` | VARCHAR(50) | NULL | Client IP address |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Action timestamp |

**Indexes:**
- `audit_logs_pkey` (UNIQUE) on `id`
- `idx_audit_logs_table_record` on `(table_name, record_id)`
- `idx_audit_logs_user` on `user_id`
- `idx_audit_logs_created_at` on `created_at`

**Important:** This table is **append-only**. Never UPDATE or DELETE records.

---

### Chart of Accounts (3 tables)

#### 4. `accounts` - Circular 200 Chart of Accounts

**Purpose:** 4-level account hierarchy following Vietnam Circular 200/2014/TT-BTC standards.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Account ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE(company_id, code) | Account code (e.g., 111, 1111) |
| `name_vn` | VARCHAR(255) | NOT NULL | Vietnamese account name |
| `name_en` | VARCHAR(255) | NULL | English account name |
| `account_type` | VARCHAR(50) | NOT NULL, CHECK | ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, OFF_BALANCE |
| `account_class` | INTEGER | NOT NULL, CHECK(0-9) | Circular 200 class (0-9) |
| `parent_id` | BIGINT | FK→accounts | Parent account (for hierarchy) |
| `level` | INTEGER | NOT NULL, CHECK(1-4) | Hierarchy level (1=Class, 4=Sub-detail) |
| `is_detail` | BOOLEAN | DEFAULT TRUE | Can post transactions to this account? |
| `normal_balance` | VARCHAR(10) | NOT NULL, CHECK | DEBIT or CREDIT |
| `is_active` | BOOLEAN | DEFAULT TRUE | Account active status |
| `description` | TEXT | NULL | Account description/notes |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Account Code Standards (Circular 200):**
- **1xx:** Assets (111=Cash, 112=Bank, 131=AR)
- **2xx:** Liabilities (211=Short-term loans, 331=AP)
- **3xx:** Equity (311=Capital, 421=Retained earnings)
- **4xx:** Equity (continued)
- **5xx:** Revenue (511=Sales revenue)
- **6xx:** Expenses (621=COGS, 642=Salaries)
- **7xx:** Other income/expenses
- **8xx-9xx:** Off-balance sheet items

**Indexes:**
- `accounts_pkey` (UNIQUE) on `id`
- `unique_company_account_code` (UNIQUE) on `(company_id, code)`
- `idx_accounts_company_code` on `(company_id, code)`
- `idx_accounts_parent` on `parent_id`
- `idx_accounts_type` on `account_type`

**Sample Data:**
```sql
-- Example: 4-level hierarchy for Cash
-- Level 1: Class 1 (Assets)
INSERT INTO accounting.accounts (company_id, code, name_vn, name_en, account_type, account_class, level, is_detail)
VALUES (1, '1', 'Tài sản', 'Assets', 'ASSET', 1, 1, FALSE);

-- Level 2: Group 11 (Current Assets)
INSERT INTO accounting.accounts (company_id, code, name_vn, name_en, account_type, account_class, parent_id, level, is_detail)
VALUES (1, '11', 'Tài sản ngắn hạn', 'Current Assets', 'ASSET', 1, (SELECT id FROM accounting.accounts WHERE code='1'), 2, FALSE);

-- Level 3: Detail 111 (Cash)
INSERT INTO accounting.accounts (company_id, code, name_vn, name_en, account_type, account_class, parent_id, level, is_detail, normal_balance)
VALUES (1, '111', 'Tiền mặt', 'Cash', 'ASSET', 1, (SELECT id FROM accounting.accounts WHERE code='11'), 3, TRUE, 'DEBIT');

-- Level 4: Sub-detail 1111 (Cash VND)
INSERT INTO accounting.accounts (company_id, code, name_vn, account_type, account_class, parent_id, level, is_detail, normal_balance)
VALUES (1, '1111', 'Tiền mặt VND', 'ASSET', 1, (SELECT id FROM accounting.accounts WHERE code='111'), 4, TRUE, 'DEBIT');
```

---

#### 5. `account_balances` - Period Balances Cache

**Purpose:** Pre-calculated account balances by fiscal period for fast financial reporting.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Balance record ID |
| `account_id` | BIGINT | NOT NULL, FK→accounts | Account reference |
| `fiscal_period_id` | BIGINT | NOT NULL, FK→fiscal_periods | Period reference |
| `opening_balance` | NUMERIC(15,2) | DEFAULT 0 | Opening balance for period |
| `debit_total` | NUMERIC(15,2) | DEFAULT 0 | Sum of debits in period |
| `credit_total` | NUMERIC(15,2) | DEFAULT 0 | Sum of credits in period |
| `closing_balance` | NUMERIC(15,2) | DEFAULT 0 | Closing balance (opening + debits - credits) |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last recalculation |

**Formula:**
```
closing_balance = opening_balance + debit_total - credit_total  (for DEBIT normal balance)
closing_balance = opening_balance - debit_total + credit_total  (for CREDIT normal balance)
```

**Indexes:**
- `account_balances_pkey` (UNIQUE) on `id`
- `unique_account_period_balance` (UNIQUE) on `(account_id, fiscal_period_id)`
- `idx_account_balances_account` on `account_id`
- `idx_account_balances_period` on `fiscal_period_id`

**Performance Note:** This table is **denormalized** to avoid slow JOIN queries when generating Balance Sheet and P&L reports.

---

#### 6. `fiscal_periods` - Period Closing Management

**Purpose:** Manage accounting periods with OPEN/CLOSED/LOCKED states for regulatory compliance.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Period ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `year` | INTEGER | NOT NULL | Fiscal year (e.g., 2024) |
| `period` | INTEGER | NOT NULL, CHECK(1-12) | Period number (1=Jan...12=Dec) |
| `period_type` | VARCHAR(20) | NOT NULL, CHECK | MONTHLY, QUARTERLY, YEARLY |
| `start_date` | DATE | NOT NULL | Period start date |
| `end_date` | DATE | NOT NULL | Period end date |
| `status` | VARCHAR(20) | NOT NULL, CHECK | OPEN, CLOSED, LOCKED |
| `closed_by` | UUID | FK→user_profiles | User who closed period |
| `closed_at` | TIMESTAMP | NULL | Period close timestamp |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

**Status Transitions:**
1. **OPEN** → Can post new transactions
2. **CLOSED** → No new transactions, but can reopen (within same fiscal year)
3. **LOCKED** → Permanently locked (10-year audit retention), cannot modify

**Indexes:**
- `fiscal_periods_pkey` (UNIQUE) on `id`
- `unique_company_period` (UNIQUE) on `(company_id, year, period, period_type)`
- `idx_fiscal_periods_company_year` on `(company_id, year)`
- `idx_fiscal_periods_status` on `status`

**Business Rule:** Cannot post journal entries to CLOSED or LOCKED periods.

---

### General Ledger (3 tables)

#### 7. `journal_entries` - GL Master Records

**Purpose:** Header records for double-entry journal entries.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Entry ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `fiscal_period_id` | BIGINT | NOT NULL, FK→fiscal_periods | Period assignment |
| `entry_number` | VARCHAR(50) | NOT NULL, UNIQUE(company_id, entry_number) | GL entry number (auto-generated) |
| `entry_date` | DATE | NOT NULL | Transaction date |
| `entry_type` | VARCHAR(50) | NOT NULL | GENERAL, SALES, PURCHASE, PAYMENT, etc. |
| `description` | TEXT | NOT NULL | Entry description |
| `reference_no` | VARCHAR(100) | NULL | External reference (invoice #, etc.) |
| `reference_type` | VARCHAR(50) | NULL | INVOICE, PAYMENT, CASH_TRANSACTION |
| `reference_id` | BIGINT | NULL | ID of referenced record |
| `status` | VARCHAR(20) | NOT NULL, CHECK | DRAFT, POSTED, REVERSED |
| `reversed_by` | BIGINT | FK→journal_entries | Reversing entry ID (if reversed) |
| `created_by` | UUID | NOT NULL, FK→user_profiles | User who created entry |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `posted_by` | UUID | FK→user_profiles | User who posted entry |
| `posted_at` | TIMESTAMP | NULL | Posting timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Status Workflow:**
1. **DRAFT** → Entry created, not yet affecting balances
2. **POSTED** → Entry finalized, balances updated
3. **REVERSED** → Entry reversed (via new reversing entry)

**Indexes:**
- `journal_entries_pkey` (UNIQUE) on `id`
- `unique_company_entry_number` (UNIQUE) on `(company_id, entry_number)`
- `idx_journal_entries_company_date` on `(company_id, entry_date)`
- `idx_journal_entries_fiscal_period` on `fiscal_period_id`
- `idx_journal_entries_reference` on `(reference_type, reference_id)`
- `idx_journal_entries_status` on `status`

**Important:** Entry numbers are auto-generated per company (e.g., "JE-2024-001").

---

#### 8. `journal_entry_lines` - Debit/Credit Lines

**Purpose:** Individual debit/credit lines for journal entries. Enforces double-entry balance.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Line ID |
| `journal_entry_id` | BIGINT | NOT NULL, FK→journal_entries | Parent entry |
| `account_id` | BIGINT | NOT NULL, FK→accounts | Posting account |
| `debit` | NUMERIC(15,2) | DEFAULT 0, CHECK(≥0) | Debit amount |
| `credit` | NUMERIC(15,2) | DEFAULT 0, CHECK(≥0) | Credit amount |
| `description` | TEXT | NULL | Line-level description |
| `line_order` | INTEGER | NOT NULL | Display order |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

**Double-Entry Validation:**
```sql
-- Enforced by database trigger:
-- SUM(debit) = SUM(credit) for each journal_entry_id
CHECK (
    SELECT SUM(debit) - SUM(credit)
    FROM accounting.journal_entry_lines
    WHERE journal_entry_id = NEW.journal_entry_id
) = 0
```

**Indexes:**
- `journal_entry_lines_pkey` (UNIQUE) on `id`
- `idx_journal_entry_lines_entry` on `journal_entry_id`
- `idx_journal_entry_lines_account` on `account_id`

**Sample Entry:**
```sql
-- Example: Sale on credit (AR invoice posting)
-- Debit: Accounts Receivable (131) 110,000
-- Credit: Sales Revenue (511) 100,000
-- Credit: VAT Payable (33311) 10,000

INSERT INTO accounting.journal_entries (company_id, fiscal_period_id, entry_number, entry_date, entry_type, description, status, created_by)
VALUES (1, 1, 'JE-2024-001', '2024-10-19', 'SALES', 'Invoice #INV-001 - ABC Corp', 'POSTED', 'user-uuid-here');

INSERT INTO accounting.journal_entry_lines (journal_entry_id, account_id, debit, credit, description, line_order)
VALUES
    ((SELECT id FROM journal_entries WHERE entry_number='JE-2024-001'), (SELECT id FROM accounts WHERE code='131'), 110000, 0, 'AR - ABC Corp', 1),
    ((SELECT id FROM journal_entries WHERE entry_number='JE-2024-001'), (SELECT id FROM accounts WHERE code='511'), 0, 100000, 'Sales Revenue', 2),
    ((SELECT id FROM journal_entries WHERE entry_number='JE-2024-001'), (SELECT id FROM accounts WHERE code='33311'), 0, 10000, 'VAT Payable (10%)', 3);
```

---

### Accounts Receivable (5 tables)

#### 9. `customers` - Customer Master Data

**Purpose:** Customer profiles for AR module.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Customer ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE(company_id, code) | Customer code (e.g., CUST-001) |
| `name` | VARCHAR(255) | NOT NULL | Customer legal name |
| `tax_code` | VARCHAR(50) | NULL | Customer tax ID (MST) |
| `address` | TEXT | NULL | Customer address |
| `phone` | VARCHAR(50) | NULL | Contact phone |
| `email` | VARCHAR(255) | NULL | Contact email |
| `contact_person` | VARCHAR(255) | NULL | Contact person name |
| `credit_limit` | NUMERIC(15,2) | DEFAULT 0 | Maximum outstanding AR |
| `payment_terms` | INTEGER | DEFAULT 30 | Payment terms (days) |
| `account_id` | BIGINT | FK→accounts | Linked AR account (typically 131) |
| `is_active` | BOOLEAN | DEFAULT TRUE | Customer active status |
| `notes` | TEXT | NULL | Additional notes |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Indexes:**
- `customers_pkey` (UNIQUE) on `id`
- `unique_company_customer_code` (UNIQUE) on `(company_id, code)`
- `idx_customers_company_code` on `(company_id, code)`
- `idx_customers_name` on `name`
- `idx_customers_tax_code` on `tax_code`

---

#### 10. `invoices` - Sales Invoices (AR)

**Purpose:** Accounts receivable invoices with automatic GL posting.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Invoice ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `customer_id` | BIGINT | NOT NULL, FK→customers | Customer reference |
| `fiscal_period_id` | BIGINT | NOT NULL, FK→fiscal_periods | Period assignment |
| `invoice_number` | VARCHAR(50) | NOT NULL, UNIQUE(company_id, invoice_number) | Invoice # (e.g., INV-2024-001) |
| `invoice_date` | DATE | NOT NULL | Invoice issue date |
| `due_date` | DATE | NOT NULL | Payment due date |
| `subtotal` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | Before-tax amount |
| `tax_rate` | NUMERIC(5,2) | DEFAULT 10.00 | VAT rate (%) |
| `tax_amount` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | VAT amount |
| `total_amount` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | Total (subtotal + tax) |
| `paid_amount` | NUMERIC(15,2) | DEFAULT 0, CHECK(≥0) | Amount paid so far |
| `status` | VARCHAR(20) | NOT NULL, CHECK | DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED |
| `notes` | TEXT | NULL | Invoice notes |
| `journal_entry_id` | BIGINT | FK→journal_entries | Linked GL entry |
| `created_by` | UUID | NOT NULL, FK→user_profiles | User who created invoice |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Status Flow:**
1. **DRAFT** → Invoice created, not sent
2. **SENT** → Invoice sent to customer
3. **PARTIAL** → Partially paid
4. **PAID** → Fully paid
5. **OVERDUE** → Past due date, unpaid
6. **CANCELLED** → Invoice cancelled

**Indexes:**
- `invoices_pkey` (UNIQUE) on `id`
- `unique_company_invoice_number` (UNIQUE) on `(company_id, invoice_number)`
- `idx_invoices_company_date` on `(company_id, invoice_date)`
- `idx_invoices_customer` on `customer_id`
- `idx_invoices_due_date` on `due_date`
- `idx_invoices_status` on `status`

---

#### 11. `invoice_lines` - Invoice Line Items

**Purpose:** Line-item details for invoices (products/services).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Line ID |
| `invoice_id` | BIGINT | NOT NULL, FK→invoices | Parent invoice |
| `description` | VARCHAR(500) | NOT NULL | Product/service description |
| `quantity` | NUMERIC(10,2) | NOT NULL, CHECK(>0) | Quantity sold |
| `unit_price` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | Price per unit |
| `tax_rate` | NUMERIC(5,2) | DEFAULT 10.00 | VAT rate (%) |
| `tax_amount` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | VAT for this line |
| `line_total` | NUMERIC(15,2) | NOT NULL, CHECK(≥0) | Total (quantity × unit_price) |
| `line_order` | INTEGER | NOT NULL | Display order |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `invoice_lines_pkey` (UNIQUE) on `id`
- `idx_invoice_lines_invoice` on `invoice_id`

**Calculation:**
```sql
line_total = quantity * unit_price
tax_amount = line_total * (tax_rate / 100)
```

---

#### 12. `payments` - Customer Payments

**Purpose:** Payment records from customers (can apply to multiple invoices).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Payment ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `customer_id` | BIGINT | NOT NULL, FK→customers | Customer reference |
| `fiscal_period_id` | BIGINT | NOT NULL, FK→fiscal_periods | Period assignment |
| `payment_number` | VARCHAR(50) | NOT NULL, UNIQUE(company_id, payment_number) | Payment # (auto-generated) |
| `payment_date` | DATE | NOT NULL | Payment received date |
| `amount` | NUMERIC(15,2) | NOT NULL, CHECK(>0) | Payment amount |
| `payment_method` | VARCHAR(50) | NOT NULL, CHECK | CASH, BANK_TRANSFER, CHECK, CREDIT_CARD, OTHER |
| `bank_account_id` | BIGINT | FK→bank_accounts | Receiving bank account |
| `reference_no` | VARCHAR(100) | NULL | Bank reference/check number |
| `notes` | TEXT | NULL | Payment notes |
| `journal_entry_id` | BIGINT | FK→journal_entries | Linked GL entry |
| `created_by` | UUID | NOT NULL, FK→user_profiles | User who recorded payment |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Indexes:**
- `payments_pkey` (UNIQUE) on `id`
- `unique_company_payment_number` (UNIQUE) on `(company_id, payment_number)`
- `idx_payments_company_date` on `(company_id, payment_date)`
- `idx_payments_customer` on `customer_id`

---

#### 13. `invoice_payments` - Payment Application (Link Table)

**Purpose:** Many-to-many relationship: one payment can apply to multiple invoices.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Link ID |
| `invoice_id` | BIGINT | NOT NULL, FK→invoices | Invoice being paid |
| `payment_id` | BIGINT | NOT NULL, FK→payments | Payment applied |
| `amount_applied` | NUMERIC(15,2) | NOT NULL, CHECK(>0) | Amount applied to this invoice |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Application timestamp |

**Indexes:**
- `invoice_payments_pkey` (UNIQUE) on `id`
- `idx_invoice_payments_invoice` on `invoice_id`
- `idx_invoice_payments_payment` on `payment_id`

**Example:** Payment of 150,000 applied to 2 invoices:
```sql
-- Payment record
INSERT INTO payments (company_id, customer_id, payment_number, payment_date, amount, payment_method)
VALUES (1, 1, 'PAY-2024-001', '2024-10-19', 150000, 'BANK_TRANSFER');

-- Apply to Invoice #1 (100,000)
INSERT INTO invoice_payments (invoice_id, payment_id, amount_applied)
VALUES (1, LAST_INSERT_ID(), 100000);

-- Apply to Invoice #2 (50,000)
INSERT INTO invoice_payments (invoice_id, payment_id, amount_applied)
VALUES (2, LAST_INSERT_ID(), 50000);
```

---

### Cash & Bank (2 tables)

#### 14. `bank_accounts` - Bank & Cash Accounts

**Purpose:** Manage bank accounts and petty cash with real-time balance tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Account ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `account_id` | BIGINT | NOT NULL, FK→accounts | Linked GL account (111 for cash, 112 for bank) |
| `account_name` | VARCHAR(255) | NOT NULL | Account display name |
| `bank_name` | VARCHAR(255) | NULL | Bank name (if bank account) |
| `account_number` | VARCHAR(50) | NULL | Bank account number |
| `currency` | VARCHAR(3) | DEFAULT 'VND', CHECK | VND or USD |
| `opening_balance` | NUMERIC(15,2) | DEFAULT 0 | Opening balance |
| `current_balance` | NUMERIC(15,2) | DEFAULT 0 | Real-time balance (auto-updated) |
| `is_active` | BOOLEAN | DEFAULT TRUE | Account active status |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Indexes:**
- `bank_accounts_pkey` (UNIQUE) on `id`
- `idx_bank_accounts_company` on `company_id`
- `idx_bank_accounts_account` on `account_id`

**Balance Updates:** Automatically updated by triggers on `cash_transactions`.

---

#### 15. `cash_transactions` - Cash/Bank Movements

**Purpose:** Record deposits, withdrawals, and transfers between bank/cash accounts.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Transaction ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `bank_account_id` | BIGINT | NOT NULL, FK→bank_accounts | Affected account |
| `fiscal_period_id` | BIGINT | NOT NULL, FK→fiscal_periods | Period assignment |
| `transaction_number` | VARCHAR(50) | NOT NULL | Transaction # (auto-generated) |
| `transaction_date` | DATE | NOT NULL | Transaction date |
| `transaction_type` | VARCHAR(20) | NOT NULL, CHECK | DEPOSIT, WITHDRAWAL, TRANSFER |
| `amount` | NUMERIC(15,2) | NOT NULL, CHECK(>0) | Transaction amount |
| `description` | TEXT | NOT NULL | Transaction description |
| `reference_no` | VARCHAR(100) | NULL | External reference |
| `balance_after` | NUMERIC(15,2) | NOT NULL | Balance after transaction |
| `journal_entry_id` | BIGINT | FK→journal_entries | Linked GL entry |
| `created_by` | UUID | NOT NULL, FK→user_profiles | User who recorded transaction |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Indexes:**
- `cash_transactions_pkey` (UNIQUE) on `id`
- `idx_cash_transactions_bank_account` on `bank_account_id`
- `idx_cash_transactions_date` on `transaction_date`
- `idx_cash_transactions_type` on `transaction_type`

---

### RAG Platform (2 tables)

#### 16. `vector_documents` - RAG Embeddings Storage

**Purpose:** Store vector embeddings (1536-dim) for all ERP documents to enable semantic search.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Vector document ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `source_table` | VARCHAR(100) | NOT NULL | Source table (invoices, journal_entries, etc.) |
| `source_id` | BIGINT | NOT NULL | Source record ID |
| `content_type` | VARCHAR(50) | NOT NULL | invoice, journal_entry, customer, etc. |
| `content_text` | TEXT | NOT NULL | Text content for embedding |
| `content_tsv` | TSVECTOR | NULL | Full-text search vector (hybrid retrieval) |
| `embedding` | VECTOR(1536) | NULL | OpenAI ada-002 embedding (1536-dim) |
| `metadata` | JSONB | DEFAULT '{}' | Additional context (fiscal_period, account_codes, amounts, etc.) |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update (embedding refresh) |
| `deleted_at` | TIMESTAMP | NULL | Soft delete |

**Unique Constraint:** `(company_id, source_table, source_id)` - One embedding per source record.

**Indexes:**
- `vector_documents_pkey` (UNIQUE) on `id`
- `unique_source_record` (UNIQUE) on `(company_id, source_table, source_id)`
- `idx_vector_docs_company` on `company_id`
- `idx_vector_docs_source` on `(source_table, source_id)`
- `idx_vector_docs_deleted` on `deleted_at`
- `idx_vector_docs_fts` (GIN) on `content_tsv` - Full-text search
- **`idx_vector_docs_embedding_hnsw` (HNSW)** on `embedding` - Vector similarity search (m=16, ef_construction=64)

**Sample Query (Semantic Search):**
```sql
-- Find similar documents (cosine similarity)
SELECT
    id,
    source_table,
    source_id,
    content_text,
    1 - (embedding <=> '<query_embedding>'::vector(1536)) AS similarity
FROM accounting.vector_documents
WHERE company_id = 1
  AND deleted_at IS NULL
ORDER BY embedding <=> '<query_embedding>'::vector(1536)
LIMIT 10;
```

**Performance:** HNSW index enables ~2ms P95 latency for 1,000 vectors (see Task 2 benchmark).

---

#### 17. `rag_queries` - RAG Query Audit Log

**Purpose:** Log all RAG chatbot queries for monitoring, debugging, and performance analysis.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGSERIAL | PK | Query log ID |
| `company_id` | BIGINT | NOT NULL, FK→companies | Multi-tenant key |
| `user_id` | UUID | NOT NULL, FK→user_profiles | User who asked query |
| `query_text` | TEXT | NOT NULL | Natural language query |
| `query_embedding` | VECTOR(1536) | NULL | Query embedding (for recall analysis) |
| `response_text` | TEXT | NULL | LLM-generated response |
| `retrieved_doc_ids` | BIGINT[] | NULL | IDs of retrieved documents |
| `latency_ms` | INTEGER | NULL | End-to-end query latency (ms) |
| `model_name` | VARCHAR(100) | NULL | LLM model used (gpt-4o-mini, etc.) |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Query timestamp |

**Indexes:**
- `rag_queries_pkey` (UNIQUE) on `id`
- `idx_rag_queries_company` on `company_id`
- `idx_rag_queries_user` on `user_id`
- `idx_rag_queries_created` on `created_at`

**Analytics Queries:**
```sql
-- Average query latency by model
SELECT
    model_name,
    AVG(latency_ms) AS avg_latency,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms) AS p95_latency,
    COUNT(*) AS query_count
FROM accounting.rag_queries
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY model_name;
```

---

## Multi-Tenancy Architecture

### Tenant Isolation Strategy

Every business table includes `company_id` as the **first column** in composite indexes:

```sql
-- Example: Index on invoices
CREATE INDEX idx_invoices_company_date ON invoices (company_id, invoice_date);
```

**Benefits:**
- PostgreSQL can efficiently prune partitions by `company_id`
- All queries filter by `company_id` first → fast index scans
- Future: Can partition tables by `company_id` for horizontal scaling

### Row Level Security (RLS)

RLS is **enabled** on all 18 tables. Example policy:

```sql
-- Users can only see data from their own company
CREATE POLICY tenant_isolation ON accounting.invoices
    USING (company_id = current_setting('app.current_company_id')::BIGINT);
```

**Usage:**
```sql
-- Set company context at session start
SET app.current_company_id = 1;

-- Now all queries automatically filter by company_id=1
SELECT * FROM accounting.invoices;
```

---

## Indexes & Performance

### Index Summary

**Total Indexes:** 84
**HNSW Indexes:** 1 (vector_documents.embedding)
**GIN Indexes:** 1 (vector_documents.content_tsv)
**B-tree Indexes:** 82

### Index Naming Convention

- `<table>_pkey` - Primary key
- `idx_<table>_<column>` - Single-column index
- `idx_<table>_<col1>_<col2>` - Composite index
- `unique_<description>` - Unique constraint index

### Critical Composite Indexes

**For fast company-scoped queries:**

| Table | Index | Columns |
|-------|-------|---------|
| accounts | idx_accounts_company_code | (company_id, code) |
| invoices | idx_invoices_company_date | (company_id, invoice_date) |
| journal_entries | idx_journal_entries_company_date | (company_id, entry_date) |
| payments | idx_payments_company_date | (company_id, payment_date) |
| customers | idx_customers_company_code | (company_id, code) |

**Recommendation:** Always include `company_id` in WHERE clauses to leverage these indexes.

---

## Sample Queries

### 1. Balance Sheet Query

```sql
-- Generate Balance Sheet for a specific period
SELECT
    a.code,
    a.name_vn,
    a.account_type,
    COALESCE(ab.closing_balance, 0) AS balance
FROM accounting.accounts a
LEFT JOIN accounting.account_balances ab ON a.id = ab.account_id
LEFT JOIN accounting.fiscal_periods fp ON ab.fiscal_period_id = fp.id
WHERE a.company_id = 1
  AND a.account_type IN ('ASSET', 'LIABILITY', 'EQUITY')
  AND a.is_detail = TRUE
  AND fp.year = 2024
  AND fp.period = 10  -- October
  AND a.deleted_at IS NULL
ORDER BY a.code;
```

### 2. Aging AR Report

```sql
-- Aging report: Invoices grouped by days overdue
SELECT
    c.name AS customer_name,
    i.invoice_number,
    i.invoice_date,
    i.due_date,
    i.total_amount,
    i.paid_amount,
    (i.total_amount - i.paid_amount) AS outstanding,
    CASE
        WHEN CURRENT_DATE <= i.due_date THEN '0-30 days'
        WHEN CURRENT_DATE <= i.due_date + INTERVAL '30 days' THEN '31-60 days'
        WHEN CURRENT_DATE <= i.due_date + INTERVAL '60 days' THEN '61-90 days'
        ELSE 'Over 90 days'
    END AS aging_bucket
FROM accounting.invoices i
JOIN accounting.customers c ON i.customer_id = c.id
WHERE i.company_id = 1
  AND i.status NOT IN ('PAID', 'CANCELLED')
  AND i.deleted_at IS NULL
ORDER BY i.due_date;
```

### 3. Trial Balance

```sql
-- Trial Balance: Sum debits and credits by account
SELECT
    a.code,
    a.name_vn,
    SUM(jel.debit) AS total_debit,
    SUM(jel.credit) AS total_credit,
    SUM(jel.debit) - SUM(jel.credit) AS net_balance
FROM accounting.accounts a
LEFT JOIN accounting.journal_entry_lines jel ON a.id = jel.account_id
LEFT JOIN accounting.journal_entries je ON jel.journal_entry_id = je.id
WHERE je.company_id = 1
  AND je.status = 'POSTED'
  AND je.entry_date BETWEEN '2024-01-01' AND '2024-12-31'
  AND je.deleted_at IS NULL
GROUP BY a.id, a.code, a.name_vn
ORDER BY a.code;
```

### 4. RAG Semantic Search

```sql
-- Find documents similar to a query embedding
-- (Use helper function from Task 2)
SELECT * FROM accounting.search_similar_documents(
    '<query_embedding_1536_dim>'::vector(1536),
    1,  -- company_id
    10  -- limit
);
```

---

## Vietnam Circular 200 Compliance

### Account Code Requirements

**Circular 200/2014/TT-BTC** mandates specific account codes for Vietnamese businesses:

| Code | Name (Vietnamese) | Name (English) | Type |
|------|-------------------|----------------|------|
| 111 | Tiền mặt | Cash | ASSET |
| 112 | Tiền gửi ngân hàng | Bank deposits | ASSET |
| 131 | Phải thu khách hàng | Accounts Receivable | ASSET |
| 331 | Phải trả người bán | Accounts Payable | LIABILITY |
| 33311 | Thuế GTGT phải nộp | VAT Payable | LIABILITY |
| 13311 | Thuế GTGT được khấu trừ | VAT Deductible | ASSET |
| 511 | Doanh thu bán hàng | Sales Revenue | REVENUE |
| 621 | Giá vốn hàng bán | Cost of Goods Sold | EXPENSE |
| 642 | Chi phí nhân viên | Salaries Expense | EXPENSE |

### 4-Level Hierarchy

```
1 (Assets)
└── 11 (Current Assets)
    └── 111 (Cash)
        └── 1111 (Cash VND)
            └── 11111 (Petty Cash - Branch 1)
```

**Max depth:** 4 levels (enforced by `level` CHECK constraint).

### Audit Retention

**Circular 200 requires 10-year retention** of all accounting records. Implementation:

1. **Soft deletes:** `deleted_at` timestamp (never hard DELETE)
2. **Audit logs:** Immutable `audit_logs` table with all changes
3. **Period locking:** LOCKED fiscal periods cannot be modified
4. **Backup strategy:** Daily backups with 10-year retention policy (PM responsibility)

---

## Notes for RAG Embedding Pipeline

### Tables to Embed (Priority Order)

1. **invoices** (High priority) - Revenue transactions
2. **journal_entries** (High priority) - All GL activity
3. **customers** (Medium) - Customer profiles
4. **accounts** (Medium) - Chart of accounts structure
5. **payments** (Medium) - Payment history
6. **cash_transactions** (Low) - Bank movements
7. **fiscal_periods** (Low) - Period metadata

### Embedding Template Example

**Invoice embedding:**
```python
content_text = f"""
Invoice #{invoice_number} dated {invoice_date}
Customer: {customer_name} (Tax Code: {tax_code})
Subtotal: {subtotal:,.0f} VND
VAT ({tax_rate}%): {tax_amount:,.0f} VND
Total: {total_amount:,.0f} VND
Status: {status}
Due Date: {due_date}
Items: {', '.join(line_items)}
"""

metadata = {
    "fiscal_period": f"{year}-{period:02d}",
    "customer_id": customer_id,
    "account_codes": ["131", "511", "33311"],
    "amount_vnd": total_amount,
    "transaction_type": "revenue"
}
```

### Hybrid Search Strategy

Combine **vector similarity** + **full-text search** for best results:

```sql
-- Hybrid search (vector + FTS)
WITH vector_results AS (
    SELECT id, embedding <=> <query_vec> AS distance
    FROM accounting.vector_documents
    WHERE company_id = 1
    ORDER BY distance
    LIMIT 100
),
fts_results AS (
    SELECT id, ts_rank(content_tsv, plainto_tsquery('vietnamese', 'hoá đơn')) AS rank
    FROM accounting.vector_documents
    WHERE company_id = 1
      AND content_tsv @@ plainto_tsquery('vietnamese', 'hoá đơn')
    LIMIT 100
)
SELECT DISTINCT vd.*
FROM accounting.vector_documents vd
WHERE vd.id IN (SELECT id FROM vector_results)
   OR vd.id IN (SELECT id FROM fts_results)
ORDER BY (
    COALESCE((SELECT distance FROM vector_results WHERE id = vd.id), 999) * 0.7 +
    COALESCE(1.0 - (SELECT rank FROM fts_results WHERE id = vd.id), 1.0) * 0.3
)
LIMIT 10;
```

---

## Changelog

**2025-10-19:** Initial schema documentation generated for Preparation Sprint Task 3.

---

**Generated by:** DEV Agent (Sonnet 4.5)
**For:** Preparation Sprint Task 3 - Schema Documentation
**Version:** 1.0.0

-- ============================================================================
-- AI-Powered Accounting Support System
-- Initial Database Schema - Circular 200 Compliant
-- Schema: accounting
-- Version: 1.0
-- Date: October 7, 2025
-- ============================================================================

-- Set search path to accounting schema
SET search_path TO accounting, public;

-- Enable required extensions (extensions are installed at database level)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" SCHEMA public;

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Companies
CREATE TABLE accounting.companies (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL,
                                      name_en VARCHAR(255),
                                      tax_code VARCHAR(50) NOT NULL UNIQUE,
                                      address TEXT,
                                      phone VARCHAR(50),
                                      email VARCHAR(255),
                                      legal_representative VARCHAR(255),
                                      business_type VARCHAR(100),
                                      fiscal_year_start INTEGER DEFAULT 1 CHECK (fiscal_year_start BETWEEN 1 AND 12),
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      deleted_at TIMESTAMP
);

CREATE INDEX idx_companies_tax_code ON accounting.companies(tax_code);
CREATE INDEX idx_companies_deleted_at ON accounting.companies(deleted_at);

COMMENT ON TABLE accounting.companies IS 'Company/organization master data';
COMMENT ON COLUMN accounting.companies.tax_code IS 'Vietnamese tax code (Mã số thuế - MST)';
COMMENT ON COLUMN accounting.companies.fiscal_year_start IS 'Fiscal year start month (1-12)';

-- User Profiles (Supabase Auth)
CREATE TABLE accounting.user_profiles (
                                          user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
                                          company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                          username VARCHAR(100) NOT NULL,
                                          full_name VARCHAR(255) NOT NULL,
                                          role VARCHAR(50) NOT NULL CHECK (role IN ('ADMIN', 'ACCOUNTANT', 'VIEWER')),
                                          is_active BOOLEAN DEFAULT TRUE,
                                          last_login_at TIMESTAMP,
                                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                          deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_profiles_username_lower ON accounting.user_profiles (LOWER(username));
CREATE INDEX idx_user_profiles_company_id ON accounting.user_profiles(company_id);

COMMENT ON TABLE accounting.user_profiles IS 'User profile metadata linked to Supabase auth.users';
COMMENT ON COLUMN accounting.user_profiles.role IS 'User role: ADMIN, ACCOUNTANT, VIEWER';

-- Accounts (Chart of Accounts)
CREATE TABLE accounting.accounts (
                                     id BIGSERIAL PRIMARY KEY,
                                     company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                     code VARCHAR(20) NOT NULL,
                                     name_vn VARCHAR(255) NOT NULL,
                                     name_en VARCHAR(255),
                                     account_type VARCHAR(50) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE', 'OFF_BALANCE')),
                                     account_class INTEGER NOT NULL CHECK (account_class BETWEEN 0 AND 9),
                                     parent_id BIGINT REFERENCES accounting.accounts(id),
                                     level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 4),
                                     is_detail BOOLEAN DEFAULT TRUE,
                                     normal_balance VARCHAR(10) NOT NULL CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
                                     is_active BOOLEAN DEFAULT TRUE,
                                     description TEXT,
                                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMP,
                                     CONSTRAINT unique_company_account_code UNIQUE (company_id, code)
);

CREATE INDEX idx_accounts_company_code ON accounting.accounts(company_id, code);
CREATE INDEX idx_accounts_type ON accounting.accounts(account_type);
CREATE INDEX idx_accounts_parent ON accounting.accounts(parent_id);

COMMENT ON TABLE accounting.accounts IS 'Chart of Accounts - Circular 200 compliant';
COMMENT ON COLUMN accounting.accounts.account_class IS 'Circular 200 account class (0-9)';
COMMENT ON COLUMN accounting.accounts.level IS 'Account hierarchy level (1-4)';
COMMENT ON COLUMN accounting.accounts.is_detail IS 'Whether transactions can be posted to this account';

-- Fiscal Periods
CREATE TABLE accounting.fiscal_periods (
                                           id BIGSERIAL PRIMARY KEY,
                                           company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                           year INTEGER NOT NULL,
                                           period INTEGER NOT NULL CHECK (period BETWEEN 1 AND 12),
                                           period_type VARCHAR(20) NOT NULL CHECK (period_type IN ('MONTHLY', 'QUARTERLY', 'YEARLY')),
                                           start_date DATE NOT NULL,
                                           end_date DATE NOT NULL,
                                           status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'CLOSED', 'LOCKED')),
                                           closed_by UUID REFERENCES accounting.user_profiles(user_id),
                                           closed_at TIMESTAMP,
                                           created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                           CONSTRAINT unique_company_period UNIQUE (company_id, year, period, period_type)
);

CREATE INDEX idx_fiscal_periods_company_year ON accounting.fiscal_periods(company_id, year);
CREATE INDEX idx_fiscal_periods_status ON accounting.fiscal_periods(status);

COMMENT ON TABLE accounting.fiscal_periods IS 'Accounting periods for period closing';
COMMENT ON COLUMN accounting.fiscal_periods.status IS 'Period status: OPEN, CLOSED, LOCKED';

-- ============================================================================
-- GENERAL LEDGER TABLES
-- ============================================================================

-- Journal Entries
CREATE TABLE accounting.journal_entries (
                                            id BIGSERIAL PRIMARY KEY,
                                            company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                            fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                            entry_number VARCHAR(50) NOT NULL,
                                            entry_date DATE NOT NULL,
                                            entry_type VARCHAR(50) NOT NULL,
                                            description TEXT NOT NULL,
                                            reference_no VARCHAR(100),
                                            reference_type VARCHAR(50),
                                            reference_id BIGINT,
                                            status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED')),
                                            reversed_by BIGINT REFERENCES accounting.journal_entries(id),
                                            created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                            posted_by UUID REFERENCES accounting.user_profiles(user_id),
                                            posted_at TIMESTAMP,
                                            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                            deleted_at TIMESTAMP,
                                            CONSTRAINT unique_company_entry_number UNIQUE (company_id, entry_number)
);

CREATE INDEX idx_journal_entries_company_date ON accounting.journal_entries(company_id, entry_date);
CREATE INDEX idx_journal_entries_status ON accounting.journal_entries(status);
CREATE INDEX idx_journal_entries_reference ON accounting.journal_entries(reference_type, reference_id);
CREATE INDEX idx_journal_entries_fiscal_period ON accounting.journal_entries(fiscal_period_id);

COMMENT ON TABLE accounting.journal_entries IS 'General ledger journal entries';
COMMENT ON COLUMN accounting.journal_entries.status IS 'Entry status: DRAFT, POSTED, REVERSED';

-- Journal Entry Lines
CREATE TABLE accounting.journal_entry_lines (
                                                id BIGSERIAL PRIMARY KEY,
                                                journal_entry_id BIGINT NOT NULL REFERENCES accounting.journal_entries(id) ON DELETE CASCADE,
                                                account_id BIGINT NOT NULL REFERENCES accounting.accounts(id),
                                                debit DECIMAL(19,2) NOT NULL DEFAULT 0 CHECK (debit >= 0),
                                                credit DECIMAL(19,2) NOT NULL DEFAULT 0 CHECK (credit >= 0),
                                                description TEXT,
                                                line_order INTEGER NOT NULL,
                                                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                                CONSTRAINT check_debit_or_credit CHECK (
                                                    (debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)
                                                    )
);

CREATE INDEX idx_journal_entry_lines_entry ON accounting.journal_entry_lines(journal_entry_id);
CREATE INDEX idx_journal_entry_lines_account ON accounting.journal_entry_lines(account_id);

COMMENT ON TABLE accounting.journal_entry_lines IS 'Individual debit/credit lines for journal entries';
COMMENT ON CONSTRAINT check_debit_or_credit ON accounting.journal_entry_lines IS 'A line must have either debit or credit, not both';

-- Account Balances
CREATE TABLE accounting.account_balances (
                                             id BIGSERIAL PRIMARY KEY,
                                             account_id BIGINT NOT NULL REFERENCES accounting.accounts(id),
                                             fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                             opening_balance DECIMAL(19,2) DEFAULT 0,
                                             debit_total DECIMAL(19,2) DEFAULT 0,
                                             credit_total DECIMAL(19,2) DEFAULT 0,
                                             closing_balance DECIMAL(19,2) DEFAULT 0,
                                             updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                             CONSTRAINT unique_account_period_balance UNIQUE (account_id, fiscal_period_id)
);

CREATE INDEX idx_account_balances_account ON accounting.account_balances(account_id);
CREATE INDEX idx_account_balances_period ON accounting.account_balances(fiscal_period_id);

COMMENT ON TABLE accounting.account_balances IS 'Pre-calculated account balances by period for performance';

-- ============================================================================
-- ACCOUNTS RECEIVABLE TABLES
-- ============================================================================

-- Customers
CREATE TABLE accounting.customers (
                                      id BIGSERIAL PRIMARY KEY,
                                      company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                      code VARCHAR(50) NOT NULL,
                                      name VARCHAR(255) NOT NULL,
                                      tax_code VARCHAR(50),
                                      address TEXT,
                                      phone VARCHAR(50),
                                      email VARCHAR(255),
                                      contact_person VARCHAR(255),
                                      credit_limit DECIMAL(19,2) DEFAULT 0,
                                      payment_terms INTEGER DEFAULT 30,
                                      account_id BIGINT REFERENCES accounting.accounts(id),
                                      is_active BOOLEAN DEFAULT TRUE,
                                      notes TEXT,
                                      created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                      deleted_at TIMESTAMP,
                                      CONSTRAINT unique_company_customer_code UNIQUE (company_id, code)
);

CREATE INDEX idx_customers_company_code ON accounting.customers(company_id, code);
CREATE INDEX idx_customers_name ON accounting.customers(name);
CREATE INDEX idx_customers_tax_code ON accounting.customers(tax_code);

COMMENT ON TABLE accounting.customers IS 'Customer master data';
COMMENT ON COLUMN accounting.customers.account_id IS 'Linked AR account (typically 131)';

-- Invoices
CREATE TABLE accounting.invoices (
                                     id BIGSERIAL PRIMARY KEY,
                                     company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                     customer_id BIGINT NOT NULL REFERENCES accounting.customers(id),
                                     fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                     invoice_number VARCHAR(50) NOT NULL,
                                     invoice_date DATE NOT NULL,
                                     due_date DATE NOT NULL,
                                     subtotal DECIMAL(19,2) NOT NULL CHECK (subtotal >= 0),
                                     tax_rate DECIMAL(5,2) DEFAULT 10.00,
                                     tax_amount DECIMAL(19,2) NOT NULL CHECK (tax_amount >= 0),
                                     total_amount DECIMAL(19,2) NOT NULL CHECK (total_amount >= 0),
                                     paid_amount DECIMAL(19,2) DEFAULT 0 CHECK (paid_amount >= 0),
                                     status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'PARTIAL', 'OVERDUE', 'CANCELLED')),
                                     notes TEXT,
                                     journal_entry_id BIGINT REFERENCES accounting.journal_entries(id),
                                     created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMP,
                                     CONSTRAINT unique_company_invoice_number UNIQUE (company_id, invoice_number),
                                     CONSTRAINT check_paid_amount CHECK (paid_amount <= total_amount)
);

CREATE INDEX idx_invoices_company_date ON accounting.invoices(company_id, invoice_date);
CREATE INDEX idx_invoices_customer ON accounting.invoices(customer_id);
CREATE INDEX idx_invoices_status ON accounting.invoices(status);
CREATE INDEX idx_invoices_due_date ON accounting.invoices(due_date);

COMMENT ON TABLE accounting.invoices IS 'Sales invoices (Accounts Receivable)';
COMMENT ON COLUMN accounting.invoices.status IS 'Invoice status: DRAFT, SENT, PAID, PARTIAL, OVERDUE, CANCELLED';

-- Invoice Lines
CREATE TABLE accounting.invoice_lines (
                                          id BIGSERIAL PRIMARY KEY,
                                          invoice_id BIGINT NOT NULL REFERENCES accounting.invoices(id) ON DELETE CASCADE,
                                          description VARCHAR(500) NOT NULL,
                                          quantity DECIMAL(19,4) NOT NULL CHECK (quantity > 0),
                                          unit_price DECIMAL(19,2) NOT NULL CHECK (unit_price >= 0),
                                          tax_rate DECIMAL(5,2) DEFAULT 10.00,
                                          tax_amount DECIMAL(19,2) NOT NULL CHECK (tax_amount >= 0),
                                          line_total DECIMAL(19,2) NOT NULL CHECK (line_total >= 0),
                                          line_order INTEGER NOT NULL,
                                          created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_lines_invoice ON accounting.invoice_lines(invoice_id);

COMMENT ON TABLE accounting.invoice_lines IS 'Invoice line items';

-- ============================================================================
-- ACCOUNTS PAYABLE TABLES
-- ============================================================================

-- Vendors
CREATE TABLE accounting.vendors (
                                    id BIGSERIAL PRIMARY KEY,
                                    company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                    code VARCHAR(50) NOT NULL,
                                    name VARCHAR(255) NOT NULL,
                                    name_en VARCHAR(255),
                                    tax_code VARCHAR(50),
                                    address TEXT,
                                    phone VARCHAR(50),
                                    email VARCHAR(255),
                                    contact_person VARCHAR(255),
                                    payment_terms INTEGER DEFAULT 30,
                                    account_id BIGINT REFERENCES accounting.accounts(id),
                                    is_active BOOLEAN DEFAULT TRUE,
                                    notes TEXT,
                                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                    deleted_at TIMESTAMP,
                                    CONSTRAINT unique_company_vendor_code UNIQUE (company_id, code)
);

CREATE INDEX idx_vendors_company_code ON accounting.vendors(company_id, code);
CREATE INDEX idx_vendors_name ON accounting.vendors(name);
CREATE INDEX idx_vendors_tax_code ON accounting.vendors(tax_code);

COMMENT ON TABLE accounting.vendors IS 'Vendor master data';
COMMENT ON COLUMN accounting.vendors.account_id IS 'Linked AP account (typically 331)';

-- Bills
CREATE TABLE accounting.bills (
                                  id BIGSERIAL PRIMARY KEY,
                                  company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                  vendor_id BIGINT NOT NULL REFERENCES accounting.vendors(id),
                                  fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                  bill_number VARCHAR(50) NOT NULL,
                                  bill_date DATE NOT NULL,
                                  due_date DATE NOT NULL,
                                  subtotal DECIMAL(19,2) NOT NULL CHECK (subtotal >= 0),
                                  tax_rate DECIMAL(5,2) DEFAULT 10.00,
                                  tax_amount DECIMAL(19,2) NOT NULL CHECK (tax_amount >= 0),
                                  total_amount DECIMAL(19,2) NOT NULL CHECK (total_amount >= 0),
                                  paid_amount DECIMAL(19,2) DEFAULT 0 CHECK (paid_amount >= 0),
                                  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'RECEIVED', 'APPROVED', 'PAID', 'PARTIAL', 'CANCELLED')),
                                  notes TEXT,
                                  journal_entry_id BIGINT REFERENCES accounting.journal_entries(id),
                                  created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                  deleted_at TIMESTAMP,
                                  CONSTRAINT unique_company_bill_number UNIQUE (company_id, bill_number),
                                  CONSTRAINT check_bill_paid_amount CHECK (paid_amount <= total_amount)
);

CREATE INDEX idx_bills_company_date ON accounting.bills(company_id, bill_date);
CREATE INDEX idx_bills_vendor ON accounting.bills(vendor_id);
CREATE INDEX idx_bills_status ON accounting.bills(status);
CREATE INDEX idx_bills_due_date ON accounting.bills(due_date);

COMMENT ON TABLE accounting.bills IS 'Vendor bills / Accounts Payable obligations';
COMMENT ON COLUMN accounting.bills.status IS 'Bill status: DRAFT, RECEIVED, APPROVED, PAID, PARTIAL, CANCELLED';

-- Bill Lines
CREATE TABLE accounting.bill_lines (
                                       id BIGSERIAL PRIMARY KEY,
                                       bill_id BIGINT NOT NULL REFERENCES accounting.bills(id) ON DELETE CASCADE,
                                       description VARCHAR(500) NOT NULL,
                                       quantity DECIMAL(19,4) NOT NULL CHECK (quantity > 0),
                                       unit_price DECIMAL(19,2) NOT NULL CHECK (unit_price >= 0),
                                       tax_rate DECIMAL(5,2) DEFAULT 10.00,
                                       tax_amount DECIMAL(19,2) NOT NULL CHECK (tax_amount >= 0),
                                       line_total DECIMAL(19,2) NOT NULL CHECK (line_total >= 0),
                                       line_order INTEGER NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bill_lines_bill ON accounting.bill_lines(bill_id);

COMMENT ON TABLE accounting.bill_lines IS 'Bill line items';

-- Vendor Payments
CREATE TABLE accounting.vendor_payments (
                                            id BIGSERIAL PRIMARY KEY,
                                            company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                            vendor_id BIGINT NOT NULL REFERENCES accounting.vendors(id),
                                            fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                            payment_number VARCHAR(50) NOT NULL,
                                            payment_date DATE NOT NULL,
                                            amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
                                            payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHECK', 'CREDIT_CARD', 'OTHER')),
                                            bank_account_id BIGINT,
                                            reference_no VARCHAR(100),
                                            notes TEXT,
                                            journal_entry_id BIGINT REFERENCES accounting.journal_entries(id),
                                            created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                            deleted_at TIMESTAMP,
                                            CONSTRAINT unique_company_vendor_payment_number UNIQUE (company_id, payment_number)
);

CREATE INDEX idx_vendor_payments_company_date ON accounting.vendor_payments(company_id, payment_date);
CREATE INDEX idx_vendor_payments_vendor ON accounting.vendor_payments(vendor_id);

COMMENT ON TABLE accounting.vendor_payments IS 'Payments issued to vendors';
COMMENT ON COLUMN accounting.vendor_payments.payment_method IS 'Payment method: CASH, BANK_TRANSFER, CHECK, CREDIT_CARD, OTHER';

-- Bill Payments (Many-to-Many)
CREATE TABLE accounting.bill_payments (
                                          id BIGSERIAL PRIMARY KEY,
                                          bill_id BIGINT NOT NULL REFERENCES accounting.bills(id),
                                          payment_id BIGINT NOT NULL REFERENCES accounting.vendor_payments(id),
                                          amount_applied DECIMAL(19,2) NOT NULL CHECK (amount_applied > 0),
                                          created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bill_payments_bill ON accounting.bill_payments(bill_id);
CREATE INDEX idx_bill_payments_payment ON accounting.bill_payments(payment_id);

COMMENT ON TABLE accounting.bill_payments IS 'Links vendor payments to bills (many-to-many relationship)';

-- Payments
CREATE TABLE accounting.payments (
                                     id BIGSERIAL PRIMARY KEY,
                                     company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                     customer_id BIGINT NOT NULL REFERENCES accounting.customers(id),
                                     fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                     payment_number VARCHAR(50) NOT NULL,
                                     payment_date DATE NOT NULL,
                                     amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
                                     payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('CASH', 'BANK_TRANSFER', 'CHECK', 'CREDIT_CARD', 'OTHER')),
                                     bank_account_id BIGINT,
                                     reference_no VARCHAR(100),
                                     notes TEXT,
                                     journal_entry_id BIGINT REFERENCES accounting.journal_entries(id),
                                     created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMP,
                                     CONSTRAINT unique_company_payment_number UNIQUE (company_id, payment_number)
);

CREATE INDEX idx_payments_company_date ON accounting.payments(company_id, payment_date);
CREATE INDEX idx_payments_customer ON accounting.payments(customer_id);

COMMENT ON TABLE accounting.payments IS 'Customer payments';
COMMENT ON COLUMN accounting.payments.payment_method IS 'Payment method: CASH, BANK_TRANSFER, CHECK, CREDIT_CARD, OTHER';

-- Invoice Payments (Many-to-Many)
CREATE TABLE accounting.invoice_payments (
                                             id BIGSERIAL PRIMARY KEY,
                                             invoice_id BIGINT NOT NULL REFERENCES accounting.invoices(id),
                                             payment_id BIGINT NOT NULL REFERENCES accounting.payments(id),
                                             amount_applied DECIMAL(19,2) NOT NULL CHECK (amount_applied > 0),
                                             created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_payments_invoice ON accounting.invoice_payments(invoice_id);
CREATE INDEX idx_invoice_payments_payment ON accounting.invoice_payments(payment_id);

COMMENT ON TABLE accounting.invoice_payments IS 'Links payments to invoices (many-to-many relationship)';

-- ============================================================================
-- CASH/BANK TABLES
-- ============================================================================

-- Bank Accounts
CREATE TABLE accounting.bank_accounts (
                                          id BIGSERIAL PRIMARY KEY,
                                          company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                          account_id BIGINT NOT NULL REFERENCES accounting.accounts(id),
                                          account_name VARCHAR(255) NOT NULL,
                                          bank_name VARCHAR(255),
                                          account_number VARCHAR(100),
                                          currency VARCHAR(3) DEFAULT 'VND' CHECK (currency IN ('VND', 'USD')),
                                          opening_balance DECIMAL(19,2) DEFAULT 0,
                                          current_balance DECIMAL(19,2) DEFAULT 0,
                                          is_active BOOLEAN DEFAULT TRUE,
                                          created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                          updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                          deleted_at TIMESTAMP
);

CREATE INDEX idx_bank_accounts_company ON accounting.bank_accounts(company_id);
CREATE INDEX idx_bank_accounts_account ON accounting.bank_accounts(account_id);

COMMENT ON TABLE accounting.bank_accounts IS 'Bank and cash accounts';
COMMENT ON COLUMN accounting.bank_accounts.account_id IS 'Linked GL account (111 for cash, 112 for bank)';

-- Add foreign key to payments (circular reference)
ALTER TABLE accounting.payments ADD CONSTRAINT fk_payments_bank_account
    FOREIGN KEY (bank_account_id) REFERENCES accounting.bank_accounts(id);

ALTER TABLE accounting.vendor_payments ADD CONSTRAINT fk_vendor_payments_bank_account
    FOREIGN KEY (bank_account_id) REFERENCES accounting.bank_accounts(id);

-- Cash Transactions
CREATE TABLE accounting.cash_transactions (
                                              id BIGSERIAL PRIMARY KEY,
                                              company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
                                              bank_account_id BIGINT NOT NULL REFERENCES accounting.bank_accounts(id),
                                              fiscal_period_id BIGINT NOT NULL REFERENCES accounting.fiscal_periods(id),
                                              transaction_number VARCHAR(50) NOT NULL,
                                              transaction_date DATE NOT NULL,
                                              transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
                                              amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
                                              description TEXT NOT NULL,
                                              reference_no VARCHAR(100),
                                              balance_after DECIMAL(19,2) NOT NULL,
                                              journal_entry_id BIGINT REFERENCES accounting.journal_entries(id),
                                              created_by UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
                                              created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                              updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                              deleted_at TIMESTAMP
);

CREATE INDEX idx_cash_transactions_bank_account ON accounting.cash_transactions(bank_account_id);
CREATE INDEX idx_cash_transactions_date ON accounting.cash_transactions(transaction_date);
CREATE INDEX idx_cash_transactions_type ON accounting.cash_transactions(transaction_type);

COMMENT ON TABLE accounting.cash_transactions IS 'Cash and bank transactions';
COMMENT ON COLUMN accounting.cash_transactions.transaction_type IS 'Transaction type: DEPOSIT, WITHDRAWAL, TRANSFER';

-- ============================================================================
-- AUDIT TABLE
-- ============================================================================

-- Audit Logs
CREATE TABLE accounting.audit_logs (
                                       id BIGSERIAL PRIMARY KEY,
                                       company_id BIGINT REFERENCES accounting.companies(id),
                                       user_id UUID REFERENCES accounting.user_profiles(user_id),
                                       table_name VARCHAR(100) NOT NULL,
                                       record_id BIGINT NOT NULL,
                                       action VARCHAR(20) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_table_record ON accounting.audit_logs(table_name, record_id);
CREATE INDEX idx_audit_logs_user ON accounting.audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON accounting.audit_logs(created_at);

COMMENT ON TABLE accounting.audit_logs IS 'Audit trail for all changes';
COMMENT ON COLUMN accounting.audit_logs.action IS 'Action performed: INSERT, UPDATE, DELETE';

-- ============================================================================
-- TRIGGERS AND FUNCTIONS
-- ============================================================================

-- Function: Update updated_at timestamp
CREATE OR REPLACE FUNCTION accounting.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at
CREATE TRIGGER update_companies_timestamp BEFORE UPDATE ON accounting.companies
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_user_profiles_timestamp BEFORE UPDATE ON accounting.user_profiles
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_accounts_timestamp BEFORE UPDATE ON accounting.accounts
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_journal_entries_timestamp BEFORE UPDATE ON accounting.journal_entries
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_customers_timestamp BEFORE UPDATE ON accounting.customers
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_invoices_timestamp BEFORE UPDATE ON accounting.invoices
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_vendors_timestamp BEFORE UPDATE ON accounting.vendors
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_bills_timestamp BEFORE UPDATE ON accounting.bills
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_vendor_payments_timestamp BEFORE UPDATE ON accounting.vendor_payments
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_payments_timestamp BEFORE UPDATE ON accounting.payments
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_bank_accounts_timestamp BEFORE UPDATE ON accounting.bank_accounts
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_cash_transactions_timestamp BEFORE UPDATE ON accounting.cash_transactions
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

CREATE TRIGGER update_account_balances_timestamp BEFORE UPDATE ON accounting.account_balances
    FOR EACH ROW EXECUTE FUNCTION accounting.update_updated_at_column();

-- Function: Normalize username casing and enforce uniqueness
CREATE OR REPLACE FUNCTION accounting.normalize_user_profile_username()
RETURNS TRIGGER AS $$
DECLARE
normalized_username TEXT;
BEGIN
    IF NEW.username IS NULL THEN
        RAISE EXCEPTION 'Username must not be null.' USING ERRCODE = 'not_null_violation';
END IF;

    normalized_username := LOWER(TRIM(NEW.username));

    -- Skip duplicate check if username casing change only
    IF TG_OP = 'UPDATE' AND normalized_username = LOWER(TRIM(OLD.username)) THEN
        NEW.username := normalized_username;
RETURN NEW;
END IF;

    IF EXISTS (
        SELECT 1
        FROM accounting.user_profiles p
        WHERE LOWER(p.username) = normalized_username
          AND (TG_OP = 'INSERT' OR p.user_id <> NEW.user_id)
    ) THEN
        RAISE EXCEPTION 'Username "%" is already taken. Please choose another username.', normalized_username
            USING ERRCODE = 'unique_violation';
END IF;

    NEW.username := normalized_username;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER normalize_user_profiles_username
    BEFORE INSERT OR UPDATE ON accounting.user_profiles
                         FOR EACH ROW EXECUTE FUNCTION accounting.normalize_user_profile_username();

-- Function: Validate journal entry balance (double-entry)
CREATE OR REPLACE FUNCTION accounting.validate_journal_entry_balance()
RETURNS TRIGGER AS $$
DECLARE
total_debit DECIMAL(19,2);
    total_credit DECIMAL(19,2);
    entry_status VARCHAR(20);
BEGIN
    -- Get entry status
SELECT status INTO entry_status
FROM accounting.journal_entries
WHERE id = NEW.journal_entry_id;

-- Only validate if entry is being posted
IF entry_status = 'POSTED' THEN
        -- Calculate totals for the journal entry
SELECT
    COALESCE(SUM(debit), 0),
    COALESCE(SUM(credit), 0)
INTO total_debit, total_credit
FROM accounting.journal_entry_lines
WHERE journal_entry_id = NEW.journal_entry_id;

-- Check if debits equal credits
IF total_debit != total_credit THEN
            RAISE EXCEPTION 'Journal entry must balance: Debit (%) != Credit (%)',
                total_debit, total_credit;
END IF;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_journal_balance
    AFTER INSERT OR UPDATE ON accounting.journal_entry_lines
                        FOR EACH ROW
                        EXECUTE FUNCTION accounting.validate_journal_entry_balance();

-- Function: Update invoice status based on payments
CREATE OR REPLACE FUNCTION accounting.update_invoice_status()
RETURNS TRIGGER AS $$
DECLARE
invoice_total DECIMAL(19,2);
    invoice_paid DECIMAL(19,2);
    invoice_due_date DATE;
    new_status VARCHAR(20);
BEGIN
    -- Get invoice details
SELECT total_amount, paid_amount, due_date
INTO invoice_total, invoice_paid, invoice_due_date
FROM accounting.invoices
WHERE id = NEW.invoice_id;

-- Determine new status
IF invoice_paid >= invoice_total THEN
        new_status := 'PAID';
    ELSIF invoice_paid > 0 THEN
        new_status := 'PARTIAL';
    ELSIF CURRENT_DATE > invoice_due_date THEN
        new_status := 'OVERDUE';
ELSE
        new_status := 'SENT';
END IF;

    -- Update invoice status
UPDATE accounting.invoices
SET status = new_status,
    paid_amount = (
        SELECT COALESCE(SUM(amount_applied), 0)
        FROM accounting.invoice_payments
        WHERE invoice_id = NEW.invoice_id
    )
WHERE id = NEW.invoice_id;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_invoice_payment_status
    AFTER INSERT OR UPDATE ON accounting.invoice_payments
                        FOR EACH ROW
                        EXECUTE FUNCTION accounting.update_invoice_status();

-- ============================================================================
-- VIEWS FOR RAG OPTIMIZATION
-- ============================================================================

-- View: Account Summary for Chatbot
CREATE OR REPLACE VIEW accounting.v_account_summary AS
SELECT
    a.id,
    a.company_id,
    a.code,
    a.name_vn,
    a.name_en,
    a.account_type,
    a.account_class,
    COALESCE(ab.closing_balance, 0) as current_balance,
    fp.year as fiscal_year,
    fp.period as fiscal_period
FROM accounting.accounts a
         LEFT JOIN accounting.account_balances ab ON a.id = ab.account_id
         LEFT JOIN accounting.fiscal_periods fp ON ab.fiscal_period_id = fp.id
WHERE a.deleted_at IS NULL
  AND a.is_active = TRUE;

COMMENT ON VIEW accounting.v_account_summary IS 'Denormalized view for chatbot queries - account balances';

-- View: Customer Aging Report
CREATE OR REPLACE VIEW accounting.v_customer_aging AS
SELECT
    c.id as customer_id,
    c.code as customer_code,
    c.name as customer_name,
    i.id as invoice_id,
    i.invoice_number,
    i.invoice_date,
    i.due_date,
    i.total_amount,
    i.paid_amount,
    (i.total_amount - i.paid_amount) as balance,
    CASE
        WHEN CURRENT_DATE <= i.due_date THEN 'CURRENT'
        WHEN CURRENT_DATE - i.due_date BETWEEN 1 AND 30 THEN '1-30_DAYS'
        WHEN CURRENT_DATE - i.due_date BETWEEN 31 AND 60 THEN '31-60_DAYS'
        WHEN CURRENT_DATE - i.due_date BETWEEN 61 AND 90 THEN '61-90_DAYS'
        ELSE 'OVER_90_DAYS'
        END as aging_bucket,
    CURRENT_DATE - i.due_date as days_overdue
FROM accounting.customers c
         JOIN accounting.invoices i ON c.id = i.customer_id
WHERE i.status IN ('SENT', 'PARTIAL', 'OVERDUE')
  AND i.paid_amount < i.total_amount
  AND c.deleted_at IS NULL
  AND i.deleted_at IS NULL;

COMMENT ON VIEW accounting.v_customer_aging IS 'AR aging report for chatbot queries';

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

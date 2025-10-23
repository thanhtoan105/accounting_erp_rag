# accounting_erp_rag Product Requirements Document (PRD)

**Author:** thanhtoan105
**Date:** 2025-10-16
**Last Updated:** 2025-10-16
**Project Level:** 4
**Project Type:** web
**Target Scale:** Platform/ecosystem - 60-70 stories, 8 epics
**Team Size:** 2 developers
**Timeline:** Phase 1 (4-5 months) + Phase 2 (4-5 months) = 8-10 months total
**Architecture:** Dual-mode (Traditional ERP UI + AI-Powered RAG Chatbot)

---

## Description, Context and Goals

**accounting_erp_rag** is a comprehensive AI-native ERP accounting platform combining traditional ERP functionality with an intelligent RAG (Retrieval-Augmented Generation) chatbot interface. The system provides dual interaction modes: a full-featured traditional web UI for structured accounting workflows and an AI-powered natural language chatbot for instant insights and query-driven operations.

**Core Accounting Platform:** The ERP foundation delivers complete accounting management compliant with Vietnam's Circular 200/2014/TT-BTC standards, covering:
- **System Management:** Multi-tenant company management, user authentication with RBAC (Admin, Chief Accountant, Accountant, Viewer), comprehensive audit logging
- **Chart of Accounts & Bookkeeping:** 4-level hierarchical accounts, double-entry journal entries with automatic balancing, fiscal period management, and statutory book generation
- **Accounts Receivable (AR):** Customer management, sales invoicing, payment tracking, aging analysis with automatic status updates
- **Accounts Payable (AP):** Vendor management, purchase bills, payment scheduling, payable tracking
- **Cash & Bank Management:** Multi-currency support (VND/USD), cash transactions, bank reconciliation, automated balance tracking
- **Tax & E-Invoice Management:** VAT/CIT/PIT calculations, e-invoice integration with Vietnam Tax Authority, XML export for regulatory compliance
- **Financial Reporting:** Balance Sheet, Income Statement, Cash Flow Statement per Circular 200, export to PDF/Excel/XML
- **Period Management:** Accounting period opening/closing, expense-revenue transfers, profit determination, balance forwarding

**AI Intelligence Layer:** The RAG chatbot transforms data access by enabling natural-language queries in Vietnamese or English. Users ask questions like "What is our current AR/AP status?" or "Show outstanding invoices from Q4" and receive grounded, cited answers with source document references. The RAG pipeline leverages vector databases (Supabase Vector) for semantic indexing, cross-module orchestration for complex queries, and LLM-based generation for natural responses—all while maintaining strict audit trails and regulatory compliance.

**Unique Value Proposition:** **accounting_erp_rag** eliminates the traditional trade-off between power and usability. Accountants get full ERP capabilities for transaction recording and compliance, while simultaneously enjoying ChatGPT-like conversational access for instant insights. The system reduces time-to-insight by 60%+, cuts onboarding time through AI-guided learning, and ensures Vietnam regulatory compliance throughout. Built on existing PostgreSQL schema (~60% complete), the platform accelerates development while maintaining enterprise-grade data integrity.

**Target Users:** Accounting staff (transaction recording, reconciliation), finance managers (reporting, analysis), CFOs (strategic insights), and new employees (learning system via AI assistant).

### Deployment Intent

**Phase 1: Core ERP Platform with Basic RAG** (4-5 months) - Establish complete accounting foundation with essential ERP modules (System Management, Chart of Accounts, AR, **AP**, Cash/Bank, **Tax/E-Invoice**, **Financial Reporting**) plus basic RAG chatbot for GL, AR, and AP queries. Target: 5-10 pilot users validating both traditional UI and AI assistant workflows.

**Phase 2: Advanced RAG Intelligence & Production** (4-5 months) - Enhance RAG with cross-module orchestration, complex analytics, multi-turn conversations, and advanced UX features. Complete period management automation. Harden security and compliance for production deployment. Target: General availability for 50+ users with full ERP + AI capabilities.

### Context

**The problem with traditional ERP workflows:** Most ERPs optimize for transaction recording, not answering questions or providing instant insights. To determine "How much does Customer A owe and which invoices have pending VAT?" users navigate AR/AP/GL/VAT screens separately, apply filters, export to Excel, and manually cross-reference—consuming 10-20 minutes per query. Search is keyword-based at best. Supporting documents (e-invoices, contracts, receipts) scatter across email, Google Drive, and chat, increasing audit risk. Knowledge silos in individual employees rather than systems. Month-end close takes days; onboarding requires weeks of training on chart-of-account codes, screen workflows, and document locations.

**The current market gap:** Existing Vietnamese accounting software provides traditional UI but lacks:
1. **AI-powered insights:** No semantic search, no natural language queries, no ChatGPT-like assistant
2. **Modern UX:** Cluttered interfaces optimized for desktops, not mobile/tablet
3. **Developer-friendly:** Closed ecosystems, no API-first design, difficult integration
4. **Compliance automation:** Manual e-invoice handling, manual tax XML generation

**Why now:** Vietnam's mandatory e-invoicing (2022+) creates structured, machine-readable data perfect for RAG indexing. LLMs (GPT-4, Claude) with RAG deliver cited, grounded answers with dramatically reduced hallucinations. Vector databases (Supabase Vector/pgvector) and affordable cloud infrastructure make low-latency RAG MVPs economically feasible. Accounting teams demand near-real-time reporting while regulatory pressure (Circular 200/2014/TT-BTC) increases. Strategic window exists for an AI-native ERP that provides immediate, defensible business value through dual-mode interaction.

**Competitive advantage:** Most "AI accounting" tools are chatbots over existing ERPs—not integrated platforms. **accounting_erp_rag** is AI-native from day one: schema designed for RAG optimization (denormalized views), audit logs capture query lineage, and UX seamlessly blends traditional forms with conversational AI. Built on Supabase (PostgreSQL + Auth + Vector), the platform offers developer-friendly APIs, modern stack, and Vietnam-specific compliance built-in.

### Goals

**Phase 1 Goals (Core ERP + Basic RAG):**

1. **Complete accounting foundation**: Deliver fully functional ERP modules for System Management, Chart of Accounts, GL, AR, **AP**, Cash/Bank, **Tax/E-Invoice**, and **Financial Reporting** compliant with Vietnam Circular 200/2014/TT-BTC standards.

2. **Basic RAG operational**: Index ≥ 80% of GL, AR, and AP documents with retrieval recall@10 ≥ 0.90, answer groundedness ≥ 0.80, and ≥ 95% citation coverage. Support natural language queries in Vietnamese and English.

3. **Reduce time-to-insight (RAG)**: Achieve P95 response time ≤ 5 seconds for typical queries and cut task completion time by ≥ 60% compared to traditional navigation.

4. **User validation**: ≥ 70% of 5-10 pilot users successfully complete core accounting tasks (journal entry, invoicing, payment recording) AND core RAG queries without assistance, with CSAT ≥ 4.0/5.0.

5. **Data integrity**: Zero data loss, 100% double-entry validation, automated audit logging for all transactions, and accurate financial reports matching manual calculations.

**Phase 2 Goals (Advanced RAG + Production):**

6. **Advanced RAG intelligence**: Support cross-module queries (e.g., "cash position and payment obligations"), multi-turn conversations, query disambiguation, and complex analytics with correctness ≥ 0.75.

7. **Production readiness**: Achieve 99% uptime during business hours, pass security penetration testing, complete Vietnam compliance audit, and operational runbook validated.

8. **Period management automation**: Automate period closing workflows, expense-revenue transfers, and profit determination with validation against ERP-generated reports (≤ 1% divergence threshold).

9. **Scale and performance**: Support 50+ concurrent users, handle 500K+ indexed documents, maintain P95 latency ≤ 5 sec under load, and cost per query ≤ $0.10.

10. **Developer ecosystem**: Provide REST API documentation, webhook support for integrations, and export capabilities (PDF/Excel/XML) for downstream systems.

## Requirements

### Functional Requirements

**Requirements Organization:** The system is organized into 9 major functional modules. Phase 1 focuses on modules 1-7 + basic Module 9 (RAG). Phase 2 completes Module 8 (Period Management) and advances Module 9 (Advanced RAG).

---

#### Module 1: System Management & Access Control

**FR-1.1: User Management**
- User registration with email verification
- Login with username/password (Supabase Auth)
- Two-factor authentication (2FA) via SMS or authenticator app
- Role-based permissions: Admin, Chief Accountant, Accountant, Viewer
- User access history tracking (last login, IP address, session duration)
- Account activation/deactivation by administrators

**FR-1.2: Role-Based Access Control (RBAC)**
- **Admin**: Full system access, user management, company configuration, all data modules
- **Chief Accountant**: All accounting operations, period closing, report generation, limited user management
- **Accountant**: Transaction recording (journal entries, invoices, payments), report viewing
- **Viewer**: Read-only access to reports and data (no transaction recording)
- Permission enforcement at API and UI levels
- Audit log records all permission checks and access denials

**FR-1.3: Audit Logging**
- Record all CREATE, UPDATE, DELETE operations across all tables
- Capture: user ID, timestamp, IP address, table name, record ID, old values (JSONB), new values (JSONB), action type
- Immutable audit trail (append-only, no deletions)
- Audit log retention: minimum 10 years per Vietnam regulations
- Support audit export in JSON/CSV format with schema documentation
- UI for administrators to search and filter audit logs

**FR-1.4: Company Management**
- Multi-tenant architecture: each company isolated by company_id
- Configure business information: name, tax ID (Mã số thuế - MST), address, phone, email, legal representative
- Business type classification
- Fiscal year settings: start month (1-12), configurable accounting periods
- System parameters: default currency, date format, number format, tax rates
- Company logo upload for reports and invoices

**FR-1.5: Session Management**
- Secure session tokens (JWT via Supabase Auth)
- Configurable session timeout (default: 8 hours)
- Automatic logout after inactivity
- "Remember me" option for trusted devices
- Force logout capability by administrators
- Concurrent session handling (allow/deny multiple sessions per user)

---

#### Module 2: Chart of Accounts & Bookkeeping

**FR-2.1: Account Management (Circular 200 Compliant)**
- 4-level hierarchical account structure (Level 1: Class → Level 4: Detail)
- Account classification: Assets (1), Liabilities (2), Equity (3), Revenue (5), Expenses (6), Off-balance sheet (0, 8, 9)
- Account types: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, OFF_BALANCE
- Normal balance designation: DEBIT or CREDIT
- Parent-child account relationships with navigation
- Detail accounts (can post transactions) vs. summary accounts (aggregate only)
- Account activation/deactivation (soft delete, preserves history)
- Custom account creation for business-specific needs
- Account code uniqueness per company with validation
- Search and filter: by code, name, type, class, active status

**FR-2.2: Fiscal Period Management**
- Create fiscal periods by month, quarter, or year
- Period status workflow: OPEN → CLOSED → LOCKED
- Prevent posting to closed/locked periods (configurable override for Chief Accountant)
- Fiscal year configuration: start month, end month
- Period opening balance calculation from previous period closing
- Period closing validation: ensure all entries balanced, no draft entries
- Locked period protection: no modifications allowed (audit compliance)

**FR-2.3: Journal Entry Recording**
- Create manual journal entries with entry number, date, description, reference
- Entry types: General Journal, Adjusting Entry, Closing Entry, Opening Entry, Reversal
- Multi-line entries: unlimited debit/credit lines per entry
- Entry status workflow: DRAFT → POSTED → REVERSED
- Automatic debit-credit balance validation: total debits must equal total credits
- Prevent posting unbalanced entries (enforced at database trigger level)
- Line-level description and account selection
- Entry reversal: create offsetting entry with reference to original
- Attachment support: link scanned receipts, contracts, supporting documents
- Entry templates for recurring transactions (rent, salaries, depreciation)

**FR-2.4: Transaction Validation & Business Rules**
- Prevent posting to inactive accounts
- Prevent posting to summary accounts (must use detail accounts)
- Validate account normal balance warnings (e.g., credit balance in cash account)
- Date validation: entry date must fall within open fiscal period
- Enforce account code existence before posting
- Automatic audit log creation for all journal entry operations
- Double-entry integrity enforced at trigger level (debits = credits)

**FR-2.5: Account Balance Tracking**
- Pre-calculated account balances by fiscal period (performance optimization)
- Real-time balance updates when entries posted
- Opening balance, debit total, credit total, closing balance per period
- Balance calculation: opening + debits - credits = closing (for debit normal balance accounts)
- Balance calculation: opening + credits - debits = closing (for credit normal balance accounts)
- Historical balance retention across all periods

**FR-2.6: Accounting Books & Ledgers**
- **General Journal**: Chronological listing of all journal entries with entry number, date, description, debits, credits
- **General Ledger**: Account-by-account transaction history with running balance
- **Subsidiary Ledger**: Detailed ledger for specific account codes (e.g., 131 - AR, 331 - AP)
- **Trial Balance**: Summary of all account balances (debits and credits) for a period, validates balance
- Export all books to PDF and Excel with company header, period, and page numbers
- Circular 200 formatting compliance for statutory books

---

#### Module 3: Accounts Receivable (AR)

**FR-3.1: Customer Management**
- Create and manage customer master data: code, name, tax ID, address, phone, email, contact person
- Credit limit configuration per customer with warnings when exceeded
- Payment terms: configurable days (e.g., 30 days, 60 days, net 15)
- Link customer to AR account (typically account 131)
- Customer activation/deactivation (soft delete)
- Customer notes field for additional information
- Search and filter: by code, name, tax ID, active status
- Customer transaction history view: all invoices and payments

**FR-3.2: Sales Invoice Creation**
- Create invoices with invoice number (auto-generated or manual), invoice date, due date
- Select customer from master list
- Multi-line invoices: description, quantity, unit price, tax rate, line total
- Automatic subtotal, tax amount, and total calculation
- Tax rate configurable per line (default 10% VAT, support 0%, 5%, 8%)
- Invoice status workflow: DRAFT → SENT → PAID / PARTIAL / OVERDUE / CANCELLED
- Automatic due date calculation based on customer payment terms
- Invoice notes field
- Print invoice in Vietnam standard format with company logo
- Export invoice to PDF with e-invoice reference number

**FR-3.3: Payment Recording**
- Record customer payments with payment number, payment date, amount, payment method
- Payment methods: CASH, BANK_TRANSFER, CHECK, CREDIT_CARD, OTHER
- Link payment to bank account (if applicable)
- Reference number field for bank transfer reference
- Many-to-many invoice-payment allocation: one payment can apply to multiple invoices, one invoice can receive multiple payments
- Automatic paid_amount update on invoices when payment applied
- Automatic invoice status update: SENT → PARTIAL (if partially paid) → PAID (if fully paid)
- Payment notes field

**FR-3.4: AR Aging Analysis**
- Generate AR aging report: Current, 1-30 days, 31-60 days, 61-90 days, Over 90 days
- Aging calculation based on invoice due date vs. current date
- Display customer code, name, invoice number, invoice date, due date, total amount, paid amount, balance, days overdue
- Filter by aging bucket, customer, date range
- Export aging report to Excel and PDF
- Automatic OVERDUE status update for invoices past due date (database trigger)

**FR-3.5: AR Reports**
- Customer balance report: outstanding balance by customer
- Invoice register: chronological list of all invoices with status
- Payment register: chronological list of all payments received
- Customer transaction history: all invoices and payments for a customer with running balance
- All reports exportable to PDF and Excel

**FR-3.6: Automatic Journal Entry Integration**
- Automatically generate journal entry when invoice is posted:
  - Debit: AR account (131-Customer)
  - Credit: Revenue account (511)
  - Credit: VAT Payable (33311)
- Automatically generate journal entry when payment is received:
  - Debit: Cash/Bank account (111 or 112)
  - Credit: AR account (131-Customer)
- Link invoice and payment to generated journal entries (reference tracking)

---

#### Module 4: Accounts Payable (AP) - NEW

**FR-4.1: Vendor Management**
- Create and manage vendor master data: code, name, tax ID, address, phone, email, contact person
- Payment terms: configurable days (e.g., 30 days, 60 days, net 30)
- Link vendor to AP account (typically account 331)
- Vendor activation/deactivation (soft delete)
- Vendor notes field for procurement information
- Search and filter: by code, name, tax ID, active status
- Vendor transaction history view: all bills and payments

**FR-4.2: Purchase Bill Entry**
- Create bills (purchase invoices) with bill number, bill date, due date
- Select vendor from master list
- Multi-line bills: description, quantity, unit price, tax rate, line total
- Automatic subtotal, tax amount, and total calculation
- Tax rate configurable per line (default 10% VAT for deductible input, support 0%, 5%, 8%)
- Bill status workflow: DRAFT → APPROVED → PAID / PARTIAL / OVERDUE / CANCELLED
- Automatic due date calculation based on vendor payment terms
- Bill notes field
- Attachment support: link scanned vendor invoices

**FR-4.3: Vendor Payment Processing**
- Record vendor payments with payment number, payment date, amount, payment method
- Payment methods: CASH, BANK_TRANSFER, CHECK, WIRE_TRANSFER, OTHER
- Link payment to bank account (if applicable)
- Reference number field for bank transfer/check reference
- Many-to-many bill-payment allocation: one payment can apply to multiple bills, one bill can receive multiple payments
- Automatic paid_amount update on bills when payment applied
- Automatic bill status update: APPROVED → PARTIAL (if partially paid) → PAID (if fully paid)
- Payment approval workflow (optional): Chief Accountant approval required for payments over threshold
- Payment notes field

**FR-4.4: AP Aging Analysis**
- Generate AP aging report: Current, 1-30 days, 31-60 days, 61-90 days, Over 90 days
- Aging calculation based on bill due date vs. current date
- Display vendor code, name, bill number, bill date, due date, total amount, paid amount, balance, days overdue
- Filter by aging bucket, vendor, date range
- Export aging report to Excel and PDF
- Automatic OVERDUE status update for bills past due date (database trigger)
- Payment priority dashboard: bills due in next 7 days, 14 days, 30 days

**FR-4.5: AP Reports**
- Vendor balance report: outstanding payables by vendor
- Bill register: chronological list of all bills with status
- Payment register: chronological list of all payments made to vendors
- Vendor transaction history: all bills and payments for a vendor with running balance
- Cash flow forecast: upcoming payment obligations by week/month
- All reports exportable to PDF and Excel

**FR-4.6: Automatic Journal Entry Integration**
- Automatically generate journal entry when bill is approved:
  - Debit: Expense account (621-Costs, 627-Expenses, etc.)
  - Debit: VAT Deductible (13311)
  - Credit: AP account (331-Vendor)
- Automatically generate journal entry when payment is made:
  - Debit: AP account (331-Vendor)
  - Credit: Cash/Bank account (111 or 112)
- Link bill and payment to generated journal entries (reference tracking)

---

#### Module 5: Cash & Bank Management

**FR-5.1: Bank/Cash Account Setup**
- Create and manage cash and bank accounts linked to GL accounts (111-Cash, 112-Bank deposits)
- Configure account name, bank name, account number, currency (VND/USD)
- Opening balance and current balance tracking
- Account activation/deactivation (soft delete)
- Support multiple cash accounts (petty cash, main cash) and multiple bank accounts

**FR-5.2: Cash Transaction Recording**
- Record cash transactions: DEPOSIT (cash in), WITHDRAWAL (cash out), TRANSFER (between accounts)
- Transaction fields: transaction number, date, amount, description, reference number
- Automatic balance calculation: balance_after = previous_balance ± amount
- Prevent negative balances (configurable warning vs. hard block)
- Link transaction to fiscal period
- Transaction approval workflow (optional)

**FR-5.3: Bank Transaction Recording**
- Record bank transactions: DEPOSIT (bank in), WITHDRAWAL (bank out), TRANSFER (between banks or bank-to-cash)
- Import bank statements (CSV/Excel format) with automatic transaction creation
- Transaction matching: link imported bank transactions to pending invoices/bills for reconciliation
- Bank fees and charges recording

**FR-5.4: Bank Reconciliation**
- Compare book balance (accounting records) vs. bank statement balance
- Identify unreconciled transactions: outstanding checks, deposits in transit, bank errors
- Mark transactions as reconciled with reconciliation date
- Reconciliation status dashboard: fully reconciled, partially reconciled, discrepancies
- Discrepancy investigation workflow: flag mismatches, add adjustment entries
- Reconciliation report: book balance → adjustments → reconciled balance = bank statement balance

**FR-5.5: Cash/Bank Reports**
- Cash book report: chronological list of all cash transactions with running balance
- Bank book report: chronological list of all bank transactions with running balance
- Cash flow report: cash inflows and outflows by category (operations, investing, financing)
- Bank reconciliation statement: book vs. bank with outstanding items
- All reports exportable to PDF and Excel

**FR-5.6: Automatic Journal Entry Integration**
- Automatically generate journal entry for cash deposits:
  - Debit: Cash account (111)
  - Credit: Source account (AR, Revenue, etc.)
- Automatically generate journal entry for cash withdrawals:
  - Debit: Destination account (AP, Expense, etc.)
  - Credit: Cash account (111)
- Similar automatic entries for bank transactions (account 112)
- Link cash/bank transactions to generated journal entries

---

#### Module 6: Tax & E-Invoice Management - NEW

**FR-6.1: Tax Rate Configuration**
- Configure tax types: VAT (Value Added Tax), CIT (Corporate Income Tax), PIT (Personal Income Tax)
- VAT rates: 0%, 5%, 8%, 10% (standard) with effective dates
- Tax account mapping: VAT Payable (33311), VAT Deductible (13311), CIT Payable (3334), PIT Payable (3335)
- Tax applicability rules: by product/service category, customer/vendor type, transaction type
- Support for VAT exemptions and zero-rated supplies

**FR-6.2: Automatic Tax Calculation**
- Calculate VAT on sales invoices (output VAT): subtotal × tax_rate = tax_amount
- Calculate VAT on purchase bills (input VAT): subtotal × tax_rate = tax_amount (deductible)
- Calculate withholding tax on payments (PIT, CIT) based on payment type and threshold
- Automatic tax account postings in journal entries
- Tax rounding rules per Vietnam regulations

**FR-6.3: E-Invoice Integration (Vietnam Tax Authority)**
- Link sales invoices to e-invoice system (external integration or manual reference)
- Store e-invoice metadata: e-invoice number, e-invoice code, issuance date, verification code
- E-invoice status tracking: PENDING, ISSUED, CANCELLED, REPLACED
- Support for e-invoice lookup and verification via Tax Authority portal (manual or API)
- Handle e-invoice adjustments and cancellations with replacement invoice workflow

**FR-6.4: Tax Reports & Declarations**
- **VAT Declaration (Form 01/GTGT)**: Monthly or quarterly VAT return
  - Output VAT (sales): total VAT collected from customers
  - Input VAT (purchases): total deductible VAT paid to vendors
  - Net VAT payable = Output VAT - Input VAT
- **CIT Declaration (Form 03/TNDN)**: Quarterly provisional CIT and annual finalization
- **PIT Declaration (Form 05/KK-TNCN)**: Monthly withholding tax on salaries/contractors
- Export tax declarations to XML format per Tax Authority specification (Circular 78/2021/TT-BTC)
- Tax payment deadline alerts and reminders
- Tax report export to PDF and Excel

**FR-6.5: Tax Compliance & Audit Support**
- Tax transaction register: all taxable transactions with tax amounts, rates, accounts
- VAT reconciliation: invoices vs. tax declarations vs. journal entries
- E-invoice register: all e-invoices issued with verification status
- Tax audit trail: full history of tax calculations, adjustments, declarations
- Support for Tax Authority audit requests: export transaction data with supporting documents

**FR-6.6: Tax Payment Recording**
- Record tax payments to Tax Authority with payment date, amount, tax type, reference number
- Automatic journal entry:
  - Debit: VAT Payable / CIT Payable / PIT Payable
  - Credit: Cash/Bank account
- Tax payment register and reconciliation with declarations

---

#### Module 7: Financial Reporting - NEW

**FR-7.1: Balance Sheet (Statement of Financial Position)**
- Generate Balance Sheet per Circular 200/2014/TT-BTC Form B 01-DN
- Structure: Assets = Liabilities + Equity
- **Assets Section**: Current assets (cash, AR, inventory), Fixed assets, Other assets
- **Liabilities Section**: Current liabilities (AP, short-term loans), Long-term liabilities
- **Equity Section**: Share capital, retained earnings, current year profit/loss
- Comparative reporting: current period vs. previous period
- Configurable date range: month-end, quarter-end, year-end
- Export to PDF, Excel, XML (for Tax Authority submission)

**FR-7.2: Income Statement (Profit & Loss)**
- Generate Income Statement per Circular 200/2014/TT-BTC Form B 02-DN
- Structure: Revenue - Expenses = Profit/Loss
- **Revenue Section**: Sales revenue (511), Financial income (515), Other income (711)
- **Expense Section**: Cost of goods sold (621), Operating expenses (627, 641, 642), Financial expenses (635), Other expenses (811)
- Gross profit, operating profit, pre-tax profit, net profit calculations
- Period-over-period comparison: current vs. previous month/quarter/year
- Export to PDF, Excel, XML

**FR-7.3: Cash Flow Statement**
- Generate Cash Flow Statement per Circular 200/2014/TT-BTC Form B 03-DN
- **Operating Activities**: Cash from customers, cash to suppliers, cash for expenses
- **Investing Activities**: Purchase/sale of fixed assets, investments
- **Financing Activities**: Loans received/repaid, equity contributions, dividends paid
- Net cash flow = Operating + Investing + Financing
- Reconciliation: opening cash + net cash flow = closing cash
- Direct method and indirect method support
- Export to PDF, Excel, XML

**FR-7.4: Management Reports & Dashboards**
- **Revenue & Expense Analysis**: Trend charts by month/quarter, variance analysis (actual vs. budget)
- **AR/AP Summary**: Total receivables, total payables, aging summaries, collection metrics
- **Cash Position Dashboard**: Current cash/bank balances, cash flow forecast (next 30/60/90 days)
- **Key Financial Metrics**: Current ratio, quick ratio, debt-to-equity, profit margin, ROE
- **Profitability Analysis**: Gross margin %, operating margin %, net margin %
- Role-based dashboards: Admin (full metrics), Chief Accountant (operational), Accountant (transaction-level), Viewer (summary only)
- Real-time data refresh vs. cached reports

**FR-7.5: Custom Report Builder**
- Create custom reports by selecting accounts, date ranges, grouping, and sorting
- Formula builder for calculated fields (e.g., Gross Profit = Revenue - COGS)
- Report templates: save and reuse custom report configurations
- Drill-down capability: click account balance → view underlying journal entries
- Export custom reports to PDF and Excel

**FR-7.6: Report Scheduling & Distribution**
- Schedule reports to run automatically: daily, weekly, monthly, quarter-end, year-end
- Email distribution: send reports to stakeholders automatically
- Report history: archive generated reports with generation timestamp
- Comparative reports: auto-generate period-over-period comparisons

---

#### Module 8: Period Management - NEW (Phase 2 Priority)

**FR-8.1: Period Opening**
- Open new fiscal period with start date and end date
- Carry forward closing balances from previous period as opening balances
- Validate previous period is closed before opening new period
- Period status: OPEN (allow transactions)

**FR-8.2: Period Closing Workflow**
- Pre-closing validation checklist:
  - All draft journal entries posted or deleted
  - All invoices and bills have status (no pending drafts)
  - Bank reconciliation completed
  - All debits equal credits (trial balance)
- Close fiscal period: set status to CLOSED
- Prevent new transactions in closed periods (configurable override for Chief Accountant)
- Record closed_by user and closed_at timestamp

**FR-8.3: Period-End Adjusting Entries**
- Create adjusting journal entries for accruals, deferrals, depreciation, provisions
- Adjusting entry templates for recurring adjustments
- Validation: adjusting entries must balance before period close

**FR-8.4: Expense-Revenue Transfer & Profit Determination**
- Automatically transfer all revenue accounts (511, 515, 711) to account 911 (Determination of business results)
- Automatically transfer all expense accounts (621, 627, 635, 641, 642, 811) to account 911
- Calculate profit/loss: Revenue - Expenses = Profit (if positive) or Loss (if negative)
- Transfer net profit/loss to equity account 421 (Retained earnings)
- Generate closing entries with audit trail

**FR-8.5: Period Locking**
- Lock period after closing: set status to LOCKED
- Locked periods: absolutely no modifications allowed (enforced at database level)
- Unlock capability: only Admin role with audit log justification
- Compliance requirement: locked periods ensure data integrity for audits

**FR-8.6: Year-End Closing**
- Year-end closing process: close all 12 monthly periods for the fiscal year
- Generate annual financial statements: Balance Sheet, Income Statement, Cash Flow Statement
- Transfer annual profit/loss to retained earnings
- Prepare opening balances for new fiscal year
- Archive year-end reports for regulatory retention (10 years)

---

#### Module 9: AI-Powered RAG Chatbot

**FR-9.1: Core RAG Pipeline Infrastructure**
- **Document indexing pipeline**: Automatically extract and index invoices, vouchers, journal entries, customer/vendor records, and bank transactions from ERP PostgreSQL database
- **Vector database integration**: Configure Supabase Vector (pgvector) for semantic search with optimized embedding dimensions and distance metrics
- **Embedding generation**: Generate vector embeddings for ERP documents using domain-optimized models, handling Vietnamese and English text
- **Incremental indexing**: Support real-time or near-real-time index updates (≤ 5 minutes latency per FR-9.8) when ERP transactions are created/modified
- **Index partitioning strategy**: Partition indexes by fiscal period, module, or document type to maintain search performance as data grows (FR-9.10)
- **ERP schema integration**: Read-only PostgreSQL access with connection pooling, read replica support, and graceful handling of schema evolution (FR-9.9)
- **Data quality validation**: Pre-indexing validation of data completeness, format, and referential integrity with flagging of malformed records (FR-9.7)

**FR-9.2: Query Processing & Natural Language Understanding**
- **Natural language input**: Accept queries in Vietnamese or English with support for code-switched queries (mixed language)
- **Query intent parsing**: Classify user intent (balance inquiry, aging report, transaction lookup, calculation request) with P95 latency ≤ 200ms (FR-9.10)
- **Query disambiguation**: Detect ambiguous queries and prompt for clarification (e.g., "balance" → "trial balance, account balance, or AR/AP balance?")
- **Multi-turn conversation context**: Maintain conversation history to support follow-up questions with contextual understanding
- **Query suggestions and auto-complete**: Provide real-time query suggestions based on user role, current ERP context, and common query patterns
- **Query validation**: Validate query intent and scope before expensive retrieval operations to prevent unnecessary processing
- **Query complexity detection**: Classify queries into tiers (Simple/Moderate/Complex) and display expected response time upfront to manage user expectations

**FR-9.3: Document Retrieval & Context Management**
- **Semantic vector search**: Retrieve relevant documents using cosine similarity or other distance metrics with recall@10 ≥ 0.90 target
- **Retrieval performance budget**: P95 vector retrieval latency ≤ 1500ms (FR-9.10)
- **Hybrid search strategy**: Combine semantic search with keyword filtering for accounting codes, dates, amounts, and document IDs
- **Context window management**: Select and rank top-k documents (typically 5-10) to fit within LLM context window while maximizing relevance
- **Cross-module document retrieval**: Automatically retrieve and join related documents across AR, AP, GL, and Bank modules for comprehensive answers
- **Role-based document filtering**: Filter retrieved documents based on user's ERP role and entity/department access permissions (RBAC integration)
- **Draft vs. posted transaction visibility**: Handle draft transactions appropriately based on user role and query context

**FR-9.4: Answer Generation & Grounding**
- **LLM-based generation**: Generate natural language answers using LLM (OpenAI GPT-4, Anthropic Claude, or equivalent) with accounting-domain prompts
- **Answer generation budget**: P95 LLM generation latency ≤ 1500ms (FR-9.10)
- **Groundedness enforcement**: Ensure all answers are grounded in retrieved source documents with groundedness score ≥ 0.80 target
- **Citation generation**: Include source citations with every answer (document type, ID, date, and link to source) with ≥ 95% citation coverage target
- **Citation formatting budget**: P95 citation formatting latency ≤ 300ms (FR-9.10)
- **Hallucination prevention**: Detect and prevent hallucinated information; when insufficient context exists, explicitly inform user rather than generate ungrounded answers
- **Answer correctness**: Achieve correctness score ≥ 0.75 on labeled test set for accounting domain queries
- **LLM provider abstraction**: Support multiple LLM providers with failover capability and streaming support for partial result display

**FR-9.5: Accounting Domain Query Capabilities**

**FR-9.5.1: Accounts Receivable (AR) Queries**
- Current AR balance by customer, invoice, or aging bucket (Current, 1-30, 31-60, 61-90, 90+ days)
- Outstanding invoice details with invoice numbers, dates, due dates, amounts, and payment status
- Customer payment history and credit limit tracking with trend analysis
- AR aging reports with drill-down to individual invoices and supporting documents
- Overdue account identification for collection prioritization

**FR-9.5.2: Accounts Payable (AP) Queries**
- Current AP balance by vendor, bill, or aging bucket
- Payment due dates and prioritization (upcoming payments in next 7/14/30 days)
- Vendor payment history and payment terms tracking
- AP aging reports with vendor-level and bill-level detail
- Cash flow forecasting based on upcoming payment obligations

**FR-9.5.3: General Ledger (GL) Queries**
- Account balances and transaction history by chart of accounts (4-level hierarchy per Circular 200)
- Journal entry details with entry number, date, description, debits, credits, and supporting vouchers
- Trial balance with debit/credit totals and balance validation
- Account reconciliation status and period-over-period comparisons
- Account movement analysis (opening balance, debits, credits, closing balance)

**FR-9.5.4: Bank Reconciliation Queries**
- Bank statement balance vs. book balance comparison with discrepancy identification
- Unreconciled transactions and outstanding items (checks, deposits in transit)
- Reconciliation status by bank account and fiscal period
- Discrepancy investigation and resolution tracking
- Bank transaction matching suggestions for pending items

**FR-9.5.5: Invoice and Voucher Details**
- Full transaction details including line items, quantities, unit prices, tax breakdowns, and approval status
- Supporting documents (scanned receipts, e-invoices, contracts, bank statements) with inline viewing
- Audit trail and modification history with user, timestamp, and change details
- Compliance status with Vietnam Circular 200/2014/TT-BTC standards
- Tax compliance information (VAT rates, tax amounts, e-invoice verification status)

**FR-9.6: Cross-Module Query Orchestration**
- **Multi-module reasoning**: Support queries requiring data from multiple ERP modules (e.g., "What is our cash position and upcoming payment obligations?")
- **Orchestration budget**: P95 cross-module orchestration latency ≤ 1000ms (FR-9.10)
- **Automated data joining**: Automatically join related records (invoices → payments → bank transactions → journal entries)
- **Calculated result validation**: Validate computed values (aging, balances, accruals) against ERP-generated reports with ≤ 1% divergence threshold
- **Period-end close handling**: Manage ERP system locks during period-end closing using read replicas or cached data to maintain query availability
- **Complex query support**: Handle multi-step reasoning queries (e.g., "show unmatched payments and suggest potential invoice matches")

**FR-9.7: Quality Monitoring & Validation**
- **Groundedness/faithfulness tracking**: Monitor and log groundedness scores for each answer with ≥ 0.80 target (Goal #3)
- **Correctness tracking**: Monitor and log correctness scores with ≥ 0.75 target for accounting domain queries (Goal #3)
- **Hallucination detection**: Detect and log hallucination events with automatic correction mechanisms and human feedback loop
- **Answer quality dashboard**: Administrator dashboard displaying latency metrics, error rates, groundedness scores, correctness scores, and citation coverage
- **Validation test suite**: Maintain 100+ representative queries across all accounting domains with automated validation against ERP ground truth
- **Daily reconciliation**: Automated daily reconciliation between RAG-generated answers and ERP-generated reports with divergence alerting (≥ 1% threshold)
- **Human feedback loop**: Support user feedback (thumbs up/down, corrections) for continuous quality improvement
- **A/B testing support**: Enable A/B testing of prompt templates, retrieval strategies, and generation parameters

**FR-9.8: User Experience & Interaction**

**FR-9.8.1: Citation and Source Transparency**
- Every answer includes visible source citations with document IDs, types, and dates
- Users can click citations to view original ERP documents (invoices, vouchers, statements)
- Display data freshness timestamp with each answer (e.g., "Data current as of 14:32 today")
- ≥ 95% of answers must include supporting citations (Goal #4)
- Show confidence scores or uncertainty indicators when answer quality is uncertain

**FR-9.8.2: Answer Verification and Drill-Down**
- "Show source data" button to view exact ERP records used to generate answer
- Display calculation logic and intermediate steps for computed values (aging, balances, totals)
- One-click navigation from answer to source vouchers, invoices, journal entries in ERP UI
- Compare RAG-generated answer against raw ERP report to build user trust
- Highlight which parts of answer came from which source documents (color-coded citations)

**FR-9.8.3: Report Export and Sharing**
- Export query results to PDF, Excel, CSV with full citations and metadata
- Include timestamp, data freshness indicator, and audit trail in all exports
- Generate shareable permalinks to queries with results (respecting RBAC permissions)
- Support scheduled/recurring query exports for regular reports (daily, weekly, monthly)
- Embed query results in presentations and board decks with proper attribution

**FR-9.8.4: Onboarding and Query Learning**
- Interactive 5-minute tutorial showcasing example queries by user role (accountant, finance manager, viewer)
- "Query of the day" feature highlighting system capabilities and teaching by example
- Contextual tooltips explaining accounting concepts when queries involve unfamiliar terms (e.g., "What is a payment voucher?")
- Query templates library organized by task: month-end close, AR aging, bank reconciliation, tax preparation
- Progressive disclosure: simple queries first, advanced features as users gain confidence
- In-app tips suggesting relevant queries based on current ERP context (e.g., suggest AR aging during period-end)
- Onboarding time target: ≤ 30 minutes to basic proficiency (NFR-7)

**FR-9.8.5: Performance and Responsiveness**
- **Overall P95 response time**: ≤ 5 seconds for typical queries (Goal #1, NFR-1)
- **P99 response time**: ≤ 10 seconds (NFR-1)
- **Query complexity tiers** with different performance targets:
  - **Tier 1 (Simple)**: Single module, recent data → 2-3 sec P95 target
  - **Tier 2 (Moderate)**: Cross-module, calculations → 4-5 sec P95 target
  - **Tier 3 (Complex)**: Historical analysis, multi-step reasoning → 8-10 sec P95 target (warn user upfront)
- Display expected response time upfront based on query complexity tier
- Show loading indicators with progress context ("Retrieving invoices..." → "Analyzing AR data..." → "Generating answer...")
- Stream partial results when possible (show summary while details load)
- Query result caching for instant responses to common queries
- **Timeout policy**: Attempt full pipeline for 8 seconds, then invoke graceful degradation (FR-9.10)

**FR-9.9: Compliance, Security & Access Control**

**FR-9.9.1: Role-Based Access Control (RBAC)**
- Users see only data permitted by their ERP role: Admin, Chief Accountant, Accountant, Viewer (Module 1 RBAC integration)
- Filter retrieved documents based on user's entity/department access permissions
- Support different query scopes: accountants (transaction-level), finance managers (analytical), auditors (compliance)
- Permission enforcement at retrieval stage (pre-LLM) to prevent unauthorized data exposure
- Audit log records all permission checks and access denials

**FR-9.9.2: Compliance Audit Trail**
- Maintain immutable, timestamped log of complete RAG pipeline execution:
  - Query text and user context (user ID, timestamp, IP address, session ID)
  - Retrieved documents (IDs, relevance scores, metadata)
  - LLM prompts and responses (sanitized for PII to prevent third-party exposure)
  - Generated answer with citations
  - User feedback and corrections
- Support regulatory audit export in standard format (JSON, CSV with schema documentation)
- Retain audit logs per Vietnam compliance requirements (minimum 10 years for financial records per Circular 200/2014/TT-BTC)
- Enable auditor to trace any answer back to source data and generation process with full lineage
- Tamper-evident logging to ensure integrity (append-only, cryptographic hashing)

**FR-9.9.3: Data Security and Privacy**
- **Encryption at rest**: AES-256 encryption for all indexed documents and audit logs (NFR-4)
- **Encryption in transit**: TLS 1.3 for all API communications (NFR-4)
- **PII handling**: Sanitize PII before sending prompts to third-party LLM providers (use document IDs/references only, not raw data)
- **Credential management**: Secure storage for external API keys (LLM, Vector DB) using secrets management service
- Query logs sanitized to prevent PII leakage in monitoring systems
- Compliance with Vietnam Circular 200/2014/TT-BTC accounting standards (FR-9.9.2)

**FR-9.9.4: ERP Schema Evolution Handling**
- Detect ERP schema changes: new tables, columns, voucher types, chart of accounts updates
- Automatically trigger index schema updates when ERP structure changes
- Validate cross-module joins after schema migrations to prevent broken queries
- Alert administrators of breaking changes requiring manual review
- Maintain backward compatibility for queries during schema transitions (gradual migration)

**FR-9.10: Performance, Reliability & Error Handling**

**FR-9.10.1: Performance Budget Allocation**
- **Query intent parsing**: 200ms P95 budget
- **Vector retrieval**: 1500ms P95 budget
- **Cross-module orchestration**: 1000ms P95 budget
- **Answer generation (LLM)**: 1500ms P95 budget
- **Citation formatting**: 300ms P95 budget
- **Buffer**: 500ms
- **Total P95**: 5000ms (aligns with FR-9.8.5 target)
- Monitor per-component latency and alert when budgets exceeded
- Automated performance profiling and bottleneck identification

**FR-9.10.2: Index Freshness and Synchronization**
- **Indexing latency SLA**: New ERP transactions available for query within 5 minutes (Goal #3)
- Display data freshness indicator with each answer: "Data current as of 14:32 today"
- Support manual index refresh trigger for critical updates during month-end close
- Handle real-time vs. batch synchronization strategies based on document type
- Alert users when index is stale or synchronization has failed
- Incremental indexing to minimize staleness (avoid full reindex)

**FR-9.10.3: Error Handling and Graceful Degradation**
- Detect when retrieved documents lack sufficient context and inform user explicitly (no hallucination)
- **Fallback to keyword search**: If semantic retrieval quality falls below threshold, automatically degrade to structured keyword search
- Provide partial answers with explicit gaps identified when full answer unavailable ("I found X but couldn't determine Y")
- **Rate limiting**: Prevent abuse of computationally expensive queries (per-user query quotas)
- Automatic context expansion when initial retrieval set is insufficient (iterative retrieval)
- Handle domain-specific accounting terminology with specialized embeddings or lexicons
- Clear error messages with actionable recovery steps ("Data not found. Try: [suggestions]")

**FR-9.10.4: External Dependency Resilience**
- **LLM API resilience**: 2-tier fallback strategy
  - Primary LLM provider (OpenAI GPT-4 or Anthropic Claude)
  - Secondary LLM provider (alternative vendor)
  - Cached template responses for common queries (last resort)
- **ERP connection resilience**: Read replica failover; stale data acceptable for non-critical queries (tolerate 5-min lag)
- **Vector DB resilience**: Query timeout with automatic degradation to keyword search (enhances FR-9.10.3)
- Monitor external dependency health metrics: latency, error rates, availability (99% uptime target)
- Proactive failover before complete outage occurs (circuit breaker pattern)
- Display degraded-mode indicator to users when operating on fallback systems

**FR-9.10.5: Scalability and Availability**
- **Concurrent users**: Support minimum 20 concurrent users without degradation (NFR-2 MVP target)
- **Document scale**: Handle up to 500,000 indexed documents (invoices, vouchers, entries) for MVP (NFR-2)
- **Query volume**: Handle 1,000 queries/day across pilot user group (NFR-2)
- **Availability**: System available during business hours (8 AM - 6 PM Vietnam time, Monday-Friday) with 99% uptime target (NFR-3)
- Horizontal scaling capability for vector database and application tier
- Planned maintenance windows outside business hours
- Automatic health checks and alerting for critical components

---

**Module 9 Key Functional Requirements Dependencies:**

**Foundation Layer**: FR-9.1 (Core Pipeline) and FR-9.3 (Retrieval) are critical foundations for all other Module 9 capabilities.

**Sequential Pipeline**: FR-9.2 (Query Processing) → FR-9.3 (Retrieval) → FR-9.6 (Orchestration) → FR-9.4 (Generation) → FR-9.8.1 (Citations) → FR-9.7 (Quality Monitoring)

**Cross-Cutting Concerns**: FR-9.9.1 (RBAC) impacts FR-9.3, FR-9.5; FR-9.10.2 (Freshness) impacts FR-9.4 answer quality; FR-9.10.3 (Error Handling) provides safety net for FR-9.3, FR-9.4, FR-9.6 failures.

**External Integration Boundaries**: ERP transaction events → FR-9.10.2 → FR-9.1; FR-9.6 validates against ERP; FR-9.9.4 monitors ERP schema changes; FR-9.10.4 manages all external dependencies (LLM, Vector DB, ERP).

### Non-Functional Requirements

**NFR-1: Performance and Responsiveness**
- P95 response time ≤ 5 seconds for typical queries (as defined in FR-16, FR-22)
- P99 response time ≤ 10 seconds
- System supports minimum 20 concurrent users without degradation
- Vector search latency ≤ 1.5 seconds (per FR-22 budget allocation)
- LLM generation latency ≤ 1.5 seconds (per FR-22 budget allocation)
- Index update latency ≤ 5 minutes for new transactions (per FR-18)

**NFR-2: Scalability**
- Support indexing up to 500,000 documents (invoices, vouchers, journal entries) for MVP
- Handle query volume of 1,000 queries/day across pilot user group
- Horizontal scaling capability for vector database and application tier
- Index partitioning strategy to maintain performance as document count grows (FR-22)
- Architecture supports scaling to 100+ concurrent users post-MVP

**NFR-3: Availability and Reliability**
- System available during business hours: 8 AM - 6 PM Vietnam time, Monday-Friday
- Target uptime: 99% during business hours (allows ~2 hours downtime/month)
- Planned maintenance windows outside business hours
- Graceful degradation per FR-20, FR-23 when dependencies fail
- Automatic health checks and failover for critical components (LLM API, Vector DB, ERP connection)

**NFR-4: Security and Data Protection**
- Encryption at rest for all indexed documents and audit logs (AES-256)
- Encryption in transit for all API communications (TLS 1.3)
- Role-Based Access Control (RBAC) integrated with ERP user permissions (FR-15)
- Query logs sanitized to prevent PII leakage in monitoring systems
- Secure credential management for external API keys (LLM, Vector DB)
- No financial data or PII transmitted to third-party LLM providers in prompts (use document IDs/references only)

**NFR-5: Compliance and Audit**
- Full compliance with Vietnam Circular 200/2014/TT-BTC accounting standards
- Immutable audit trail retention: 10 years minimum (per Vietnam regulations)
- Support for regulatory audit export in standard formats (FR-27)
- Tamper-evident logging for all financial queries and answers
- Data residency: All financial data stored in Vietnam-based infrastructure or compliant cloud region
- Support compliance reporting for internal and external audits

**NFR-6: Accuracy and Answer Quality**
- Groundedness/faithfulness score ≥ 0.80 on labeled test set (per Goal #3, FR-7)
- Correctness score ≥ 0.75 on labeled test set (per Goal #3, FR-7)
- Retrieval recall@10 ≥ 0.90 (per Goal #4, FR-5)
- Citation coverage ≥ 95% of answers include supporting citations (per Goal #4, FR-13)
- Maximum acceptable hallucination rate: ≤ 5% of queries (with continuous monitoring per FR-7)
- Human feedback loop for continuous quality improvement

**NFR-7: Usability and User Experience**
- Intuitive chat interface requiring minimal training (≤ 30 minutes onboarding per FR-28)
- Support Vietnamese and English languages for queries and responses (per FR-1)
- Responsive UI supporting desktop and tablet form factors
- Accessibility: WCAG 2.1 Level A minimum (considering vision-impaired users)
- User satisfaction (CSAT) target: ≥ 4.0/5.0 post-task (per Goal #2)
- Task completion rate: ≥ 70% of pilot users successfully complete core queries without assistance (per Goal #2)

**NFR-8: Maintainability and Operability**
- Comprehensive monitoring dashboard for admins (latency, error rates, indexing status per FR-22, FR-26)
- Automated alerts for system degradation, dependency failures, data quality issues
- Detailed logging with structured log format for troubleshooting
- Support for A/B testing of prompt templates and retrieval strategies
- Configuration-driven system parameters (no code changes for tuning)
- Clear deployment and rollback procedures with zero-downtime updates

**NFR-9: Interoperability and Integration**
- Seamless integration with existing ERP system (read-only access to PostgreSQL schema)
- API compatibility with n8n orchestration workflows
- Support for Supabase Vector or equivalent vector database
- LLM provider abstraction supporting multiple vendors (OpenAI, Anthropic, local models per FR-23)
- Standard REST API for future integrations with other systems
- Export formats compatible with Excel, PDF, CSV for downstream use (FR-25)

**NFR-10: Data Quality and Freshness**
- Data freshness indicator displayed with every answer (per FR-18)
- Data quality validation pass rate ≥ 95% during indexing (per FR-26)
- Automated detection and flagging of data anomalies
- Support for manual data correction workflow when quality issues detected
- Incremental indexing to minimize staleness (≤ 5 min lag per FR-18)

**NFR-11: Cost Efficiency (MVP Considerations)**
- Optimize LLM API costs through prompt engineering and caching
- Use cost-effective vector database tier (Supabase free tier or low-cost plan for MVP)
- Implement query result caching to reduce redundant LLM calls
- Monitor per-query costs and alert if exceeding budget thresholds
- Target: Average cost per query ≤ $0.10 (considering LLM API, vector DB, compute)

**NFR-12: Documentation and Knowledge Transfer**
- Comprehensive user documentation with examples for common queries
- Administrator runbook for operations, troubleshooting, and maintenance
- Developer documentation for architecture, APIs, and integration points
- Training materials for pilot users (quick-start guide, video tutorials per FR-28)
- Inline help and contextual guidance within the application

## User Journeys

### Journey 1: Accountant Checks AR Aging During Month-End Close

**Persona:** Lan, Senior Accountant (8 years experience)
**Context:** It's day 3 of month-end close, and Lan needs to review AR aging to identify overdue accounts for collection follow-up.
**Goal:** Quickly identify customers with overdue balances and generate collection priority list.

**Traditional ERP Flow (Current State - 15-20 minutes):**
1. Navigate to AR module → Aging Reports screen
2. Select date range, apply filters for overdue accounts
3. Export to Excel
4. Manually sort by amount and days overdue
5. Cross-reference with customer payment history (separate screen)
6. Check for disputes or holds (another screen)
7. Compile list for collections team

**RAG System Flow (Target State - 3-5 minutes):**

1. **Initial Query** (FR-1, FR-3)
   - Lan types: "Show me AR aging report for overdue accounts"
   - System displays query suggestions: "All overdue" | "30+ days" | "60+ days" | "90+ days"
   - Lan selects "60+ days"

2. **Retrieval and Answer** (FR-5, FR-6, FR-8)
   - System retrieves AR records, invoices, payment history (P95 latency: 2-3 sec per FR-29 Tier 1)
   - Displays aging summary with citations:
     - "15 customers with 60+ day overdue balances totaling 2.3B VND"
     - Top 5 customers listed with amounts and invoice numbers
   - Data freshness indicator: "Current as of 14:30 today" (FR-18)

3. **Drill-Down and Verification** (FR-24, FR-13)
   - Lan clicks on Customer "ABC Corp - 450M VND overdue"
   - System shows: Invoice details, due dates, payment history, last contact date
   - Lan clicks "Show source data" to verify against ERP
   - Confidence restored - numbers match

4. **Follow-Up Question** (FR-2, Multi-turn context)
   - Lan asks: "Any of these have payment disputes?"
   - System searches dispute records, finds 2 customers with open disputes
   - Updates priority list accordingly

5. **Export and Share** (FR-25)
   - Lan clicks "Export to Excel" - generates report with citations and timestamp
   - Shares permalink with collections team
   - **Time saved: 10-15 minutes** ✓ (60%+ reduction per Goal #1)

**Decision Points:**
- Does Lan trust the RAG answer? → FR-24 verification builds trust
- Is the answer complete? → FR-13 citations provide transparency
- Can she act on this data? → FR-25 export enables downstream workflow

---

### Journey 2: Finance Manager Prepares for Board Meeting

**Persona:** Minh, CFO
**Context:** Board meeting in 2 hours. Board member emailed: "What's our cash position and upcoming payment obligations?"
**Goal:** Quickly gather accurate cash flow data with supporting evidence for board presentation.

**Traditional ERP Flow (Current State - 30-45 minutes):**
1. Check bank balances across multiple accounts (Banking module)
2. Pull AP aging for upcoming payments (AP module)
3. Check AR aging for expected collections (AR module)
4. Review GL for pending transactions (GL module)
5. Manually compile data into presentation
6. Double-check numbers with accounting team
7. Prepare supporting documents (invoices, bank statements)

**RAG System Flow (Target State - 8-12 minutes):**

1. **Initial Complex Query** (FR-1, FR-19 Cross-Module)
   - Minh types: "What is our current cash position and payment obligations for the next 30 days?"
   - System detects complex query (FR-29 Tier 3) - shows "Expected response time: 8-10 seconds"

2. **Cross-Module Retrieval and Analysis** (FR-5, FR-6, FR-9, FR-10, FR-11, FR-19)
   - System retrieves:
     - Bank account balances (FR-11 Bank Reconciliation)
     - AP aging for next 30 days (FR-9)
     - AR collections expected (FR-8)
     - Pending GL transactions
   - Orchestrates multi-step reasoning (FR-19)
   - **Response time: 9 seconds** (within Tier 3 target)

3. **Comprehensive Answer with Citations** (FR-6, FR-13)
   - "**Current cash position: 15.2B VND** across 3 bank accounts (citations: Bank statements dated today)"
   - "**AP obligations next 30 days: 8.7B VND** (45 vendor payments, largest: Vendor X 2.1B on Oct 25)"
   - "**AR collections expected: 6.5B VND** (23 customers, assuming 80% collection rate)"
   - "**Net cash projection: +12.5B VND** (adequate for obligations)"
   - All numbers include drill-down links to source documents (FR-24)

4. **Verification and Confidence Building** (FR-24, FR-27 Audit Trail)
   - Minh clicks "Show source data" on cash position
   - Sees actual bank reconciliation records from this morning
   - High confidence - this is current and accurate

5. **Follow-Up Questions** (FR-2, Multi-turn)
   - Minh asks: "What are our top 3 payment obligations?"
   - System responds immediately (cached context): "Vendor X: 2.1B (Oct 25), Vendor Y: 1.8B (Oct 28), Vendor Z: 1.2B (Nov 2)"
   - Minh asks: "Show me the invoices for Vendor X"
   - System displays invoice details with PDFs attached

6. **Export for Board Presentation** (FR-25)
   - Minh clicks "Export to PDF" - formatted report with all citations
   - Embeds key charts in board deck
   - **Time saved: 20-35 minutes** ✓ (60%+ reduction per Goal #1)
   - **Confidence level: High** ✓ (CSAT likely 5/5 per Goal #2)

**Decision Points:**
- Can Minh trust this for the board? → FR-27 audit trail + FR-24 verification
- Is data current enough? → FR-18 freshness indicator shows "Data current as of 14:30"
- What if a board member challenges a number? → FR-13 citations provide instant backup

---

### Journey 3: New Accountant Learns System During Onboarding

**Persona:** Thu, Junior Accountant (3 months at company, fresh graduate)
**Context:** First week on the job. Manager asks Thu to check if Customer XYZ has paid their October invoice.
**Goal:** Successfully answer manager's question despite limited ERP knowledge and accounting experience.

**Traditional ERP Flow (Current State - 45-60 minutes with help):**
1. Thu doesn't know which module to check
2. Asks colleague for help → interrupts their work
3. Colleague shows AR module, invoice lookup screen
4. Thu struggles with filters and search syntax
5. Finds invoice, but unsure how to check payment status
6. Checks payment matching screen (another module)
7. Gets confused by partial payments and credits
8. Eventually finds answer with significant colleague assistance

**RAG System Flow (Target State - 5-8 minutes, no assistance):**

1. **Onboarding Tutorial** (FR-28, First-time user)
   - Thu opens RAG system - sees "Welcome! Take 5-minute tutorial?"
   - Clicks "Yes" - sees example queries: "Check invoice status", "Find customer balance", "View payment history"
   - Tutorial shows how to ask questions in natural language

2. **Initial Query with Suggestions** (FR-1, FR-3, FR-28)
   - Thu types: "Customer XYZ October invoice"
   - System suggests: "Show invoice status for Customer XYZ in October" | "Has Customer XYZ paid their October invoice?"
   - Thu selects second suggestion

3. **Clarification for Ambiguity** (FR-17 Disambiguation)
   - System responds: "Customer XYZ has 2 invoices in October. Which one?"
     - Invoice INV-2024-001 (5M VND, due Oct 15)
     - Invoice INV-2024-002 (3M VND, due Oct 31)
   - Thu checks manager's email - it's INV-2024-001
   - Clicks on INV-2024-001

4. **Clear Answer with Context** (FR-6, FR-8, FR-12)
   - System responds: "**Invoice INV-2024-001 is PAID**"
   - "Payment received: Oct 12, 2024 (3 days early)"
   - "Amount: 5M VND"
   - "Payment method: Bank transfer"
   - "Citation: Payment voucher PV-2024-789, Bank statement line 45"
   - Includes tooltip: "💡 Paid invoices are marked as closed in AR"

5. **Learning Accounting Concepts** (FR-28, Contextual help)
   - Thu hovers over "Payment voucher" - sees explanation: "A payment voucher records money received from customers"
   - System suggests: "📚 Learn more about invoice lifecycle" link
   - Thu feels empowered - successfully answered manager's question independently

6. **Manager Asks Follow-Up** (Real-time scenario)
   - Manager: "Great! What about the other October invoice?"
   - Thu asks system: "What about invoice INV-2024-002?"
   - System: "**Invoice INV-2024-002 is OUTSTANDING**. Due Oct 31 (21 days from now). Amount: 3M VND. No payments received yet."
   - Thu reports back confidently - **task completed without assistance** ✓ (Goal #2: 70% completion target)

**Decision Points:**
- Can Thu use the system without training? → FR-28 onboarding + FR-3 suggestions = yes
- Does Thu trust the answer? → Clear, simple language + citations build confidence
- Will Thu use it again? → Positive first experience → High CSAT ✓ (Goal #2: ≥4/5 target)

**Learning Outcomes:**
- Thu learned: How to check invoice status, what payment vouchers are, AR terminology
- System demonstrated: Natural language works, suggestions help, tooltips teach
- **Onboarding time: < 30 minutes** ✓ (NFR-7 target met)

## UX Design Principles

**UX-1: Trust Through Transparency**
- Every answer includes visible citations linking to source documents (FR-13)
- Display data freshness indicators prominently (FR-18)
- Show confidence scores or uncertainty indicators when answer quality is uncertain (FR-6)
- Provide "Show source data" drill-down to verify RAG answers against raw ERP records (FR-24)
- Make the RAG pipeline visible, not magic - users should understand how answers are generated

**UX-2: Natural Conversation, Not Commands**
- Users ask questions in plain Vietnamese or English, not SQL or technical syntax (FR-1)
- Support multi-turn conversations with contextual follow-ups (FR-2)
- Disambiguate gracefully when queries are ambiguous, offering clear choices (FR-17)
- Conversational tone in responses: clear, concise, accounting-domain appropriate
- Avoid jargon unless user demonstrates familiarity with technical terms

**UX-3: Progressive Disclosure - Simple First, Details on Demand**
- Start with summary answers, expand to details on user request
- Default view: Key metrics and top results
- Click to expand: Full transaction lists, detailed calculations, supporting documents
- New users see simplified interface; power users access advanced features
- Onboarding tutorial introduces features progressively (FR-28)

**UX-4: Speed and Responsiveness as a Feature**
- Display query complexity tier and expected response time upfront (FR-29)
- Show loading indicators with progress context ("Retrieving invoices..." → "Analyzing AR data..." → "Generating answer...")
- Stream partial results when possible (show summary while details load)
- Cache common queries for instant responses
- Optimize perceived performance: show something useful within 1 second, complete answer within target SLA

**UX-5: Error Prevention and Graceful Degradation**
- Query suggestions and auto-complete prevent malformed queries (FR-3)
- Validate query intent before expensive operations (FR-17)
- When retrieval quality is low, inform user explicitly rather than hallucinate (FR-20)
- Graceful fallback to keyword search when semantic search fails (FR-20)
- Clear error messages with actionable recovery steps ("Data not found. Try: [suggestions]")

**UX-6: Empowerment Through Learning**
- Contextual tooltips explain accounting concepts inline (FR-28)
- "Query of the day" showcases capabilities and teaches by example (FR-28)
- Help users learn both the system AND the accounting domain simultaneously
- Progressive complexity: simple queries → intermediate → advanced (guided learning path)
- Celebrate successes: "✓ Task completed" confirmations build confidence

**UX-7: Accessibility and Inclusivity**
- Support bilingual interface (Vietnamese and English) with seamless switching
- WCAG 2.1 Level A minimum for vision-impaired users (NFR-7)
- Keyboard navigation for power users
- Responsive design: desktop-optimized, tablet-friendly (NFR-7)
- Clear visual hierarchy: important information stands out, secondary details recede

**UX-8: Consistency and Familiarity**
- Citation format consistent across all answers (document type + ID + date)
- Consistent interaction patterns: click citations → see source, click metrics → see details
- Familiar accounting terminology aligned with Vietnam Circular 200/2014/TT-BTC standards
- Visual design aligned with existing ERP system where possible (reduce cognitive load)
- Predictable behavior: same query yields same answer (unless data changed)

**UX-9: Actionable Insights, Not Just Data**
- Answers prioritize actionable information: "Top 3 overdue customers" vs. "15 customers overdue"
- Highlight what needs attention: overdue items, discrepancies, unusual patterns
- Support common next actions: "Export to Excel", "Share with team", "Set reminder"
- Reduce decision fatigue: provide recommendations when appropriate ("Suggest following up with these 3 customers first")
- Connect answers to business outcomes: "This will help you prioritize collections"

**UX-10: Auditability and Accountability Built-In**
- Every interaction logged with timestamp and user ID (FR-27)
- Users can review their query history
- Exported reports include full audit trail (who, what, when, sources)
- Support collaborative workflows: permalink sharing with context preserved (FR-25)
- Compliance-first design: audit trail is a feature, not an afterthought

## Epics

### Epic Overview

This Level 3 project is structured into **4 epics** delivering incremental value toward the MVP goal of an AI-powered RAG chatbot for ERP accounting queries. Each epic builds upon the previous, enabling early validation and iterative refinement.

**Total Estimated Scope:** 28-35 stories, ~70-90 story points

---

### Epic 1: Core RAG Pipeline and Infrastructure (Foundation)

**Goal:** Establish the foundational RAG pipeline capable of indexing ERP data, retrieving relevant documents, and generating grounded answers with citations.

**Business Value:** Enables basic question-answering capability. Proves technical feasibility of RAG for accounting domain.

**Estimated Stories:** 11-13 stories (~25-30 points)

**Key Capabilities:**
- Document indexing pipeline for invoices, vouchers, journal entries (FR-4)
- **ERP access validation and PII masking** (Week 1) - verify all required tables, implement data anonymization
- Vector database setup and configuration (Supabase Vector)
- Semantic search and retrieval with recall@10 ≥ 0.90 (FR-5)
- **Performance spike testing with realistic data volume** (Week 3-4) - benchmark LLM, vector DB, end-to-end latency
- LLM-based answer generation with citation support (FR-6, FR-13)
- **LLM provider abstraction layer with streaming support** (foundation for FR-23) - enables failover and partial result streaming
- Basic query processing for natural language input (FR-1)
- ERP schema integration (read-only PostgreSQL access)
- **ERP schema evolution monitoring** (FR-21 moved from Epic 4) - early warning prevents late-stage breakage
- Index freshness monitoring and incremental updates (FR-18)
- Basic error handling and logging (FR-20)

**Success Criteria:**
- Successfully index ≥ 80% of target document types (Goal #5)
- Retrieve relevant documents with recall@10 ≥ 0.90
- Generate answers with ≥ 95% citation coverage
- **P95 latency ≤ 8 seconds with 500K documents indexed**
- **LLM P95 latency ≤ 2 seconds, Vector DB P95 ≤ 1.5 seconds**
- Schema monitoring detects ERP changes within 24 hours
- PII properly masked in all indexed data

**Critical External Dependencies (Must Secure Before Kickoff):**
- **ERP database access** (read-only PostgreSQL) - BLOCKING, needed Week 1
  - Pre-project requirement: Access request submitted Week -4 with PII handling plan
  - Specific tables documented: invoices, vouchers, AR, AP, GL, bank transactions
  - Security assessment completed before project start
- Supabase Vector account or equivalent vector database (tier supports 20 concurrent queries)
- LLM API credentials (OpenAI, Anthropic, or equivalent) with demonstrated P95 latency ≤ 2 sec
- **Accounting domain expert** engaged Week 3 (not Week 5) for early validation
- **Vietnam Circular 200/2014/TT-BTC compliance expert** engaged pre-project for requirements documentation

**Success Gate (Epic 1 → Epic 2):**
- ✅ Performance benchmarks meet targets (P95 ≤ 8 sec) OR escalation plan active
- ✅ Accounting expert has validated indexing logic and calculation assumptions
- ✅ ERP access stable with all required tables available

**Risks:**
- ERP schema complexity, data quality issues, vector embedding quality for accounting terminology
- Performance bottlenecks discovered late
- **Mitigation**: Synthetic test data ready as backup; performance testing in Week 3-4; early expert engagement

---

### Epic 2: Accounting Domain Intelligence and Query Capabilities

**Goal:** Extend RAG pipeline with accounting-specific query patterns, multi-module orchestration, and domain-optimized retrieval for AR, AP, GL, and bank reconciliation queries.

**Business Value:** Enables practical accounting use cases. Users can ask real questions and get domain-appropriate answers.

**Estimated Stories:** 10-12 stories (~24-28 points)

**Key Capabilities:**
- AR query support: balances, aging, invoice details (FR-8)
- AP query support: payables, due dates, vendor history (FR-9)
- GL query support: account balances, journal entries (FR-10)
- Bank reconciliation query support (FR-11)
- Cross-module query orchestration (FR-19) - **CRITICAL DEPENDENCY for Epic 3**
- **LLM response caching for common query patterns** (performance optimization)
- Invoice and voucher detail retrieval (FR-12)
- Multi-turn conversational context (FR-2)
- Query disambiguation when ambiguous (FR-17)
- **Comprehensive accuracy validation test suite** (FR-30) - 100+ queries validated against ERP
- **Compliance design review checkpoint** (Week 8) - validate FR-14, FR-15, FR-27 against Vietnam regulations
- **Performance baseline testing** - measure latency/quality before optimization
- **Pilot user recruitment and engagement** (Week 7-8) - recruit 10-15 users, pre-briefing session

**Success Criteria:**
- Support all core accounting query patterns per user journeys
- Cross-module queries work correctly (e.g., "cash position and payment obligations")
- **Accuracy ≥ 99% on 100+ query validation test suite** (FR-30 requirement)
- Users successfully complete 70% of pilot tasks without assistance (Goal #2)
- **Performance baseline documented** (median, P95, P99 latency per query type)
- **10+ pilot users confirmed and briefed** for Epic 3 testing

**Critical External Dependencies:**
- Epic 1 complete
- **Accounting domain expert** for validation - weekly sessions Week 5-9
- **Compliance expert** for design review in Week 8
- **Pilot user group** recruitment completed by end of Week 8

**Success Gate (Epic 2 → Epic 3):**
- ✅ Accuracy ≥ 99% on validation test suite
- ✅ Pilot users confirmed (10+) and briefed on value proposition
- ✅ Compliance design review sign-off received
- ✅ Performance baseline shows path to 5-sec P95 target

**Risks:**
- Calculation accuracy, cross-module data consistency, handling ERP system locks during period-end close
- Pilot user recruitment challenges
- **Mitigation**: Weekly validation sessions with domain expert; early pilot recruitment; compliance checkpoint

---

### Epic 3: User Experience, Performance, and Quality Assurance

**Goal:** Optimize user-facing experience with fast performance, quality monitoring, verification tools, and onboarding features to achieve MVP usability targets.

**Business Value:** Transforms working prototype into production-ready MVP. Builds user trust and adoption.

**Estimated Stories:** 7-9 stories (~18-23 points)

**Key Capabilities:**
- Performance optimization to meet P95 ≤ 5 sec target (FR-16, FR-22)
- Query complexity tiering and expectation management (FR-29)
- Answer verification and drill-down to source data (FR-24)
- Answer quality monitoring dashboard (FR-7)
- Query suggestions and auto-complete (FR-3)
- **Pilot user training materials** (Week 10) - quick-start guide, 5-min video tutorial
- **Pre-pilot dry run** (Week 11) - test with 2 users before full pilot
- Onboarding tutorial and query learning features (FR-28)
- Data quality monitoring and validation (FR-26)
- Report export to PDF/Excel/CSV (FR-25)

**Success Criteria:**
- P95 response time ≤ 5 seconds for typical queries (Goal #1)
- Task completion time reduced by ≥ 60% vs. traditional ERP (Goal #1)
- Groundedness ≥ 0.80, correctness ≥ 0.75 on test set (Goal #3)
- **CSAT ≥ 4.0/5.0 post-task measured with pilot users** (Goal #2)
- Onboarding time ≤ 30 minutes (NFR-7)
- **Pilot users validate system usability and provide actionable feedback**

**Critical External Dependencies:**
- Epic 2 complete
- **Pilot user group** (10+ users) committed and trained

**Success Gate (Epic 3 → Epic 4):**
- ✅ CSAT ≥ 4.0/5.0 measured with pilot users
- ✅ Performance targets met (P95 ≤ 5 sec)
- ✅ Pilot users confirm system is usable and valuable
- ✅ Accuracy validation maintains ≥ 99% on test suite

**Risks:**
- Performance bottlenecks in vector search or LLM latency, user adoption challenges, pilot user no-shows
- **Mitigation**: Training materials, dry run, confirmed pilot commitments, performance work starting in Epic 2

---

### Epic 4: Security, Compliance, and Production Readiness

**Goal:** Harden system for production deployment with security controls, compliance features, audit trails, resilience mechanisms, and operational monitoring.

**Business Value:** Enables safe production rollout. Meets regulatory requirements. Provides operational visibility.

**Estimated Stories:** 6-7 stories (~15-20 points) - reduced due to Epic 1 adjustments and parallel prep work

**Key Capabilities:**
- Role-based access control integrated with ERP permissions (FR-15)
- Compliance audit trail with immutable logging (FR-27)
- Regulatory audit export functionality (FR-27)
- Security hardening: encryption at rest/transit, credential management (NFR-4)
- **External dependency resilience and failover** (FR-23) - complete LLM/Vector DB/ERP failover (foundation built in Epic 1)
- Monitoring dashboard for admins (latency, errors, index status) (NFR-8)
- Alerting for system degradation and data quality issues

**Success Criteria:**
- All security controls pass penetration testing
- **Audit trail meets Vietnam Circular 200/2014/TT-BTC requirements (validated by compliance expert)**
- System achieves 99% uptime during business hours (NFR-3)
- Zero security or compliance violations during pilot
- Operational runbook complete and validated
- **Compliance review PASSED** (not just scheduled)

**Critical External Dependencies:**
- Epic 3 complete
- **Security audit team** - schedule for Week 14-15
- **Compliance expert** for pre-audit in Week 14 before formal review

**Parallel Opportunity:**
- **Weeks 10-12** (during Epic 3): Begin security design, compliance documentation, monitoring architecture
- Allows Epic 4 implementation to focus on execution vs. planning

**Success Gate (Epic 4 → Production):**
- ✅ Penetration testing passed with no critical vulnerabilities
- ✅ Compliance review passed by Vietnam expert
- ✅ Operational runbook validated with dry-run deployment
- ✅ Monitoring and alerting tested with simulated failures

**Risks:**
- Compliance gaps, security vulnerabilities, operational complexity
- **Mitigation**: Early compliance consultation (pre-project), security-by-design principles, parallel prep work

---

### Epic Sequencing and Phasing (Dependency-Optimized with Success Gates)

**Pre-Project (Weeks -4 to 0): Risk Mitigation**
- Submit formal ERP access request with PII handling plan, specific table list, security assessment
- Engage Vietnam Circular 200/2014/TT-BTC compliance expert, document all requirements
- Prepare synthetic test dataset as backup for development
- Confirm LLM API provider with demonstrated P95 latency ≤ 2 seconds

**Phase 1 (Weeks 1-5):** Epic 1 - Core RAG Pipeline + Foundation
- Week 1-2: **CRITICAL**: Validate ERP access + PII masking, basic RAG pipeline, LLM abstraction
- Week 3-4: Vector DB optimization, **performance spike testing**, schema monitoring (FR-21), indexing at scale
- Week 5: Epic 1 complete + **Begin pilot user recruitment**
- Deliverable: Working RAG prototype with basic Q&A capability + schema monitoring + performance benchmarks
- Validation: Internal team testing with sample queries
- **Success Gate**: Performance ≤ 8 sec P95 ✅, Accounting expert validates approach ✅, ERP access stable ✅

**Phase 2 (Weeks 6-9):** Epic 2 - Accounting Domain Intelligence
- Week 6-7: AR/AP/GL queries, domain expert validation sessions, **LLM caching implementation**
- Week 7-8: **Pilot user recruitment and pre-briefing** (target: 10-15 users)
- Week 8: Cross-module orchestration (FR-19), **compliance design review** - CRITICAL checkpoint
- Week 9: **Comprehensive accuracy validation** (100+ queries), performance baseline testing, Epic 2 complete
- Deliverable: Domain-optimized system supporting all core query types
- Validation: Accounting team review, accuracy validation against ERP
- **Success Gate**: Accuracy ≥ 99% ✅, 10+ pilot users confirmed ✅, Compliance design approved ✅, Performance path to 5-sec clear ✅

**Phase 3 (Weeks 10-13):** Epic 3 - UX and Performance (+ Epic 4 Prep in Parallel)
- Week 10-11:
  - **Team A**: UX optimization, query complexity tiering, verification features, **training materials**
  - **Team B**: Security design, compliance documentation prep
- Week 11: **Pre-pilot dry run** with 2 users
- Week 12:
  - **Team A**: **Pilot testing (10+ users)**, CSAT measurement, feedback collection
  - **Team B**: Monitoring architecture, operational runbook draft
- Week 13: Epic 3 complete, Epic 4 ready to implement
- Deliverable: Production-ready MVP with optimized UX
- Validation: Pilot user group testing
- **Success Gate**: CSAT ≥ 4.0/5.0 measured ✅, Performance P95 ≤ 5 sec ✅, Pilot users validate usability ✅, Accuracy ≥ 99% maintained ✅

**Phase 4 (Weeks 14-16):** Epic 4 - Security and Compliance (Accelerated)
- Week 14: RBAC implementation, audit trail, **compliance pre-audit by expert**
- Week 15: Failover completion, security hardening, **penetration testing**
- Week 16: **Formal compliance review**, operational readiness check, production deployment
- Deliverable: Hardened system ready for production rollout
- Validation: Security and compliance validated
- **Success Gate**: Penetration testing passed ✅, **Compliance review PASSED** (not just scheduled) ✅, Runbook validated ✅, Monitoring tested ✅

**Total Timeline:** 16 weeks (4 months) to production-ready MVP

---

### Critical Path and Dependency Highlights

**Blocking Dependencies:**
1. **ERP Database Access** (Week 1) → Blocks Epic 1 entirely - **PRE-PROJECT MITIGATION REQUIRED**
2. **Accounting Domain Expert** (Week 3-9) → Validates Epic 1 & 2 - **ENGAGE WEEK 3**
3. **Pilot User Group** (Week 9) → Blocks Epic 3 testing - **RECRUIT WEEKS 7-8, CONFIRM BY WEEK 9**
4. **Compliance Expert** (Pre-project, Week 8, Week 14) → Design review, pre-audit, formal review
5. **Epic Sequential Chain**: Epic 1 → Epic 2 → Epic 3 → Epic 4 (cannot significantly parallelize)

**Success Gates Between Epics:**
- **Epic 1 → 2**: Performance benchmarks, expert validation, ERP access
- **Epic 2 → 3**: Accuracy ≥ 99%, pilot users confirmed, compliance design sign-off
- **Epic 3 → 4**: CSAT ≥ 4.0, performance targets, usability validated
- **Epic 4 → Production**: Security passed, compliance PASSED, runbook validated

**Parallel Opportunities:**
- Epic 4 prep work during Epic 3 execution (Weeks 10-12) reduces Epic 4 duration
- Performance work (caching, streaming) starts in Epic 2, optimized in Epic 3

**Cross-Epic Features:**
- FR-18 (Index Freshness): Foundation in Epic 1, incremental in Epic 2, UX in Epic 3, monitoring in Epic 4
- FR-19 (Cross-Module): Built in Epic 2, optimized in Epic 3, secured in Epic 4
- FR-23 (Resilience): Abstraction in Epic 1, failover in Epic 4
- FR-30 (Validation): Test suite in Epic 2, automated monitoring in Epic 4

---

**Note:** Detailed story breakdown with acceptance criteria is provided in the separate `epics.md` document.

{{epic_note}}

## Out of Scope

{{out_of_scope}}

---

## Next Steps

{{next_steps}}

## Document Status

- [ ] Goals and context validated with stakeholders
- [ ] All functional requirements reviewed
- [ ] User journeys cover all major personas
- [ ] Epic structure approved for phased delivery
- [ ] Ready for architecture phase

_Note: See technical-decisions.md for captured technical context_

---

_This PRD adapts to project level 3 - providing appropriate detail without overburden._

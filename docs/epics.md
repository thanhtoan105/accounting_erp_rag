# accounting_erp_rag - Epic & Story Breakdown

**Project:** accounting_erp_rag
**Date:** 2025-10-16
**Project Level:** 3
**Total Estimated Stories:** 38 stories
**Total Estimated Points:** 82-92 story points
**Timeline:** 16 weeks (4 months)

---

## Epic Summary

| Epic | Name | Stories | Points | Weeks | Dependencies |
|------|------|---------|--------|-------|--------------|
| Epic 1 | Core RAG Pipeline and Infrastructure | 13 | 28 | 1-5 | ERP access, Vector DB, LLM API |
| Epic 2 | Accounting Domain Intelligence | 11 | 26 | 6-9 | Epic 1, Domain expert |
| Epic 3 | User Experience & Performance | 8 | 20 | 10-13 | Epic 2, Pilot users |
| Epic 4 | Security & Production Readiness | 6 | 16 | 14-16 | Epic 3, Security audit |
| **Total** | | **38** | **90** | **16** | |

---

# Epic 1: Core RAG Pipeline and Infrastructure (Foundation)

**Goal:** Establish the foundational RAG pipeline capable of indexing ERP data, retrieving relevant documents, and generating grounded answers with citations.

**Timeline:** Weeks 1-5
**Estimated Stories:** 13 stories
**Estimated Points:** 28 points

---

## E1-S1: ERP Database Access Setup and Validation

**Story Points:** 3
**Priority:** CRITICAL - MUST START WEEK 1
**Dependencies:** None (blocking for all other stories)

**Description:**
Set up secure read-only PostgreSQL access to the existing ERP database. Validate all required tables are accessible (invoices, vouchers, journal entries, AR, AP, GL, bank transactions, customers, vendors). Document schema structure and implement connection pooling.

**Acceptance Criteria:**
- [ ] Read-only PostgreSQL connection established with proper credentials
- [ ] Connection pooling configured (min: 2, max: 10 connections)
- [ ] All 60+ required tables accessible and documented
- [ ] Schema documentation generated (table names, columns, relationships)
- [ ] Connection health check endpoint implemented
- [ ] Read replica configuration tested if available
- [ ] Zero data modification capability (read-only enforced at DB level)
- [ ] Connection retry logic implemented (exponential backoff)

**Technical Notes:**
- Use Supabase client or pg library for PostgreSQL connection
- Document specific tables: `invoices`, `payments`, `journal_entries`, `accounts`, `customers`, `vendors`, `bank_transactions`, `tax_declarations`
- Verify Circular 200/2014/TT-BTC compliance fields exist
- Test connection under load (simulate 20 concurrent reads)

**Risk Mitigation:**
- If ERP access delayed: Use synthetic test dataset prepared pre-project
- Escalation path: PM notified immediately if access not secured by Day 2

---

## E1-S2: PII Masking and Data Anonymization

**Story Points:** 2
**Priority:** CRITICAL - WEEK 1
**Dependencies:** E1-S1

**Description:**
Implement PII (Personally Identifiable Information) detection and masking for all data extracted from ERP before indexing. Ensure customer names, tax IDs, phone numbers, emails, and addresses are anonymized or tokenized for non-production environments.

**Acceptance Criteria:**
- [ ] PII fields identified across all tables (name, tax_id, phone, email, address)
- [ ] Masking rules implemented for each PII field type
- [ ] Tokenization strategy for linking records without exposing PII
- [ ] Validation that no PII appears in indexed vector embeddings
- [ ] Compliance documentation: list of all PII fields and masking approach
- [ ] Test suite validates masking on 100+ sample records
- [ ] Performance impact measured (masking adds < 100ms per document)

**Technical Notes:**
- Use deterministic hashing for customer/vendor IDs (allows joins without PII)
- Mask patterns: names → "Customer_12345", tax_ids → "TAX_*****1234"
- Store original → masked mapping in secure vault for production audit trail
- Consider using libraries like `Faker` for realistic test data generation

---

## E1-S3: Vector Database Setup (Supabase Vector)

**Story Points:** 2
**Priority:** HIGH - WEEK 1-2
**Dependencies:** None (can run parallel to E1-S1)

**Description:**
Configure Supabase Vector (pgvector) for semantic search. Set up vector tables, indexes, and distance metrics. Optimize embedding dimensions and index parameters for accounting domain documents.

**Acceptance Criteria:**
- [ ] Supabase project created with Vector extension enabled
- [ ] Vector table schema designed: `id`, `document_id`, `embedding` (vector dimension), `metadata` (JSONB), `created_at`
- [ ] HNSW or IVFFlat index created for cosine similarity search
- [ ] Index tuning: test with 10K, 50K, 100K vectors
- [ ] Query latency tested: P95 < 1500ms for top-10 retrieval
- [ ] Metadata filtering tested (by module, date range, document type)
- [ ] Backup and restore procedures documented
- [ ] Connection pooling configured for vector DB

**Technical Notes:**
- Embedding dimension: 1536 (OpenAI ada-002) or 768 (sentence-transformers)
- Distance metric: cosine similarity (best for semantic search)
- HNSW parameters: `m=16, ef_construction=64` (balance speed/accuracy)
- Test query: "Show AR aging report" should return < 1.5s with 100K vectors
- Consider partitioning by fiscal_period for large datasets

---

## E1-S4: Document Embedding Generation Pipeline

**Story Points:** 3
**Priority:** HIGH - WEEK 2
**Dependencies:** E1-S1, E1-S2, E1-S3

**Description:**
Implement pipeline to extract documents from ERP, generate vector embeddings using domain-optimized models (supporting Vietnamese and English), and store in vector database. Handle batch and incremental processing.

**Acceptance Criteria:**
- [ ] Document extraction logic for all document types (invoices, vouchers, entries, etc.)
- [ ] Text preparation: concatenate relevant fields (description, amounts, dates, codes)
- [ ] Embedding generation: support OpenAI ada-002 or sentence-transformers
- [ ] Batch processing: handle 10K documents in < 30 minutes
- [ ] Vietnamese text handling validated (UTF-8, diacritics preserved)
- [ ] Metadata extraction: document_type, fiscal_period, module, status
- [ ] Error handling: skip malformed documents, log errors
- [ ] Progress tracking: log embedding generation progress (every 1000 docs)

**Technical Notes:**
- Embedding API rate limits: OpenAI 3000 RPM → batch requests
- Text preparation template: `"Invoice {number} from {customer} dated {date}: {description}. Amount: {amount} VND. Status: {status}."`
- Consider caching embeddings to avoid regeneration (hash document content)
- Test with mixed Vietnamese/English queries

**Performance Target:**
- Embedding generation: < 100ms per document (API call + processing)
- Batch throughput: 200 documents/minute

---

## E1-S5: Basic RAG Query Processing Pipeline

**Story Points:** 3
**Priority:** HIGH - WEEK 2-3
**Dependencies:** E1-S4

**Description:**
Implement end-to-end RAG query pipeline: accept natural language query (Vietnamese/English), generate query embedding, retrieve top-k relevant documents from vector DB, and prepare context for LLM.

**Acceptance Criteria:**
- [ ] Query API endpoint accepts natural language input
- [ ] Query embedding generation (same model as document embeddings)
- [ ] Vector similarity search retrieves top-10 documents
- [ ] Retrieved documents ranked by relevance score
- [ ] Context window management: fit documents within LLM token limit (e.g., 8K tokens)
- [ ] Metadata filtering: support filters by date range, module, document type
- [ ] Query logging: log all queries with timestamp and user_id
- [ ] P95 retrieval latency < 1500ms measured and logged

**Technical Notes:**
- Query embedding: use same model as document embeddings (consistency)
- Context pruning: if documents exceed token limit, truncate lowest-ranked docs
- Return document metadata: `document_id`, `document_type`, `relevance_score`, `excerpt`
- Test queries: "What is current AR balance?", "Show overdue invoices", "Khách hàng nào còn nợ?" (Vietnamese)

---

## E1-S6: LLM Integration and Answer Generation

**Story Points:** 3
**Priority:** HIGH - WEEK 3
**Dependencies:** E1-S5

**Description:**
Integrate LLM (OpenAI GPT-4 or Anthropic Claude) for answer generation. Implement prompt engineering for accounting domain, citation generation, and grounding in retrieved documents. Support streaming for partial results.

**Acceptance Criteria:**
- [ ] LLM API integration (OpenAI or Anthropic) with error handling
- [ ] Prompt template designed for accounting domain with retrieved documents
- [ ] Prompt includes instruction: "Answer only from provided documents. Cite sources."
- [ ] Citation extraction: parse LLM response for document references
- [ ] Streaming support: return partial results as they generate
- [ ] P95 LLM latency < 1500ms measured
- [ ] Groundedness validation: ensure answer references retrieved docs
- [ ] Test with 20+ sample queries across AR/AP/GL domains

**Technical Notes:**
- Prompt structure:
  ```
  Context: [Retrieved documents with IDs]
  Question: {user_query}
  Instructions: Answer based only on the context provided. Cite document IDs. If uncertain, say so.
  Answer:
  ```
- Temperature: 0.1 (low for factual accuracy)
- Max tokens: 500 (concise answers)
- Streaming: use SSE (Server-Sent Events) for real-time response

**Performance Target:**
- LLM API call: P95 < 1.5s, P99 < 3s
- End-to-end query: P95 < 5s (retrieval + generation)

---

## E1-S7: LLM Provider Abstraction Layer

**Story Points:** 2
**Priority:** MEDIUM - WEEK 3
**Dependencies:** E1-S6

**Description:**
Create abstraction layer supporting multiple LLM providers (OpenAI, Anthropic, local models) with unified interface. Enable failover and streaming support. Foundation for FR-9.10.4 resilience.

**Acceptance Criteria:**
- [ ] Abstract LLM interface with methods: `generate()`, `stream()`, `embed()`
- [ ] Implementations for OpenAI GPT-4 and Anthropic Claude
- [ ] Configuration-driven provider selection (no code changes)
- [ ] Automatic retry logic with exponential backoff
- [ ] Provider health check: ping before routing requests
- [ ] Streaming support abstracted across providers
- [ ] Test failover: simulate primary provider failure → secondary takes over
- [ ] Performance parity validated (both providers meet latency targets)

**Technical Notes:**
- Use strategy pattern or factory pattern for provider abstraction
- Config example: `LLM_PRIMARY=openai`, `LLM_SECONDARY=anthropic`
- Failover trigger: 3 consecutive failures or P95 > 3s
- Consider circuit breaker pattern for automatic failover
- Document provider-specific quirks (token limits, streaming format)

---

## E1-S8: ERP Schema Evolution Monitoring

**Story Points:** 2
**Priority:** MEDIUM - WEEK 4
**Dependencies:** E1-S1

**Description:**
Implement automated monitoring to detect ERP schema changes (new tables, columns, renamed fields). Alert administrators and trigger index schema updates. Moved from Epic 4 for early warning.

**Acceptance Criteria:**
- [ ] Daily schema snapshot: capture table names, column names, data types
- [ ] Schema diff detection: compare current vs. previous snapshot
- [ ] Alert triggered on breaking changes (removed columns, renamed tables)
- [ ] Dashboard displays schema change history
- [ ] Automatic index schema update for non-breaking changes (new columns)
- [ ] Manual review required for breaking changes
- [ ] Test with simulated schema changes (add column, rename table)
- [ ] Detection latency < 24 hours (daily check sufficient)

**Technical Notes:**
- Query PostgreSQL information_schema: `SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public'`
- Store snapshots in JSONB column: `schema_snapshots` table
- Breaking changes: removed columns, renamed tables, changed data types
- Non-breaking: new columns, new tables
- Email alert to admin@example.com on breaking changes

---

## E1-S9: Incremental Indexing Pipeline

**Story Points:** 3
**Priority:** MEDIUM - WEEK 4
**Dependencies:** E1-S4, E1-S8

**Description:**
Implement incremental indexing to keep vector database synchronized with ERP updates. Detect new/modified documents and update embeddings within 5-minute latency SLA (FR-9.10.2).

**Acceptance Criteria:**
- [ ] Change detection: identify new/updated documents via `updated_at` timestamp
- [ ] Incremental indexing job runs every 5 minutes
- [ ] Only process documents modified since last sync
- [ ] Delete outdated embeddings for modified documents before re-indexing
- [ ] Handle document deletions (soft delete in ERP → remove from vector DB)
- [ ] Sync status tracking: last_sync_timestamp, docs_processed, errors
- [ ] Alert if sync fails or latency exceeds 10 minutes
- [ ] Test with 1000 document updates → validate < 5 min sync

**Technical Notes:**
- Track last sync timestamp in `sync_status` table
- Query: `SELECT * FROM invoices WHERE updated_at > {last_sync_timestamp}`
- Handle concurrent updates: use transaction isolation
- Consider CDC (Change Data Capture) for real-time sync if available
- Batch updates: process 100 documents per batch

---

## E1-S10: Performance Spike Testing (LLM, Vector DB, End-to-End)

**Story Points:** 3
**Priority:** CRITICAL - WEEK 3-4
**Dependencies:** E1-S6, E1-S9

**Description:**
Conduct comprehensive performance testing with realistic data volume (500K documents). Benchmark LLM latency, vector DB retrieval, and end-to-end query performance. Identify bottlenecks early.

**Acceptance Criteria:**
- [ ] Load 500K test documents into vector DB
- [ ] Benchmark vector retrieval: P95, P99, max latency for 1000 queries
- [ ] Benchmark LLM generation: P95, P99 for 1000 queries
- [ ] Benchmark end-to-end: P95 ≤ 8s (Epic 1 target, Epic 3 will optimize to 5s)
- [ ] Identify bottlenecks: profiling report with slowest components
- [ ] Test concurrent load: 20 users querying simultaneously
- [ ] Document results: latency breakdown per component
- [ ] Escalate if P95 > 10s (blocks Epic 1 success gate)

**Technical Notes:**
- Use load testing tool: Locust, k6, or Artillery
- Test query mix: 50% simple (AR balance), 30% moderate (aging report), 20% complex (cross-module)
- Profile with APM tool: Datadog, New Relic, or OpenTelemetry
- Success gate: P95 ≤ 8s, if not → investigate before Epic 2

**Performance Targets (Epic 1 Baseline):**
- Vector retrieval: P95 < 1.5s
- LLM generation: P95 < 2s
- End-to-end: P95 < 8s (buffer for Epic 3 optimization)

---

## E1-S11: Basic Error Handling and Logging

**Story Points:** 2
**Priority:** MEDIUM - WEEK 4
**Dependencies:** E1-S6

**Description:**
Implement error handling for RAG pipeline failures: LLM API errors, vector DB timeouts, malformed queries. Log all errors with context for debugging. Foundation for FR-9.10.3 graceful degradation.

**Acceptance Criteria:**
- [ ] Try-catch blocks around all external API calls (LLM, Vector DB, ERP)
- [ ] Error types classified: retrieval_error, generation_error, database_error, validation_error
- [ ] User-friendly error messages: "Unable to retrieve data. Please try again."
- [ ] Detailed error logs: timestamp, user_id, query, error_type, stack_trace
- [ ] Retry logic for transient errors (3 retries with exponential backoff)
- [ ] Circuit breaker for repeated failures (5 failures → open circuit for 60s)
- [ ] Test error scenarios: LLM API down, Vector DB timeout, malformed query
- [ ] Error rate monitored: alert if error rate > 5%

**Technical Notes:**
- Log to structured logging system: JSON format with fields
- Example log: `{"timestamp": "2024-10-16T14:32:00Z", "user_id": "user123", "query": "Show AR", "error": "LLM_API_TIMEOUT", "latency_ms": 5000}`
- Transient errors: network timeouts, rate limits, temporary API unavailability
- Permanent errors: authentication failure, invalid API key
- User sees generic message; admin sees detailed logs

---

## E1-S12: Accounting Domain Expert Engagement and Validation

**Story Points:** 1
**Priority:** CRITICAL - WEEK 3 (not Week 5)
**Dependencies:** E1-S6 (basic RAG working)

**Description:**
Engage accounting domain expert to validate indexing logic, accounting calculations, and chart of accounts mapping. Ensure system understands Vietnam Circular 200/2014/TT-BTC terminology. Weekly validation sessions.

**Acceptance Criteria:**
- [ ] Domain expert recruited and briefed on project goals
- [ ] Weekly 1-hour validation sessions scheduled (Weeks 3-9)
- [ ] Expert reviews indexed document samples: validate completeness and accuracy
- [ ] Expert tests sample queries: "Show AR aging", "Khách hàng nợ", "Trial balance"
- [ ] Expert validates calculation logic: aging buckets, balance calculations
- [ ] Expert confirms terminology aligns with Circular 200 standards
- [ ] Feedback documented and prioritized for fixes
- [ ] Sign-off obtained: "Indexing logic is sound for accounting domain"

**Technical Notes:**
- Expert profile: CPA or Chief Accountant with 5+ years Vietnam accounting experience
- Prepare demo with 20-30 test queries across AR/AP/GL/Bank domains
- Document feedback: create issues in backlog for each finding
- Success gate: Expert confirms approach is correct before Epic 2

---

## E1-S13: Epic 1 Integration Testing and Documentation

**Story Points:** 2
**Priority:** HIGH - WEEK 5
**Dependencies:** E1-S1 through E1-S12

**Description:**
Conduct end-to-end integration testing of complete RAG pipeline. Validate all components work together. Document architecture, API endpoints, and deployment procedures. Confirm Epic 1 success gates met.

**Acceptance Criteria:**
- [ ] End-to-end test: user query → retrieval → generation → answer with citations
- [ ] Test 50+ queries across all document types and domains
- [ ] Validate success criteria: recall@10 ≥ 0.90, citation coverage ≥ 95%, P95 ≤ 8s
- [ ] Architecture diagram documented (data flow, components, dependencies)
- [ ] API documentation: endpoints, request/response schemas, authentication
- [ ] Deployment runbook: setup steps, configuration, troubleshooting
- [ ] **Epic 1 Success Gates validated:**
  - ✅ Performance P95 ≤ 8s achieved OR escalation plan active
  - ✅ Accounting expert validated approach
  - ✅ ERP access stable
- [ ] Retrospective conducted: lessons learned, blockers, improvements

**Technical Notes:**
- Use Postman or Swagger for API testing
- Document environment variables: `DB_CONNECTION_STRING`, `LLM_API_KEY`, `VECTOR_DB_URL`
- Include troubleshooting guide: common errors and solutions
- Success gate review: PM, Tech Lead, Accounting Expert sign-off

---

# Epic 2: Accounting Domain Intelligence and Query Capabilities

**Goal:** Extend RAG pipeline with accounting-specific query patterns, multi-module orchestration, and domain-optimized retrieval for AR, AP, GL, and bank reconciliation queries.

**Timeline:** Weeks 6-9
**Estimated Stories:** 11 stories
**Estimated Points:** 26 points

---

## E2-S1: Accounts Receivable (AR) Query Support

**Story Points:** 3
**Priority:** HIGH - WEEK 6
**Dependencies:** Epic 1 complete

**Description:**
Implement AR-specific query patterns: customer balances, aging reports, invoice details, payment history. Optimize retrieval for AR documents (invoices, payments, customers). Support queries like "Show AR aging report" and "Customer ABC outstanding balance".

**Acceptance Criteria:**
- [ ] AR document types indexed: invoices, payments, customer master data
- [ ] Query patterns supported:
  - Current AR balance by customer
  - AR aging buckets (Current, 1-30, 31-60, 61-90, 90+ days)
  - Outstanding invoice details with due dates
  - Customer payment history
  - Overdue account identification
- [ ] AR-specific prompt templates for accurate aging calculations
- [ ] Test with 20+ AR queries in Vietnamese and English
- [ ] Aging calculation validated against ERP reports (100% accuracy)
- [ ] Response includes citations: invoice numbers, payment vouchers
- [ ] P95 latency < 5s for AR queries (simple query tier)

**Technical Notes:**
- AR table joins: `invoices` JOIN `payments` JOIN `customers`
- Aging calculation: `CASE WHEN due_date < CURRENT_DATE THEN CURRENT_DATE - due_date ELSE 0 END`
- Example queries: "Hiện tại khách hàng ABC nợ bao nhiêu?", "Show invoices overdue 60+ days"
- Validate against ERP aging report: generate same report via RAG vs. ERP UI

---

## E2-S2: Accounts Payable (AP) Query Support

**Story Points:** 3
**Priority:** HIGH - WEEK 6
**Dependencies:** E2-S1 (can run parallel)

**Description:**
Implement AP-specific query patterns: vendor balances, payment due dates, bill details, payment history, cash flow forecasting. Support queries like "Show upcoming payments" and "Vendor XYZ outstanding balance".

**Acceptance Criteria:**
- [ ] AP document types indexed: bills, vendor payments, vendor master data
- [ ] Query patterns supported:
  - Current AP balance by vendor
  - AP aging buckets (Current, 1-30, 31-60, 61-90, 90+ days)
  - Payment due dates and prioritization (next 7/14/30 days)
  - Vendor payment history
  - Cash flow forecast based on upcoming obligations
- [ ] AP-specific prompt templates for payment prioritization
- [ ] Test with 20+ AP queries in Vietnamese and English
- [ ] Aging calculation validated against ERP reports (100% accuracy)
- [ ] Response includes citations: bill numbers, payment vouchers
- [ ] P95 latency < 5s for AP queries (simple query tier)

**Technical Notes:**
- AP table joins: `bills` JOIN `vendor_payments` JOIN `vendors`
- Payment prioritization: sort by `due_date ASC`, flag overdue bills
- Example queries: "Các khoản phải trả đến hạn tuần tới?", "Show vendors with overdue bills"
- Cash flow forecast: sum bills due in next 7/14/30/60/90 days

---

## E2-S3: General Ledger (GL) Query Support

**Story Points:** 3
**Priority:** HIGH - WEEK 7
**Dependencies:** E2-S1, E2-S2

**Description:**
Implement GL-specific query patterns: account balances, journal entry details, trial balance, account reconciliation, period-over-period comparisons. Support 4-level chart of accounts hierarchy per Circular 200.

**Acceptance Criteria:**
- [ ] GL document types indexed: journal entries, accounts (chart of accounts), account balances
- [ ] Query patterns supported:
  - Account balance by code (e.g., "Balance of account 131")
  - Journal entry details with entry number, date, debits, credits
  - Trial balance: all account balances with debit/credit totals
  - Account reconciliation status
  - Period-over-period comparison (month, quarter, year)
  - Account movement (opening, debits, credits, closing)
- [ ] Support 4-level hierarchy: Class (1xx) → Group (131) → Detail (131-001)
- [ ] Test with 20+ GL queries in Vietnamese and English
- [ ] Trial balance validated: sum(debits) = sum(credits)
- [ ] Response includes citations: journal entry numbers, voucher references
- [ ] P95 latency < 5s for GL queries

**Technical Notes:**
- GL table joins: `journal_entries` JOIN `accounts` JOIN `account_balances`
- Hierarchy navigation: parent-child relationships in `accounts` table
- Trial balance query: `SELECT account_code, SUM(debit), SUM(credit) FROM account_balances GROUP BY account_code`
- Example queries: "Show trial balance for Q3", "Account 131 balance", "Số dư tài khoản 511"

---

## E2-S4: Bank Reconciliation Query Support

**Story Points:** 2
**Priority:** MEDIUM - WEEK 7
**Dependencies:** E2-S3

**Description:**
Implement bank reconciliation query patterns: book balance vs. bank statement balance, unreconciled transactions, outstanding items, discrepancies. Support queries like "Show bank reconciliation status" and "Unreconciled transactions for October".

**Acceptance Criteria:**
- [ ] Bank documents indexed: bank transactions, reconciliation records, bank statements
- [ ] Query patterns supported:
  - Book balance vs. bank statement balance comparison
  - Unreconciled transactions (outstanding checks, deposits in transit)
  - Reconciliation status by bank account and period
  - Discrepancy identification
  - Bank transaction matching suggestions
- [ ] Test with 15+ bank reconciliation queries
- [ ] Reconciliation logic validated against ERP reconciliation module
- [ ] Response includes citations: transaction IDs, statement references
- [ ] P95 latency < 5s for bank queries

**Technical Notes:**
- Bank table joins: `bank_transactions` JOIN `reconciliations` JOIN `bank_accounts`
- Reconciliation status: `reconciled = TRUE/FALSE`
- Discrepancy detection: book_balance - bank_statement_balance != 0
- Example queries: "Show unreconciled transactions", "Bank account 112 reconciliation status"

---

## E2-S5: Cross-Module Query Orchestration

**Story Points:** 5
**Priority:** CRITICAL - WEEK 8 (Epic 3 dependency)
**Dependencies:** E2-S1, E2-S2, E2-S3, E2-S4

**Description:**
Implement cross-module orchestration for complex queries requiring data from multiple ERP modules (AR + AP + GL + Bank). Support queries like "What is our current cash position and payment obligations?" or "Show unmatched payments across AR and Bank".

**Acceptance Criteria:**
- [ ] Multi-module query detection: identify when query spans AR, AP, GL, Bank
- [ ] Orchestration logic: retrieve documents from multiple modules and join
- [ ] Automated data joining: link invoices → payments → bank transactions → journal entries
- [ ] Calculated result validation: validate against ERP-generated reports (≤ 1% divergence)
- [ ] Handle draft vs. posted transactions based on user role
- [ ] Test with 20+ cross-module queries
- [ ] Example queries validated:
  - "Cash position and upcoming payment obligations"
  - "Unmatched payments and suggested invoice matches"
  - "Customer ABC: invoices, payments, and bank deposits"
- [ ] P95 latency < 5s for moderate complexity queries (Tier 2)
- [ ] Orchestration budget: P95 ≤ 1000ms measured

**Technical Notes:**
- Query intent classification: detect keywords like "cash position + payments", "unmatched", "reconcile"
- Join strategy: retrieve from each module → deduplicate → rank by relevance
- Validation query: generate same report via RAG vs. direct ERP SQL query → compare
- Handle ERP locks during period-end close: use read replica or cached data
- Example: "Cash position" = sum(bank_balances) + expected_ar_collections - upcoming_ap_payments

**Success Gate:** This story is CRITICAL for Epic 3. Must complete before E3-S1.

---

## E2-S6: LLM Response Caching for Common Queries

**Story Points:** 2
**Priority:** MEDIUM - WEEK 7
**Dependencies:** E2-S5

**Description:**
Implement query result caching for common query patterns to reduce LLM API costs and improve response time. Cache results for 5 minutes with automatic invalidation on data updates.

**Acceptance Criteria:**
- [ ] Query normalization: "Show AR aging" = "AR aging report" = "AR老化報告"
- [ ] Cache storage: Redis or in-memory cache with TTL=5 minutes
- [ ] Cache key: hash of normalized query + user permissions + data timestamp
- [ ] Cache hit rate monitored: target ≥ 30% for common queries
- [ ] Automatic invalidation: clear cache when ERP data updates
- [ ] Test: same query returns cached result < 200ms
- [ ] Cache statistics dashboard: hit rate, miss rate, cache size
- [ ] Cost savings measured: reduced LLM API calls by 20-30%

**Technical Notes:**
- Use Redis with key pattern: `cache:query:{hash}:{user_id}:{timestamp}`
- Normalization: lowercase, remove punctuation, stem keywords
- Invalidation trigger: ERP sync job updates `last_data_update` timestamp → clear cache
- Monitor cache memory usage: alert if > 1GB
- Common queries: "AR aging", "AP aging", "Trial balance", "Cash position"

---

## E2-S7: Multi-Turn Conversational Context

**Story Points:** 2
**Priority:** MEDIUM - WEEK 8
**Dependencies:** E2-S5

**Description:**
Implement conversation history tracking to support follow-up questions without repeating context. Enable queries like "What about Customer ABC?" after initial query "Show AR aging report".

**Acceptance Criteria:**
- [ ] Conversation session management: unique session_id per user
- [ ] Store conversation history: previous queries and answers (last 5 turns)
- [ ] Context injection: append conversation history to LLM prompt
- [ ] Follow-up question detection: identify pronouns ("it", "that", "them") and implicit references
- [ ] Test multi-turn conversations: 10+ scenarios with 3-5 turns each
- [ ] Example conversation validated:
  - User: "Show AR aging report"
  - System: [Shows aging report]
  - User: "What about Customer ABC?" (implicit: Customer ABC in AR aging)
  - System: [Shows Customer ABC details]
- [ ] Session timeout: 30 minutes of inactivity clears context
- [ ] Context token budget: conversation history ≤ 2000 tokens

**Technical Notes:**
- Store in Redis: `session:{user_id}:{session_id}` → list of {query, answer} pairs
- Prompt template: "Previous conversation: [...] Current question: {query}"
- Follow-up detection: check for pronouns or entity references without full context
- Clear session on explicit user action: "New conversation" button

---

## E2-S8: Query Disambiguation and Clarification

**Story Points:** 2
**Priority:** MEDIUM - WEEK 8
**Dependencies:** E2-S7

**Description:**
Detect ambiguous queries and prompt user for clarification before expensive retrieval. Handle queries like "Show balance" (which balance? account, AR, AP, trial?) or "Customer ABC" (which action? balance, history, invoices?).

**Acceptance Criteria:**
- [ ] Ambiguity detection: identify queries missing context (entity, time, action)
- [ ] Clarification prompts: present multiple choice options
- [ ] Example disambiguations:
  - "balance" → "Which balance: Trial balance, Account balance, AR balance, AP balance?"
  - "Customer ABC" → "What about Customer ABC: Outstanding balance, Payment history, Invoice list?"
  - "October" → "October 2024 or previous year? Which report?"
- [ ] Test 20+ ambiguous queries
- [ ] User selection captured and used for refined query
- [ ] Fallback: if no clarification, proceed with most likely interpretation (log assumption)
- [ ] Disambiguation rate monitored: track % of queries requiring clarification

**Technical Notes:**
- Ambiguity triggers: query contains generic terms without specificity
- Generic terms: "balance", "report", "status", "list" without entity/module
- Present max 4 options to avoid decision fatigue
- Log user selections to improve ambiguity detection over time

---

## E2-S9: Comprehensive Accuracy Validation Test Suite

**Story Points:** 3
**Priority:** CRITICAL - WEEK 9 (Epic 3 success gate)
**Dependencies:** E2-S5, E2-S8

**Description:**
Create comprehensive test suite with 100+ representative queries across all accounting domains. Validate RAG-generated answers against ground truth ERP reports. Achieve ≥ 99% accuracy target (FR-9.7).

**Acceptance Criteria:**
- [ ] 100+ test queries created covering AR (20), AP (20), GL (30), Bank (15), Cross-module (15)
- [ ] Ground truth answers generated from ERP system (manual or scripted)
- [ ] Automated validation: compare RAG answer vs. ground truth
- [ ] Accuracy metrics calculated: exact match, semantic similarity, calculation correctness
- [ ] Accuracy ≥ 99% achieved on test suite
- [ ] Failed queries documented: root cause analysis for each failure
- [ ] Test suite runs automatically on every deployment (CI/CD integration)
- [ ] Regression prevention: test suite prevents accuracy degradation

**Technical Notes:**
- Ground truth generation: export ERP reports to CSV/JSON, write validation scripts
- Validation metrics:
  - Exact match: answer text matches ground truth (for simple queries)
  - Calculation match: numeric values within ≤ 1% divergence (for aging, balances)
  - Semantic similarity: cosine similarity ≥ 0.95 (for descriptive answers)
- Test query examples:
  - "AR aging report as of 2024-10-16" → validate buckets match ERP aging report
  - "Trial balance for Q3 2024" → validate debits = credits, all account balances match
  - "Upcoming payments in next 30 days" → validate list matches AP module
- Store test suite in version control: `tests/accuracy_suite.json`

**Success Gate:** This story must achieve ≥ 99% accuracy before Epic 3 starts.

---

## E2-S10: Compliance Design Review Checkpoint

**Story Points:** 1
**Priority:** CRITICAL - WEEK 8
**Dependencies:** E2-S5

**Description:**
Conduct formal compliance design review with Vietnam Circular 200/2014/TT-BTC compliance expert. Validate RBAC (FR-9.9.1), audit trail (FR-9.9.2), and data security (FR-9.9.3) design before implementation in Epic 4.

**Acceptance Criteria:**
- [ ] Compliance expert engaged and briefed on system design
- [ ] Design review session conducted (2-hour workshop)
- [ ] Review topics:
  - RBAC: user roles match ERP permissions, data filtering by role
  - Audit trail: query logging captures all required fields per Circular 200
  - Data security: encryption, PII handling, data residency
  - Retention: 10-year retention for financial queries/answers
- [ ] Expert feedback documented: approval items and required changes
- [ ] Required changes prioritized and scheduled for Epic 4
- [ ] Sign-off obtained: "Design complies with Vietnam regulations"
- [ ] Escalate if major compliance gaps identified (blocks Epic 3)

**Technical Notes:**
- Compliance expert profile: CPA or auditor with 5+ years Vietnam regulatory experience
- Prepare design documents: architecture diagram, RBAC matrix, audit trail schema, data flow diagram
- Document approval: formal sign-off email or document
- Success gate: Compliance sign-off required before Epic 3

---

## E2-S11: Pilot User Recruitment and Engagement

**Story Points:** 1
**Priority:** CRITICAL - WEEK 7-8 (Epic 3 dependency)
**Dependencies:** E2-S1, E2-S2, E2-S3

**Description:**
Recruit 10-15 pilot users (accountants, finance managers) for Epic 3 testing. Conduct pre-briefing session to explain project goals, value proposition, and testing expectations.

**Acceptance Criteria:**
- [ ] 10-15 pilot users recruited from target user groups:
  - 5 accountants (transaction-level users)
  - 3 finance managers (analytical users)
  - 2 CFO/executives (strategic users)
- [ ] Pre-briefing session conducted (1-hour presentation)
- [ ] Pilot users understand:
  - Project goals and AI/RAG benefits
  - Testing timeline and time commitment (2-3 hours/week for 4 weeks)
  - Feedback collection process (surveys, interviews)
- [ ] Pilot users commit to testing schedule
- [ ] Pilot user contact list finalized: names, roles, emails, availability
- [ ] Training materials prepared for Week 10 (quick-start guide, video tutorial)
- [ ] Escalate if < 10 users recruited (blocks Epic 3)

**Technical Notes:**
- Recruitment channels: internal email, manager nominations, volunteer sign-up
- Incentives: early access, training, certificate of participation
- Commitment: 2-3 hours/week for 4 weeks (Weeks 10-13)
- Pre-briefing agenda: project overview, demo of current system, Q&A
- Success gate: 10+ committed pilot users before Epic 3

---

# Epic 3: User Experience, Performance, and Quality Assurance

**Goal:** Optimize user-facing experience with fast performance, quality monitoring, verification tools, and onboarding features to achieve MVP usability targets.

**Timeline:** Weeks 10-13
**Estimated Stories:** 8 stories
**Estimated Points:** 20 points

---

## E3-S1: Performance Optimization to P95 ≤ 5s Target

**Story Points:** 5
**Priority:** CRITICAL - WEEK 10-11
**Dependencies:** Epic 2 complete, E2-S5 (orchestration), E2-S9 (accuracy validated)

**Description:**
Optimize end-to-end query performance from P95 ≤ 8s (Epic 1 baseline) to P95 ≤ 5s (MVP target). Focus on vector retrieval, LLM generation, and orchestration bottlenecks identified in E1-S10 performance testing.

**Acceptance Criteria:**
- [ ] Bottleneck analysis: identify slowest components from Epic 1 performance tests
- [ ] Vector retrieval optimization:
  - Index tuning: adjust HNSW parameters (m, ef_search)
  - Query optimization: reduce embedding dimension or use approximate search
  - Target: P95 < 1200ms (down from 1500ms)
- [ ] LLM generation optimization:
  - Prompt reduction: shorten prompts, remove redundant context
  - Streaming: return partial results faster (perceived performance)
  - Target: P95 < 1200ms (down from 1500ms)
- [ ] Orchestration optimization:
  - Parallel retrieval: fetch from multiple modules concurrently
  - Caching: leverage E2-S6 caching for common sub-queries
  - Target: P95 < 800ms (down from 1000ms)
- [ ] End-to-end P95 ≤ 5s achieved for 90% of test queries
- [ ] Performance regression testing: validate optimization doesn't degrade accuracy
- [ ] Load testing: validate performance under 20 concurrent users

**Technical Notes:**
- Profiling tools: use APM (New Relic, Datadog) to identify hot paths
- HNSW tuning: increase `ef_search` for accuracy, decrease `m` for speed
- Prompt optimization: remove examples, reduce context duplication
- Parallel retrieval: use `Promise.all()` or goroutines to fetch AR/AP/GL concurrently
- Success gate: P95 ≤ 5s for Tier 1-2 queries, ≤ 8s for Tier 3

**Risk:** If P95 cannot reach 5s, escalate and adjust target to 6-7s with stakeholder approval.

---

## E3-S2: Query Complexity Tiering and Expectation Management

**Story Points:** 2
**Priority:** HIGH - WEEK 11
**Dependencies:** E3-S1

**Description:**
Classify queries into complexity tiers (Simple/Moderate/Complex) and display expected response time upfront. Manage user expectations for long-running queries (FR-9.8.5).

**Acceptance Criteria:**
- [ ] Query complexity classifier:
  - **Tier 1 (Simple)**: Single module, recent data (e.g., "AR balance Customer ABC") → 2-3s target
  - **Tier 2 (Moderate)**: Cross-module, calculations (e.g., "Cash position and payments") → 4-5s target
  - **Tier 3 (Complex)**: Historical analysis, multi-step reasoning (e.g., "AR trends last 6 months") → 8-10s target
- [ ] Display expected response time before query execution
- [ ] UI message: "This is a complex query. Expected response time: 8-10 seconds."
- [ ] Loading indicators with progress context: "Retrieving invoices...", "Analyzing AR data...", "Generating answer..."
- [ ] Test classifier accuracy: 20 queries correctly classified
- [ ] User feedback: CSAT survey includes "Were expectations met for query speed?"

**Technical Notes:**
- Complexity detection: keyword-based (e.g., "trend", "historical", "6 months" → Tier 3)
- Module detection: AR/AP/GL/Bank keywords → Single or cross-module
- UI component: progress bar or spinner with status text
- Timeout handling: Tier 3 queries get 10s timeout, Tier 1-2 get 6s timeout

---

## E3-S3: Answer Verification and Drill-Down to Source Data

**Story Points:** 2
**Priority:** HIGH - WEEK 11
**Dependencies:** E3-S1

**Description:**
Implement "Show source data" feature allowing users to view exact ERP records used to generate answer. Display calculation logic and intermediate steps for computed values. Build user trust (FR-9.8.2).

**Acceptance Criteria:**
- [ ] "Show source data" button visible on every answer
- [ ] Clicking button displays:
  - List of source documents (invoices, vouchers, entries) with IDs
  - Excerpts or full text of source documents
  - Calculation logic for computed values (e.g., "Total AR = sum of invoices 001, 002, 003")
- [ ] One-click navigation to source document in ERP UI (deep link if available)
- [ ] Highlight which parts of answer came from which source (color-coded citations)
- [ ] Test with 10+ queries: verify source data matches answer
- [ ] User feedback: "Show source data" used by ≥ 50% of pilot users

**Technical Notes:**
- Store retrieved documents in session or database for drill-down
- UI: modal or expandable section with source document list
- Deep linking: construct URL to ERP UI (e.g., `https://erp.example.com/invoices/12345`)
- Color-coded citations: "Customer ABC owes [2.3M VND] (source: Invoice INV-001)"

---

## E3-S4: Answer Quality Monitoring Dashboard

**Story Points:** 2
**Priority:** HIGH - WEEK 12
**Dependencies:** E3-S3

**Description:**
Build administrator dashboard displaying real-time metrics: latency (P95, P99), error rates, groundedness scores, correctness scores, citation coverage. Enable proactive quality monitoring (FR-9.7).

**Acceptance Criteria:**
- [ ] Dashboard displays metrics:
  - Query volume: total queries, queries/hour, peak load times
  - Latency: P50, P95, P99 (overall and per component: retrieval, generation)
  - Error rate: % of queries with errors, error types breakdown
  - Quality scores: average groundedness (target ≥ 0.80), correctness (target ≥ 0.75), citation coverage (target ≥ 95%)
  - User satisfaction: CSAT scores from pilot feedback
- [ ] Real-time updates: metrics refresh every 5 minutes
- [ ] Historical trends: view metrics over last 7/30 days
- [ ] Alerts configured: email/Slack alert if P95 > 7s, error rate > 5%, groundedness < 0.70
- [ ] Test dashboard: simulate 1000 queries, verify metrics accuracy

**Technical Notes:**
- Metrics storage: time-series database (InfluxDB, Prometheus) or PostgreSQL
- Dashboard UI: Grafana, custom React dashboard, or admin panel
- Metric calculation:
  - Groundedness: LLM self-evaluation or RAGAS framework
  - Correctness: validation against ground truth (E2-S9 test suite)
  - Citation coverage: % of answers with ≥ 1 citation
- Alert integration: email (SendGrid), Slack webhook, or PagerDuty

---

## E3-S5: Query Suggestions and Auto-Complete

**Story Points:** 2
**Priority:** MEDIUM - WEEK 12
**Dependencies:** E3-S4

**Description:**
Implement query suggestions and auto-complete to guide users and prevent malformed queries. Suggest queries based on user role, current ERP context, and common patterns (FR-9.2, FR-9.8.4).

**Acceptance Criteria:**
- [ ] Query suggestion engine:
  - Role-based: accountants see transaction queries, managers see analytical queries
  - Context-aware: during period-end, suggest "AR aging", "Trial balance"
  - Common patterns: "Show AR aging report", "Customer X balance", "Upcoming payments"
- [ ] Auto-complete: as user types, suggest completions (e.g., "Show AR" → "Show AR aging report")
- [ ] UI: dropdown with top 5 suggestions, keyboard navigation (arrow keys, Enter)
- [ ] Test with 10+ pilot users: measure usage rate of suggestions (target ≥ 40%)
- [ ] Error prevention: suggested queries always return valid results

**Technical Notes:**
- Suggestion source: curated list of 50-100 common queries + query history
- Context detection: check current date (month-end?), user role, recent queries
- Auto-complete: fuzzy matching on query templates
- UI component: searchable dropdown (React Select, Material-UI Autocomplete)

---

## E3-S6: Pilot User Training Materials and Pre-Pilot Dry Run

**Story Points:** 2
**Priority:** CRITICAL - WEEK 10-11
**Dependencies:** E3-S1, E2-S11 (pilot users recruited)

**Description:**
Create training materials (quick-start guide, 5-minute video tutorial) and conduct pre-pilot dry run with 2 users to validate system usability before full pilot (Week 12).

**Acceptance Criteria:**
- [ ] Training materials created:
  - Quick-start guide: 2-page PDF with screenshots, example queries, FAQ
  - 5-minute video tutorial: system overview, how to ask queries, how to interpret answers
  - Cheat sheet: 20 example queries by use case (AR aging, AP payments, etc.)
- [ ] Pre-pilot dry run (Week 11):
  - 2 pilot users test system for 1 hour
  - Users complete 10 test tasks (e.g., "Check Customer ABC balance")
  - Observe usability issues: confusing UI, unclear error messages, slow performance
- [ ] Feedback documented: prioritize fixes before full pilot
- [ ] Critical issues fixed before full pilot (Week 12)
- [ ] Training session scheduled: 1-hour workshop for all pilot users (Week 12)

**Technical Notes:**
- Video tutorial: screen recording with voiceover (Loom, Camtasia)
- Quick-start guide: Google Docs → export to PDF
- Dry run participants: 1 accountant, 1 finance manager (different skill levels)
- Success criteria: dry run users complete ≥ 80% of tasks without assistance

---

## E3-S7: Pilot Testing with 10+ Users and CSAT Measurement

**Story Points:** 3
**Priority:** CRITICAL - WEEK 12
**Dependencies:** E3-S6, E3-S5, E3-S4

**Description:**
Conduct full pilot testing with 10+ users. Measure CSAT ≥ 4.0/5.0 post-task, validate task completion rate ≥ 70%, and collect qualitative feedback for improvements.

**Acceptance Criteria:**
- [ ] Pilot testing conducted (Week 12):
  - 10+ pilot users complete training workshop (1 hour)
  - Users test system for 2 weeks (2-3 hours/week)
  - Users complete assigned tasks covering AR/AP/GL/Bank queries
- [ ] CSAT measurement:
  - Post-task survey: "How satisfied are you with the system? (1-5)"
  - Target: CSAT ≥ 4.0/5.0 achieved
- [ ] Task completion rate: ≥ 70% of users complete core queries without assistance
- [ ] Qualitative feedback collected: interviews (30 min/user), open-ended survey questions
- [ ] Feedback analyzed: categorize by theme (usability, performance, accuracy, trust)
- [ ] Top 10 improvement items prioritized for post-MVP or Epic 4

**Technical Notes:**
- Survey tool: Google Forms, Typeform, or Qualtrics
- Survey questions:
  - CSAT: "Overall satisfaction (1-5)"
  - NPS: "Would you recommend this system? (0-10)"
  - Open-ended: "What did you like?", "What needs improvement?"
- Task completion tracking: log which users completed which tasks
- Success gate: CSAT ≥ 4.0 required before Epic 4

**Risk:** If CSAT < 3.5, pause and address critical issues before Epic 4.

---

## E3-S8: Report Export and Sharing Features

**Story Points:** 2
**Priority:** MEDIUM - WEEK 13
**Dependencies:** E3-S7

**Description:**
Implement export functionality for query results (PDF, Excel, CSV) with full citations and metadata. Support shareable permalinks respecting RBAC permissions (FR-9.8.3).

**Acceptance Criteria:**
- [ ] Export formats supported: PDF, Excel, CSV
- [ ] Export includes:
  - Query text and timestamp
  - Answer with citations
  - Data freshness indicator
  - User name and role (for audit trail)
  - Full source document list
- [ ] PDF formatting: company header, page numbers, professional layout
- [ ] Shareable permalinks:
  - Generate unique URL for query result (e.g., `https://app.example.com/queries/abc123`)
  - Permalink respects RBAC: recipient must have same permissions to view
  - Permalink expiration: 30 days or admin-configurable
- [ ] Test exports: validate PDF/Excel/CSV formatting and completeness
- [ ] User feedback: "Export was useful" rated ≥ 4/5

**Technical Notes:**
- PDF generation: use library like Puppeteer (headless Chrome), jsPDF, or PDFKit
- Excel generation: use library like ExcelJS, xlsx, or Pandas (Python)
- CSV generation: simple text format with headers
- Permalink storage: store query result in database, generate short URL

---

# Epic 4: Security, Compliance, and Production Readiness

**Goal:** Harden system for production deployment with security controls, compliance features, audit trails, resilience mechanisms, and operational monitoring.

**Timeline:** Weeks 14-16
**Estimated Stories:** 6 stories
**Estimated Points:** 16 points

---

## E4-S1: Role-Based Access Control (RBAC) Implementation

**Story Points:** 3
**Priority:** CRITICAL - WEEK 14
**Dependencies:** Epic 3 complete

**Description:**
Implement RBAC integrated with ERP user permissions (Admin, Chief Accountant, Accountant, Viewer). Filter retrieved documents and answers based on user role (FR-9.9.1).

**Acceptance Criteria:**
- [ ] RBAC roles defined and mapped to ERP roles:
  - **Admin**: Full system access (all modules, all data)
  - **Chief Accountant**: All accounting operations, reports, limited user management
  - **Accountant**: Transaction recording and viewing (AR/AP/GL queries)
  - **Viewer**: Read-only reports and summaries (no transaction details)
- [ ] Permission matrix documented: what each role can query
- [ ] Document filtering at retrieval stage: only retrieve documents user has access to
- [ ] Query scope enforcement: Accountant cannot query executive-level analytics
- [ ] Test RBAC:
  - Accountant queries "AR aging" → sees only their assigned customers
  - Viewer queries "Trial balance" → sees summary, not drill-down
  - Admin queries anything → full access
- [ ] Audit log records all permission checks and access denials
- [ ] Penetration test: attempt to bypass RBAC (should fail)

**Technical Notes:**
- Store user role in session/JWT token
- Filter at database level: `WHERE customer_id IN (user_assigned_customers)`
- LLM prompt includes role: "Answer as if user is [role]. Provide [role-appropriate detail]."
- Test with test users from each role

---

## E4-S2: Compliance Audit Trail Implementation

**Story Points:** 3
**Priority:** CRITICAL - WEEK 14
**Dependencies:** E4-S1, E2-S10 (compliance design approved)

**Description:**
Implement immutable, timestamped audit trail logging complete RAG pipeline execution: query, retrieved documents, LLM prompts, answers, user feedback. Support regulatory audit export (FR-9.9.2).

**Acceptance Criteria:**
- [ ] Audit log table created with fields:
  - `log_id` (UUID), `timestamp`, `user_id`, `session_id`, `ip_address`
  - `query_text`, `query_intent`, `query_complexity_tier`
  - `retrieved_document_ids` (JSONB array), `relevance_scores` (JSONB)
  - `llm_prompt` (sanitized for PII), `llm_response`, `answer_text`
  - `citations` (JSONB), `groundedness_score`, `correctness_score`
  - `user_feedback` (thumbs up/down, corrections)
- [ ] Append-only logging: no DELETE allowed (database constraint)
- [ ] Tamper-evident: cryptographic hash chain or blockchain-style verification
- [ ] Retention policy: 10 years minimum (Vietnam Circular 200 requirement)
- [ ] Audit export functionality:
  - Export logs to JSON/CSV with schema documentation
  - Support date range filtering, user filtering, query filtering
  - Exported file includes integrity hash
- [ ] Test audit trail: trace 10 queries from input to output to user feedback
- [ ] Compliance review: expert validates audit trail meets regulations

**Technical Notes:**
- Use PostgreSQL with `CREATE RULE no_delete ON audit_log AS ON DELETE DO INSTEAD NOTHING;`
- Sanitize PII in `llm_prompt`: replace customer names/IDs with tokens
- Hash chain: each log entry includes hash of previous entry (Merkle tree-style)
- Export format: JSON with schema version number for future-proofing

---

## E4-S3: Security Hardening (Encryption, Credentials, Pen Test)

**Story Points:** 3
**Priority:** CRITICAL - WEEK 15
**Dependencies:** E4-S1, E4-S2

**Description:**
Harden system security: encryption at rest and in transit, secure credential management, input validation, rate limiting. Conduct penetration testing to validate security controls (NFR-4).

**Acceptance Criteria:**
- [ ] Encryption at rest: AES-256 for indexed documents and audit logs (database-level encryption)
- [ ] Encryption in transit: TLS 1.3 for all API communications (enforce HTTPS only)
- [ ] Credential management: external API keys (LLM, Vector DB) stored in secrets manager (AWS Secrets Manager, HashiCorp Vault)
- [ ] Input validation: sanitize user queries (prevent SQL injection, prompt injection)
- [ ] Rate limiting: 100 queries/hour per user, 500 queries/hour per IP
- [ ] Security headers: HSTS, CSP, X-Frame-Options, X-Content-Type-Options
- [ ] Penetration testing conducted (Week 15):
  - External pen test by security firm or internal security team
  - Test: SQL injection, XSS, CSRF, authentication bypass, RBAC bypass, API abuse
  - No critical or high-severity vulnerabilities found
- [ ] Security report generated: findings, remediation status

**Technical Notes:**
- TLS 1.3: configure web server (Nginx, CloudFlare)
- Secrets manager: rotate keys every 90 days
- Input validation: use library like `validator.js`, sanitize with DOMPurify
- Rate limiting: use middleware (express-rate-limit) or API gateway (Kong, AWS API Gateway)
- Pen test scope: OWASP Top 10, API security, data access controls

**Success Gate:** Zero critical vulnerabilities before production.

---

## E4-S4: External Dependency Resilience and Failover

**Story Points:** 3
**Priority:** HIGH - WEEK 15
**Dependencies:** E1-S7 (LLM abstraction), E4-S3

**Description:**
Complete implementation of external dependency resilience: LLM failover, Vector DB timeout handling, ERP connection failover. Display degraded-mode indicator when operating on fallback systems (FR-9.10.4).

**Acceptance Criteria:**
- [ ] LLM failover tested:
  - Primary LLM fails → automatic switch to secondary LLM
  - Both LLMs fail → return cached template response ("System temporarily unavailable. Please try again.")
- [ ] Vector DB resilience:
  - Vector DB timeout (>2s) → automatic degradation to keyword search
  - Display degraded-mode banner: "Using simplified search due to high load"
- [ ] ERP connection resilience:
  - Read replica failover: primary replica fails → switch to secondary
  - Stale data acceptable: tolerate 5-minute lag for non-critical queries
- [ ] Circuit breaker pattern implemented: 5 consecutive failures → open circuit for 60s
- [ ] Health check dashboard: display status of LLM, Vector DB, ERP (green/yellow/red)
- [ ] Test failover scenarios: simulate each dependency failure, verify graceful degradation
- [ ] User notification: clear message when system is degraded

**Technical Notes:**
- Circuit breaker: use library like `opossum` (Node.js) or custom implementation
- Health check: poll dependencies every 30s, update status dashboard
- Keyword search fallback: simple PostgreSQL full-text search (tsvector)
- Degraded-mode UI: yellow banner at top of screen, "System operating in degraded mode"

---

## E4-S5: Monitoring Dashboard and Alerting for Admins

**Story Points:** 2
**Priority:** HIGH - WEEK 15
**Dependencies:** E4-S4, E3-S4 (quality dashboard)

**Description:**
Expand quality dashboard (E3-S4) with operational monitoring: dependency health, index status, error logs, system resources. Configure alerting for critical issues (NFR-8).

**Acceptance Criteria:**
- [ ] Monitoring dashboard displays:
  - Dependency health: LLM API status, Vector DB status, ERP connection status
  - Index status: last sync timestamp, documents indexed, indexing errors
  - Error logs: recent errors with filters (last 1 hour, 24 hours, 7 days)
  - System resources: CPU, memory, disk usage (if applicable)
  - Query queue: current queue length, average wait time
- [ ] Alerts configured:
  - Email/Slack: P95 latency > 7s, error rate > 5%, dependency down, index sync failed
  - PagerDuty (optional): critical outage (system unavailable)
- [ ] Alert thresholds configurable via admin UI
- [ ] Test alerts: simulate each alert condition, verify notification received
- [ ] Historical logs: view past 30 days of metrics and alerts

**Technical Notes:**
- Monitoring stack: Prometheus + Grafana, Datadog, or custom dashboard
- Alerting: use Alertmanager (Prometheus), Grafana alerts, or custom email/Slack webhooks
- Log aggregation: ELK stack (Elasticsearch, Logstash, Kibana) or Loki
- System resources: collect via node_exporter (Prometheus) or cloud monitoring (AWS CloudWatch)

---

## E4-S6: Production Readiness Validation and Deployment

**Story Points:** 2
**Priority:** CRITICAL - WEEK 16
**Dependencies:** E4-S1, E4-S2, E4-S3, E4-S4, E4-S5

**Description:**
Validate all Epic 4 success gates, conduct formal compliance review, complete operational runbook, and execute production deployment with zero-downtime strategy.

**Acceptance Criteria:**
- [ ] Epic 4 Success Gates validated:
  - ✅ Penetration testing passed (no critical vulnerabilities)
  - ✅ Compliance review PASSED by Vietnam expert
  - ✅ Operational runbook validated with dry-run deployment
  - ✅ Monitoring and alerting tested with simulated failures
- [ ] Formal compliance review (Week 16):
  - 2-hour session with compliance expert
  - Expert validates audit trail, RBAC, data security, retention
  - Sign-off document obtained: "System complies with Vietnam Circular 200/2014/TT-BTC"
- [ ] Operational runbook completed:
  - Deployment procedures (step-by-step)
  - Configuration management (environment variables, secrets)
  - Backup and restore procedures
  - Incident response procedures (outage, data breach, performance degradation)
  - Troubleshooting guide (common issues and solutions)
- [ ] Dry-run deployment to staging environment: validate zero-downtime deployment
- [ ] Production deployment executed:
  - Blue-green deployment or rolling deployment
  - Zero downtime for users
  - Post-deployment validation: smoke tests, health checks
- [ ] Retrospective conducted: lessons learned, celebrate wins, identify improvements

**Technical Notes:**
- Compliance review: prepare evidence (test results, screenshots, audit logs)
- Runbook format: Wiki (Confluence, Notion) or Markdown in Git repo
- Deployment strategy: blue-green (two environments, switch traffic) or rolling (gradual rollout)
- Smoke tests: run 20 test queries post-deployment, validate responses
- Success metrics: 99% uptime in first week, CSAT maintained ≥ 4.0

---

## Story Summary by Epic

| Epic | Story Count | Total Points | Avg Points/Story |
|------|-------------|--------------|------------------|
| Epic 1 | 13 | 28 | 2.2 |
| Epic 2 | 11 | 26 | 2.4 |
| Epic 3 | 8 | 20 | 2.5 |
| Epic 4 | 6 | 16 | 2.7 |
| **Total** | **38** | **90** | **2.4** |

---

## Critical Path Highlights

**Blocking Stories:**
1. **E1-S1** (ERP Access) → Blocks entire Epic 1
2. **E1-S12** (Domain Expert) → Validates Epic 1 approach
3. **E2-S5** (Cross-Module Orchestration) → Blocks Epic 3
4. **E2-S9** (Accuracy Validation) → Success gate for Epic 3
5. **E2-S10** (Compliance Review) → Success gate for Epic 4
6. **E2-S11** (Pilot Recruitment) → Blocks E3-S7
7. **E3-S1** (Performance Optimization) → Critical for MVP usability
8. **E3-S7** (Pilot Testing) → Success gate for Epic 4
9. **E4-S3** (Security/Pen Test) → Production blocker

**Success Gates Between Epics:**
- **Epic 1 → 2**: E1-S10 (performance ≤ 8s), E1-S12 (expert validation), E1-S1 (ERP stable)
- **Epic 2 → 3**: E2-S9 (accuracy ≥ 99%), E2-S11 (pilots confirmed), E2-S10 (compliance approved)
- **Epic 3 → 4**: E3-S7 (CSAT ≥ 4.0), E3-S1 (performance ≤ 5s)
- **Epic 4 → Production**: E4-S3 (pen test passed), E4-S6 (compliance passed)

---

## Appendix: Story Point Estimation Guide

**Story Point Scale (Fibonacci):**
- **1 point**: Trivial task, <4 hours, no unknowns (e.g., config change, documentation)
- **2 points**: Simple task, 4-8 hours, minimal complexity (e.g., single API endpoint, UI component)
- **3 points**: Moderate task, 1-2 days, some complexity (e.g., feature with tests, integration)
- **5 points**: Complex task, 2-4 days, multiple components (e.g., cross-module feature, optimization)
- **8 points**: Very complex, 4-5 days, high uncertainty (e.g., major refactoring, new architecture)
- **13 points**: Epic-level, >1 week, break into smaller stories (generally avoided in this breakdown)

**Team Velocity Assumption:**
- 2 developers
- ~10-12 story points/developer/week
- Total team velocity: 20-24 points/week
- Epic 1 (28 pts): 5 weeks @ 5.6 pts/week
- Epic 2 (26 pts): 4 weeks @ 6.5 pts/week
- Epic 3 (20 pts): 4 weeks @ 5 pts/week
- Epic 4 (16 pts): 3 weeks @ 5.3 pts/week
- **Total: 16 weeks**

---

_This epic breakdown aligns with PRD v2025-10-16. For updates or questions, contact PM or Tech Lead._

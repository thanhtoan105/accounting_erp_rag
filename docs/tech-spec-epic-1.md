# Technical Specification: Core RAG Pipeline and Infrastructure (Foundation)

Date: 2025-10-17
Author: thanhtoan105
Epic ID: 1
Status: Draft

---

## Overview

The PRD positions Epic 1 as the foundation for the AI-native accounting platform, delivering a production-ready RAG pipeline that can index Vietnam Circular 200 data, retrieve grounded evidence, and answer bilingual finance questions with citations. This epic focuses on making Supabase-hosted ERP data queryable through embeddings while preserving auditability, privacy, and the dual UI/RAG experience outlined in the product vision.

By the end of Weeks 1‑5 the team must prove the architecture can meet pilot expectations: end-to-end query latency ≤ 8 s P95, vector recall ≥ 0.90, citation coverage ≥ 95 %, and successful validation from the accounting domain expert. These gates unblock subsequent epics that add domain intelligence, UX polish, and compliance hardening.

## Objectives and Scope

**In Scope**

- Establish secure, read-only PostgreSQL connectivity to the ERP schema with pooling, health checks, and documented table coverage.
- Implement PII masking/tokenization so non-production indexing never exposes customer or vendor identities.
- Stand up Supabase pgvector with tuned HNSW indexes, embedding pipelines (batch + incremental), and metadata taxonomy.
- Deliver the baseline RAG flow: query ingestion, similarity search, LLM prompt construction, answer streaming, and citation extraction.
- Provide resilience foundations: provider abstraction with failover, error logging, circuit breakers, schema change monitoring, and performance benchmarking at pilot scale.
- Engage the domain expert for iterative validation and capture integration documentation/runbooks for handover.

**Out of Scope**

- Domain-specific reasoning and orchestration (multi-ledger joins, compliance workflows) targeted for Epic 2.
- Assistant UX enhancements, user education flows, and performance refinement to ≤ 5 s P95 reserved for Epic 3.
- Security hardening, RBAC expansions, audit dashboards, and production readiness controls planned for Epic 4.
- Story-level delivery outside the Core RAG platform (AR/AP UI features, automation) covered by other epics.
- Non-critical integrations such as third-party BI exports or advanced conversational analytics deferred until after MVP foundation.

## System Architecture Alignment

Epic 1 activates the architecture’s core “rag-platform” slice described in `docs/solution-architecture.md`: the Spring Boot modular monolith hosts retrieval controllers, `supabase-gateway` JDBC adapters, and `async-jobs` worker profiles that feed pgvector on Supabase. n8n orchestrations supply long-running ingestion hooks, Redis caches metadata per the performance plan, and the React SPA exposes a feature-gated assistant entry point. This alignment keeps the monorepo contracts stable while laying the telemetry and observability hooks that the architecture earmarks for downstream epics.

## Detailed Design

### Services and Modules

| Component | Layer | Responsibilities | Key I/O | Primary Owner |
|-----------|-------|------------------|---------|---------------|
| `rag-query-controller` | Backend (Spring Boot, `rag-platform`) | Accept chat queries, orchestrate retrieval + generation pipeline, stream responses via SSE, enforce RBAC | In: POST `/api/v1/rag/query`; Out: SSE `/api/v1/rag/query/{id}/events`, audit events | Backend Lead |
| `supabase-gateway` | Backend shared library | Manage JDBC pools to Supabase Postgres/vector, expose DAO for ERP tables, enforce read-only role, surface health metrics | In: DAO requests from services; Out: Pooled connections, health check `/internal/rag/db-health` | Platform Engineer |
| `embedding-worker` | Async job (Spring Boot worker profile) | Batch extraction of ERP documents, PII masking, embedding generation, pgvector writes, error retries | In: n8n schedule, change-feed events; Out: `vector_documents`, `embedding_batches` records | Data Engineer |
| `incremental-sync` | Async job | Poll `updated_at` deltas, remove stale vectors, process soft deletes, update `sync_status` | In: cron trigger (5 min); Out: Supabase pgvector updates, alert hooks | Data Engineer |
| `schema-monitor` | Async job | Snapshot ERP schema, diff changes, raise alerts, queue re-index tasks | In: daily cron; Out: `schema_snapshots`, Slack/Email alerts | Platform Engineer |
| `llm-provider-adapter` | Backend service (`shared-kernel`) | Strategy layer over OpenAI/Anthropic, streaming adapters, retries, circuit breaker, telemetry | In: Normalized prompt payloads; Out: Streamed tokens, provider metrics | Backend Lead |
| `rag-telemetry` | Observability stack | Emit OpenTelemetry spans, Prometheus metrics (latency, recall, errors), structured logs with query IDs | In: pipeline trace data; Out: Grafana dashboards, alert rules | SRE |
| `frontend-assistant-gateway` | React module (feature flag) | Provide guarded entry point, call query endpoint, render SSE stream, expose debug info for pilots | In: User prompts; Out: Streaming UI components, citation toggles | Frontend Lead |

### Data Models and Contracts

| Entity | Purpose | Key Fields / Types | Notes |
|--------|---------|--------------------|-------|
| `vector_documents` (pgvector) | Store embeddings per ERP artifact | `id UUID PK`, `company_id UUID`, `source_table TEXT`, `source_id UUID`, `fiscal_period TEXT`, `content_tsv tsvector`, `embedding VECTOR(1536)`, `metadata JSONB`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`, `deleted_at TIMESTAMPTZ` | Partition by `company_id` and optionally `fiscal_period`; `deleted_at` supports soft deletes. |
| `embedding_batches` | Track pipeline runs | `id UUID`, `batch_type ENUM('full','incremental')`, `status ENUM('queued','running','failed','complete')`, `started_at`, `completed_at`, `doc_count INT`, `error_count INT`, `hash TEXT` | `hash` prevents duplicate processing; link to logs for troubleshooting. |
| `rag_queries` | Persist query metadata for audit/analytics | `id UUID`, `company_id UUID`, `user_id UUID`, `language TEXT`, `query_text TEXT`, `status ENUM('pending','streaming','complete','error')`, `llm_provider TEXT`, `retrieval_latency_ms INT`, `generation_latency_ms INT`, `total_latency_ms INT`, `created_at`, `completed_at` | Drives NFR reporting (latency budgets, error rates). |
| `rag_query_documents` | Junction for citations | `id UUID`, `query_id UUID FK`, `document_vector_id UUID FK`, `rank INT`, `relevance FLOAT`, `tokens_used INT`, `excerpt TEXT` | Enables recall/precision evaluation and UI drill-down. |
| `schema_snapshots` | Record ERP schema structure over time | `id UUID`, `captured_at TIMESTAMPTZ`, `schema JSONB`, `breaking_changes JSONB`, `notified BOOLEAN` | Daily snapshot diff; `breaking_changes` populated when columns missing/altered. |
| `sync_status` | Maintain incremental sync pointers | `id UUID`, `source_table TEXT`, `last_synced_at TIMESTAMPTZ`, `last_batch_id UUID`, `lag_minutes INT`, `error_state JSONB` | Alert when `lag_minutes > 10`; used by DevOps dashboards. |
| `pii_mask_map` | Deterministic PII masking | `id UUID`, `source_table TEXT`, `source_id UUID`, `field TEXT`, `masked_value TEXT`, `hash TEXT`, `created_at` | Stored only in protected Supabase schema; ensures traceability without leaking original values. |
| `llm_provider_config` | Runtime provider settings | `id UUID`, `provider TEXT`, `priority INT`, `status ENUM('active','degraded','disabled')`, `latency_p95_ms INT`, `error_rate FLOAT`, `last_healthcheck TIMESTAMPTZ`, `metadata JSONB` | Supports circuit breaker + failover routing decisions. |

### APIs and Interfaces

| Endpoint / Interface | Method / Type | Request Contract | Response Contract | Notes |
|----------------------|---------------|------------------|-------------------|-------|
| `/api/v1/rag/query` | REST POST | JSON `{ "companyId": UUID, "query": string, "language": "vi"|"en", "filters": { "module": string, "fiscalPeriod": string, "minConfidence": number } }` | `202 Accepted` `{ "queryId": UUID, "streamUrl": string }` | Validates RBAC via Supabase JWT; enqueues retrieval workflow; returns SSE URL. |
| `/api/v1/rag/query/{id}/events` | REST GET (SSE) | Headers: `Authorization: Bearer <JWT>` | SSE stream events `{type: "token"|"citation"|"complete"|"error", data: {...}}` | Streams partial answers, citations, telemetry markers; closes on completion or error. |
| `/internal/rag/index-batch` | REST POST (authenticated service-to-service) | JSON `{ "batchType": "full"|"incremental", "tables": [string], "startFrom": timestamp }` | `202 Accepted` with batch ID | Trigger manual re-index; restricted to worker profiles/n8n webhook. |
| `/internal/rag/provider-health` | REST GET | Query `provider=<name>` | JSON `{ "provider": string, "status": string, "latencyP95": number, "errorRate": number, "lastFailure": timestamp }` | Consumed by circuit breaker + observability dashboards. |
| `/internal/rag/schema/changes` | REST GET | Query `since=<timestamp>` | JSON `{ "breaking": [ ... ], "nonBreaking": [ ... ] }` | Surfaces daily diff for compliance + DBA review. |
| `/internal/rag/metrics` | REST GET | Query `range=24h` | JSON `{ "queryVolume": int, "latency": { "p50": int, "p95": int }, "errorRate": float, "recall": float }` | Feeds admin console + SLO dashboards. |
| Supabase Change Feed | Realtime subscription | Channel per ERP table (`realtime:public:journal_entries`) | JSON payload of row changes | Drives incremental indexing trigger in `incremental-sync`. |
| LLM Provider SDK | Streaming API (`generateStream`) | Normalized prompt `{ system, context[], user, metadata }` | Async iterator of `{token, citationRefs?}` | Adapter translates provider-specific streams to unified format. |

### Workflows and Sequencing

**Document Ingestion & Embedding (n8n + `embedding-worker`)**
1. n8n cron triggers `embedding-worker` with `batchType=full|incremental`.
2. Worker pulls candidate ERP records via `supabase-gateway`, applying deterministic PII masking and content templates scoped by module.
3. Batched payloads (≤100 docs) sent to `llm-provider-adapter.embed()`; responses persisted to `vector_documents` and `embedding_batches`.
4. Metadata (module, fiscal period, status) normalized; `sync_status` updated per table; failures logged and retried with exponential backoff.
5. Completion emits OpenTelemetry spans and posts status to Slack for observability; if `error_rate > 5%`, pipeline auto-escalates to on-call.

**Incremental Sync Loop**
1. Every 5 minutes, `incremental-sync` reads `sync_status` to determine `updated_at` window per ERP table.
2. For each changed record set: delete existing vector rows, regenerate embeddings, handle soft deletes (`deleted_at` not null).
3. Update `lag_minutes` metric; if >10 minutes, send alert and pause new RAG queries (degrade gracefully per FR-23).

**User Query Execution**
1. Frontend assistant collects prompt + optional filters, posts to `/api/v1/rag/query`; controller logs intent and returns `queryId`.
2. Retrieval stage: compute query embedding, run pgvector search with metadata filters, enforce recall@10 ≥ 0.90 by adjusting `ef_search`, prune to token budget.
3. Compose grounded prompt with citations, call `llm-provider-adapter.generateStream()`; circuit breaker selects primary/secondary provider.
4. Stream tokens back via SSE; inject `citation` events when document references emitted; record latency telemetry per stage.
5. On completion, persist summary into `rag_queries` + `rag_query_documents`; update audit log with query, answer confidence, and data freshness indicator.

## Non-Functional Requirements

### Performance

- Meet PRD NFR-1 budgets: ≤ 5 s P95 / ≤ 10 s P99 for conversational answers once optimized; Epic 1 baseline must reach ≤ 8 s P95 with 500 K documents (E1-S10) to unblock Epic 2.
- Vector retrieval latency ≤ 1.5 s P95 and LLM generation ≤ 1.5 s P95 with adaptive batching; budgets logged in `rag_queries` for compliance reporting.
- Incremental indexing completes within 5 minutes of ERP data changes (FR-18), with `lag_minutes` telemetry raising alerts when >10 minutes.
- Support ≥ 20 concurrent accountant sessions + 5 simultaneous assistant streams without throughput degradation, validated via k6 load profile.
- Provide data freshness indicator in every response referencing last sync timestamp per PRD requirement.

### Security

- Enforce Supabase-authenticated JWT on all endpoints, aligning with RBAC roles (ADMIN, ACCOUNTANT, VIEWER) and zero-trust posture described in PRD FR-15.
- Apply deterministic masking for PII outside production; ensure embeddings and logs never contain raw tax IDs/emails (E1-S2).
- Maintain TLS 1.3 for all service communication; secrets reside in AWS Secrets Manager with short-lived OIDC tokens for CI/CD as per architecture section 10.4.
- Prevent data leakage to LLM providers by sending masked context + citation identifiers only; enforce provider agreements supporting 10-year audit retention.
- Capture immutable audit logs for every query, including user, prompt hash, and cited documents, satisfying PRD compliance and audit requirements.

### Reliability/Availability

- Achieve ≥ 99 % uptime during 08:00–18:00 ICT (business hours) for RAG endpoints with automated health probes and blue/green deploy strategy defined in solution architecture.
- Implement circuit breakers and provider failover (E1-S7) so LLM outages trigger graceful degradation with user-facing guidance, maintaining service continuity.
- Monitor ERP connectivity with pooled read-only replicas; failures automatically pause incremental sync and raise alerts while preserving existing indexed content.
- Support daily schema diffing and alerting (E1-S8) to prevent silent failures when ERP schema changes, ensuring ingestion/retrieval stays accurate.
- Provide retry policies (max 3 with exponential backoff) for vector DB and LLM calls; escalate to on-call when error rate exceeds 5 % in a 5-minute window.

### Observability

- Instrument OTel traces spanning controller → retrieval → LLM adapter, exporting to Prometheus/Grafana dashboards for latency/error SLOs (section 8.3).
- Aggregate structured JSON logs with query IDs, company IDs, and error classifications to support finance audit and root-cause investigations (E1-S11).
- Publish metrics: `rag_query_latency_p95`, `vector_retrieval_latency_p95`, `index_lag_minutes`, `llm_provider_error_rate`, `recall_at_10`, and `citation_coverage` with alert thresholds defined in PRD NFR-8.
- Provide admin dashboard panels summarizing query volume, indexing backlog, schema change alerts, and provider health (leveraging `/internal/rag/metrics`).

## Dependencies and Integrations

- **Supabase Platform (Postgres + Auth + Storage + pgvector)** — Managed database of record, authentication provider, pgvector extension for retrieval, storage buckets for supporting documents. Requires enabling `uuid-ossp`, `pg_stat_statements`, and vector extensions; monitor usage quotas.
- **Java 21 + Spring Boot 3.3** — Modular monolith runtime for `rag-platform`, leveraging Spring Data JPA, WebFlux SSE, Actuator, HikariCP. Depends on Liquibase changelog pipeline and Testcontainers for integration tests.
- **React 18 + Vite + shadcn/ui** — SPA surface with TanStack Query, SSE client for assistant streaming, bilingual localization infrastructure (vi/en). Requires pnpm toolchain once frontend repo is scaffolded.
- **OpenAI GPT‑4.1 & Anthropic Claude 3** (via MCP adapters) — Primary and fallback LLM providers; circuit breaker chooses provider based on health telemetry. Contractual requirements: data residency assurances, streaming API access, cost monitoring.
- **Sentence-Transformer / OpenAI `text-embedding-3-large`** — Embedding generators supporting Vietnamese and English; run via provider abstraction with caching to control cost.
- **Redis (Upstash) or Supabase Redis add-on** — Cache metadata, store query throttling tokens, hold provider health state; must support TLS and multi-AZ availability.
- **n8n Workflow Engine** — Triggers ingestion batches, coordinates long-running compliance workflows, dispatches alerts; interacts via signed webhooks.
- **GitHub Actions + Terraform** — CI/CD pipeline orchestrating builds, Liquibase migrations, infrastructure provisioning (ECS Fargate, CloudFront, Secrets Manager).
- **Slack / Email Webhooks** — Notification channels for schema drift, indexing lag, high error rate events; integrate with compliance audit logs.
- **Monitoring Stack (Prometheus, Grafana, Sentry)** — Collects metrics, traces, and exceptions as required by PRD NFR-8; Sentry DSN configured for both backend and frontend clients.
- **Supabase Realtime Change Feeds** — Source for incremental indexing; requires RLS policies granting worker role access while preserving tenant isolation.

## Acceptance Criteria (Authoritative)

1. **AC1 – Performance Baseline:** With 500 K indexed documents and 20 concurrent users, end-to-end RAG queries complete in ≤ 8 s P95 / ≤ 10 s P99, with vector retrieval ≤ 1.5 s P95 and LLM generation ≤ 1.5 s P95; results logged in `rag_queries` and validated by k6 load test (E1-S10, PRD NFR-1).
2. **AC2 – Secure ERP Connectivity:** Read-only Supabase connection exposes all required accounting tables (≥ 60) with pooling, health check endpoint, and zero data mutation capability demonstrated via automated integration tests (E1-S1).
3. **AC3 – PII Protection:** Embedding pipeline masks/tokens all PII fields for non-production indexing; automated scan confirms vector tables, logs, and LLM prompts contain no raw names/tax IDs/emails (E1-S2, PRD NFR-4).
4. **AC4 – Incremental Freshness:** Incremental sync processes ERP updates within 5 minutes, updates data freshness indicator in responses, and raises alerts when lag > 10 minutes; verified via staged replay scenario (E1-S9, FR-18).
5. **AC5 – Provider Resilience:** LLM abstraction layer routes queries to secondary provider after 3 consecutive primary failures, resumes to primary once health checks pass, and emits audit entries for failover events (E1-S7, PRD NFR-9).
6. **AC6 – Grounded Answers with Citations:** 95 %+ of pilot queries include at least one citation referencing source document metadata; accounting expert validation confirms accuracy ≥ 99 % on curated test set (E1-S6, E1-S12, PRD Goal #3).
7. **AC7 – Auditability:** Every query persists immutable audit log entries capturing user, company, prompt hash, cited documents, provider, and latency metrics; logs retained ≥ 10 years per Circular 200 mandate (PRD NFR-5).

## Traceability Mapping

| Acceptance Criterion | Spec Sections Referenced | Components / APIs | Test Idea |
|----------------------|---------------------------|-------------------|-----------|
| AC1 | Services and Modules (§Detailed Design), Non-Functional Performance | `rag-query-controller`, `embedding-worker`, `/api/v1/rag/query`, `/api/v1/rag/query/{id}/events` | Execute k6 scenario with 500 K fixture; assert latency metrics in Prometheus and API response timings. |
| AC2 | Services and Modules, Data Models, Dependencies | `supabase-gateway`, `vector_documents`, `/internal/rag/db-health` | Run Testcontainers integration verifying read-only credentials; attempt write → expect denial; check health endpoint. |
| AC3 | Data Models, Workflows, Security NFR | `embedding-worker`, `pii_mask_map`, `llm-provider-adapter` | Generate sample data with PII, run embedding job, scan resulting vectors/logs for sensitive regex hits (should be zero). |
| AC4 | Workflows and Sequencing, Observability NFR | `incremental-sync`, `sync_status`, `/internal/rag/metrics` | Simulate batch of updated ERP records; assert freshness indicator in assistant response and alert if lag threshold exceeded. |
| AC5 | Services and Modules, Dependencies, Reliability NFR | `llm-provider-adapter`, `llm_provider_config`, `/internal/rag/provider-health` | Inject fault to primary provider; verify automatic failover within 3 attempts and telemetry event captured. |
| AC6 | Overview, Detailed Design, Acceptance Criteria | `rag-query-controller`, `rag_query_documents`, frontend assistant | Run curated accounting QA set; compute citation coverage metric and have domain expert sign-off on accuracy. |
| AC7 | Non-Functional Security/Reliability, Data Models | `rag_queries`, audit log pipeline, `/internal/rag/metrics` | Issue queries as different roles; verify audit records persisted with immutable timestamps and retention policy enforced. |

## Risks, Assumptions, Open Questions

- **Risk – ERP access delay blocks Weeks 1‑2:** Read-only credentials or VPN setup slipping past Day 2 stalls every downstream story. *Mitigation:* secure credentials before kickoff, maintain synthetic dataset fallback, escalate to PM by end of Day 2 if unresolved (per E1-S1 notes).
- **Risk – LLM provider latency/cost spikes:** External providers may exceed 1.5 s P95 or violate cost targets during pilot. *Mitigation:* enforce streaming responses, cache embeddings/prompts, enable rapid provider failover, and capture per-query cost telemetry for throttling.
- **Risk – Schema drift causing stale vectors:** Unannounced ERP schema changes can break ingestion silently. *Mitigation:* daily diff alerts (E1-S8), gating incremental jobs until review, and automated regression suite after schema updates.
- **Assumption – Supabase Realtime feeds deliver <1 min latency:** Incremental sync design assumes change feeds or timestamp polling stay within 60 s; if slower, adjust cron frequency and alert thresholds.
- **Assumption – Domain expert availability Weeks 3‑9:** Validation cadence relies on contracted accountant; PM to confirm engagement and backup before Week 3.
- **Question – Preferred observability stack hosting?** Architecture references Prometheus/Grafana/Sentry, but final hosting (Self-managed vs. Supabase add-ons vs. Datadog) requires stakeholder decision before Sprint 2 to avoid rework.

## Test Strategy Summary

- **Unit Tests:** JUnit/Mockito for `rag-query-controller`, `llm-provider-adapter`, PII masking utilities, and Supabase DAO conversions; coverage target ≥ 80 % on critical services.
- **Integration Tests:** Testcontainers harness spinning Supabase-compatible Postgres + pgvector to verify embedding writes, read-only enforcement, incremental sync, and schema monitoring; include SSE contract tests using Spring MVC test framework.
- **Performance & Load:** k6 scripts replay 20 accountant + 5 assistant streams with 500 K fixture to validate AC1 metrics; separate Locust scenario tests provider failover impact and ensures no regression beyond P95 budgets.
- **Security & Compliance:** Static analysis for secrets leakage, automated scans ensuring embeddings/logs contain no PII, and audit log immutability checks; execute role-based access tests for ADMIN/ACCOUNTANT/VIEWER coverage.
- **User Acceptance & Domain Validation:** Weekly expert sessions exercising curated query sets, measuring accuracy ≥ 99 % and citation coverage ≥ 95 %; capture findings in decision log for traceability.
- **Observability Verification:** Synthetic probe scripts invoke `/internal/rag/metrics`, `/internal/rag/provider-health`, and simulate error spikes to confirm alert routing to Slack/email per NFR-8.

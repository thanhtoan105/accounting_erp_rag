# Solution Architecture – Accounting ERP RAG

## Prerequisites and Scale Assessment
- Status file: docs/bmm-workflow-status.md (level 3, greenfield web project)
- PRD: complete (docs/PRD.md)
- UX specification: complete (docs/ux-specification.md), UI complexity marked as moderate
- Standalone mode: false (workflow-state file found)
- Prerequisite verdict: ✅ All required inputs validated; proceed with solution-architecture workflow

## PRD and UX Analysis
**Project understanding:** 76 functional requirements span nine ERP modules plus the RAG layer, backed by 12 NFRs covering performance (≤5 s P95), compliance (10-year audit, Circular 200), security (RBAC, encryption), and operability (monitoring, failover). Four epics (Core RAG, Domain Intelligence, UX & Performance, Security & Production) with 38 stories target a 16-week MVP, emphasizing Supabase PostgreSQL/Vector, bilingual UX, and AI-grounded transparency.

**UI/UX summary:**
- Screen count: 32 primary surfaces across dashboard, system management, ledger modules, period close, and RAG workspace.
- Navigation complexity: complex – desktop left-rail with contextual tabs, global quick actions, and mobile-specific patterns.
- UI complexity: moderate (data-dense dashboards, wizards, reconciliation workspaces, conversational assistant coexist).
- Key flows: Sales invoice capture, vendor bill intake, month-end close, conversational insight drill-down, audit evidence export.
- Responsive/accessibility: desktop-first responsive grid, tablet support, WCAG 2.1 Level A targets, bilingual VI⇄EN controls.

**PRD ↔ UX alignment:** Each PRD epic has UI coverage—Core RAG maps to Conversational Workspace/Insight Library; domain accounting modules align with AR/AP/Cash/Tax screens; UX & Performance epic reflected in dashboards, latency badges, onboarding aids; Security/Production readiness partially represented via audit log explorer and compliance repository. Gaps: monitoring/ops console for NFR-8 not yet visualized; security hardening screens (RBAC approval flows, retention policy management) require explicit mockups; RAG accuracy validation workflow (FR-9.7/FR-30) needs UI depiction for test suites.

**Detected characteristics:** Architecture leans toward modular web app with shared Supabase backend, suggesting monorepo-friendly structure; workload hints at hybrid event/batch (incremental indexing plus scheduled close). Real-time latency budgets and cross-module orchestration indicate need for streaming pipelines and observability stack.

**Known vs unknown:** Specified – Supabase (Postgres/Auth/Vector), shadcn/ui component system, bilingual content strategy, latency/accuracy SLAs, compliance mandates. Unknown – frontend framework/runtime (assumed React/Next.js but unstated), backend service language, infrastructure hosting, LLM provider & fallback strategy, monitoring/alerting tooling, CI/CD approach.

## Architecture Pattern Determination

**Candidate approach:** Modular monolith with domain partitioning on Spring Boot, React+Vite SPA frontend, Supabase platform services, and n8n for orchestration.

- **Architecture style:** Modular monolith (Spring Boot) with async worker modules for ingestion/index refresh; React SPA consumes RESTful APIs; Supabase covers database/auth/vector.
- **Repository strategy:** Monorepo (`/apps/backend`, `/apps/frontend`, `/packages/shared`, `/infra`, `/docs`) to keep schema, API contracts, and IaC aligned while the team remains small.
- **Frontend delivery:** Client-side SPA (React Router via Vite) with RESTful API integration, SSE/WebSocket listeners for live dashboards, optional SSR only if future SEO/first-paint needs emerge.

**Dependency mapping**
- **Frontend (React + Vite SPA)** depends on Spring Boot APIs for ERP/RAG endpoints, Supabase Auth (JWT exchange), Supabase Storage/Vector (attachments, embeddings), and MCP server adapters for shadcn tokens.
- **Backend (Spring Boot modular monolith)** depends on Supabase PostgreSQL (core schema, audit logs), Supabase Vector via pgvector, n8n workflows (long-running automations, e-invoice sync), MCP/LLM providers for RAG, and optional Redis for caching/queues.
- **Async workers (Spring Boot module/kotlin coroutines)** depend on Supabase change feeds for incremental indexing, n8n webhooks for orchestration callbacks, and Supabase Vector for regeneration jobs.
- **Observability stack (Grafana/Prometheus + ELK/Loki)** depends on backend metrics exporters, Supabase log export APIs, and frontend telemetry (Web Vitals/Sentry).
- **Docs & infrastructure segments** depend on Terraform/Supabase CLI for environment provisioning and CI/CD workflows coordinating monorepo builds, tests, and deploys.

**Risk scan**
- **Scaling asymmetry:** RAG-heavy workloads (vector search/inference) may saturate the monolith; plan for extraction of retrieval/generation services if latency approaches the ≤5 s SLA.
- **Monorepo contention:** Shared repo simplifies contracts but requires strict CI gating to avoid cross-app breakage; adopt feature-flagged pipelines.
- **Supabase concentration:** Database, auth, vector, storage, and change feeds centralized—define outage playbooks, cross-region backups, and ensure Vietnam data residency compliance.
- **n8n coupling:** Workflow engine becomes critical dependency; implement retries, alerting, and clear ownership for failed automations.
- **Observability coverage:** Level 3 project demands proactive instrumentation; enforce metrics/tracing budgets early to meet NFR-8 maintainability.
- **LLM provider dependency:** MCP abstraction shields providers, but backend must handle token quotas, cost fluctuations, and graceful failover on provider degradation.

## Component Boundaries

**Epic domain analysis**
- **Epic 1 – Core RAG Pipeline & Infrastructure:** Secure ERP connectivity, PII masking, vector ingestion, and LLM abstraction. Works on transactional ERP tables, embeddings, and audit metadata. Integrates with Supabase PostgreSQL/vector, LLM providers (via MCP), n8n ingestion workflows, optional Redis.
- **Epic 2 – Accounting Domain Intelligence:** Cross-module orchestration, query accuracy validation, compliance checkpoints, pilot enablement. Relies on AR/AP/GL aggregates, tax datasets, curated knowledge packs. Integrates with Supabase functions, domain expert review loops, n8n notifications.
- **Epic 3 – UX & Performance:** Frontend UX polish, streaming responses, onboarding, latency optimization, telemetry dashboards. Uses auth sessions, cached prompt tiers, UI tokens. Integrates with Web Vitals telemetry, SSE/WebSocket channels, feature-flag service.
- **Epic 4 – Security & Production Readiness:** RBAC hardening, audit trail, monitoring, failover drills, deployment runbooks. Touches auth policy tables, audit logs, infra configs, monitoring metrics. Integrates with Supabase audit APIs, observability stack, n8n security automations.

**Domain-aligned backend modules**
- `system-management`: company provisioning, RBAC, Supabase auth sync, audit policy enforcement.
- `chart-of-accounts`: account hierarchy CRUD, Circular 200 validation helpers, trial balance services.
- `accounts-receivable`: customer ledger, invoicing workflows, receipt allocation, aging analytics.
- `accounts-payable`: vendor ledger, bill intake, payment scheduler, disputes.
- `cash-bank`: bank feed ingestion, reconciliation engine, cash forecasting.
- `tax-einvoice`: VAT/CIT/PIT engines, e-invoice lifecycle, XML export.
- `financial-reporting`: statutory reports, management dashboards, export services.
- `period-management`: close checklist, ledger locking, rollover automation.
- `rag-platform`: ingestion pipelines, vector management, retrieval orchestration, answer formatter.
- `shared-kernel`: common utilities (currency/date, Supabase gateway, event abstractions).

**Shared infrastructure components**
- `supabase-gateway`: JDBC connectors, pgvector adapters, change-feed consumers.
- `async-jobs`: Spring Boot worker profile handling indexing, scheduled reconciliations, n8n triggers.
- `observability`: Prometheus exporters, OpenTelemetry tracing, log shippers (ELK/Loki).
- `n8n-bridge`: webhook handlers, signed payload utilities, workflow health checks.
- `frontend-app`: React+Vite SPA using shadcn library, localization, SSE clients.
- `devops-tooling`: Terraform/Supabase CLI scripts, CI/CD pipelines, secrets management.

**Epic-to-component mapping**

| Epic | Core backend modules | Supporting services | Frontend impact |
| --- | --- | --- | --- |
| Epic 1 – Core RAG Pipeline | `rag-platform`, `supabase-gateway`, `async-jobs`, `shared-kernel` | Supabase Vector, LLM providers, n8n ingestion/cleanup flows | Feature-gated RAG entry and telemetry toast |
| Epic 2 – Domain Intelligence | `accounts-receivable`, `accounts-payable`, `financial-reporting`, `rag-platform` orchestration | Accuracy validation suite, compliance review tooling, n8n alerts | RAG workspace UI, insight cards, drill-down panels |
| Epic 3 – UX & Performance | `frontend-app`, `shared-kernel` caches, `rag-platform` streaming adaptors | Web telemetry, CDN/cache config, feature flags | Entire SPA, onboarding guides, latency indicators |
| Epic 4 – Security & Production | `system-management`, `tax-einvoice`, `period-management`, `observability`, `devops-tooling` | Pen-test harness, monitoring/alerting stack, n8n security workflows | Admin consoles, audit explorers, compliance dashboards |

**Boundary implications**
- Vertical slices map to finance domains while `rag-platform` spans horizontally; enable squad-aligned ownership without duplicating shared code.
- Shared modules (`shared-kernel`, `supabase-gateway`) must maintain stable contracts—apply semantic versioning and contract tests within monorepo.
- Async workloads remain co-deployed initially but `async-jobs` and `rag-platform` packages are isolation-ready for future service extraction if load or compliance dictates.

## Architecture Decisions (Web Application)

**Web architecture decisions**
- Frontend: React + Vite SPA using shadcn components styled with Tailwind CSS; TanStack Query manages server state with React Context for lightweight UI state.
- Backend: Spring Boot REST APIs serving ERP + RAG endpoints (JSON responses with SSE/WebSocket channels for live dashboards).
- Data layer: Spring Data JPA backed by Supabase PostgreSQL with Liquibase migrations; pgvector extension managed via Supabase for embedding storage.
- CI/CD: GitHub Actions monorepo pipeline (lint/test → build → integration tests → stage/prod deploy) with manual approval before production aligned to compliance checklist.
- Platform services: Supabase Auth/email/storage/vector unified for ERP and RAG workloads; Redis cluster handles caching/session tokens; n8n orchestrates long-running workflows (e-invoice sync, compliance exports).
- Monitoring/search: Adopt Sentry + Prometheus/Grafana for observability in Phase 1; reassess need for dedicated search engine (Elasticsearch/Meilisearch) once pgvector metadata queries are validated in pilot.

**Refinements after critique**
- Frontend: Enable React Query Devtools in development; document cache invalidation policy reacting to Supabase change feeds; keep Zustand in reserve for future complex client state.
- Backend: Adopt JSON:API-style conventions for filters/pagination; define SSE channel naming (`/events/{module}`) for dashboards and assistant streams.
- Data layer: Enforce Liquibase changelog templates honoring Circular 200 schema rules (four-level accounts, soft deletes, audit triggers).
- CI/CD: Stage pipeline as lint/test → build → integration tests → deploy with compliance approval gate for production.
- Platform services: Supabase email acceptable for MVP; plan fallback (e.g., Resend via MCP) if reliability lags; host Redis via Upstash or self-managed depending on latency benchmarking.
- Observability/search follow-up: Select Sentry + Prometheus/Grafana suite in Sprint 1; evaluate external search only if pgvector recall falls below 0.9 during pilot.

# Solution Architecture Document

**Project:** accounting_erp_rag  
**Date:** 2025-10-17  
**Author:** thanhtoan105

## Executive Summary

The accounting_erp_rag platform delivers a dual experience: a traditional ERP interface compliant with Vietnam Circular 200/2014/TT-BTC and an AI-native RAG assistant that answers finance questions in Vietnamese or English with citations. The solution balances structured accounting workflows—journal entries, AR/AP, tax filing, financial reporting—with conversational insights, giving accountants rapid answers without sacrificing auditability.

To reach that goal quickly we adopt a modular monolith architecture. A React + Vite single-page application (SPA) renders rich data-heavy screens while a Spring Boot back end orchestrates ERP operations, RAG pipelines, and integrations. Supabase provides the PostgreSQL schema, managed authentication, and pgvector-powered retrieval storage. Redis, n8n automation, and MCP-managed LLM providers round out the stack so the team can ship a coherent MVP in 16 weeks and iterate safely.

Cross-cutting concerns—RBAC, immutable audit logs, soft deletes, 10-year retention, performance budgets ≤5 s P95, and bilingual UX—are addressed through explicit design choices: Liquibase-enforced schema rules, JSON:API-style endpoints, TanStack Query caching, OpenTelemetry instrumentation, and staged GitHub Actions pipelines with compliance approvals. The rest of this document drills into each layer so new contributors understand the why, what, and how.

## 1. Technology Stack and Decisions

### 1.1 Technology and Library Decision Table

| Category              | Technology                     | Version    | Justification |
| --------------------- | ------------------------------ | ---------- | ------------- |
| Frontend Framework    | React                          | 18.3.1     | Mature ecosystem, strong TypeScript support, meshes with shadcn patterns for complex data grids. |
| Backend Framework     | Spring Boot                    | 3.2.5      | Proven for enterprise accounting workloads, supports modular packages and actuator metrics out of the box. |
| Language (Frontend)   | TypeScript                     | 5.4.5      | Type safety across UI and shared API clients, reducing onboarding risk for junior devs. |
| Language (Backend)    | Java (Temurin)                 | 21         | LTS release with virtual threads for concurrency spikes during RAG requests and close runs. |
| Build Tooling         | Vite                           | 5.2.9      | Fast dev server + optimized builds, easy integration with React Refresh and Playwright. |
| Database              | Supabase PostgreSQL            | 15.3       | Managed Postgres with row-level security, foreign data wrappers, and audit trails aligned to multi-tenant accounting. |
| Vector Store          | pgvector (Supabase)            | 0.7.4      | Native to Supabase, enabling low-latency semantic search without extra infra. |
| Authentication        | Supabase Auth                  | 2.51.0     | Built-in RBAC primitives, multi-factor options, and session handling integrated with the database. |
| State Management      | TanStack Query                 | 5.28.7     | Handles server cache invalidation, background refetch, and optimistic mutations for ledger updates. |
| Styling               | Tailwind CSS                   | 3.4.3      | Utility-first styling that aligns with shadcn/ui tokens and supports dark mode. |
| Component Library     | shadcn/ui                      | 0.9.0      | Accessible, headless components that map to UX specification patterns (tables, dialogs, wizards). |
| Caching Layer         | Redis (Upstash)                | 7.2.4      | Provides query result caching and distributed locks for RAG indexing jobs. |
| Workflow Automation   | n8n                            | 1.50.1     | Visual orchestration for e-invoice sync, compliance exports, and scheduled notifications. |
| Data Migrations       | Liquibase                      | 4.27.0     | Declarative changelog management enforcing Circular 200 constraints per deployment. |
| Testing (E2E)         | Playwright                     | 1.43.1     | Cross-browser automation with tracing, suitable for bilingual UI checks. |
| Backend Testing       | JUnit + Testcontainers         | 5.10.2 / 1.19.6 | Enables containerized Supabase-compatible tests with realistic schema coverage. |
| Observability         | OpenTelemetry + Sentry         | 1.38.0 / 23.3 | Trace instrumentation from Spring + React, surfaced via Grafana dashboards and Sentry alerts. |
| CI/CD                 | GitHub Actions Runner          | 2.313.0    | Monorepo-aware pipelines with caching and required reviews for production. |

### 1.2 Stack Summary

- **Frontend bundle**: React + Vite SPA consuming REST and Server-Sent Events (SSE) from the Spring Boot API. TanStack Query manages cache invalidation when Supabase emits change events.
- **Backend services**: A modular Spring Boot application with packages per domain (AR, AP, RAG, Reporting) plus dedicated worker profiles for asynchronous jobs.
- **Data platform**: Supabase manages PostgreSQL, Row Level Security (RLS), and pgvector. Liquibase tracks schema migrations, while n8n orchestrates periodic tasks (e.g., e-invoice pushes).
- **DevOps**: GitHub Actions builds both apps, runs Liquibase dry runs, and deploys to AWS ECS Fargate (backend) and CloudFront/S3 (frontend). Observability flows into Grafana + Sentry.

## 2. Application Architecture

### 2.1 Architecture Pattern

We use a **modular monolith** on the server: Spring Boot hosts domain-specific packages (system-management, AR, AP, cash-bank, tax, reporting, rag-platform) behind a single API gateway. Each package exposes controllers, services, repositories, and event listeners, but shares the same deployment artifact to simplify ops for a Level 3 project. Async workloads run through dedicated Spring profiles (`worker`) so we can horizontally scale ingestion without fragmenting the code base prematurely.

On the client we ship a **React SPA** that renders dashboards, ledgers, and the RAG workspace. A shared design system (shadcn + Tailwind tokens) keeps ERP tables and assistant chat surfaces consistent. Communication relies on REST/JSON for CRUD operations and SSE/WebSockets for live analytics and assistant streaming.

### 2.2 Server-Side Rendering Strategy

No SSR is required because ERP users authenticate before accessing any content and SEO is irrelevant. We prioritize blazing-fast client bootstrapping instead: prefetch essential TanStack Query caches after login, lazy-load heavy modules (e.g., financial reports), and hydrate document metadata for accessibility. If we later need partial SSR (e.g., email templates), we can add Spring MVC `thymeleaf` views without disturbing the SPA.

### 2.3 Page Routing and Navigation

React Router v6 organizes routes into guarded layouts:
- `/dashboard` – role-aware overview (cash, AR, AP, tasks) plus assistant summaries.
- `/transactions/ar|ap|cash-bank` – data grids with detail drawers and modals.
- `/ledger/chart-of-accounts` – tree-based navigation for account hierarchy.
- `/periods` – month-end close checklist with progress wizard.
- `/intelligence` – RAG workspace, insight library, prompt builder.
- `/admin` – company settings, user management, audit explorer.

Route guards enforce RBAC; unauthorized users get contextual messaging and a prompt to request access via n8n workflow.

### 2.4 Data Fetching Approach

TanStack Query fetches data through a typed API client generated from OpenAPI specs. Queries use normalized keys (`['ar','invoices',filters]`) and respond to Supabase change-feed webhooks plus manual invalidation after mutations. Background refetch keeps dashboards fresh while SSE streams deliver assistant responses and reconciliation status updates. Mutations wrap optimistic updates with rollback handlers to honor double-entry integrity.

## 3. Data Architecture

### 3.1 Database Schema

The Supabase PostgreSQL schema builds on the existing `accounting_schema.sql` (≈60 % complete). Key tables include:
- `companies`, `user_profiles`, `role_assignments` for multi-tenant identity and RBAC.
- `accounts`, `account_types`, `account_categories` implementing the four-level Circular 200 chart.
- `fiscal_periods`, `journal_entries`, `journal_entry_lines` ensuring double-entry balance.
- `customers`, `vendors`, `sales_invoices`, `purchase_bills`, `payments`, `payment_allocations`.
- `tax_returns`, `e_invoice_batches`, `compliance_events` for statutory reporting.
- `rag_documents`, `rag_chunks`, `rag_queries`, `rag_feedback` supporting retrieval grounding and validation.
- `audit_logs`, `system_events` capturing immutable trails with 10-year retention.

### 3.2 Data Models and Relationships

- Every table is scoped by `company_id` with Row Level Security and composite indexes (`company_id`, `deleted_at`) to meet soft-delete requirements.
- Journal entries enforce double-entry by constraint: `journal_entry_lines` sum debit = sum credit per entry.
- AR/AP modules link invoices/bills to journal entries and payments through bridging tables (`payment_allocations`, `invoice_journal_links`).
- RAG documents store embeddings in pgvector, referencing source records via `source_table` + `source_id`; this allows cross-module drill-down and citation.
- Supabase storage buckets hold attachments with metadata rows referencing them (e.g., invoice PDFs).

### 3.3 Data Migrations Strategy

Liquibase changelogs live in `/apps/backend/src/main/resources/db/changelog`. Each change set:
1. Creates/updates tables or constraints, always with `company_id`, timestamps, `deleted_at`.
2. Adds indexes for `company_id`, `deleted_at`, and vector search columns.
3. Seeds lookup data (account types, roles) via idempotent scripts.
Migration pipeline:
- `./gradlew update -Penv=dev` runs locally against Supabase dev.
- GitHub Actions executes `liquibase updateSQL` for staging/prod validation.
- Change windows require compliance sign-off documented in ADRs.

## 4. API Design

### 4.1 API Structure

The REST API follows a JSON:API-inspired structure under `/api/v1/`. Resources support filtering, pagination (`page[number]`, `page[size]`), and sparse fieldsets. Responses wrap data with `meta` (totals, performance metrics) and `links`. Errors adopt RFC 7807 Problem Details.

### 4.2 API Routes

| Resource | Method(s) | Purpose |
| -------- | --------- | ------- |
| `/api/v1/auth/session` | POST/DELETE | Create or revoke Supabase-authenticated sessions via JWT exchange. |
| `/api/v1/companies/{id}/accounts` | GET/POST/PATCH | Manage chart of accounts (Circular 200 validation). |
| `/api/v1/journal-entries` | GET/POST | Browse and post double-entry journals with automatic balancing guard. |
| `/api/v1/ar/invoices` | GET/POST/PATCH | Issue invoices, attach documents, sync status with e-invoice service. |
| `/api/v1/ap/bills` | GET/POST/PATCH | Capture vendor bills, run duplicate checks. |
| `/api/v1/payments` | POST | Allocate receipts/disbursements with validation of open items. |
| `/api/v1/reports/{type}` | GET | Generate statutory reports (Balance Sheet, Income Statement, Cash Flow). |
| `/api/v1/rag/query` | POST | Submit natural-language questions; returns streaming response ID. |
| `/api/v1/rag/query/{id}/events` | GET (SSE) | Stream assistant messages, citations, and confidence updates. |
| `/api/v1/audit/logs` | GET | Filter audit trail by module, user, timeframe. |

### 4.3 Form Actions and Mutations

- Create/update flows submit JSON payloads with optimistic UI updates; validation errors return field-level details.
- Sensitive operations (period close, e-invoice issuance) require `X-Workflow-Token` generated through n8n to ensure approval gates.
- Bulk uploads (CSV, Excel) hit `/api/v1/import` endpoints, processed asynchronously with progress streamed via SSE.

## 5. Authentication and Authorization

### 5.1 Auth Strategy

Supabase Auth issues JWTs after email/password or magic-link login. We enforce multi-factor for ADMIN and ACCOUNTANT roles. Login flows live entirely within the SPA; backend verifies tokens using Supabase's JWKS.

### 5.2 Session Management

JWTs live in secure HTTP-only cookies (`SameSite=Strict`). Refresh tokens rotate via Supabase `auth.refreshSession`. Session expiry events trigger TanStack Query cache clears and n8n-driven alert emails for suspicious activity.

### 5.3 Protected Routes

React Router guards check role-based access before rendering modules. Backend controllers annotate endpoints with Spring Security `@PreAuthorize("hasRole('ACCOUNTANT')")` etc., aligning to RBAC matrix (ADMIN, ACCOUNTANT, VIEWER).

### 5.4 Role-Based Access Control

- `ADMIN`: full access, manage companies, users, settings.
- `ACCOUNTANT`: post transactions, manage AR/AP, run reports.
- `VIEWER`: read-only access to dashboards and reports.
- Future roles (e.g., `AUDITOR`) can be added by extending Supabase `role_assignments` and Spring security expressions.

## 6. State Management

### 6.1 Server State

TanStack Query caches server data per module with background refetch timers based on latency budgets (e.g., 60 s for AR lists, 5 s for assistant responses). Query invalidation triggers on Supabase change webhooks.

### 6.2 Client State

Minimal local state lives in React Contexts (theme, language preference) and component-level hooks. Zustand is reserved for future offline-first features.

### 6.3 Form State

Forms use React Hook Form + Zod schemas for validation, ensuring consistent error messaging and localization.

### 6.4 Caching Strategy

- **Backend**: Redis caches expensive reports and RAG embeddings metadata for 15 minutes. Key pattern: `company:{id}:report:{type}:{period}`.
- **Frontend**: TanStack Query caches and persists session data per tab; network status watchers handle offline fallbacks.

## 7. UI/UX Architecture

### 7.1 Component Structure

Component folders mirror domains (e.g., `modules/ar/invoices-table.tsx`). Shared primitives live in `components/ui` wrapping shadcn elements. Each complex screen uses a "shell + widgets" model for readability.

### 7.2 Styling Approach

Tailwind CSS with design tokens synchronized from Supabase metadata ensures bilingual label consistency. Custom Tailwind config includes accounting color scales (status chips) and typography for ledger readability.

### 7.3 Responsive Design

Desktop-first with breakpoints for tablet dashboards and read-only mobile summaries. Critical workflows (approvals, assistant) have compact layouts; heavy data entry stays desktop-only per UX spec.

### 7.4 Accessibility

WCAG 2.1 Level AA targets: semantic headings, ARIA labels on data grids, keyboard navigable forms, and dual-language toggles (`vi-VN` / `en-US`). Playwright + axe-core audits run in CI.

## 8. Performance Optimization

### 8.1 Frontend Optimizations

- Code splitting by route, leveraging Vite's prefetch.
- Virtualized tables for large ledgers (react-virtual).
- Skeleton loaders + streaming assistant responses for perceived speed.
- Service worker caches static assets for repeat visits.

### 8.2 Backend Optimizations

- Spring Boot virtual threads handle concurrent RAG requests.
- Connection pooling via HikariCP tuned for Supabase limits.
- Prepared statements and read replicas for heavy reporting queries.
- pgvector indexes partitioned by fiscal period to maintain ≤5 s P95.

### 8.3 RAG Pipeline Performance

- Embedding batching (100 docs per batch) with asynchronous workers.
- Retrieval caching: HNSW indexes with `ef_search=64`.
- Adaptive response streaming: send partial insights within 2 s; degrade gracefully if LLM latency spikes.

### 8.4 Load Testing Plan

Use k6 scripts to simulate 20 concurrent accountants + 5 assistant sessions. Targets: API P95 ≤800 ms for CRUD, SSE initial message ≤2 s, overall assistant response ≤5 s.

## 9. SEO and Discoverability

### 9.1 Meta Tag Strategy

Although product is gated, we maintain accurate metadata for shareable dashboards (within org). Dynamic `og:title`/`og:description` ensures clarity in Slack/MS Teams previews.

### 9.2 Sitemap

No public sitemap is required. Internal documentation includes route inventories for onboarding and automated regression tests verifying navigation.

### 9.3 Structured Data

Not applicable externally. Internally, we annotate assistant responses with JSON metadata (citations, confidence) for analytics.

## 10. Deployment Architecture

### 10.1 Hosting Platform

- **Frontend**: Deployed to AWS S3 + CloudFront (Region ap-southeast-1) with OAC restrictions.
- **Backend**: Containerized Spring Boot on AWS ECS Fargate (task size 1 vCPU/2 GiB). Blue/green deployments via CodeDeploy triggered by GitHub Actions.
- **Supabase**: Managed service (Singapore region) for Postgres, auth, storage, pgvector.
- **Redis**: Upstash (Singapore POP) with TLS enforced.

### 10.2 CDN Strategy

CloudFront caches static assets and assistant media. Response headers enforce caching policies aligned with data sensitivity.

### 10.3 Edge Functions

Optional CloudFront Functions handle language negotiation and security headers (CSP, HSTS).

### 10.4 Environment Configuration

`application-{env}.yaml` configures DB URLs, Redis endpoints, n8n webhooks, and MCP provider keys. Secrets live in AWS Secrets Manager; Supabase uses environment-specific keys managed via GitHub Actions OIDC.

## 11. Component and Integration Overview

### 11.1 Major Modules

- **System Management** – company provisioning, RBAC, audit logging.
- **Chart of Accounts** – account hierarchy editor, compliance validation.
- **Accounts Receivable / Payable** – invoicing, billing, collections, aging analytics.
- **Cash & Bank** – bank reconciliation, cash forecasting.
- **Tax & E-Invoice** – VAT/CIT/PIT calculations, XML exports, external gateway sync.
- **Financial Reporting** – statutory statements, management dashboards.
- **Period Management** – close checklist, variance tracking.
- **RAG Platform** – document ingestion, retrieval, assistant orchestration.

### 11.2 Page Structure

Each module maps to layout shells: list screens + detail drawers, wizard-style flows for approvals, dashboards with card + chart components.

### 11.3 Shared Components

Grid/table wrappers, form controls, approval banners, assistant chat panel, audit timeline, period close progress indicator.

### 11.4 Third-Party Integrations

- Supabase (DB/Auth/Storage)
- n8n workflows (approvals, e-invoice sync, Slack alerts)
- LLM providers via MCP (primary GPT-4.1, fallback Claude 3)
- Email (Supabase, fallback Resend)
- BI exports (CSV/Excel generation using Apache POI)

## 12. Architecture Decision Records

ADRs will be captured in `docs/architecture-decisions.md`. Initial entries:
1. **ADR-001** – Adopt Spring Boot modular monolith with Supabase backend to accelerate MVP while preserving future extraction paths.
2. **ADR-002** – Use React SPA + Vite due to complex interactive dashboards and no SEO requirement.
3. **ADR-003** – Centralize auth and storage on Supabase to inherit compliance-grade audit logging.
4. **ADR-004** – Stream assistant responses via SSE instead of WebRTC to simplify infrastructure.
5. **ADR-005** – Liquibase for schema governance to enforce Circular 200 constraints.

Each ADR includes context, options, decisions, and consequences using ADR template.

## 13. Implementation Guidance

### 13.1 Development Workflow

1. Create feature branch (`feature/<module>-<summary>`).
2. Update Liquibase changelog if data changes are required.
3. Develop backend + frontend in tandem; use `make dev` to start backend (Spring profile `local`) and frontend (Vite dev server) with Supabase local stack.
4. Write unit/integration tests (JUnit/Testcontainers, React Testing Library).
5. Run `pnpm lint`, `pnpm test`, `./gradlew check`, `pnpm playwright test`.
6. Open PR with checklist (tests, migrations, translations).
7. GitHub Actions runs CI; reviews required from architecture and compliance leads.

### 13.2 File Organization

Keep strict modular boundaries: `apps/backend/src/main/java/com/erp/<module>` and `apps/frontend/src/modules/<module>`. Shared DTOs live in `/packages/shared`.

### 13.3 Naming Conventions

- Database tables: `snake_case`.
- APIs: kebab-case endpoints, camelCase payloads.
- Java classes: `PascalCase`, interface suffix `Service`.
- React components: PascalCase, hook names `useSomething`.

### 13.4 Best Practices

- Always include bilingual labels (`name_en`, `name_vn`) on UI strings.
- Ensure double-entry validations run before saving journal entries.
- Document caching strategy in module README to aid future maintainers.

## 14. Proposed Source Tree

```
.
├── apps
│   ├── backend
│   │   ├── build.gradle.kts
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java/com/erp/rag/
│   │   │   │   │   ├── systemmanagement/...
│   │   │   │   │   ├── accountsreceivable/...
│   │   │   │   │   ├── accountspayable/...
│   │   │   │   │   ├── cashbank/...
│   │   │   │   │   ├── taxeinvoice/...
│   │   │   │   │   ├── reporting/...
│   │   │   │   │   ├── periodmanagement/...
│   │   │   │   │   └── ragplatform/...
│   │   │   │   └── resources
│   │   │   │       ├── application.yaml
│   │   │   │       └── db/changelog/
│   │   │   └── test
│   │   │       └── java/com/erp/rag/...
│   └── frontend
│       ├── package.json
│       ├── vite.config.ts
│       └── src
│           ├── app.tsx
│           ├── modules/
│           │   ├── dashboard/
│           │   ├── accounts-receivable/
│           │   ├── accounts-payable/
│           │   ├── cash-bank/
│           │   ├── period-management/
│           │   └── rag/
│           ├── components/ui/
│           ├── hooks/
│           ├── lib/
│           └── styles/
├── packages
│   ├── shared
│   │   ├── src/index.ts (API clients, DTOs, validation schemas)
│   └── eslint-config/
├── infra
│   ├── terraform/
│   ├── github-actions/
│   └── observability/
├── docs
│   ├── solution-architecture.md
│   ├── architecture-decisions.md
│   ├── PRD.md
│   └── ux-specification.md
└── scripts
    ├── seed-data/
    ├── migrations/
    └── dev.sh
```

**Critical folders:**
- `apps/backend/src/main/java/com/erp/rag/...`: Domain modules and controllers.
- `apps/frontend/src/modules`: Feature-specific UI bundles tied to UX flows.
- `infra/terraform`: Infrastructure-as-code defining AWS ECS, CloudFront, Redis, and n8n deployments.

## 15. Testing Strategy

### 15.1 Unit Tests

- Backend: JUnit + Mockito for services, MapStruct mappers, and validation logic.
- Frontend: React Testing Library verifying component rendering, localization, and accessibility attributes.

### 15.2 Integration Tests

- Testcontainers spin up Supabase-compatible Postgres, Redis, and n8n mock endpoints.
- API integration tests validate RBAC, double-entry enforcement, and audit logging.

### 15.3 E2E Tests

Playwright scripts cover:
- Login + MFA.
- Invoice creation -> e-invoice -> payment allocation.
- Period close wizard including assistant recommendations.
- RAG query flow with bilingual prompts and citation verification.

### 15.4 Coverage Goals

- Backend: 80 % statement coverage (critical paths >90 %).
- Frontend: 70 % component coverage with focus on forms + access control.
- E2E: Critical flows executed on each release candidate.

### Testing Specialist Section

- Automate regression packs for bilingual UI copy and WCAG checks.
- Build synthetic datasets mirroring Circular 200 edge cases (multi-currency, negative adjustments).
- Schedule quarterly penetration testing scripts integrated into CI.

## 16. DevOps and CI/CD

- GitHub Actions workflow stages: `lint-and-test`, `build-artifacts`, `integration-tests`, `deploy-staging`, `manual-approval`, `deploy-production`.
- Infrastructure provisioning with Terraform (VPC, ECS cluster, RDS proxy if needed, CloudFront distribution).
- Observability: Prometheus scrapes Spring Actuator metrics; Grafana dashboards track latency, queue depth, and error budgets. Sentry ingests frontend/backend exceptions.
- Incident response runbooks stored in `/infra/observability/runbooks`, tied to PagerDuty escalation.

### DevOps Specialist Section

- Prepare handoff to dedicated DevOps workflow for Kubernetes extraction if workloads grow.
- Document backup/restore drills and practice failover from Supabase primary to read replica.

## 17. Security

- Enforce HTTPS everywhere, HSTS, CSP with nonce for assistant rendering.
- Database encryption at rest (Supabase) and TLS 1.3 in transit.
- Supabase Row Level Security ensures tenant isolation; periodic audits via SQL policies.
- Secrets managed through AWS Secrets Manager + GitHub OIDC; no long-lived credentials.
- Implement anomaly detection: unusual login locations trigger n8n alert to ADMIN role.
- Penetration tests scheduled before each phase release; results logged in ADR.

### Security Specialist Section

- Plan dedicated security architecture review covering LLM prompt injection defenses and data exfiltration monitoring.
- Track compliance artifacts (audit logs, retention policy evidence) in `/docs/compliance`.

## Specialist Sections Summary

**DevOps – Complex (Placeholder)**
- Infrastructure spans AWS ECS Fargate, CloudFront, Supabase, Redis, and n8n orchestrations with Terraform-driven IaC and compliance approval gates.
- Requires dedicated scaling playbooks, disaster recovery drills, and detailed cost dashboards (AWS Cost Explorer + Supabase usage).
- **Next action:** Engage DevOps specialist via `bmad/bmm/workflows/devops-architecture` to design the full operational blueprint.

**Security – Complex (Placeholder)**
- Regulatory scope covers Circular 200 retention, RBAC audit trails, anomaly detection, and LLM prompt-injection safeguards.
- Needs in-depth threat modeling, data loss prevention policies, and evidence management for audits.
- **Next action:** Engage security specialist using `bmad/bmm/workflows/security-architecture` for comprehensive control design and validation.

**Testing – Complex (Placeholder)**
- Mission-critical finance workflows, bilingual UI, and RAG accuracy mandates exceed standard unit/E2E coverage.
- Requires synthetic datasets, chaos tests for retrieval pipeline failures, and automated compliance regression packs.
- **Next action:** Engage test architect with `bmad/bmm/workflows/test-architect` to finalize testing strategy and tooling roadmap.

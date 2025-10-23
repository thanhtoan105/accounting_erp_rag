# Accounting_ERP RAG UX/UI Specification

_Generated on 2025-10-17 by thanhtoan105_

## Executive Summary

**Product Scope:** AI-native accounting ERP platform delivering both traditional ERP workflows and a bilingual RAG chatbot. Coverage spans nine Circular 200-compliant modules (System, Chart of Accounts, AR/AP, Cash & Bank, Tax/E-invoice, Financial Reporting, Period Management, RAG Intelligence) with multi-tenant controls and soft-delete auditing.

**Current Status:** Phase 2 – UX specification within BMAD workflow. PRD and epic backlog are complete; solution architecture follows this artifact. PostgreSQL schema is ~60% ready, with Supabase hosting and RBAC (ADMIN/ACCOUNTANT/VIEWER) already defined.

**Delivery Phases:** Phase 1 prioritizes core ERP workflows plus foundational RAG queries for GL, AR, and AP; Phase 2 deepens RAG orchestration, compliance hardening, and production rollout. Timelines span 8–10 months with 16-week MVP path split across four epics.

**User Environment:** Desktop-first responsive web app optimized for accountants and finance leadership, with tablet support for review tasks and mobile dashboards deferred. Strong emphasis on trust-building (citations, audit trails), rapid insight generation, bilingual UX, and onboarding support.

**Key Constraints & Risks:** Strict adherence to Vietnam Circular 200 auditability, 10-year retention, latency targets (≤5s P95 for complex queries), privacy controls for PII masking, dependency on domain experts and pilot users, and requirement for accessible design (minimum WCAG 2.1 Level A).

---

## 1. UX Goals and Principles

### 1.1 Target User Personas

- **Lan – Senior Accountant (8 years experience)**

  - Goals: accelerate month-end close, maintain accurate sub-ledgers, prioritize overdue collections, comply with audit checkpoints.
  - Pain Points: fragmented ERP navigation across AR/AP/GL, manual Excel reconciliations, difficulty validating AI answers without primary documents, bilingual reporting pressure, fatigue during late close cycles.
  - UX Implications: requires trusted citations, rapid drill-down to vouchers, keyboard-friendly power-user shortcuts, ability to export/share actionable lists, and ergonomic themes (e.g., low-glare/dark mode) for extended sessions.

- **Minh – CFO / Finance Director**

  - Goals: monitor cash position, forecast liabilities, brief leadership quickly, ensure compliance posture.
  - Pain Points: slow cross-module insights, manual board reporting, lack of audit-ready packages, concern about hallucinated AI responses.
  - UX Implications: demands executive dashboards, scenario prompts, latency transparency, recurring snapshot prompts, and one-click exports with audit trails and compliance-ready packaging.

- **Thu – Junior Accountant (new hire)**

  - Goals: learn ERP processes, answer supervisor queries independently, build confidence with accounting terminology.
  - Pain Points: steep learning curve across modules, reliance on peers for guidance, confusion around document lineage, limited familiarity with bilingual terms, fear of making production-impacting mistakes.
  - UX Implications: necessitates guided onboarding, contextual tooltips/glossary hover cards, safe sandbox/preview modes, natural-language education moments, and guardrails that prevent misinterpretation.

- **Compliance & Audit Stakeholders (internal/external)**
  - Goals: verify regulatory adherence, trace decision lineage, reproduce reports and filings.
  - Pain Points: fragmented evidence, inconsistent export fidelity, limited visibility into user actions.
  - UX Implications: require immutable audit trails surfaced in UI, WCAG-aligned layouts for review workflows, and citations/export packages that preserve metadata for audits.

### 1.2 Usability Goals

- Reduce time-to-answer for recurring AR/AP/GL questions by ≥60% vs. legacy ERP navigation (supports month-end close target of ≤3 hours for Lan).
- Provide authoritative, citation-backed responses that finance leadership can trust during board preparation without manual cross-checking, sustaining CSAT ≥4.3 for AI-assisted answers post-pilot.
- Enable ≥70% of first-week onboarding tasks to be completed without peer assistance through guided flows, contextual education, and guardrails.
- Deliver bilingual (VI/EN) experiences with consistent terminology and localized number/date formatting to support cross-functional teams, including auditors and compliance reviewers.
- Uphold WCAG 2.1 Level A accessibility while preserving dense data layouts and keyboard operability required by accounting workflows.

### 1.3 Design Principles

1. **Trust Through Transparency:** Pilot must pair every financial AI response with at least two voucher citations and attach recalculation logs; Phase 2 adds automated variance alerts and forces unresolved variances into the period-close checklist.
2. **Progressive Disclosure:** “Next action / Drill deeper” affordances launch with MVP dashboards and assistant replies; Phase 2 layers ledger lineage maps, attachment previews, and reconciliation notes tied to each disclosure step.
3. **Speed as a Feature:** Instant/quick/deep tiers display upfront; MVP streams partial ledger matches within two seconds and shows queue position; by Phase 2 we publish latency SLAs and escalation rules if P95 exceeds five seconds.
4. **Guided Mastery:** Contextual playbooks and safe preview modes launch with onboarding workflows; later releases add adaptive prompts, bilingual glossary expansion, and supervisor review nudges before high-risk postings.
5. **Inclusive Consistency:** Enforce mirrored VI/EN labels, WCAG 2.1 Level AA focus states, and consistent component behavior across ERP and chat from day one; regression suite must block releases that break localization parity or accessibility baselines.

---

## 2. Information Architecture

### 2.1 Site Map

- **Global entry points**

  - Dashboard (role-aware landing for quick metrics and announcements)
  - Conversational Assistant (persistent RAG panel accessible from header)
  - Notifications & Tasks (pending approvals, alerts, reminders)

- **System Management**

  - Company Profile & Settings (fiscal years, currencies, localization)
  - User & Role Administration (RBAC, session management, audit trail viewer)
  - Audit Log Explorer (queryable 10-year retention logs)

- **Chart of Accounts**

  - Account Hierarchy (four-level tree with filters, bilingual labels)
  - Account Maintenance (create/edit, soft delete, mapping compliance checks)
  - Account Reporting (trial balance, account history)

- **Accounts Receivable (AR)**

  - Customers Directory (profiles, contact info, outstanding balances)
  - Sales Invoices (list, detail, issue workflow, attachments)
  - Receipts & Payments (cash/bank collections, allocation management)
  - Aging & Collections (aging buckets, strategy boards, dispute tracking)

- **Accounts Payable (AP)**

  - Vendor Directory (profiles, payment terms, tax IDs)
  - Purchase Bills (capture, approvals, supporting documents)
  - Payments Scheduler (cash/bank disbursement planning)
  - AP Aging & Obligations (due date heatmaps, alerts)

- **Cash & Bank**

  - Cash Transactions (receipts, disbursements, petty cash logs)
  - Bank Accounts (balances, statements, reconciliation workspace)
  - Bank Reconciliation (auto-match suggestions, manual adjustments, variance resolutions)

- **Tax & E-Invoice**

  - E-Invoice Management (issuance, status tracking, XML exports)
  - Tax Filing Workbench (VAT, CIT, PIT prep and submission status)
  - Compliance Repository (schemas, retention policy dashboard)

- **Financial Reporting**

  - Financial Statements (Balance Sheet, Income Statement, Cash Flow)
  - Management Reports (budget vs actual, KPI snapshots)
  - Export Center (PDF/Excel/XML with audit watermarking)

- **Period Management**

  - Period Calendar (open/close status, deadlines)
  - Period Closing Tasks (checklists, accruals, rollovers)
  - Reopening Workflow (controlled re-open with approvals)

- **RAG Intelligence Layer**
  - Conversational Workspace (query history, saved prompts, bilingual toggle)
  - Insight Library (predefined queries, scenario playbooks)
  - Source Explorer (document citations, traceability trail)

### 2.2 Navigation Structure

- **Primary Navigation (left rail for desktop, collapsible for tablet):**

  - Dashboard
  - Transactions
    - Accounts Receivable
    - Accounts Payable
    - Cash & Bank
  - Ledger & Reporting
    - Chart of Accounts
    - Financial Reports
    - Tax & E-Invoice
  - Period Management
  - Intelligence (RAG Assistant, Insight Library)
  - Administration
    - System Settings
    - Users & Roles
    - Audit Logs

- **Secondary Navigation / Context Tabs:**

  - Within AR/AP modules: List, Detail, Allocations, Disputes
  - Within Financial Reports: Statements, Management Views, Exports
  - Within Intelligence: Recent Queries, Saved Prompts, Sources

- **Global Header Shortcuts:**

  - Universal search with quick actions (jump to invoices, customers, vouchers)
  - Language toggle (VI ⇄ EN)
  - Notifications & approvals center
  - User menu (profile, company switcher, sign-out)

- **Mobile / Tablet Strategy:**

  - Hamburger-triggered primary nav with module badges (alerts count)
  - Bottom sheet quick actions for creating invoices, payments, journal entries
  - RAG assistant floats as expandable bubble for rapid access

- **Breadcrumbs & Contextual Trails:**
  - Display hierarchical path (e.g., Transactions → Accounts Receivable → Invoice INV-2024-001)
  - Provide “Back to list” and “Related records” shortcuts for efficiency

---

## 3. User Flows

#### Flow: Sales Invoice Capture & Payment Allocation

- **User goal:** Record a new sales invoice, issue compliant documentation, and allocate incoming payments to keep AR aging current.
- **Entry points:** Dashboard "Create invoice" CTA, AR module invoice list, or RAG assistant prompt "Issue invoice for customer".
- **Flow steps:**
  1. Lan selects company context and opens `Create Invoice` form.
  2. System pre-fills fiscal period, currency, and default tax schema; Lan adds customer, line items, and applies discounts.
  3. System validates account mappings against Chart of Accounts; if missing, prompts for quick add with compliance guardrails.
  4. Lan attaches supporting documents (PO, delivery note) and triggers bilingual invoice preview.
  5. Approval routing executes based on amount thresholds; approver reviews via notifications center and approves or requests changes.
  6. System issues e-invoice ID, updates journal entries, and posts to AR sub-ledger.
  7. Upon payment receipt, Lan records receipt or bank import auto-matches; unmatched items route to allocation workspace.
  8. Allocation confirmation updates aging buckets and triggers follow-up tasks if partial payments remain.
- **Success criteria:** Invoice posted with correct ledger impact, e-invoice generated, payment allocation completed, AR aging updated.
- **Error states & edge cases:** Missing customer tax data (prompt quick edit), currency mismatch (enforce conversion), approval timeout (escalate to backup approver), payment import duplication (dedupe check), network outage during issuance (retry with queued status).

#### Flow: Purchase Bill Intake & Payment Scheduling

- **User goal:** Capture a vendor bill, verify compliance, and schedule payment without missing credit terms.
- **Entry points:** AP module "Upload bill" CTA, email-to-bill ingestion queue, or assistant prompt "Record vendor invoice for {{vendor}}".
- **Flow steps:**
  1. Thu opens the pending ingestion queue and reviews OCR-extracted bill data.
  2. System cross-checks vendor master data (tax ID, bank account); mismatches prompt inline correction.
  3. Thu assigns expense accounts, project tags, and uploads receipts; system validates VAT rules per Circular 200.
  4. Compliance checklist runs (duplicate detection, approval routing thresholds, 3-way match exceptions).
  5. Approver receives notification, reviews supporting documents, and approves or flags discrepancy.
  6. Approved bill posts to AP sub-ledger and appears in payment scheduler with due date and cash impact forecast.
  7. Minh or treasury analyst selects bills for payment run, confirming bank source and priority rules.
  8. System generates payment batch, exports bank file or triggers Supabase function to initiate transfer, logs audit trail.
  9. Payment confirmation reconciles to bank feed; unpaid items remain scheduled with escalation notices.
- **Success criteria:** Bill posted with correct ledger entries, approvals captured, payment batch generated before due date, audit log updated.
- **Error states & edge cases:** OCR confidence below threshold (manual input), duplicate bill detection, missing approver, insufficient cash flag, bank rejection requiring reinitiation.

#### Flow: Month-End Close & Financial Reporting

- **User goal:** Complete month-end close efficiently, ensure ledger accuracy, and deliver bilingual financial statements for leadership review.
- **Entry points:** Period Management dashboard "Close current period", notifications prompting open tasks, or assistant query "What remains before closing?".
- **Flow steps:**
  1. Lan reviews period close checklist with auto-generated tasks (reconcile sub-ledgers, verify accruals, finalize tax filings).
  2. System surfaces outstanding tasks with responsible owners and due dates; Lan delegates or reassigns as needed.
  3. RAG assistant summarizes anomalies (unmatched transactions, variance spikes) with citations and recommended actions.
  4. Lan resolves exceptions (journal adjustments, approvals) using guided forms; audit trail captures changes.
  5. Compliance check ensures all mandatory documents attached, 10-year retention policies satisfied.
  6. Minh reviews preliminary financial statements, toggling Vietnamese/English, and drills into variance explanations.
  7. Once satisfied, Minh approves period closure; system locks ledgers, timestamps closure, and notifies stakeholders.
  8. Reporting pack generated (Balance Sheet, Income, Cash Flow, Management KPIs) with export-ready formats and commentary slots.
  9. Assistant posts closure summary to leadership channel with next-period prep tasks.
- **Success criteria:** All checklist items completed, ledgers locked, reports delivered, audit trail intact.
- **Error states & edge cases:** Pending transactions, unresolved variance, missing approvals, late data imports, regulatory change requiring additional disclosures.

#### Flow: Conversational Insight & Drill-Down

- **User goal:** Minh requests a consolidated cash position and drills into underlying transactions using the RAG assistant.
- **Entry points:** Global assistant invocation from header, dashboard insight card “Ask about cash”, or voice command (future).
- **Flow steps:**
  1. Minh opens the assistant, selects company context, and types "What is our cash position vs. forecast for this month?".
  2. Assistant interprets intent, retrieves latest cash ledger, bank balances, and forecast data with confidence scores.
  3. Assistant presents summary (VI/EN) with variance callouts, latency tier indicator, and top contributing factors.
  4. Minh clicks "Drill deeper" to view breakdown by bank account; assistant opens side panel with tabular data and charts.
  5. Minh asks follow-up "Show transactions driving the shortfall"; assistant filters to variance window and cites vouchers.
  6. Minh exports summarized insight and supporting transactions to PDF for board prep.
  7. System logs query, results, and data sources in audit trail; flagged anomalies create follow-up tasks for Lan.
- **Success criteria:** Insight delivered with citations, drill-down reveals source transactions, export generated, tasks assigned if anomalies.
- **Error states & edge cases:** Low confidence response (assistant offers rephrase or manual report), missing data source (prompts for refresh), access denied (RBAC warning), latency breach (fallback to scheduled report).

#### Flow: Audit Trail Review & Evidence Export

- **User goal:** External auditor validates transactions and exports evidence packages for compliance.
- **Entry points:** Audit Log Explorer, shared deep link to specific voucher, or assistant prompt "Show evidence for payment batch {{id}}".
- **Flow steps:**
  1. Auditor logs in with VIEWER role and selects company and period filters.
  2. System displays audit dashboard with pending requests, risk flags, and quick filters (module, user, action type).
  3. Auditor drills into payment batch; timeline view lists creation, approvals, edits, and system automations with timestamps.
  4. Supporting documents (invoices, receipts, approvals) preview inline with bilingual annotations.
  5. Auditor marks items for evidence pack; system assembles PDF/ZIP with metadata, signatures, and checksum.
  6. Auditor uses assistant query "Any anomalies for this batch?"; assistant summarizes unusual activity with citations.
  7. Findings exported and stored in audit workspace; system logs access and export actions.
- **Success criteria:** Comprehensive evidence pack generated, anomalies identified, audit log updated, minimal manual reconciliation.
- **Error states & edge cases:** Missing document (flag for remediation), permissions conflict (requires admin approval), large export size (async generation), time zone discrepancies (display highlight).

---

## 4. Component Library and Design System

### 4.1 Design System Approach

Adopt a modular design system built on shadcn/ui primitives layered with enterprise table patterns tuned for accounting workloads. Tokens (color, elevation, spacing, typography) are centralized in Supabase metadata so web clients and the RAG assistant stay synchronized across light/dark themes. Components follow Atomic Design, but each tier includes finance-specific variants—voucher status chips, ledger tables with pinning, approval banners with audit metadata. Every component ships with bilingual (VI/EN) content slots and keyboard-first interactions to satisfy WCAG 2.1 AA. Governance: fortnightly triad (UX, frontend, compliance) reviews new components, runs localization snapshots, and logs accessibility regression results for audit retention.

### 4.2 Core Components

| Component                   | Purpose                                                     | Key Variants                                                                                | Critical States                                          | Usage Guidelines                                                                                          |
| --------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| Data Table (Ledger)         | Present dense financial records with inline actions.        | Standard, Grouped, Pinned Summary Row, Audit View.                                          | Default, Hover, Selected, Editable, Locked, Error.       | Support column pinning, sticky totals, VI/EN headers; row-level actions surface in context menu.          |
| Drawer / Side Panel         | Secondary workspace for drill-downs and assistant insights. | Narrow (filters), Medium (record details), Wide (audit trail).                              | Default, Loading, Dirty, Read-only.                      | Triggered from tables and assistant replies; maintain breadcrumb context and keyboard trapping.           |
| Approval Banner             | Persist approvals status across transactions.               | Info, Pending, Approved, Rejected, Escalated.                                               | Default, Hover on actions, Disabled (insufficient role). | Surface approver avatars, due-by timestamps, escalation CTA tying to notifications.                       |
| Form Controls Suite         | Capture financial entries with validation.                  | Text, Numeric with thousand separators, Date picker, Account selector, Attachment uploader. | Default, Focus, Error, Disabled, Success.                | Enforce mask formatting, support bilingual helper text, show validation inline while logging audit notes. |
| Summary Card                | Snapshot KPIs and compliance health.                        | Metric, Trend, Comparison, Alert.                                                           | Default, Hover, Pressed, Critical alert.                 | Include quick action footer, latency badge when AI-derived, accessible color pairings for status.         |
| Timeline / Audit Feed       | Visualize chronological events.                             | Compact, Detailed with documents, AI narrative overlay.                                     | Default, Expanded, Collapsed, Highlighted anomaly.       | Attach documents inline, provide copyable hash metadata, allow filter by user/module.                     |
| Modal Wizard                | Guided multi-step workflows (close period, batch payments). | 2-step, 3-step+, Review-only.                                                               | Default, Active step, Error inline, Completed.           | Persist progress state, allow save-as-draft, integrate tooltips/glossary for novices.                     |
| Toast / Inline Notification | Feedback for async operations.                              | Success, Info, Warning, Error.                                                              | Default, Sticky (requires action), Auto-dismiss.         | Include undo for reversible actions, show audit trail link when applicable.                               |

---

## 5. Visual Design Foundation

### 5.1 Color Palette

- **Primary:** Deep Teal (#0F3D3E) — anchors trust and stability in finance dashboards.
- **Secondary:** Golden Amber (#F5A623) — highlights actionable insights and approvals.
- **Accent:** Electric Aqua (#2ED1C2) — signals AI-driven insights and real-time status.
- **Support:** Slate Gray (#4A5568) for typography, Mist White (#F7FAFC) for surfaces, Midnight Navy (#1A202C) for dark mode backgrounds.
- **Semantic:** Success (#2F855A), Warning (#D69E2E), Critical (#C53030) aligned with accessibility contrast ratios.

### 5.2 Typography

**Font Families:**

- **Primary:** Inter (UI), chosen for clarity in dense data tables and robust Vietnamese diacritics.
- **Secondary:** Source Serif Pro for long-form guidance and assistant narratives.
- **Fallbacks:** System sans-serif stack ("Inter", "Segoe UI", "Helvetica Neue", Arial) and system serif stack for resilience.

**Type Scale:**

- Display 32 / 40px line-height (executive summaries).
- Heading 1 26 / 32px (module headers).
- Heading 2 22 / 28px (section panels).
- Heading 3 18 / 24px (table group headers).
- Body 1 16 / 22px (primary copy).
- Body 2 14 / 20px (dense tables, helper text).
- Caption 12 / 18px (metadata, audit notes).

### 5.3 Spacing and Layout

- **Grid:** 12-column layout with 72px margins on desktop, collapsing to 8 columns on tablet and 4 on mobile.
- **Spacing scale:** 4px base unit; key steps 8, 12, 16, 24, 32px to balance dense data with breathing room.
- **Tables:** 56px header height, 44px row height desktop, 40px compact mode; sticky headers and summary rows.
- **Cards & Panels:** 16px interior padding, 24px vertical rhythm between sections, 12px chip spacing.
- **Assistant Panel:** 480px width on desktop, 100% width on mobile overlay with 16px gutters.
- **Motion spacing:** micro-interactions capped at 150ms transitions, 400ms for modal/context shifts to maintain perceived responsiveness.

---

## 6. Responsive Design

### 6.1 Breakpoints

- **Desktop ≥1280px (Lan & Compliance):** 12-column grid, persistent left nav, assistant docked with dual-pane ledger views, frozen first/last columns for reconciliations.
- **Tablet 768–1279px (Minh on-the-go):** 8-column grid, collapsible nav, assistant overlays, quick KPI tiles for finance leadership check-ins.
- **Mobile ≤767px (Thu during walkthroughs):** 4-column grid, bottom nav highlighting approvals/tasks, assistant full-screen with guided prompts and quick approve/decline gestures.
- **Audit Mode:** Lock breadcrumbs and timeline feed on desktop/tablet, provide horizontal scroll with frozen evidence columns for narrow displays.

### 6.2 Adaptation Patterns

- Persona-aware navigation drawer: collapses into quick jump menu with compliance view pinning Audit Trail and Approvals.
- Tables morph into card stacks showing overdue status, approval state, and next action; “View raw table” fallback respects RBAC permissions.
- Filters slide into drawers with sticky apply/reset controls and “Save filter set” for CFO/Compliance routines.
- Assistant presence shifts: floating bubble (desktop), panel overlay (tablet), full-height guided chat (mobile) with bilingual quick replies.
- Charts guarantee ≥44px touch targets, offer “View as table,” and narrate data for accessibility.
- Breadcrumbs, sticky headers, and mini timeline anchors persist context to prevent disorientation across breakpoints.

---

## 7. Accessibility

### 7.1 Compliance Target

WCAG 2.1 Level AA, bilingual parity, and audit-traceable interactions compliant with Circular 200 retention. OKRs: 100% workflows keyboard-operable, ≤5 critical accessibility defects per release, remediation within two sprints.

### 7.2 Key Requirements

- Keyboard-first navigation with visible focus rings across tables, wizards, and assistant surfaces (supports Lan and auditors).
- VI/EN toggle preserves Vietnamese diacritics, semantic meaning, and localized numbers/dates for all modules.
- Screen reader parity: ledger summaries exposed via ARIA regions, charts backed by data tables and narrative summaries, AI replies annotated with roles.
- Contrast ratios ≥4.5:1 for text and ≥3:1 for UI elements; density toggle retains focus states and tooltip legibility.
- Conversational assistant provides transcripts for audio, voice-to-text parity, and fallback prompts when confidence drops.
- Interaction logs exportable in accessible formats with timestamps, user IDs, and actions for compliance review.
- Accessibility QA: automated axe/pa11y checks per build, quarterly NVDA & VoiceOver audits, remediation logged for 10-year retention.

---

## 8. Interaction and Motion

### 8.1 Motion Principles

- Prioritize clarity over spectacle; animations reinforce state changes (approvals, ledger sync) within 150–300ms.
- Communicate AI thinking states with subtle progress pulses; never obscure interaction controls.
- Respect user control: minimize unexpected auto-play elements, offer reduced motion preferences.
- Align motion tokens across shadcn/ui components and custom modules for consistency.

### 8.2 Key Animations

- Approval banner slide-in with status color transition and avatar fade (200ms).
- Ledger row inline edit: expand/collapse with eased height transition (180ms) and focus handoff.
- Assistant response reveal: shimmer placeholder followed by card fade-in; citations cascade for clarity.
- Notification toast stack: staggered slide/fade preserving screen reader announcement order.
- Modal wizard step transition: horizontal shift with progress indicator pulse.

---

## 9. Design Files and Wireframes

### 9.1 Design Files

No dedicated design tool assets yet; development will proceed directly from this specification. Future Figma file to be created post-MVP for visual polish.

### 9.2 Key Screen Layouts

#### Dashboard & RAG Workspace

- **Hero metrics row:** Four summary cards (cash, receivables, payables, tasks) with latency badges, role-based widget swaps, and quick filters (company, fiscal period).
- **Dual-pane layout:** Left 8 columns for KPI charts (cash trend, invoice aging); right 4 columns for assistant conversation history and saved prompts.
- **Assistant input zone:** Sticky bottom input with prompt suggestions, citation toggle, and empty-state onboarding tips for first-time users.
- **Notifications strip:** Top inline banner for approvals, anomalies, and system alerts with quick resolve buttons.
- **Responsive notes:** KPI charts collapse into stacked cards on tablet; assistant becomes overlay on mobile while filters condense into a drawer.

#### Sales Invoice Detail & Allocation

- **Header band:** Invoice metadata (status, customer, amount, due date) with multi-currency badges, approval banner, SLA countdown, and action buttons.
- **Tabbed content:** Summary, Line items, Attachments, Audit trail with inline error messaging and keyboard shortcuts (e.g., Shift+Enter to save line).
- **Right sidebar:** Allocation suggestions, related receipts, assistant insights, and escalation prompts when approvals stall.
- **Activity timeline:** Chronological log with avatars, comments, AI notes, and audit evidence download links.
- **Responsive notes:** Tabs convert to accordion on mobile; sidebar becomes slide-over drawer with persistent SLA indicator.

#### Period Close Checklist

- **Progress overview:** Donut chart showing completion %, countdown timer, and export close packet CTA.
- **Task board:** Grouped by category with status pills, assignee avatars, due dates, and dependency lines for blocked tasks.
- **Detail drawer:** Selecting a task opens right-side drawer with checklist, attachments, assistant recommendations, and remediation notes.
- **Insights footer:** RAG assistant surfaces anomaly cards with severity tags and direct jump into impacted tasks.
- **Responsive notes:** Board shifts to Kanban cards on mobile; drawer becomes full-screen modal with quick access to completion history.

---

## 10. Next Steps

### 10.1 Immediate Actions

- Align with engineering to translate core components into shadcn/ui implementations with Supabase token sync.
- Conduct stakeholder walkthrough (Lan, Minh, Thu, Compliance) validating user flows and data states.
- Prioritize backend/API contracts for RAG assistant citations, approval SLA tracking, and audit exports.
- Create accessibility QA backlog (axe/pa11y automation, screen reader scripts) before development sprint.
- Spin up Figma workspace post-MVP to evolve visuals without blocking build.

### 10.2 Design Handoff Checklist

- [ ] All user flows documented with responsive states.
- [ ] Component inventory mapped to shadcn/ui primitives + custom variants.
- [ ] Accessibility requirements integrated into Definition of Done.
- [ ] Responsive behavior verified (desktop/tablet/mobile/audit mode).
- [ ] AI assistant interactions documented with confidence fallbacks & citations.
- [ ] Motion specs aligned with reduced-motion preferences and token library.
- [ ] Audit export workflows defined with evidence metadata.

---

## Appendix

### Related Documents

- PRD: `docs/PRD.md`
- Epics: `docs/epics.md`
- Tech Spec: `docs/tech-spec.md`
- Architecture: `docs/architecture.md`

### Version History

| Date     | Version | Changes               | Author        |
| -------- | ------- | --------------------- | ------------- |
| 2025-10-17 | 1.0     | Initial specification | thanhtoan105 |

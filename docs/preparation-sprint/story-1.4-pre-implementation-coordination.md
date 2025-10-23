# Story 1.4 – Pre-Implementation Coordination Log

**Date:** 2025-10-21  
**Engineer:** thanhtoan105  
**Objective:** Capture the coordination actions required before implementing Story 1.4 (Document Embedding Generation Pipeline).

---

## 1. PII Masking Service Alignment
- **Session:** 1-hour sync with Story 1.2 owner (PII Masking lead)
- **Scheduled:** 2025-10-22 09:00–10:00 ICT (Zoom – RAG Core Room)
- **Agenda:**
  1. Confirm `PiiMaskingService` interface contract (`maskText(String rawText, DocumentType type)`) and error taxonomy.
  2. Validate <100 ms SLA measurements and instrumentation points for `pii_masking_latency_ms`.
  3. Review escalation flow when `PiiMaskingException` occurs during batch runs.
- **Pre-work:** Collect sample masked payloads per document type to rehearse pipeline integration tests.

## 2. Indexing State Machine Whiteboard
- **Session:** 30-minute workshop with tech lead (Incremental Sync owner)
- **Scheduled:** 2025-10-22 14:30–15:00 ICT (Miro + Meet link shared via calendar)
- **Agenda:**
  1. Walk through incremental vs. full indexing transitions and `sync_status` schema.
  2. Define atomic update steps for `embedding_batches` (`queued → running → complete/failed`).
  3. Align on soft-delete propagation from ERP tables (`deleted_at` handling) to `vector_documents`.
- **Artifacts:** Whiteboard snapshot will be archived in `/docs/preparation-sprint/indexing-state-machine-whiteboard-20251022.png` after the session.

## 3. DevOps Coordination – N8N Webhook Secret
- **Owner:** DevOps squad (Anh Le)
- **Action:** Create and distribute `N8N_WEBHOOK_SECRET` for staging & production.
- **Status:** Ticket `DEVOPS-1287` opened in Jira with due date 2025-10-23. Secret will be provisioned via AWS Secrets Manager and synced to Supabase config vars.
- **Follow-up:** Validate secret availability before enabling scheduled n8n workflows.

## 4. Architecture Decision Record
- **ADR ID:** ADR-006 – Embedding Model Selection (see `docs/ADR-006-embedding-model-selection.md`)
- **Summary:** Azure OpenAI `text-embedding-3-large` selected as primary model; `text-embedding-3-small` kept as cost-saving fallback; sentence-transformers considered for self-hosting in Epic 2.
- **Next Steps:** Share ADR with architecture guild during next stand-up (2025-10-23).

---

### Checklist Alignment
- [x] PII masking sync scheduled with clear agenda.
- [x] Indexing whiteboard session arranged with tech lead, covering state machine decisions.
- [x] DevOps ticket raised for `N8N_WEBHOOK_SECRET` provisioning.
- [x] ADR-006 drafted and linked for cross-team review.


# ADR-006: Embedding Model Selection for Story 1.4

- **Date:** 2025-10-21  
- **Status:** Proposed  
- **Story Link:** docs/stories/story-1.4.md  
- **Owners:** Platform Engineering (Story 1.4 dev agent), Architecture Guild

---

## Context

Epic 1 requires a production-ready document embedding pipeline that supports Vietnamese and English ERP content. Story 1.3 provisioned the `vector_documents` schema with 1,536-dimensional vectors (aligned with prior ada-002 usage) and validated pgvector performance up to 100K documents. Story 1.2 delivered deterministic PII masking that must run prior to embedding.

Key constraints and needs:

1. **Language coverage:** High accuracy for Vietnamese accounting terminology, mixed Vietnamese/English text, and diacritics while preserving compliance masking.
2. **Throughput targets:** Batch processing must handle 10K documents in `< 30 minutes` with ≥200 docs/min sustained throughput.
3. **Cost transparency:** Embedding cost must align with MVP budgets (<$0.70 per 10K documents) and expose per-batch spend in telemetry.
4. **Vector compatibility:** Must integrate with existing `vector_documents.embedding VECTOR(1536)` column or justify schema change.
5. **Operational runway:** Prefer managed provider with existing networking/security posture, fallbacks ready for provider degradation (Story 1.7).

## Decision

Adopt **Azure OpenAI `text-embedding-3-large`** as the primary embedding model for Story 1.4 with the following implementation details:

- **Primary deployment:** Azure OpenAI East Asia resource (same tenant as GPT-4.1 usage) accessed through existing VNet integration.
- **Dimension:** 3,072. To remain compatible with `vector_documents` (1,536) we will enable provider-side dimensionality reduction (`"dimensions": 1536`) during API invocation.
- **Batching:** Use 100-document batches to respect 3,000 RPM and 1,000,000 tokens/min limits with exponential backoff retry.
- **Telemetry:** Record tokens used, API latency, and estimated USD cost in `embedding_batches.metadata`.
- **Fallback:** Configure `text-embedding-3-small` as cost-optimized fallback (1,536 dimensions natively) for outages or cost-saving runs.

## Alternatives Considered

| Option | Pros | Cons |
| ------ | ---- | ---- |
| **Azure OpenAI text-embedding-3-small (1536)** | Lower cost ($0.02 / 1K tokens); no down-projection required | Slightly lower recall for Vietnamese financial terminology; existing benchmarks show ~6% drop in recall@10 |
| **SentenceTransformers `bge-base-vi-v1.5` (self-hosted)** | Tailored Vietnamese embeddings; no vendor lock | Requires GPU hosting + MLOps investment; adds 2-3 weeks infra work; latency risk without proper scaling |
| **Legacy ada-002 (OpenAI)** | Familiar integration; schema already aligned | Deprecated by provider; poorer Vietnamese coverage; higher cost ($0.10 / 1K tokens) |

## Consequences

- Need to extend embedding service to pass `"dimensions": 1536` parameter to Azure OpenAI API. If provider drops support, schema migration to 3,072 dimensions will be planned in follow-up Story 1.4B.
- Must store provider configuration (model name, deployment, API version) in secure configuration and surface in run metadata.
- Benchmarking harness (Story 1.4) will validate recall/regression vs. 10K synthetic dataset using text-embedding-3-large.
- Cost metrics will be included in observability dashboards and Slack completion summary (AC10).

## Follow-up Actions

1. Update infrastructure secrets with `AZURE_OPENAI_EMBEDDING_ENDPOINT` and key references (DevOps ticket DEVOPS-1287 linked).
2. Extend provider adapter to implement fallback routing (text-embedding-3-small) with circuit breaker thresholds shared with Story 1.7.
3. Revisit decision post-pilot to evaluate self-hosted model readiness (Epic 2 milestone).

## References

- docs/stories/story-1.4.md (Acceptance Criteria 3, 4, 10)
- docs/performance-benchmark-report-story-1.3.md (Vector latency baselines)
- scripts/prep-sprint/embedding-dimension-planner.py (Cost/storage estimator)
- Microsoft Azure OpenAI docs: https://learn.microsoft.com/azure/ai-services/openai/how-to/embeddings


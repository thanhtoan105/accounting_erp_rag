# Requirements Traceability Matrix: Story 1.5

**Story:** Basic RAG Query Processing Pipeline  
**Story ID:** 1.5  
**Assessment Date:** 2025-10-21  
**Test Architect:** dev-agent  
**Status:** Implementation Complete, Tests Planned

---

## Executive Summary

**Overall Coverage Status:** 🟡 **PARTIAL**

| Coverage Type | Count | Percentage |
|---------------|-------|------------|
| FULL | 0 | 0% |
| PARTIAL | 5 | 50% |
| UNIT-ONLY | 0 | 0% |
| INTEGRATION-ONLY | 0 | 0% |
| NONE | 5 | 50% |
| **TOTAL AC** | **10** | **100%** |

**Critical Gaps (P0):**
- 5 service unit tests missing (QueryEmbedding, VectorSearch, ContextWindow, QueryLogger, RagQuery)
- 2 integration tests missing (Pipeline E2E, Vector Search with real data)

**Risk Level:** 🔴 **HIGH** - Core revenue-impacting functionality lacks automated test validation

---

## Traceability Matrix

### AC1: REST API Endpoint with RBAC

**Priority:** P0 (Critical - revenue-impacting)  
**Coverage Status:** 🟡 **PARTIAL**

**Requirements:**
- REST endpoint `/api/v1/rag/query` accepting JSON payload
- Request validation: companyId (UUID), query (string, 1-2000 chars), language (vi|en), filters (optional)
- RBAC validation via Supabase JWT
- Company-level tenant scoping

**Implemented:**
- ✅ RagQueryController with POST endpoint
- ✅ QueryRequest DTO with Bean Validation annotations
- ✅ Error handling with proper HTTP status codes
- ⚠️ JWT extraction placeholder (userId hardcoded)

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-001 | RagQueryControllerTest.java | Valid request returns 200 OK | EXISTS | PARTIAL |
| 1.5-UNIT-001 | RagQueryControllerTest.java | Request validation (null companyId) | EXISTS | PARTIAL |
| 1.5-UNIT-001 | RagQueryControllerTest.java | Request validation (empty query) | EXISTS | PARTIAL |
| 1.5-UNIT-001 | RagQueryControllerTest.java | Request validation (invalid language) | EXISTS | PARTIAL |
| 1.5-UNIT-001 | RagQueryControllerTest.java | Request validation (query too long) | EXISTS | PARTIAL |
| 1.5-UNIT-001 | RagQueryControllerTest.java | Vietnamese UTF-8 query handling | EXISTS | PARTIAL |

**Issues:**
- ⚠️ Test uses incorrect mocking setup (needs @MockBean instead of plain mocking)
- ⚠️ Test expects streaming response (SSE) but implementation returns synchronous QueryResponse
- ⚠️ No RBAC/JWT extraction tests (userId placeholder not validated)
- ⚠️ No tenant scoping validation tests

**Severity:** 🟡 **MEDIUM** - Tests exist but need rewrite to match implementation

**Recommendation:** Rewrite RagQueryControllerTest to match actual synchronous implementation, add tenant scoping tests

---

### AC2: Query Embedding Generation

**Priority:** P0 (Critical - core functionality)  
**Coverage Status:** 🔴 **NONE**

**Requirements:**
- Reuse AzureOpenAiEmbeddingService from Story 1.4
- Generate 1536-dimension embeddings
- Validate embedding dimensions and format
- Format embedding as Postgres vector string
- Measure embedding latency (<500ms target)

**Implemented:**
- ✅ QueryEmbeddingService wraps AzureOpenAiEmbeddingService
- ✅ Dimension validation (1536)
- ✅ Format conversion to Postgres vector string
- ⚠️ Query embedding caching DEFERRED

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-002 | QueryEmbeddingServiceTest.java | Generate embedding with dimension validation | MISSING | NONE |
| 1.5-UNIT-002 | QueryEmbeddingServiceTest.java | Format embedding for Postgres | MISSING | NONE |
| 1.5-UNIT-002 | QueryEmbeddingServiceTest.java | Handle embedding generation failure | MISSING | NONE |
| 1.5-UNIT-002 | QueryEmbeddingServiceTest.java | Validate latency metrics | MISSING | NONE |

**Severity:** 🔴 **CRITICAL** - No automated validation of core embedding pipeline

**Recommendation:** Create QueryEmbeddingServiceTest with mocked AzureOpenAiEmbeddingService

---

### AC3: Vector Similarity Search

**Priority:** P0 (Critical - core functionality)  
**Coverage Status:** 🔴 **NONE**

**Requirements:**
- Execute pgvector cosine similarity search
- Return top-10 most relevant documents
- Calculate cosine distance with `<=>` operator
- Apply company_id tenant filter
- Filter soft-deleted documents (deleted_at IS NULL)

**Implemented:**
- ✅ VectorSearchService with findSimilarVectors method
- ✅ Top-10 retrieval via VectorDocumentRepository
- ✅ Tenant filtering (company_id)
- ✅ Excerpt extraction (200 chars)
- ✅ JSONB metadata parsing

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Top-10 similarity search | MISSING | NONE |
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Relevance score calculation | MISSING | NONE |
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Document excerpt extraction | MISSING | NONE |
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Metadata parsing from JSONB | MISSING | NONE |
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Empty results handling | MISSING | NONE |
| 1.5-INT-002 | VectorSearchIntegrationTest.java | Search with seeded documents | MISSING | NONE |
| 1.5-INT-002 | VectorSearchIntegrationTest.java | Validate relevance ranking order | MISSING | NONE |

**Severity:** 🔴 **CRITICAL** - No automated validation of core search algorithm

**Recommendation:** Create VectorSearchServiceTest (unit) + VectorSearchIntegrationTest (with Testcontainers)

---

### AC4: Document Ranking with Relevance Scores

**Priority:** P0 (Critical - affects UX)  
**Coverage Status:** 🔴 **NONE**

**Requirements:**
- Convert cosine distance to relevance score: `relevance = 1.0 - distance`
- Relevance range 0.0-1.0 (higher is better)
- Rank documents by relevance descending
- Persist rank and relevance_score in RagQueryDocument

**Implemented:**
- ✅ Relevance calculation in VectorSearchService
- ⚠️ Placeholder formula (0.9 - rank * 0.05) - needs correction to use actual cosine distance

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-003 | VectorSearchServiceTest.java | Relevance score calculation | MISSING | NONE |
| 1.5-INT-001 | RagQueryPipelineIntegrationTest.java | Validate relevance scores persisted | MISSING | NONE |

**Severity:** 🔴 **CRITICAL** - Relevance calculation uses placeholder formula, no test validation

**Recommendation:** Fix relevance formula to use actual cosine distance, add unit tests

---

### AC5: Context Window Management (8K Token Budget)

**Priority:** P0 (Critical - affects LLM input)  
**Coverage Status:** 🔴 **NONE**

**Requirements:**
- Estimate tokens per document (chars ÷ 4 approximation)
- Prune documents to 8K token budget
- Prioritize highest relevance documents first
- Concatenate documents with separator "\n\n---\n\n"
- Log pruning metrics (original count, pruned count, total tokens)

**Implemented:**
- ✅ ContextWindowManager service
- ✅ Token estimation (chars ÷ 4)
- ✅ Document pruning by relevance
- ✅ Concatenation with separator
- ✅ Token calculation per document

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-004 | ContextWindowManagerTest.java | Token estimation (chars ÷ 4) | MISSING | NONE |
| 1.5-UNIT-004 | ContextWindowManagerTest.java | Context pruning at 8K budget | MISSING | NONE |
| 1.5-UNIT-004 | ContextWindowManagerTest.java | Document concatenation | MISSING | NONE |
| 1.5-UNIT-004 | ContextWindowManagerTest.java | Edge case: no documents | MISSING | NONE |
| 1.5-UNIT-004 | ContextWindowManagerTest.java | Edge case: all docs exceed budget | MISSING | NONE |

**Severity:** 🔴 **CRITICAL** - Token budget logic not validated, could cause LLM context overflow

**Recommendation:** Create ContextWindowManagerTest with various document sizes

---

### AC6: Metadata Filtering

**Priority:** P1 (High - nice to have)  
**Coverage Status:** 🟡 **PARTIAL**

**Requirements:**
- Filter by module (ar, ap, gl)
- Filter by fiscal_period (YYYY-MM)
- Filter by document_type (invoice, bill, etc.)
- Filter by minConfidence threshold
- Use JSONB containment operator `@>` for filtering

**Implemented:**
- ⚠️ Basic metadata parsing from JSONB
- ⚠️ Full JSONB filtering DEFERRED to follow-up

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-INT-002 | VectorSearchIntegrationTest.java | Filter by module (ar) | PLANNED | PARTIAL |
| 1.5-INT-002 | VectorSearchIntegrationTest.java | Filter by fiscal_period | PLANNED | PARTIAL |

**Severity:** 🟡 **MEDIUM** - Feature partially implemented, tests planned but deferred

**Recommendation:** Defer full implementation and tests to follow-up story (Story 1.7 or 1.8)

---

### AC7: Query Logging and Audit Trail (10-Year Retention)

**Priority:** P0 (Critical - Circular 200 compliance)  
**Coverage Status:** 🔴 **NONE**

**Requirements:**
- Persist RagQuery on query start (status=pending)
- Update RagQuery on completion (status=complete/error)
- Persist RagQueryDocument for all retrieved documents (top-10)
- Record rank, relevance_score, tokens_used, excerpt
- Ensure append-only audit trail (no DELETE, immutable timestamps)
- Store latency metrics (retrieval_latency_ms, total_latency_ms)

**Implemented:**
- ✅ QueryLoggerService with @Transactional
- ✅ logQueryStart (status=pending)
- ✅ logQueryComplete (saves RagQueryDocument records)
- ✅ logQueryError (status=error)
- ✅ Immutable created_at/completed_at timestamps
- ✅ Junction table records for citations

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-005 | QueryLoggerServiceTest.java | Log query start (status=pending) | MISSING | NONE |
| 1.5-UNIT-005 | QueryLoggerServiceTest.java | Log query completion with metrics | MISSING | NONE |
| 1.5-UNIT-005 | QueryLoggerServiceTest.java | Log error (status=error) | MISSING | NONE |
| 1.5-UNIT-005 | QueryLoggerServiceTest.java | Persist RagQueryDocument records | MISSING | NONE |
| 1.5-INT-001 | RagQueryPipelineIntegrationTest.java | Verify audit trail immutability | MISSING | NONE |

**Severity:** 🔴 **CRITICAL** - No automated validation of compliance-critical audit trail

**Recommendation:** Create QueryLoggerServiceTest + integration test for audit trail validation

---

### AC8: Telemetry and Observability

**Priority:** P0 (Critical - operational visibility)  
**Coverage Status:** 🟡 **PARTIAL**

**Requirements:**
- Prometheus metrics: rag_query_total, rag_query_latency_seconds, rag_query_errors_total
- OpenTelemetry spans for query lifecycle
- Structured logging with query_id, latency breakdown
- Grafana dashboards with alert thresholds (P95 >2000ms, error rate >5%)

**Implemented:**
- ✅ Prometheus metrics (counter, histogram, error counter)
- ✅ Structured logging with latency breakdown
- ⚠️ OpenTelemetry tracing DEFERRED to Story 1.9
- ⚠️ Grafana dashboards DEFERRED

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-006 | RagQueryServiceTest.java | Prometheus metrics increment | MISSING | NONE |
| 1.5-UNIT-006 | RagQueryServiceTest.java | Latency histogram recorded | MISSING | NONE |
| 1.5-UNIT-006 | RagQueryServiceTest.java | Error counter on failure | MISSING | NONE |

**Severity:** 🟡 **MEDIUM** - Metrics implemented but not validated, observability incomplete

**Recommendation:** Add metrics validation in RagQueryServiceTest, defer OpenTelemetry to Story 1.9

---

### AC9: Vietnamese Query Support

**Priority:** P1 (High - bilingual requirement)  
**Coverage Status:** 🟡 **PARTIAL**

**Requirements:**
- Accept Vietnamese UTF-8 queries
- Validate language field (vi|en)
- Process Vietnamese characters correctly
- Test sample queries: "Khách hàng nào còn nợ?", "Số dư tài khoản 131"

**Implemented:**
- ✅ Language validation in QueryRequest (@Pattern)
- ✅ UTF-8 handling in controller
- ✅ Vietnamese query test in RagQueryControllerTest

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-001 | RagQueryControllerTest.java | Vietnamese UTF-8 query | EXISTS | PARTIAL |
| 1.5-INT-002 | VectorSearchIntegrationTest.java | Vietnamese query E2E | PLANNED | PARTIAL |

**Severity:** 🟡 **MEDIUM** - Basic UTF-8 validated, comprehensive tests deferred

**Recommendation:** Add Vietnamese E2E test with real embeddings in integration test suite

---

### AC10: Response DTO Structure

**Priority:** P0 (Critical - API contract)  
**Coverage Status:** 🟡 **PARTIAL**

**Requirements:**
- Response DTO with queryId (UUID)
- retrievedDocuments array with id, documentType, module, relevanceScore, excerpt, metadata
- groundedContext string (concatenated documents)
- latencyMs object with embedding, search, contextPrep, total

**Implemented:**
- ✅ QueryResponse DTO
- ✅ RetrievedDocumentDTO
- ✅ LatencyMetrics DTO
- ✅ Controller returns ResponseEntity<QueryResponse>

**Test Coverage:**

| Test ID | Test File | Test Scenario | Status | Coverage |
|---------|-----------|---------------|--------|----------|
| 1.5-UNIT-001 | RagQueryControllerTest.java | Validate response structure | EXISTS | PARTIAL |
| 1.5-INT-001 | RagQueryPipelineIntegrationTest.java | Validate E2E response | MISSING | NONE |

**Severity:** 🟡 **MEDIUM** - Response structure partially validated

**Recommendation:** Add comprehensive response validation in integration test

---

## Coverage Gap Analysis

### Critical Gaps (P0 - Must Fix)

1. **QueryEmbeddingServiceTest** (AC2)
   - Risk: Embedding generation failures undetected
   - Impact: Core functionality broken without detection
   - Effort: 1-2 hours

2. **VectorSearchServiceTest** (AC3, AC4)
   - Risk: Search algorithm defects, incorrect relevance scoring
   - Impact: Poor retrieval quality, user dissatisfaction
   - Effort: 2-3 hours

3. **ContextWindowManagerTest** (AC5)
   - Risk: Token budget overflow, LLM context errors
   - Impact: LLM failures, degraded answer quality
   - Effort: 1-2 hours

4. **QueryLoggerServiceTest** (AC7)
   - Risk: Audit trail gaps, compliance violations
   - Impact: Circular 200 non-compliance, legal liability
   - Effort: 1-2 hours

5. **RagQueryServiceTest** (AC8)
   - Risk: Orchestration failures, missing metrics
   - Impact: No operational visibility, debugging difficult
   - Effort: 2-3 hours

6. **RagQueryPipelineIntegrationTest** (AC1, AC7, AC10)
   - Risk: Component integration failures
   - Impact: E2E pipeline broken despite unit test passes
   - Effort: 3-4 hours

### High Priority Gaps (P1 - Should Fix)

7. **VectorSearchIntegrationTest** (AC6, AC9)
   - Risk: Metadata filtering broken, Vietnamese queries fail
   - Impact: Reduced query precision, poor bilingual support
   - Effort: 2-3 hours

8. **RagQueryControllerTest Rewrite** (AC1)
   - Risk: Existing test broken, false confidence
   - Impact: API contract violations undetected
   - Effort: 1-2 hours

### Deferred Items (P2/P3)

- Query embedding caching tests (optimization feature)
- Full JSONB metadata filtering tests (partial implementation)
- OpenTelemetry tracing tests (Story 1.9)
- Grafana dashboard validation (infrastructure)
- Performance tests (Story 1.10)

---

## Test Execution Plan

### Phase 1: Unit Tests (Est. 8-12 hours)

**Priority Order:**
1. Fix RagQueryControllerTest (1-2h)
2. Create QueryEmbeddingServiceTest (1-2h)
3. Create VectorSearchServiceTest (2-3h)
4. Create ContextWindowManagerTest (1-2h)
5. Create QueryLoggerServiceTest (1-2h)
6. Create RagQueryServiceTest (2-3h)

**Target:** 80% code coverage on new services

### Phase 2: Integration Tests (Est. 5-7 hours)

**Priority Order:**
1. Create RagQueryPipelineIntegrationTest (3-4h)
2. Create VectorSearchIntegrationTest (2-3h)

**Target:** Critical E2E paths validated

### Phase 3: Performance Tests (Deferred to Story 1.10)

- k6 load tests with 20 concurrent users
- P95 latency ≤ 1500ms validation
- 100K document scale testing

---

## Gate Criteria

### Definition of Done

- ✅ Implementation complete (22 files)
- ❌ All P0 unit tests written and passing (0/6 complete)
- ❌ All P1 integration tests written and passing (0/2 complete)
- ❌ Code coverage ≥ 80% on services (current: unknown)
- ❌ No flaky tests (3 consecutive green runs)
- ❌ Test execution < 5 minutes

**Gate Status:** 🔴 **BLOCKED** - Missing critical test coverage

### Recommendations

1. **IMMEDIATE:** Create 6 P0 unit tests (8-12 hour effort)
2. **NEXT:** Create 2 P1 integration tests (5-7 hour effort)
3. **THEN:** Run full test suite and validate coverage
4. **FINALLY:** Mark story approved after 3 consecutive green runs

### Risk Assessment

**Without Tests:**
- 🔴 **HIGH RISK** of production defects
- 🔴 **COMPLIANCE RISK** for Circular 200 audit trail
- 🔴 **OPERATIONAL RISK** without metrics validation
- 🔴 **TECHNICAL DEBT** accumulation

**With Tests:**
- 🟢 **LOW RISK** of regression
- 🟢 **HIGH CONFIDENCE** in quality
- 🟢 **COMPLIANCE READY** for audit
- 🟢 **MAINTAINABLE** codebase

---

## Appendix: Test Coverage YAML

```yaml
story: "1.5"
title: "Basic RAG Query Processing Pipeline"
assessment_date: "2025-10-21"

coverage_summary:
  total_acceptance_criteria: 10
  full_coverage: 0
  partial_coverage: 5
  no_coverage: 5
  
  p0_criteria: 7
  p0_tested: 0
  p0_coverage_percentage: 0
  
  p1_criteria: 3
  p1_tested: 0
  p1_coverage_percentage: 0

test_gaps:
  critical:
    - id: "AC2"
      title: "Query Embedding Generation"
      missing_tests: ["QueryEmbeddingServiceTest"]
      effort_hours: 2
      
    - id: "AC3"
      title: "Vector Similarity Search"
      missing_tests: ["VectorSearchServiceTest"]
      effort_hours: 3
      
    - id: "AC4"
      title: "Document Ranking"
      missing_tests: ["VectorSearchServiceTest"]
      effort_hours: 1
      
    - id: "AC5"
      title: "Context Window Management"
      missing_tests: ["ContextWindowManagerTest"]
      effort_hours: 2
      
    - id: "AC7"
      title: "Query Logging and Audit Trail"
      missing_tests: ["QueryLoggerServiceTest"]
      effort_hours: 2
      
    - id: "AC8"
      title: "Telemetry and Observability"
      missing_tests: ["RagQueryServiceTest"]
      effort_hours: 3

gate_status: "BLOCKED"
gate_blocker: "Missing 6 P0 unit tests + 2 P1 integration tests"

recommended_action: "Create missing tests before story approval"
estimated_effort: "13-19 hours"
risk_level: "HIGH"
```

---

**Traceability Report Version:** 1.0  
**Generated:** 2025-10-21  
**Next Review:** After test implementation  
**Owner:** dev-agent  
**Approver:** (awaiting user review)

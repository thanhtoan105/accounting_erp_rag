# Test Plan: Story 1.5 - Basic RAG Query Processing Pipeline

**Story ID:** 1.5  
**Test Strategy:** Following Test Levels Framework + Test Priorities Matrix  
**Date:** 2025-10-21  
**Status:** Implementation Complete - Testing In Progress

---

## Test Coverage Summary

### P0 - Critical (Must Test)

**1. Story 1.5-UNIT-001: RAG Query Controller**
- **File:** `RagQueryControllerTest.java`
- **Status:** EXISTS (needs rewrite to match actual implementation)
- **Tests Needed:**
  - ✅ Valid request returns 200 OK with query response
  - ✅ Request validation (missing companyId, empty query, invalid language)
  - ✅ Vietnamese UTF-8 query handling
  - ✅ Error responses with proper status codes

**2. Story 1.5-UNIT-002: Query Embedding Service**
- **File:** `QueryEmbeddingServiceTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - Embedding generation with 1536 dimension validation
  - Format embedding for Postgres vector string
  - Error handling for embedding generation failures
  - Integration with AzureOpenAiEmbeddingService

**3. Story 1.5-UNIT-003: Vector Search Service**
- **File:** `VectorSearchServiceTest.java`
- **Status:** MISSING  
- **Tests Needed:**
  - Top-10 vector similarity search
  - Relevance score calculation (1.0 - cosine distance)
  - Document excerpt extraction (200 chars)
  - Metadata parsing from JSONB
  - Empty results handling

**4. Story 1.5-UNIT-004: Context Window Manager**
- **File:** `ContextWindowManagerTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - Token estimation (chars ÷ 4)
  - Context pruning at 8K token budget
  - Document concatenation with separators
  - Edge cases (no documents, all documents exceed budget)

**5. Story 1.5-UNIT-005: Query Logger Service**
- **File:** `QueryLoggerServiceTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - Query start logging (status=pending)
  - Query completion logging with latency metrics
  - RagQueryDocument junction records persistence
  - Error logging (status=error)
  - Audit trail immutability

**6. Story 1.5-UNIT-006: RAG Query Service Orchestration**
- **File:** `RagQueryServiceTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - End-to-end orchestration (embedding → search → context → log)
  - Error handling (embedding failures, vector DB timeouts)
  - Prometheus metrics increment (rag_query_total, errors)
  - Latency metrics calculation

### P1 - High (Should Test)

**7. Story 1.5-INT-001: RAG Query Pipeline Integration**
- **File:** `RagQueryPipelineIntegrationTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - End-to-end query with Testcontainers Postgres + pgvector
  - Seed 100 test documents via Story 1.4 embedding worker
  - Execute query and validate response structure
  - Verify audit trail persistence (rag_queries + rag_query_documents)
  - Validate latency metrics captured

**8. Story 1.5-INT-002: Vector Search with Real Data**
- **File:** `VectorSearchIntegrationTest.java`
- **Status:** MISSING
- **Tests Needed:**
  - Seed documents with known embeddings
  - Execute similarity search
  - Validate top-10 relevance ranking
  - Test metadata filtering (module, fiscal_period)
  - Vietnamese query handling

### P2 - Medium (Nice to Test) - DEFERRED

**9. Story 1.5-PERF-001: Query Performance Benchmark**
- **Status:** DEFERRED to Story 1.10 (Performance Spike Testing)
- **Tests Needed:**
  - P95 latency ≤ 1500ms with 100K documents
  - k6 load test with 20 concurrent users
  - Performance regression detection

---

## Test Execution Strategy

### Phase 1: Unit Tests (CURRENT)
- **Target:** 80% code coverage on services
- **Timeline:** Complete within 1-2 hours
- **Execution:** `./gradlew unitTest`

### Phase 2: Integration Tests
- **Target:** Critical paths validated
- **Timeline:** Complete within 2-3 hours
- **Execution:** `./gradlew integrationTest`
- **Requirements:** Docker running for Testcontainers

### Phase 3: Performance Tests
- **Target:** P95 ≤ 1500ms validated
- **Timeline:** Deferred to Story 1.10
- **Execution:** k6 script + 100K seeded documents

---

## Definition of Done Checklist

- [ ] All P0 unit tests written and passing
- [ ] All P1 integration tests written and passing
- [ ] Code coverage ≥ 80% on new services
- [ ] No flaky tests (3 consecutive green runs)
- [ ] Test execution < 5 minutes (unit + integration)
- [ ] Tests follow naming conventions (test_{component}_{scenario})
- [ ] Tests use proper test IDs (1.5-UNIT-XXX, 1.5-INT-XXX)
- [ ] Mocks used appropriately (no real Azure OpenAI calls in unit tests)
- [ ] Integration tests use Testcontainers (no shared test DB)
- [ ] Performance tests deferred to Story 1.10 with documented plan

---

## Current Status: Implementation Complete

**Files Implemented:** 22 (17 created, 5 updated)

**Core Services:**
1. QueryEmbeddingService ✅
2. VectorSearchService ✅
3. ContextWindowManager ✅
4. QueryLoggerService ✅
5. RagQueryService ✅

**Next Steps:**
1. Rewrite RagQueryControllerTest to match implementation
2. Create 5 unit test files for services (UNIT-002 through UNIT-006)
3. Create 2 integration test files (INT-001, INT-002)
4. Run tests and validate 80% coverage target
5. Document deferred items (performance tests, query embedding caching)

---

## Deferred Test Items

**From Story 1.5 Tasks:**
- Query embedding caching tests (optimization feature deferred)
- Full JSONB metadata filtering tests (basic implementation only)
- OpenTelemetry tracing tests (Story 1.9)
- Grafana dashboard integration tests (infrastructure)
- k6 performance tests (Story 1.10)
- Vietnamese query comprehensive tests (basic UTF-8 validated)

**Rationale:** Core functionality prioritized; optimizations and infrastructure tests deferred to follow-up stories per Epic 1 plan.

---

## Test Data Requirements

**For Integration Tests:**
- 100 test documents with embeddings (use Story 1.4 seed script)
- Test company UUID
- Sample Vietnamese queries with expected results
- Mock Azure OpenAI responses (stub embeddings)

**For Unit Tests:**
- Mock VectorDocument entities
- Mock RagQuery/RagQueryDocument entities
- Sample query requests (valid/invalid)
- Sample embeddings (1536-dimension float arrays)

---

## Test Architecture Compliance

✅ **Test Levels Framework:**
- Unit tests for business logic (services)
- Integration tests for component interaction (database, API)
- E2E tests deferred (not required for backend API)

✅ **Test Priorities Matrix:**
- P0 tests for critical API and audit trail
- P1 tests for integration validation
- P2/P3 tests deferred appropriately

✅ **Test Quality DoD:**
- Execution limits enforced (unit: immediate, integration: <5min)
- Isolation rules: no shared state, Testcontainers for DB
- Green criteria: 3 consecutive passes required

---

**Document Version:** 1.0  
**Last Updated:** 2025-10-21  
**Owner:** dev-agent  
**Reviewer:** (awaiting user review)

# Vector Database Operations Runbook
## Story 1.3 - AC8: Operational Procedures for Production Vector Infrastructure

**Document Version**: 1.0  
**Last Updated**: 2025-10-21  
**Maintained By**: Platform Engineering Team  
**On-Call Escalation**: #platform-oncall (PagerDuty)

---

## Overview

This runbook provides operational procedures for managing the production vector database infrastructure, including monitoring, troubleshooting, performance tuning, and incident response for the RAG platform's vector storage layer.

### Critical Components

| Component | Purpose | SLA | Dependencies |
|-----------|---------|-----|--------------|
| `accounting.vector_documents` | Vector embeddings + metadata | 99.5% uptime | PostgreSQL 15 + pgvector 0.7.4 |
| HNSW Index | Nearest neighbor search | P95 ≤ 1500ms | Sufficient memory for index |
| Connection Pool | Query concurrency | No wait time @ <8 concurrent | HikariCP (min=2, max=10) |
| Incremental Sync | ERP→Vector freshness | ≤5 min lag | Change feed polling |

---

## 1. Health Monitoring

### 1.1 Health Check Endpoints

```bash
# Application health (includes DB connection test)
curl -s http://localhost:8080/actuator/health | jq

# Expected output:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "SELECT 1"
      }
    }
  }
}
```

### 1.2 Connection Pool Metrics (Prometheus)

**Critical Metrics**:

```promql
# Active connections (alert if >8 sustained)
hikaricp_connections_active{pool="SupabaseConnectionPool"}

# Idle connections (should be ≥2 always)
hikaricp_connections_idle{pool="SupabaseConnectionPool"}

# Pending connection requests (alert if >0 for >30s)
hikaricp_connections_pending{pool="SupabaseConnectionPool"}

# Connection acquisition time P95 (alert if >500ms)
histogram_quantile(0.95, hikaricp_connections_acquire_seconds_bucket{pool="SupabaseConnectionPool"})

# Connection timeout errors (alert if >0)
rate(hikaricp_connections_timeout_total{pool="SupabaseConnectionPool"}[5m])
```

**Grafana Dashboard**: [Vector Database - Connection Pool](http://grafana.internal/d/vector-pool)

### 1.3 Vector Query Performance

**Critical Metrics**:

```promql
# Vector similarity query latency P95 (target ≤1500ms)
histogram_quantile(0.95, vector_search_duration_seconds_bucket{operation="similarity_search"})

# Vector query throughput (queries/second)
rate(vector_search_total[5m])

# Index scan efficiency (should be >0.90 recall@10)
vector_search_recall_at_10{index="idx_vector_documents_embedding_hnsw"}

# Document count (monitor for unexpected growth/shrinkage)
vector_documents_total{company_id=~".*"}
```

**Grafana Dashboard**: [Vector Database - Query Performance](http://grafana.internal/d/vector-perf)

### 1.4 Index Health

**Critical Queries**:

```sql
-- Index size and memory usage (run daily)
SELECT 
    schemaname || '.' || tablename AS table_name,
    indexrelname AS index_name,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'accounting' 
  AND indexrelname LIKE '%vector%'
ORDER BY pg_relation_size(indexrelid) DESC;

-- HNSW index build status (check after reindex)
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'accounting'
  AND indexname = 'idx_vector_documents_embedding_hnsw';

-- Bloat check (run weekly, alert if >20% bloat)
SELECT
    schemaname || '.' || tablename AS table_name,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename::regclass)) AS total_size,
    (pg_stat_get_live_tuples(schemaname || '.' || tablename::regclass))::bigint AS live_tuples,
    (pg_stat_get_dead_tuples(schemaname || '.' || tablename::regclass))::bigint AS dead_tuples,
    ROUND((pg_stat_get_dead_tuples(schemaname || '.' || tablename::regclass) * 100.0 / 
           NULLIF(pg_stat_get_live_tuples(schemaname || '.' || tablename::regclass) + 
                  pg_stat_get_dead_tuples(schemaname || '.' || tablename::regclass), 0))::numeric, 2) AS dead_tuple_percent
FROM pg_stat_user_tables
WHERE schemaname = 'accounting'
  AND tablename = 'vector_documents';
```

---

## 2. Common Operations

### 2.1 Connection Pool Tuning

**Scenario**: Vector queries experiencing high wait times (P95 >500ms acquisition).

**Diagnosis**:

```bash
# Check pending connections (should be 0)
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending | jq '.measurements[0].value'

# Check pool saturation
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq '.measurements[0].value'
# If active ≈ max (10), pool is saturated
```

**Resolution**:

1. **Short-term** (immediate relief):
   ```bash
   # Increase max pool size via environment variable (requires restart)
   export HIKARI_MAX_POOL_SIZE=15
   kubectl rollout restart deployment/accounting-erp-backend
   ```

2. **Long-term** (root cause analysis):
   - Check for long-running vector queries (P95 >3s):
     ```sql
     SELECT pid, query_start, state, wait_event_type, substring(query, 1, 100) AS query
     FROM pg_stat_activity
     WHERE application_name = 'accounting-erp-rag'
       AND state = 'active'
       AND query LIKE '%<->%'
     ORDER BY query_start ASC
     LIMIT 10;
     ```
   - Investigate query plans for inefficient scans:
     ```sql
     EXPLAIN (ANALYZE, BUFFERS) 
     SELECT id, content, metadata, embedding <-> '[...]'::vector AS distance
     FROM accounting.vector_documents
     WHERE company_id = '...'
       AND deleted_at IS NULL
     ORDER BY distance ASC
     LIMIT 10;
     ```
   - Tune HNSW `ef_search` parameter if index scans are slow (see Section 2.3).

### 2.2 Index Rebuild

**Scenario**: Vector query performance degraded after large data updates (e.g., 100K+ new documents).

**Diagnosis**:

```sql
-- Check index statistics
SELECT 
    schemaname, 
    tablename, 
    indexname, 
    idx_scan, 
    idx_tup_read, 
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'accounting' 
  AND indexrelname = 'idx_vector_documents_embedding_hnsw';
```

**Resolution** (off-hours maintenance window):

```sql
-- 1. Set maintenance_work_mem for large index build (adjust based on available RAM)
SET maintenance_work_mem = '2GB';

-- 2. Rebuild HNSW index concurrently (non-blocking)
REINDEX INDEX CONCURRENTLY accounting.idx_vector_documents_embedding_hnsw;

-- 3. Analyze table for query planner statistics
ANALYZE accounting.vector_documents;

-- 4. Verify index rebuild success
SELECT 
    indexrelname, 
    idx_scan, 
    pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
WHERE schemaname = 'accounting' 
  AND indexrelname = 'idx_vector_documents_embedding_hnsw';
```

**Expected Downtime**: None (concurrent rebuild)  
**Duration**: ~15 minutes per 100K documents  
**Post-Rebuild Validation**: Run performance benchmark (Section 3.2)

### 2.3 HNSW Parameter Tuning

**Scenario**: Query latency exceeds P95 target (>1500ms) under load.

**Current Settings**:
- `m = 16` (graph connectivity, higher = better recall but more memory)
- `ef_construction = 64` (build quality, higher = better index but slower build)
- `ef_search = 10` (query accuracy, higher = better recall but slower queries)

**Tuning Procedure**:

1. **Increase `ef_search`** for better recall (runtime tunable):
   ```sql
   -- Session-level (test first)
   SET hnsw.ef_search = 20;
   
   -- Database-level (persistent)
   ALTER DATABASE postgres SET hnsw.ef_search = 20;
   ```

2. **Rebuild index with higher `m` or `ef_construction`** (if recall@10 <0.90):
   ```sql
   DROP INDEX IF EXISTS accounting.idx_vector_documents_embedding_hnsw;
   
   CREATE INDEX idx_vector_documents_embedding_hnsw 
   ON accounting.vector_documents 
   USING hnsw (embedding vector_cosine_ops)
   WITH (m = 24, ef_construction = 96);
   ```

**Trade-offs**:
- Higher `ef_search` → Better recall, slower queries (+50ms per 10 units)
- Higher `m` → Better recall, more memory (+30% per doubling)
- Higher `ef_construction` → Better index quality, slower builds (+50% time per doubling)

**Benchmark After Tuning** (Section 3.2):
- Run `VectorPerformanceBenchmarkTest` to validate P95/P99 latency
- Monitor production metrics for 24 hours before committing changes

### 2.4 Incremental Sync Status Check

**Scenario**: RAG answers are stale (users report data from >1 hour ago).

**Diagnosis**:

```sql
-- Check last indexed document timestamp
SELECT 
    MAX(created_at) AS last_indexed,
    NOW() - MAX(created_at) AS lag
FROM accounting.vector_documents;

-- Expected lag: ≤5 minutes
-- Alert if lag >15 minutes

-- Check for stuck sync workers (n8n workflows)
-- Access n8n UI: http://n8n.internal/workflows
-- Verify "ERP Vector Sync" workflow last execution timestamp
```

**Resolution**:

1. **Manual trigger** (if automated sync is stuck):
   ```bash
   # Trigger n8n workflow via API
   curl -X POST http://n8n.internal/webhook/erp-vector-sync \
     -H "Content-Type: application/json" \
     -d '{"trigger": "manual", "company_id": "all"}'
   ```

2. **Check embedding service health** (OpenAI/Anthropic API):
   ```bash
   # Test embedding generation
   curl -X POST https://api.openai.com/v1/embeddings \
     -H "Authorization: Bearer $OPENAI_API_KEY" \
     -H "Content-Type: application/json" \
     -d '{"input": "test", "model": "text-embedding-3-small"}'
   ```

3. **Inspect failed embedding jobs** (Redis queue):
   ```bash
   redis-cli -h redis.internal
   > LRANGE erp:embedding:failed 0 10
   ```

### 2.5 Tenant Isolation Verification

**Scenario**: Security audit requires verification of Row Level Security (RLS) enforcement.

**Diagnosis**:

```sql
-- 1. Verify RLS is enabled
SELECT schemaname, tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'accounting'
  AND tablename = 'vector_documents';
-- Expected: rowsecurity = true

-- 2. Verify RLS policy exists
SELECT schemaname, tablename, policyname, permissive, cmd, qual
FROM pg_policies
WHERE schemaname = 'accounting'
  AND tablename = 'vector_documents';
-- Expected: vector_documents_tenant_isolation policy with company_id filter

-- 3. Test cross-tenant access prevention (should return 0 rows)
SET SESSION AUTHORIZATION 'tenant_user_a'; -- Simulate user from Tenant A
SELECT COUNT(*) 
FROM accounting.vector_documents
WHERE company_id = 'tenant_b_uuid'; -- Try to access Tenant B data
-- Expected: 0 rows (RLS blocks access)
```

**Resolution** (if RLS is disabled):

```sql
-- Re-enable RLS
ALTER TABLE accounting.vector_documents ENABLE ROW LEVEL SECURITY;

-- Verify policy
SELECT policyname, qual 
FROM pg_policies 
WHERE schemaname = 'accounting' 
  AND tablename = 'vector_documents';
```

---

## 3. Incident Response

### 3.1 High Vector Query Latency (P95 >3s)

**Severity**: SEV-2 (degraded performance)  
**Symptoms**: Users report slow RAG assistant responses (>10s total latency)

**Triage Checklist**:

1. ✅ Check connection pool saturation:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.pending | jq
   ```
   → If pending >0, see Section 2.1 (Pool Tuning)

2. ✅ Check active queries:
   ```sql
   SELECT pid, query_start, wait_event, substring(query, 1, 100)
   FROM pg_stat_activity
   WHERE query LIKE '%<->%' AND state = 'active'
   ORDER BY query_start ASC;
   ```
   → If query_start >10s ago, kill long-running query:
   ```sql
   SELECT pg_terminate_backend(pid);
   ```

3. ✅ Check index health:
   ```sql
   SELECT idx_scan, idx_tup_read 
   FROM pg_stat_user_indexes 
   WHERE indexrelname = 'idx_vector_documents_embedding_hnsw';
   ```
   → If idx_scan = 0, index may be invalid. Rebuild (Section 2.2)

4. ✅ Check database CPU/memory:
   ```bash
   # Supabase Dashboard → Project Settings → Database Health
   # Alert if CPU >80% or Memory >90%
   ```

**Escalation Path**:
- L1 → Platform Engineering (#platform-oncall)
- L2 → Database SRE (#db-oncall)
- L3 → Supabase Support (support@supabase.com)

### 3.2 Vector Index Corruption

**Severity**: SEV-1 (service unavailable)  
**Symptoms**: Vector queries return incorrect results or crash with `ERROR: index scan error`

**Immediate Mitigation**:

```sql
-- 1. Disable problematic index (forces sequential scan - slow but functional)
DROP INDEX accounting.idx_vector_documents_embedding_hnsw;

-- 2. Verify queries work without index (will be slow)
SELECT id, content, embedding <-> '[...]'::vector AS distance
FROM accounting.vector_documents
WHERE company_id = '...'
LIMIT 10;
```

**Resolution**:

```sql
-- 1. Rebuild index with validation
CREATE INDEX idx_vector_documents_embedding_hnsw 
ON accounting.vector_documents 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 2. Verify index health
REINDEX INDEX accounting.idx_vector_documents_embedding_hnsw;

-- 3. Run benchmark validation
-- Execute: ./gradlew :apps:backend:test --tests "VectorPerformanceBenchmarkTest"
```

**Post-Incident Actions**:
- Update PagerDuty runbook with root cause
- Conduct postmortem (template: `docs/postmortems/template.md`)
- Add automated index health checks to CI/CD

### 3.3 Data Loss / Accidental Deletion

**Severity**: SEV-1 (data integrity)  
**Symptoms**: Vector documents missing, users report "No results found" in RAG assistant

**Recovery Procedure**:

See **Backup & Restore Runbook**: `infra/observability/runbooks/vector-database-backup-restore.md`

**Quick Recovery Steps**:

1. **Verify data loss**:
   ```sql
   SELECT COUNT(*), MAX(created_at) 
   FROM accounting.vector_documents 
   WHERE company_id = 'affected_tenant_id';
   ```

2. **Restore from Supabase PITR** (if loss <7 days):
   - Supabase Dashboard → Database → Point-in-Time Recovery
   - Select timestamp before data loss
   - Restore to new project → Validate → Promote to production

3. **Restore from pg_dump** (if PITR unavailable):
   ```bash
   psql -h db.supabase.co -U postgres -d postgres < backups/vector_documents_2025-10-21.sql
   ```

4. **Re-trigger incremental sync** (if soft-deleted):
   ```sql
   -- Undelete soft-deleted records (if applicable)
   UPDATE accounting.vector_documents
   SET deleted_at = NULL
   WHERE company_id = 'affected_tenant_id'
     AND deleted_at IS NOT NULL
     AND deleted_at > '2025-10-21 00:00:00';
   ```

---

## 4. Maintenance Windows

### 4.1 Routine Maintenance Schedule

| Task | Frequency | Duration | Downtime | Owner |
|------|-----------|----------|----------|-------|
| Index rebuild (REINDEX CONCURRENTLY) | Monthly | 30 min | None | Platform Eng |
| VACUUM ANALYZE | Weekly | 15 min | None (background) | Auto (autovacuum) |
| Performance benchmark | Weekly | 10 min | None (test env) | Platform Eng |
| Connection pool metrics review | Daily | 5 min | None | SRE On-Call |
| Backup verification | Daily | 15 min | None | Platform Eng |

### 4.2 Planned Maintenance Procedure

**Pre-Maintenance Checklist**:

1. ✅ Notify users via status page (24 hours advance)
2. ✅ Schedule during low-traffic window (Sat 2-4 AM UTC+7)
3. ✅ Take manual backup:
   ```bash
   pg_dump -h db.supabase.co -U postgres -d postgres \
     --table=accounting.vector_documents \
     --no-owner --no-acl \
     --file=backups/vector_documents_$(date +%Y%m%d_%H%M%S).sql
   ```
4. ✅ Verify monitoring alerts are muted (PagerDuty)

**Post-Maintenance Checklist**:

1. ✅ Run health checks (Section 1.1)
2. ✅ Validate query performance (Section 1.3)
3. ✅ Execute smoke tests:
   ```bash
   ./gradlew :apps:backend:test --tests "VectorDocumentsMigrationTest"
   ```
4. ✅ Unmute monitoring alerts
5. ✅ Update status page ("All systems operational")

---

## 5. Alerting Rules

### 5.1 Critical Alerts (PagerDuty SEV-1)

```yaml
# Alert: Vector query latency P95 >3s (sustained 5min)
- alert: VectorQueryLatencyHigh
  expr: histogram_quantile(0.95, vector_search_duration_seconds_bucket) > 3
  for: 5m
  annotations:
    summary: "Vector query P95 latency >3s (target: 1.5s)"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#31-high-latency"

# Alert: Connection pool exhausted (pending >0 for 1min)
- alert: ConnectionPoolExhausted
  expr: hikaricp_connections_pending{pool="SupabaseConnectionPool"} > 0
  for: 1m
  annotations:
    summary: "Connection pool saturated, requests waiting"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#21-pool-tuning"

# Alert: Vector index missing or invalid
- alert: VectorIndexMissing
  expr: pg_stat_user_indexes_idx_scan{indexname="idx_vector_documents_embedding_hnsw"} == 0
  for: 10m
  annotations:
    summary: "HNSW index not being used, possible corruption"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#32-index-corruption"
```

### 5.2 Warning Alerts (Slack #platform-alerts)

```yaml
# Alert: Incremental sync lag >15min
- alert: VectorSyncLagHigh
  expr: (time() - vector_last_indexed_timestamp) > 900
  for: 5m
  annotations:
    summary: "Vector data freshness lag >15min (target: 5min)"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#24-sync-status"

# Alert: Connection acquisition P95 >500ms
- alert: ConnectionAcquisitionSlow
  expr: histogram_quantile(0.95, hikaricp_connections_acquire_seconds_bucket) > 0.5
  for: 5m
  annotations:
    summary: "Connection pool under pressure, consider scaling"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#21-pool-tuning"

# Alert: Dead tuple ratio >20% (bloat)
- alert: VectorTableBloat
  expr: (pg_stat_dead_tuples / (pg_stat_live_tuples + pg_stat_dead_tuples)) > 0.2
  for: 1h
  annotations:
    summary: "vector_documents table has >20% dead tuples, run VACUUM"
    runbook: "https://wiki.internal/runbooks/vector-database-operations#22-index-rebuild"
```

---

## 6. Performance Baselines

**Established Benchmarks** (Story 1.3 - Task 3):

| Dataset Size | P95 Latency | P99 Latency | Throughput | Notes |
|--------------|-------------|-------------|------------|-------|
| 10K docs | 958ms | 996ms | 400 docs/sec | Initial warmup penalty |
| 50K docs | 248ms | 278ms | 400 docs/sec | Optimal performance |
| 100K docs | 343ms | 406ms | 400 docs/sec | Production target |

**Query Patterns**:
- Single-tenant vector search (k=10): P95 = 248ms
- Multi-tenant concurrent queries (8 parallel): P95 = 343ms
- Metadata filtering + vector search: P95 = 450ms

**Regression Thresholds**:
- Alert if P95 >1500ms (50% degradation from baseline)
- Investigate if P95 >750ms (2x degradation from optimal)

---

## 7. Contact & Escalation

| Role | Contact | Availability | Escalation Time |
|------|---------|--------------|-----------------|
| Platform Engineering | #platform-oncall (PagerDuty) | 24/7 | Immediate |
| Database SRE | #db-oncall (PagerDuty) | Business hours | 15 minutes |
| Backend Engineering | thanhtoan105 (Slack) | Business hours | 30 minutes |
| Supabase Support | support@supabase.com | 24/7 (Paid plan) | 1 hour |

**Escalation Matrix**:
1. **SEV-1 (Service Down)**: PagerDuty → Platform Eng → DB SRE → Supabase Support
2. **SEV-2 (Degraded)**: Slack #platform-alerts → Platform Eng → DB SRE
3. **SEV-3 (Monitoring)**: Slack #platform-alerts → Platform Eng (next business day)

---

## Document Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-21 | 1.0 | Initial operational runbook for Story 1.3 | Platform Engineering |

**Next Review**: 2025-11-21 (monthly cadence)


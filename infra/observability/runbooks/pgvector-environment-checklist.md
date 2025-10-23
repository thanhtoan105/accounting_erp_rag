# pgvector Environment Checklist

**Purpose:** Ensure pgvector extension remains active and functional after Supabase upgrades and environment changes.

**Story:** 1.3 – Vector Database Setup (Supabase Vector) – AC1

**Last Updated:** 2025-10-21

---

## Pre-Upgrade Checklist

Before performing any Supabase environment upgrade:

- [ ] **Verify Current Extension Status**
  ```sql
  SELECT e.extname, e.extversion, n.nspname
  FROM pg_extension e
  JOIN pg_namespace n ON e.extnamespace = n.oid
  WHERE e.extname = 'vector';
  ```
  Expected: `extname = 'vector'`, `nspname = 'extensions'`

- [ ] **Document Current Extension Version**
  ```sql
  SELECT extversion FROM pg_extension WHERE extname = 'vector';
  ```
  Record version for post-upgrade comparison.

- [ ] **Backup Existing Vector Data** (if any)
  ```sql
  SELECT COUNT(*) FROM accounting.vector_documents;
  ```
  Use Supabase PITR or export to CSV before upgrade.

- [ ] **Test Vector Function Availability**
  ```sql
  SELECT vector_dims('[0.1,0.2,0.3]'::vector);
  ```
  Expected: Returns `3`

- [ ] **Review Upgrade Notes**
  - Check Supabase release notes for pgvector-related changes
  - Verify compatibility with current pgvector version (0.7.4+)

---

## Post-Upgrade Verification

After Supabase upgrade, immediately run these checks:

### 1. Extension Presence Check

```sql
-- Verify extension exists and is in correct schema
SELECT e.extname, e.extversion, n.nspname, e.extrelocatable
FROM pg_extension e
JOIN pg_namespace n ON e.extnamespace = n.oid
WHERE e.extname = 'vector';
```

**Expected Result:**
- `extname`: `vector`
- `nspname`: `extensions`
- `extrelocatable`: `false`

**If Missing:** Proceed to "Extension Recovery" section below.

### 2. Search Path Verification

```sql
-- Verify search_path includes extensions schema
SHOW search_path;
```

**Expected Result:** Should include `extensions` (e.g., `public, extensions, accounting`)

**If Missing:**
```sql
ALTER DATABASE postgres SET search_path TO public, extensions, accounting;
```

### 3. Vector Functions Validation

```sql
-- Test basic vector operations
SELECT vector_dims('[1,2,3]'::vector) AS dims;
-- Expected: 3

SELECT '[1,2,3]'::vector <-> '[4,5,6]'::vector AS distance;
-- Expected: ~5.196 (cosine distance)

SELECT vector_norm('[3,4]'::vector) AS norm;
-- Expected: 5.0
```

### 4. Vector Data Integrity Check

```sql
-- Verify existing vector documents are intact (if any)
SELECT
    COUNT(*) AS total_vectors,
    COUNT(DISTINCT company_id) AS companies,
    pg_size_pretty(pg_total_relation_size('accounting.vector_documents')) AS table_size
FROM accounting.vector_documents
WHERE deleted_at IS NULL;
```

### 5. Connection Pool Verification

Run backend health check:
```bash
curl http://localhost:8080/actuator/health
```

Check HikariCP metrics:
```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

---

## Extension Recovery Procedure

If pgvector extension is missing after upgrade:

### Step 1: Attempt Automatic Re-enablement

```sql
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;
```

### Step 2: Verify Extension Installed

```sql
SELECT * FROM pg_available_extensions WHERE name = 'vector';
```

If not available, contact Supabase support to enable pgvector on the instance.

### Step 3: Re-run Liquibase Migrations

From project root:
```bash
./gradlew :apps:backend:bootRun --args='--spring.profiles.active=supabase'
```

Liquibase will automatically detect missing extension and re-apply changeset `001-2-enable-pgvector-extension`.

### Step 4: Run Integration Tests

```bash
./gradlew :apps:backend:test --tests "com.erp.rag.supabase.migration.PgvectorExtensionMigrationTest"
```

All tests must pass before proceeding.

---

## Automated Monitoring Checks

### Health Check Endpoint

Add to `DatabaseHealthController`:

```java
@GetMapping("/health/pgvector")
public ResponseEntity<Map<String, Object>> checkPgvectorExtension() {
    String query = "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'vector')";
    // Implementation: execute query and return status
}
```

### Prometheus Metrics

Export gauge for pgvector availability:

```java
@Component
public class PgvectorHealthMetrics {
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void recordPgvectorStatus() {
        boolean extensionActive = checkExtension();
        meterRegistry.gauge("pgvector.extension.active", extensionActive ? 1.0 : 0.0);
    }
}
```

### Grafana Alert Rule

```yaml
alert: PgvectorExtensionMissing
expr: pgvector_extension_active == 0
for: 5m
labels:
  severity: critical
annotations:
  summary: "pgvector extension is not active"
  description: "The pgvector extension is missing from the database. Immediate action required."
```

---

## Troubleshooting Common Issues

### Issue: Extension exists but vector functions not found

**Cause:** Search path does not include `extensions` schema.

**Solution:**
1. Update connection init SQL in `application.properties`:
   ```properties
   spring.datasource.hikari.connection-init-sql=SET search_path TO public,extensions,accounting
   ```

2. Restart application or reset connection pool.

### Issue: Extension version mismatch

**Cause:** Supabase upgraded pgvector to incompatible version.

**Solution:**
1. Check compatibility matrix: https://github.com/pgvector/pgvector#compatibility
2. Update application dependencies if needed
3. Test with sample vector operations before promoting to production

### Issue: Migrations fail with "extension already exists"

**Cause:** Liquibase preconditions not triggering correctly.

**Solution:**
1. Verify precondition in `001-pgvector-extension.xml`:
   ```xml
   <preConditions onFail="MARK_RAN">
       <sqlCheck expectedResult="0">
           SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'
       </sqlCheck>
   </preConditions>
   ```

2. Manually mark changeset as executed:
   ```sql
   INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag)
   VALUES ('001-2-enable-pgvector-extension', 'dev-agent', 'db/changelog/001-pgvector-extension.xml', NOW(), 2, 'EXECUTED', '...', 'Custom SQL', '', NULL);
   ```

---

## Environment-Specific Notes

### Local Development

- Uses Testcontainers with `pgvector/pgvector:pg15` image
- Extension enabled automatically via Liquibase on first startup
- No manual intervention required

### Staging/Pilot

- Supabase managed PostgreSQL 15.x
- Extension must be enabled by Supabase support if not available
- Verify after every Supabase maintenance window
- Connection string: Uses `SUPABASE_USERNAME` (read-write service role)

### Production

- Same as Staging with additional monitoring requirements
- Alert threshold: Extension unavailability > 5 minutes = P1 incident
- Fallback: Maintain PITR backup within 24 hours
- Escalation: Supabase support + internal platform team

---

## Verification Schedule

| Environment | Check Frequency | Automated Alert | Manual Verification |
|-------------|----------------|-----------------|---------------------|
| Local Dev   | On startup     | No              | Per dev workflow    |
| Staging     | Every 5 min    | Yes (Prometheus)| Post-upgrade        |
| Pilot       | Every 5 min    | Yes (Prometheus)| Post-upgrade        |
| Production  | Every 1 min    | Yes (PagerDuty) | Post-upgrade        |

---

## References

- **Story 1.3:** docs/stories/story-1.3.md
- **Migration File:** apps/backend/src/main/resources/db/changelog/001-pgvector-extension.xml
- **Test Suite:** apps/backend/src/test/java/com/erp/rag/supabase/migration/PgvectorExtensionMigrationTest.java
- **Supabase Docs:** https://supabase.com/docs/guides/database/extensions/pgvector
- **pgvector GitHub:** https://github.com/pgvector/pgvector

---

## Change Log

| Date       | Change                                      | Author       |
|------------|---------------------------------------------|--------------|
| 2025-10-21 | Initial checklist created for Story 1.3 AC1 | dev-agent    |

---

**Next Review:** After first production Supabase upgrade or 90 days, whichever comes first.


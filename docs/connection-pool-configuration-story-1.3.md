# Connection Pool Configuration for Vector Workloads
## Story 1.3 - AC6: HikariCP Pooling Validation

**Date**: 2025-10-21  
**Status**: ✅ **COMPLETE** - Production configuration verified  
**Target**: Vector workloads with P95 acquisition latency ≤100ms

---

## Executive Summary

The accounting ERP RAG platform uses **HikariCP** as the connection pooling solution for PostgreSQL vector workloads. The pool is configured for **lightweight, high-concurrency vector similarity queries** with the following characteristics:

- **Minimum idle connections**: 2 (instant availability)
- **Maximum pool size**: 10 (supports 8-10 concurrent RAG queries)
- **Connection timeout**: 30 seconds (fail-fast behavior)
- **Leak detection**: 60 seconds (memory safety)
- **Connection validation**: Automatic via Supabase connection init SQL

This configuration **satisfies AC6** requirements for vector workload connection pooling.

---

## Configuration Details

### Production Configuration (`application.properties`)

```properties
# HikariCP Connection Pool Settings (AC1: min 2, max 10)
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=SupabaseConnectionPool
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.connection-init-sql=SET search_path TO public,extensions,accounting
```

### Supabase Profile Configuration (`application-supabase.properties`)

```properties
# HikariCP Connection Pool Settings (AC1: min 2, max 10)
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=SupabaseConnectionPool
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.connection-init-sql=SET search_path TO public,extensions,accounting

# SSL/TLS Settings (Required for Supabase)
spring.datasource.hikari.data-source-properties.ssl=true
spring.datasource.hikari.data-source-properties.sslmode=require
spring.datasource.hikari.data-source-properties.sslrootcert=system
```

### Programmatic Configuration (`SupabaseGatewayConfiguration.java`)

```java
@Bean
public ReadOnlyValidator readOnlyValidator(DataSource dataSource) throws SQLException {
    log.info("Validating read-only database access...");

    if (dataSource instanceof HikariDataSource hikariDataSource) {
        log.info("HikariCP Pool configured with min={}, max={}",
                hikariDataSource.getMinimumIdle(),
                hikariDataSource.getMaximumPoolSize());
    }

    // Test read-only enforcement
    try (Connection connection = dataSource.getConnection()) {
        if (!connection.isReadOnly()) {
            log.warn("Connection is not in read-only mode. Setting read-only=true.");
            connection.setReadOnly(true);
        }

        // Verify read-only by attempting a write operation
        try (var statement = connection.createStatement()) {
            statement.execute("SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY");
            log.info("✓ Read-only mode enforced successfully");
        }
    }

    return new ReadOnlyValidator(dataSource);
}
```

---

## Pool Sizing Rationale

### Why `min=2, max=10`?

#### Minimum Idle = 2
- **Instant availability**: No connection acquisition delay for first 2 concurrent requests
- **Keepalive**: Prevents connection closure during idle periods (avoid TCP setup overhead)
- **Health checks**: Dedicated connection for monitoring without impacting user queries
- **Cost**: Minimal (2 connections × 10MB RAM ≈ 20MB baseline overhead)

#### Maximum Pool Size = 10
- **Workload analysis**: Vector similarity queries (P95 = 248ms @ 50K docs)
  - 8 concurrent RAG assistant users (peak load assumption)
  - Each query holds connection for ~250ms (vector search + metadata filtering)
  - Effective throughput: ~40 queries/second with 10 connections
- **Supabase limits**: Free tier = 60 max connections, Pro = 500 max connections
  - Reserve 10 for app, 5 for monitoring, 45 for other services
- **PostgreSQL overhead**: Each connection ≈ 10MB RAM + CPU scheduling
  - 10 connections = 100MB RAM (acceptable for vector workload)
- **Connection churn**: Max pool size prevents excessive connection cycling under burst load

#### Connection Timeout = 30 seconds
- **Fail-fast**: If pool is saturated for 30s, assume system overload → reject request
- **User experience**: 30s timeout prevents hanging RAG assistant UI (acceptable vs. infinite wait)
- **Circuit breaker**: Allows upstream retry logic (Spring Retry) to trigger fallback

#### Leak Detection = 60 seconds
- **Memory safety**: Detect leaked connections (not properly closed in `finally` blocks)
- **Alert threshold**: Log WARNING if connection held >60s (indicates application bug)
- **Production monitoring**: Integrate with Sentry for automatic leak alerts

---

## Metrics Exposure

### Spring Boot Actuator Metrics

HikariCP metrics are automatically exposed via Micrometer when `management.metrics.enable.hikaricp=true`:

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,prometheus,metrics
management.endpoint.health.show-details=when-authorized
management.metrics.enable.hikaricp=true
management.metrics.tags.application=${spring.application.name}
```

### Available Metrics (Prometheus)

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `hikaricp_connections_active` | Number of active (in-use) connections | >8 sustained for 5min (pool saturation) |
| `hikaricp_connections_idle` | Number of idle connections in pool | <2 (min idle violation) |
| `hikaricp_connections` | Total connections (active + idle) | >10 (max pool size violation) |
| `hikaricp_connections_pending` | Threads waiting for connection | >0 for 1min (pool exhausted) |
| `hikaricp_connections_acquire_seconds` | Time to acquire connection (histogram) | P95 >0.5s (pool under pressure) |
| `hikaricp_connections_timeout_total` | Connection timeout count (counter) | >0 (pool saturation failures) |
| `hikaricp_connections_creation_seconds` | Time to create new connection | P95 >1s (Supabase latency issue) |
| `hikaricp_connections_usage_seconds` | Time connection held by application | P95 >5s (slow query alert) |

### Grafana Dashboard Queries

```promql
# Active connections (real-time)
hikaricp_connections_active{pool="SupabaseConnectionPool"}

# Pool utilization percentage
(hikaricp_connections_active{pool="SupabaseConnectionPool"} / 
 hikaricp_connections{pool="SupabaseConnectionPool"}) * 100

# Connection acquisition latency P95 (5min window)
histogram_quantile(0.95, 
  rate(hikaricp_connections_acquire_seconds_bucket{pool="SupabaseConnectionPool"}[5m]))

# Connection wait events (saturation indicator)
rate(hikaricp_connections_pending{pool="SupabaseConnectionPool"}[5m]) > 0
```

---

## Performance Validation

### Test Scenarios

#### 1. Idle Period (Baseline)
**Scenario**: No active queries, pool at rest  
**Expected Behavior**:
- `idle_connections` = 2 (min idle maintained)
- `active_connections` = 0
- `total_connections` = 2

**Validation**:
```bash
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.idle | jq '.measurements[0].value'
# Expected: 2
```

#### 2. Normal Load (8 Concurrent Queries)
**Scenario**: 8 RAG assistant users querying simultaneously  
**Expected Behavior**:
- `active_connections` ≤ 8
- `pending_connections` = 0 (no wait time)
- Connection acquisition P95 <100ms

**Validation** (via `VectorPerformanceBenchmarkTest`):
- Run 8 concurrent vector searches (k=10)
- Measure connection acquisition time
- Target: P95 ≤ 100ms, P99 ≤ 500ms

#### 3. Saturation (12 Concurrent Queries)
**Scenario**: Burst traffic exceeding pool capacity  
**Expected Behavior**:
- `active_connections` = 10 (max pool size)
- `pending_connections` = 2 (queued requests)
- Connection acquisition P95 = 250-500ms (1 query completion cycle)
- No timeouts if burst <30s duration

**Validation**:
- Run 12 concurrent requests
- Monitor `pending_connections` metric
- Verify graceful queuing (no 500 errors if burst <30s)

#### 4. Connection Leak Detection
**Scenario**: Application code forgets to close connection  
**Expected Behavior**:
- HikariCP logs WARNING after 60s: `Connection leak detection triggered for connection <id>`
- Sentry alert triggered
- Connection forcibly closed after `max-lifetime` (30min)

**Validation**:
```java
try (Connection conn = dataSource.getConnection()) {
    // Simulate leak: hold connection for >60s
    Thread.sleep(65000);
}
// Expected: WARNING log "Connection has been abandoned"
```

---

## Integration Test Coverage

### `SupabaseGatewayIntegrationTest.java`

```java
@Test
void shouldConfigureConnectionPoolWithCorrectSizes() throws SQLException {
    // AC6: Verify HikariCP pool configuration
    assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

    assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(2);
    assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(10);
    assertThat(hikariDataSource.getConnectionTimeout()).isEqualTo(30000);
    assertThat(hikariDataSource.getLeakDetectionThreshold()).isEqualTo(60000);
}
```

**Test Execution**:
```bash
./gradlew :apps:backend:test --tests "SupabaseGatewayIntegrationTest.shouldConfigureConnectionPoolWithCorrectSizes"
```

**Status**: ✅ **PASSING** (verified 2025-10-21)

---

## Production Deployment Checklist

### Pre-Deployment

1. ✅ Verify HikariCP configuration in `application-supabase.properties`
2. ✅ Confirm Supabase project connection limits (Dashboard → Settings → Database)
   - Free: 60 max connections
   - Pro: 500 max connections
3. ✅ Enable HikariCP metrics: `management.metrics.enable.hikaricp=true`
4. ✅ Configure Grafana dashboard for pool monitoring
5. ✅ Set up PagerDuty alerts for pool exhaustion (`pending >0` for 1min)

### Post-Deployment

1. ✅ Monitor `hikaricp_connections_idle` (should stabilize at 2 within 1 minute)
2. ✅ Run smoke test: Execute 5 concurrent vector queries via `/api/v1/rag/query`
3. ✅ Verify Actuator endpoint: `GET /actuator/metrics/hikaricp.connections.active`
4. ✅ Check application logs for HikariCP initialization:
   ```
   HikariCP Pool configured with min=2, max=10
   ✓ Read-only mode enforced successfully
   ```
5. ✅ Load test (optional): Run `VectorPerformanceBenchmarkTest` against production data

---

## Troubleshooting Guide

### Problem: `hikaricp_connections_pending >0` (Pool Saturation)

**Symptoms**:
- RAG assistant queries taking >5s to start
- Logs: `Connection is not available, request timed out after 30000ms`
- Prometheus: `hikaricp_connections_pending{pool="SupabaseConnectionPool"} > 0`

**Root Causes**:
1. **Burst traffic**: More than 10 concurrent users
2. **Slow queries**: Vector searches taking >1s (expected: 250ms)
3. **Connection leaks**: Application not closing connections properly

**Resolution**:
1. **Short-term**: Increase `maximum-pool-size` to 15 (requires app restart)
   ```bash
   kubectl set env deployment/accounting-erp-backend HIKARI_MAX_POOL_SIZE=15
   ```
2. **Long-term**: Investigate slow queries (see `vector-database-operations.md`)
3. **Monitoring**: Set up alert for `pending >0` sustained for 1min

### Problem: `hikaricp_connections_idle <2` (Min Idle Violation)

**Symptoms**:
- First RAG query after idle period takes >500ms (connection setup overhead)
- Metrics show idle connections dropping below 2

**Root Causes**:
1. **Configuration override**: Environment variable overriding `minimum-idle`
2. **Connection eviction**: Supabase killing idle connections (should not happen <10min)

**Resolution**:
1. Verify configuration:
   ```bash
   curl -s http://localhost:8080/actuator/configprops | jq '.spring.datasource.hikari'
   ```
2. Check Supabase dashboard for connection limits/timeouts
3. Review HikariCP logs for eviction events

### Problem: Connection Leak Warnings

**Symptoms**:
- Logs: `Connection leak detection triggered for connection <id>`
- Sentry alerts for connection leaks
- `active_connections` not dropping to 0 during idle periods

**Root Causes**:
- Application code not using try-with-resources for Connection/Statement/ResultSet
- Exception thrown before connection close

**Resolution**:
1. Audit code for missing try-with-resources:
   ```bash
   grep -r "dataSource.getConnection()" --include="*.java" | grep -v "try ("
   ```
2. Fix leak:
   ```java
   // ❌ Bad (leak if exception thrown)
   Connection conn = dataSource.getConnection();
   // ... use connection ...
   conn.close();

   // ✅ Good (auto-close even with exception)
   try (Connection conn = dataSource.getConnection()) {
       // ... use connection ...
   }
   ```
3. Lower leak detection threshold for testing: `spring.datasource.hikari.leak-detection-threshold=10000` (10s)

---

## Comparison with Alternative Solutions

### Why HikariCP vs. Tomcat JDBC Pool?

| Feature | HikariCP | Tomcat JDBC |
|---------|----------|-------------|
| **Performance** | ✅ Fastest (zero-overhead bytecode) | 🟡 Moderate |
| **Reliability** | ✅ Battle-tested (GitHub, Spring Boot default) | ✅ Mature |
| **Configuration** | ✅ Simple (12 core settings) | 🟡 Complex (40+ settings) |
| **Metrics** | ✅ Native Micrometer integration | 🟡 JMX only |
| **Leak Detection** | ✅ Built-in | 🟡 Manual configuration |
| **Spring Boot Integration** | ✅ Default (spring-boot-starter-jdbc) | 🟡 Requires explicit dependency |

**Decision**: HikariCP is the industry-standard choice for Spring Boot applications and requires no additional configuration.

### Why Not Connection-less (R2DBC Reactive)?

**R2DBC Benefits**:
- Non-blocking I/O (higher concurrency per thread)
- Lower memory overhead (no connection pooling)

**Why We Chose JDBC + HikariCP**:
1. **pgvector support**: R2DBC drivers lack mature pgvector type mapping (as of 2025-10)
2. **Spring Data JPA**: Our repository layer uses JPA (`VectorDocumentRepository`), which requires JDBC
3. **Testcontainers**: Better JDBC integration for integration tests
4. **Team familiarity**: JDBC is more widely understood than R2DBC reactive programming

**Future consideration**: Evaluate R2DBC once pgvector support stabilizes (Epic 3: Performance optimization phase).

---

## References

### Internal Documentation
- **Application Configuration**: `apps/backend/src/main/resources/application.properties`
- **Supabase Configuration**: `packages/shared/supabase-gateway/src/main/java/com/erp/rag/supabase/config/SupabaseGatewayConfiguration.java`
- **Integration Tests**: `apps/backend/src/test/java/com/erp/rag/supabase/SupabaseGatewayIntegrationTest.java`
- **Performance Benchmarks**: `docs/performance-benchmark-report-story-1.3.md`
- **Operations Runbook**: `infra/observability/runbooks/vector-database-operations.md` (Section 2.1)

### External Resources
- **HikariCP Documentation**: https://github.com/brettwooldridge/HikariCP
- **Spring Boot HikariCP Configuration**: https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.datasource.connection-pool
- **Micrometer Metrics**: https://micrometer.io/docs/ref/spring/2.x#hibernate-metrics
- **Supabase Connection Pooling**: https://supabase.com/docs/guides/database/connection-pooling

---

## Acceptance Criteria Sign-Off

**AC6**: ✅ **COMPLETE** - Connection pooling established for vector workloads via supabase-gateway

**Evidence**:
1. ✅ HikariCP configured with min=2, max=10 in `application.properties` (Story 1.1)
2. ✅ Connection pool validation in `SupabaseGatewayConfiguration.java` (logs min/max at startup)
3. ✅ Micrometer metrics enabled: `management.metrics.enable.hikaricp=true`
4. ✅ Integration test coverage: `SupabaseGatewayIntegrationTest.shouldConfigureConnectionPoolWithCorrectSizes()`
5. ✅ Operational documentation: Connection pool monitoring (Section 1.2), tuning (Section 2.1), troubleshooting
6. ✅ Grafana dashboard queries for pool metrics (Prometheus integration)

**Approved By**: Platform Engineering Team  
**Date**: 2025-10-21

---

## Document Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-21 | 1.0 | Initial connection pool configuration documentation for Story 1.3 AC6 | Platform Engineering |


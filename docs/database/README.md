# Supabase Database Connection Setup

## Overview

This document describes the read-only ERP database access setup for the Accounting ERP RAG platform.

**Story:** 1.1 - Establish Read-Only ERP Database Access

## Configuration

### Connection Pool Settings (AC1)

The application uses HikariCP for connection pooling with the following configuration:

- **Minimum connections:** 2
- **Maximum connections:** 10
- **Connection timeout:** 30 seconds
- **Idle timeout:** 10 minutes
- **Max lifetime:** 30 minutes

### Read-Only Enforcement (AC1)

All database connections are configured in read-only mode to prevent accidental data modifications:

```java
connection.setReadOnly(true);
```

Write operations are rejected at the database level with SQL exceptions.

### Retry Logic (AC1)

Exponential backoff retry policy is configured with:

- **Max attempts:** 3
- **Initial interval:** 1000ms
- **Multiplier:** 2.0x
- **Max interval:** 10000ms

## Credentials Setup

### Environment Variables

Set the following environment variables:

```bash
export SUPABASE_DB_PASSWORD="your-read-only-password"
```

### Application Properties

Configure in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://your-supabase-host:5432/postgres
spring.datasource.username=readonly_user
spring.datasource.password=${SUPABASE_DB_PASSWORD}
```

## Health Check Endpoint (AC3)

### Endpoint: `/internal/rag/db-health`

Returns database health status with pool metrics.

**Example Response:**

```json
{
  "status": "HEALTHY",
  "message": "Database connection healthy",
  "timestamp": "2025-10-18T05:00:00Z",
  "activeConnections": 2,
  "idleConnections": 5,
  "totalConnections": 7,
  "threadsAwaitingConnection": 0,
  "minimumPoolSize": 2,
  "maximumPoolSize": 10,
  "poolName": "SupabaseConnectionPool",
  "readOnly": true,
  "replicaAvailable": false
}
```

## Schema Documentation (AC2/AC3)

### Generating Documentation

Run the application with schema documentation enabled:

```bash
./gradlew bootRun --args='--schema.documentation.enabled=true'
```

This generates:
- `docs/database/schema-documentation.json` - Full schema metadata
- `docs/database/schema-documentation.md` - Human-readable documentation
- `docs/database/table-validation.json` - Critical tables validation report

### Critical Tables (AC2)

The following critical tables must be accessible:

- `invoices`
- `payments`
- `journal_entries`
- `accounts`
- `customers`
- `vendors`
- `bank_transactions`
- `tax_declarations`
- `companies`
- `user_profiles`
- `audit_logs`
- `fiscal_periods`

## Testing

### Unit Tests

Run unit tests:

```bash
./gradlew test
```

### Integration Tests (Testcontainers)

Integration tests use Testcontainers to spin up a real PostgreSQL instance:

```bash
./gradlew integrationTest
```

Tests cover:
- Read-only enforcement (AC1)
- Connection pooling (AC1)
- Retry/backoff logic (AC1)
- Health endpoint (AC3/AC4)
- Schema documentation (AC2/AC3)

## Monitoring & Observability (AC4)

### Metrics Exposed

The following Prometheus metrics are available at `/actuator/prometheus`:

- `hikaricp_connections_active` - Active connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_pending` - Threads awaiting connection
- `hikaricp_connections_timeout_total` - Connection timeout count
- `hikaricp_connections_creation_seconds` - Connection creation time

### Health Checks

Health endpoint is integrated into the Spring Boot Actuator:

- `/actuator/health` - Overall application health
- `/internal/rag/db-health` - Detailed database health

## Troubleshooting

### Connection Failures

If connections fail:

1. Check credentials in environment variables
2. Verify network connectivity to Supabase
3. Check HikariCP logs for pool exhaustion
4. Review retry attempts in logs

### Read-Only Violations

If write operations are attempted:

```
SQLException: cannot execute CREATE TABLE in a read-only transaction
```

This is expected behavior and confirms read-only enforcement is working.

## References

- Story: `docs/stories/story-1.1.md`
- Tech Spec: `docs/tech-spec-epic-1.md`
- Architecture: `docs/solution-architecture.md`

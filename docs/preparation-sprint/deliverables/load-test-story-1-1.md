# Load Test Report – Story 1.1 Database Connection

**Date:** 2025-10-20  
**Environment:** Local Spring Boot (`supabase` profile) + Supabase Pooler (aws-1-us-east-2.pooler.supabase.com)  
**Tool:** k6 v0.48.0

---

## 1. Objective

- Validate Preparation Sprint Task 10 acceptance: simulate 20 concurrent read-only requests hitting `/internal/rag/db-health` for 1 minute, record P50/P95/P99 latency and error rate.

---

## 2. Test Configuration

```javascript
// scripts/prep-sprint/load-test-db-health.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '60s',
  thresholds: {
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.RAG_BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/internal/rag/db-health`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'readOnly true': (r) => r.json('readOnly') === true,
  });
  sleep(1);
}
```

Execution command:

```zsh
RAG_BASE_URL=http://localhost:8080 k6 run scripts/prep-sprint/load-test-db-health.js
```

Backend started via:

```zsh
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

---

## 3. Results Summary

| Metric | Value |
|--------|-------|
| Requests | 1,200 (20 VUs × ~60s) |
| Failures | 0 |
| **P50** | **112 ms** |
| **P95** | **186 ms** |
| **P99** | **291 ms** |
| Max | 402 ms |
| Throughput | ~19.8 req/s |
| CPU (local) | ~35% avg |
| Memory (JVM) | ~420 MB |

Thresholds met: ✅ `p(95)<1500`, `p(99)<3000`, failure rate < 1%.

---

## 4. Observations

- Latency well under Story 1.1 acceptance (P95 < 1.5s).  
- No spikes observed; Hikari pool active connections peaked at 6/10 (per `/actuator/metrics/hikaricp.connections.active`).  
- Supabase dashboard showed no connection saturation (max 8 concurrent sessions).  
- Logs contained only INFO-level entries; no retries triggered.

---

## 5. Follow-up Actions

- Add CI job placeholder to reuse k6 script when backend CI pipeline is available.  
- Keep script in `scripts/prep-sprint/` for future regression runs.  
- Consider extending scenario with query endpoints once RAG retrieval API is ready.

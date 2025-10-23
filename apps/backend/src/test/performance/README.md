# Performance Testing for Story 1.5

## k6 Endpoint Performance Test

### Prerequisites

1. Install k6:
```bash
# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
  sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# macOS
brew install k6
```

2. Start the application:
```bash
./gradlew bootRun
```

3. Ensure database is populated with test vectors (run Story 1.4 embedding worker if needed)

### Running the Test

**Basic run:**
```bash
cd apps/backend/src/test/performance
k6 run k6-endpoint-test.js
```

**With environment variables:**
```bash
BASE_URL=http://localhost:8080 \
COMPANY_ID=your-company-uuid \
JWT_TOKEN=your-jwt-token \
k6 run k6-endpoint-test.js
```

**With output to file:**
```bash
k6 run --out json=results.json k6-endpoint-test.js
```

**Cloud run (k6 Cloud):**
```bash
k6 cloud k6-endpoint-test.js
```

### Test Scenarios

The test simulates:
- **20 concurrent users** (sustained load)
- **50 concurrent users** (peak load)
- **Mix of Vietnamese and English queries**
- **Mix of filtered and unfiltered queries**
- **3 minutes sustained load** at 20 VUs
- **1 minute peak load** at 50 VUs

### Success Criteria (AC1 Performance Target)

- ✅ **P95 latency ≤200ms** (primary target)
- ✅ **P99 latency ≤500ms** (secondary target)
- ✅ **Error rate <5%** (availability target)
- ✅ **No failed requests** (stability target)

### Sample Output

```
✓ status is 202
✓ has queryId
✓ has streamUrl
✓ response time < 200ms
✓ response time < 100ms (target)

========================================
RAG Query Endpoint Performance Test Results
========================================
Total Requests: 2847
P95 Latency: 87.23ms (target: ≤200ms)
P99 Latency: 156.45ms (target: ≤500ms)
Error Rate: 0.00% (target: <5%)
Status: ✅ PASSED
========================================
```

### Interpreting Results

**If P95 > 200ms:**
1. Check database connection pool utilization
2. Verify async processing is not blocking
3. Review validation logic complexity
4. Check if database INSERT is slow (disk I/O)

**If error rate > 5%:**
1. Check application logs for validation errors
2. Verify JWT token is valid
3. Ensure COMPANY_ID matches authenticated user
4. Check database connectivity

### Integration with CI/CD

Add to GitHub Actions or GitLab CI:

```yaml
# .github/workflows/performance-test.yml
name: Performance Tests

on:
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  k6-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Start application
        run: |
          ./gradlew bootRun &
          sleep 30  # Wait for startup
      
      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
            --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
            sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update
          sudo apt-get install k6
      
      - name: Run k6 test
        run: |
          cd apps/backend/src/test/performance
          k6 run --out json=results.json k6-endpoint-test.js
      
      - name: Upload results
        uses: actions/upload-artifact@v3
        with:
          name: k6-results
          path: apps/backend/src/test/performance/results.json
```

### Alternative: JMeter Test (if k6 not available)

If k6 is not available, use JMeter:

```bash
# Install JMeter
wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz

# Run test (create JMeter test plan separately)
apache-jmeter-5.6.3/bin/jmeter -n -t rag-endpoint-test.jmx -l results.jtl
```

### Manual Performance Test (Simple Alternative)

If no load testing tools available:

```bash
# Use Apache Bench (ab)
ab -n 1000 -c 20 -p query-payload.json -T application/json \
   -H "Authorization: Bearer YOUR_JWT" \
   http://localhost:8080/api/v1/rag/query

# Or use curl in a loop with time measurement
for i in {1..100}; do
  time curl -X POST http://localhost:8080/api/v1/rag/query \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer YOUR_JWT" \
    -d @query-payload.json
done
```

### Next Steps

After running performance tests:
1. Capture P95/P99 results
2. Update TEST-REPORT-STORY-1.5.md with actual metrics
3. Compare against Story 1.3 vector retrieval benchmarks
4. Mark Subtask 1.5 as complete in story-1.5.md

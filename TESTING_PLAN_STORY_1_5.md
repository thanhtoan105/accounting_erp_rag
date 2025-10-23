# Comprehensive Testing Plan - Story 1.5 (Basic RAG Query Processing Pipeline)
## Vietnamese Language Support Testing Guide

This document provides a complete testing strategy for Story 1.5 with detailed commands for validating the RAG Query Processing Pipeline, including comprehensive Vietnamese language support testing.

---

## 📋 Table of Contents

1. [Prerequisites & Environment Setup](#prerequisites--environment-setup)
2. [Test Execution Strategy](#test-execution-strategy)
3. [Vietnamese Language Testing](#vietnamese-language-testing)
4. [Performance Testing](#performance-testing)
5. [Error Handling & Edge Cases](#error-handling--edge-cases)
6. [End-to-End Testing](#end-to-end-testing)
7. [API Contract Testing](#api-contract-testing)
8. [Frontend Testing](#frontend-testing)
9. [Test Data Management](#test-data-management)
10. [Coverage & Reporting](#coverage--reporting)

---

## 🔧 Prerequisites & Environment Setup

### 1.1 Database Setup
```bash
# Ensure PostgreSQL is running locally or Supabase is configured
cd /home/duong/code/accounting_erp_rag

# Copy environment template and configure
cp .env.example .env
# Edit .env with your actual Supabase credentials

# Verify database connection
./gradlew :apps:backend:bootRun --args="--spring.profiles.active=supabase" &
sleep 10
curl -f http://localhost:8080/actuator/health || echo "❌ Backend not healthy"
```

### 1.2 Vietnamese Locale Configuration
```bash
# Set Vietnamese locale for testing environment
export LANG=vi_VN.UTF-8
export LC_ALL=vi_VN.UTF-8
locale

# Verify UTF-8 support
echo "Testing Vietnamese: Tổng công nợ phải thu" | hexdump -C
```

### 1.3 Frontend Dependencies
```bash
# Install frontend dependencies and setup test environment
cd /home/duong/code/accounting_erp_rag/apps/frontend
npm install

# Install Playwright browsers for E2E testing
npx playwright install chromium firefox webkit
```

### 1.4 Java Environment Verification
```bash
# Verify Java 21 is installed
java -version
echo $JAVA_HOME

# Set UTF-8 encoding for Java
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8"
```

---

## 🚀 Test Execution Strategy

### 2.1 Unit Tests (Fast, No External Dependencies)
```bash
cd /home/duong/code/accounting_erp_rag

# Run all unit tests
./gradlew :apps:backend:unitTest

# Run specific unit test classes
./gradlew :apps:backend:test --tests "QueryEmbeddingServiceTest"
./gradlew :apps:backend:test --tests "VectorRetrievalServiceTest"
./gradlew :apps:backend:test --tests "QueryValidationServiceTest"
./gradlew :apps:backend:test --tests "KeywordRetrievalServiceTest"
./gradlew :apps:backend:test --tests "ContextWindowServiceTest"
./gradlew :apps:backend:test --tests "QueryOrchestrationServiceTest"

# Run unit tests with Vietnamese locale
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:unitTest
```

### 2.2 Integration Tests (With TestContainers)
```bash
# Run all integration tests
./gradlew :apps:backend:integrationTest

# Run specific integration test classes
./gradlew :apps:backend:test --tests "RagQueryPipelineIntegrationTest"
./gradlew :apps:backend:test --tests "VectorRetrievalIntegrationTest"
./gradlew :apps:backend:test --tests "RagQueryErrorHandlingIntegrationTest"
./gradlew :apps:backend:test --tests "SupabaseGatewayIntegrationTest"
./gradlew :apps:backend:test --tests "DatabaseHealthControllerIntegrationTest"

# Run integration tests with Vietnamese locale
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:integrationTest
```

### 2.3 Performance Tests
```bash
# Run all performance tests
./gradlew :apps:backend:performanceTest

# Run specific performance tests
./gradlew :apps:backend:test --tests "RagQueryPipelinePerformanceTest"
./gradlew :apps:backend:test --tests "MemoryUsageMonitoringTest"

# Run performance tests with memory monitoring
./gradlew :apps:backend:performanceTest -Dorg.gradle.jvmargs="-Xmx4g -XX:+PrintGCDetails"
```

### 2.4 Architecture Tests
```bash
# Run architecture validation tests
./gradlew :apps:backend:architectureTest

# Run specific architecture test
./gradlew :apps:backend:test --tests "ArchitectureRulesTest"
```

### 2.5 Contract Tests
```bash
# Run API contract tests
./gradlew :apps:backend:contractTest

# Run specific contract test
./gradlew :apps:backend:test --tests "RagQueryApiContractTest"
```

---

## 🇻🇳 Vietnamese Language Testing

### 3.1 Backend Vietnamese Language Tests
```bash
# Run bilingual terminology tests
./gradlew :apps:backend:test --tests "BilingualAccountingTerminologyTest"

# Run Vietnamese-specific query tests
./gradlew :apps:backend:test --tests "*Test" --tests "*vietnamese*" -i

# Run tests with Vietnamese system properties
./gradlew :apps:backend:test -Duser.language=vi -Duser.country=VN -Dfile.encoding=UTF-8
```

### 3.2 Vietnamese Query Test Cases
```bash
# Test Vietnamese accounting terminology
cat > /tmp/vietnamese_test_queries.txt << 'EOF'
Tổng công nợ phải thu là bao nhiêu?
Hiển thị các hóa đơn chưa thanh toán trong tháng này
Tổng chi phí quý 4 là bao nhiêu?
Danh sách khách hàng nợ quá hạn
Xem báo cáo kết quả kinh doanh
Khách hàng nào còn nợ phải trả?
Tình hình thanh toán của khách hàng ABC
Báo cáo thu chi tháng 10/2024
EOF

# Test Vietnamese Unicode handling
cat > /tmp/vietnamese_unicode_test.txt << 'EOF'
📊 Tổng nợ: $1,234,567.89 (€1,099.99)
Khách hàng: Nguyễn Văn An
Địa chỉ: 123 Đường Trần Hưng Đạo, Quận 1, TP.HCM
Mã số thuế: 123456789
Số điện thoại: +84-28-1234-5678
EOF
```

### 3.3 Frontend Vietnamese Testing
```bash
cd /home/duong/code/accounting_erp_rag/apps/frontend

# Run frontend component tests with Vietnamese locale
LANG=vi_VN.UTF-8 npm test

# Run specific Vietnamese test scenarios
npm test -- --grep "Vietnamese"
npm test -- --grep "bilingual"
npm test -- --grep "Unicode"
```

### 3.4 Vietnamese API Testing
```bash
# Test Vietnamese query submission via API
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -H "Accept-Language: vi-VN" \
  -d '{
    "companyId": "test-company-id",
    "queryText": "Tổng công nợ phải thu là bao nhiêu?",
    "language": "vi",
    "moduleId": "ar",
    "fiscalPeriod": "2024-10"
  }'

# Test Vietnamese Unicode handling
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Accept-Language: vi-VN" \
  -d '{
    "companyId": "test-company-id",
    "queryText": "Khách hàng: Nguyễn Văn An - 📊 Tổng nợ: ₫1,234,567.89",
    "language": "vi"
  }'
```

---

## ⚡ Performance Testing

### 4.1 Latency Testing
```bash
# Run performance benchmarks
./gradlew :apps:backend:performanceTest --info

# Test with JMH for microbenchmarks (if available)
./gradlew :apps:backend:jmh

# Monitor performance during Vietnamese queries
./gradlew :apps:backend:performanceTest \
  -Duser.language=vi \
  -Duser.country=VN \
  -Dorg.gradle.jvmargs="-Xmx4g -XX:+PrintGC"
```

### 4.2 Load Testing with Vietnamese Data
```bash
# Create Vietnamese test data load
cat > /tmp/vietnamese_load_test.json << 'EOF'
{
  "queries": [
    "Tổng công nợ phải thu hiện tại?",
    "Danh sách hóa đơn quá hạn",
    "Báo cáo thu chi tháng 10",
    "Khách hàng nợ lớn nhất",
    "Tình hình thanh toán Q4"
  ],
  "companyId": "test-vi-company",
  "language": "vi"
}
EOF

# Run concurrent Vietnamese query tests
./gradlew :apps:backend:test --tests "RagQueryPipelinePerformanceTest.processQueryAsync_concurrentQueries_handlesLoad"
```

### 4.3 Memory Monitoring
```bash
# Run memory usage tests with Vietnamese locale
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:test \
  --tests "MemoryUsageMonitoringTest" \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:+PrintGCDetails -XX:+PrintGCTimeStamps"

# Monitor memory during Vietnamese query processing
jstat -gc -t $(jps | grep "gradle" | cut -d' ' -f1) 5s
```

### 4.4 Performance Validation Commands
```bash
# Validate P95 latency targets (should be < 5 seconds)
./gradlew :apps:backend:performanceTest --tests "*latency*"

# Validate throughput targets (should be > 5 queries/second)
./gradlew :apps:backend:performanceTest --tests "*throughput*"

# Validate memory usage (should be < 100MB increase)
./gradlew :apps:backend:performanceTest --tests "*memory*"
```

---

## 🚨 Error Handling & Edge Cases

### 5.1 Network Failure Testing
```bash
# Test embedding service failure scenarios
./gradlew :apps:backend:test --tests "*EmbeddingException*"

# Test vector retrieval fallback scenarios
./gradlew :apps:backend:test --tests "*RetrievalException*"

# Test retry logic with Vietnamese queries
./gradlew :apps:backend:test --tests "*retry*" -Duser.language=vi
```

### 5.2 Vietnamese Input Validation
```bash
# Test Vietnamese special characters and diacritics
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{
    "companyId": "test-company-id",
    "queryText": "ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯăâêôơư",
    "language": "vi"
  }'

# Test mixed Vietnamese-English queries
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "test-company-id",
    "queryText": "Show me báo cáo debt aging cho khách hàng ABC",
    "language": "vi"
  }'
```

### 5.3 Edge Case Testing
```bash
# Test empty Vietnamese queries
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"companyId": "test", "queryText": "", "language": "vi"}'

# Test very long Vietnamese queries (> 500 characters)
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d "{
    \"companyId\": \"test\",
    \"queryText\": \"$(python3 -c 'print(\"Tổng công nợ phải thu là bao nhiêu? \" * 50)')\",
    \"language\": \"vi\"
  }"

# Test invalid Vietnamese language codes
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"companyId": "test", "queryText": "Test query", "language": "vn"}'
```

---

## 🔄 End-to-End Testing

### 6.1 Backend E2E with Vietnamese
```bash
# Start backend with Vietnamese locale
cd /home/duong/code/accounting_erp_rag
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:bootRun \
  --args="--spring.profiles.active=supabase --server.port=8080" &
BACKEND_PID=$!

# Wait for startup
sleep 15

# Test complete Vietnamese query flow
QUERY_ID=$(curl -s -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -H "Accept-Language: vi-VN" \
  -d '{
    "companyId": "e2e-test-company",
    "queryText": "Tổng công nợ phải thu hiện tại là bao nhiêu?",
    "language": "vi"
  }' | jq -r '.queryId')

echo "Query ID: $QUERY_ID"

# Check query status
curl -s http://localhost:8080/api/v1/rag/query/$QUERY_ID | jq

# Cleanup
kill $BACKEND_PID
```

### 6.2 Frontend E2E Testing
```bash
cd /home/duong/code/accounting_erp_rag/apps/frontend

# Start frontend development server
npm run dev &
FRONTEND_PID=$!

# Start backend (if not running)
cd ../..
./gradlew :apps:backend:bootRun --args="--spring.profiles.active=supabase" &
BACKEND_PID=$!

# Wait for services to start
sleep 20

# Run Playwright E2E tests with Vietnamese locale
LANG=vi_VN.UTF-8 npm test

# Run specific Vietnamese E2E scenarios
npx playwright test --grep "Vietnamese"
npx playwright test --grep "bilingual"

# Cleanup
kill $FRONTEND_PID $BACKEND_PID
```

### 6.3 Full Vietnamese User Journey
```bash
# Create comprehensive Vietnamese test script
cat > /tmp/vietnamese_e2e_test.sh << 'EOF'
#!/bin/bash

echo "🇻🇳 Testing Vietnamese End-to-End User Journey"

# Vietnamese test queries
QUERIES=(
    "Tổng công nợ phải thu hiện tại là bao nhiêu?"
    "Hiển thị các hóa đơn chưa thanh toán"
    "Danh sách khách hàng nợ quá hạn"
    "Báo cáo kết quả kinh doanh quý 4"
    "Khách hàng nào còn nợ lớn nhất?"
)

for query in "${QUERIES[@]}"; do
    echo "Testing query: $query"

    response=$(curl -s -X POST http://localhost:8080/api/v1/rag/query \
        -H "Content-Type: application/json" \
        -H "Accept-Language: vi-VN" \
        -d "{
            \"companyId\": \"e2e-vi-test\",
            \"queryText\": \"$query\",
            \"language\": \"vi\"
        }")

    query_id=$(echo $response | jq -r '.queryId')
    status=$(echo $response | jq -r '.status')

    echo "Query ID: $query_id, Status: $status"

    # Wait for processing
    sleep 2

    # Check final status
    final_status=$(curl -s http://localhost:8080/api/v1/rag/query/$query_id | jq -r '.status')
    echo "Final status: $final_status"

    if [ "$final_status" = "RETRIEVED" ]; then
        echo "✅ Query processed successfully"
    else
        echo "❌ Query processing failed"
    fi
    echo "---"
done
EOF

chmod +x /tmp/vietnamese_e2e_test.sh
/tmp/vietnamese_e2e_test.sh
```

---

## 📝 API Contract Testing

### 7.1 Vietnamese API Contract Validation
```bash
# Run API contract tests with Vietnamese locale
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:contractTest

# Test specific Vietnamese API contracts
./gradlew :apps:backend:test --tests "RagQueryApiContractTest" \
  -Duser.language=vi -Duser.country=VN

# Test Vietnamese request/response format
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Accept-Language: vi-VN, vi;q=0.9" \
  -d '{
    "companyId": "contract-test-vi",
    "queryText": "Tình hình công nợ phải thu?",
    "language": "vi",
    "moduleId": "ar",
    "fiscalPeriod": "2024-10"
  }' | jq
```

### 7.2 Schema Validation for Vietnamese
```bash
# Test Vietnamese content against API schema
cat > /tmp/vietnamese_schema_test.json << 'EOF'
{
  "test_cases": [
    {
      "description": "Basic Vietnamese query",
      "request": {
        "companyId": "schema-test-vi",
        "queryText": "Tổng công nợ phải thu",
        "language": "vi"
      },
      "expected_response_fields": ["queryId", "status", "createdAt"]
    },
    {
      "description": "Vietnamese with special characters",
      "request": {
        "companyId": "schema-test-vi",
        "queryText": "Khách hàng: Nguyễn Văn An - Địa chỉ: TP.HCM",
        "language": "vi"
      },
      "expected_response_fields": ["queryId", "status", "createdAt"]
    }
  ]
}
EOF

# Run schema validation tests
./gradlew :apps:backend:test --tests "*Contract*" \
  -Dtest.schema.file=/tmp/vietnamese_schema_test.json
```

---

## 🎨 Frontend Testing

### 8.1 React Component Testing with Vietnamese
```bash
cd /home/duong/code/accounting_erp_rag/apps/frontend

# Run component tests with Vietnamese locale
LANG=vi_VN.UTF-8 npm test

# Run specific Vietnamese component tests
npm test -- --testNamePattern="Vietnamese"
npm test -- --testNamePattern="Bilingual"
npm test -- --testNamePattern="Unicode"

# Run QueryTestInterface Vietnamese tests
npm test QueryTestInterface.test.tsx
```

### 8.2 Playwright E2E with Vietnamese
```bash
# Run E2E tests with Vietnamese browser locale
npx playwright test -- --reporter=list

# Run specific Vietnamese test cases
npx playwright test --grep "Vietnamese language"
npx playwright test --grep "Vietnamese queries"
npx playwright test --grep "Unicode characters"

# Run tests in headed mode for debugging Vietnamese rendering
npx playwright test --headed --grep "Vietnamese"

# Run tests with Vietnamese UI
LANG=vi_VN.UTF-8 npx playwright test
```

### 8.3 Frontend Performance Testing
```bash
# Measure frontend performance with Vietnamese content
npm run build

# Analyze bundle size for Vietnamese font support
ls -la dist/assets/*.css | head -5

# Test Vietnamese font rendering
npm run preview &
PREVIEW_PID=$!

# Capture screenshots for Vietnamese content testing
npx playwright test -- --update-snapshots
```

---

## 📊 Test Data Management

### 9.1 Vietnamese Test Data Setup
```bash
# Create Vietnamese test documents
cat > /tmp/vietnamese_test_docs.json << 'EOF'
{
  "documents": [
    {
      "title": "Báo cáo công nợ phải thu",
      "content": "Tổng công nợ phải thu tại thời điểm 31/10/2024 là 5.234.567.000 VNĐ",
      "module": "ar",
      "language": "vi"
    },
    {
      "title": "Hóa đơn chưa thanh toán",
      "content": "Danh sách các hóa đơn chưa thanh toán trong tháng 10/2024 với tổng số tiền 1.890.000 VNĐ",
      "module": "ar",
      "language": "vi"
    },
    {
      "title": "Báo cáo kết quả kinh doanh",
      "content": "Doanh thu quý 4: 15.678.000 VNĐ, Lợi nhuận: 2.345.000 VNĐ",
      "module": "gl",
      "language": "vi"
    }
  ]
}
EOF

# Seed Vietnamese test data
curl -X POST http://localhost:8080/api/v1/test/seed-vietnamese-data \
  -H "Content-Type: application/json" \
  -d @/tmp/vietnamese_test_docs.json
```

### 9.2 Test Cleanup
```bash
# Clean up Vietnamese test data after testing
curl -X DELETE http://localhost:8080/api/v1/test/cleanup-vietnamese-data

# Reset test database
./gradlew :apps:backend:test --tests "*Cleanup*"

# Clean up test files
rm -f /tmp/vietnamese_*.json /tmp/vietnamese_*.txt /tmp/vietnamese_*.sh
```

### 9.3 Test Isolation
```bash
# Run tests with isolated Vietnamese test profile
./gradlew :apps:backend:test \
  --tests="*Vietnamese*" \
  -Dspring.profiles.active=test-vietnamese \
  -Dtest.database.unique=true

# Verify test isolation between Vietnamese and English tests
./gradlew :apps:backend:unitTest :apps:backend:integrationTest
```

---

## 📈 Coverage & Reporting

### 10.1 Code Coverage for Vietnamese Tests
```bash
# Generate comprehensive coverage report including Vietnamese tests
./gradlew :apps:backend:jacocoTestReport

# View coverage report
open /home/duong/code/accounting_erp_rag/apps/backend/build/reports/jacoco/test/html/index.html

# Check Vietnamese-specific test coverage
./gradlew :apps:backend:jacocoTestReport \
  --tests="*Vietnamese*" --tests="*Bilingual*"

# Verify coverage targets (80% overall, 70% per class)
./gradlew :apps:backend:jacocoTestCoverageVerification
```

### 10.2 Test Reports
```bash
# Generate HTML test reports
./gradlew :apps:backend:test --continue

# View unit test reports
open /home/duong/code/accounting_erp_rag/apps/backend/build/reports/tests/unitTest/index.html

# View integration test reports
open /home/duong/code/accounting_erp_rag/apps/backend/build/reports/tests/integrationTest/index.html

# View performance test reports
open /home/duong/code/accounting_erp_rag/apps/backend/build/reports/tests/performanceTest/index.html
```

### 10.3 Vietnamese Testing Summary
```bash
# Create Vietnamese testing summary script
cat > /tmp/vietnamese_test_summary.sh << 'EOF'
#!/bin/bash

echo "🇻🇳 Vietnamese Language Testing Summary"
echo "======================================"

# Count Vietnamese-related tests
VIETNAMESE_TESTS=$(find /home/duong/code/accounting_erp_rag/apps/backend/src/test -name "*.java" -exec grep -l "vi\|Vietnamese\|bilingual" {} \; | wc -l)
echo "Vietnamese test files: $VIETNAMESE_TESTS"

# Run Vietnamese test coverage
echo "Running Vietnamese test coverage..."
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:test --tests "*Vietnamese*" --tests "*Bilingual*" --continue

# Check test results
if [ $? -eq 0 ]; then
    echo "✅ All Vietnamese tests passed"
else
    echo "❌ Some Vietnamese tests failed"
fi

# Generate coverage report
echo "Generating coverage report..."
./gradlew :apps:backend:jacocoTestReport

echo "Coverage report available at: apps/backend/build/reports/jacoco/test/html/index.html"
EOF

chmod +x /tmp/vietnamese_test_summary.sh
/tmp/vietnamese_test_summary.sh
```

### 10.4 Quality Gate Validation
```bash
# Run complete quality gate including Vietnamese tests
./gradlew :apps:backend:qualityGate

# Run quality gate with Vietnamese locale
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:qualityGate

# Verify all quality checks pass
./gradlew :apps:backend:check
```

---

## 🎯 Quick Reference Commands

### Essential Vietnamese Testing Commands
```bash
# Quick Vietnamese unit test run
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:unitTest

# Quick Vietnamese integration test run
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:integrationTest

# Quick Vietnamese performance test
LANG=vi_VN.UTF-8 ./gradlew :apps:backend:performanceTest

# Quick frontend Vietnamese test
cd apps/frontend && LANG=vi_VN.UTF-8 npm test

# Quick Vietnamese API test
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -H "Accept-Language: vi-VN" \
  -d '{"companyId": "test", "queryText": "Tổng công nợ phải thu?", "language": "vi"}'
```

### Vietnamese Test Data
```bash
# Vietnamese accounting terms for testing
TERMS=("Tổng công nợ phải thu" "Công nợ phải trả" "Báo cáo kết quả kinh doanh"
         "Hóa đơn chưa thanh toán" "Khách hàng nợ quá hạn" "Doanh thu" "Chi phí")

# Test each term
for term in "${TERMS[@]}"; do
    echo "Testing: $term"
    curl -X POST http://localhost:8080/api/v1/rag/query \
        -H "Content-Type: application/json" \
        -H "Accept-Language: vi-VN" \
        -d "{\"companyId\": \"test\", \"queryText\": \"$term\", \"language\": \"vi\"}"
    echo "---"
done
```

---

## ✅ Success Criteria Checklist

- [ ] All unit tests pass with Vietnamese locale
- [ ] All integration tests pass with Vietnamese locale
- [ ] Performance tests meet latency targets (< 5s P95)
- [ ] Vietnamese Unicode characters handled correctly
- [ ] Mixed Vietnamese-English queries processed successfully
- [ ] API responses properly formatted for Vietnamese content
- [ ] Frontend renders Vietnamese text correctly
- [ ] Memory usage within acceptable limits (< 100MB increase)
- [ ] Code coverage targets met (80% overall, 70% per class)
- [ ] Error handling works for Vietnamese edge cases
- [ ] End-to-end Vietnamese user journey works smoothly

---

## 🔧 Troubleshooting

### Common Issues and Solutions

1. **Character Encoding Issues**
   ```bash
   export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8"
   export LANG=vi_VN.UTF-8
   export LC_ALL=vi_VN.UTF-8
   ```

2. **TestContainer Issues**
   ```bash
   docker system prune -f
   ./gradlew :apps:backend:integrationTest --refresh-dependencies
   ```

3. **Memory Issues in Performance Tests**
   ```bash
   ./gradlew :apps:backend:performanceTest -Dorg.gradle.jvmargs="-Xmx4g -XX:+UseG1GC"
   ```

4. **Frontend Vietnamese Font Issues**
   ```bash
   cd apps/frontend
   npm install --save-dev @fontsource/vietnamese
   ```

---

*This comprehensive testing plan ensures thorough validation of Story 1.5 with complete Vietnamese language support, performance validation, and quality assurance.*
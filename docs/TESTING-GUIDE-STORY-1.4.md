# Hướng Dẫn Kiểm Tra Story 1.4: Document Embedding Generation Pipeline

**Ngày tạo**: 2025-10-21  
**Story**: 1.4 - Document Embedding Generation Pipeline  
**Ngôn ngữ**: Tiếng Việt

---

## 📋 Mục Lục

1. [Tổng Quan](#tổng-quan)
2. [Chuẩn Bị Môi Trường](#chuẩn-bị-môi-trường)
3. [Chạy Unit Tests](#1-chạy-unit-tests)
4. [Chạy Integration Tests](#2-chạy-integration-tests)
5. [Kiểm Tra Từng Acceptance Criteria](#3-kiểm-tra-từng-acceptance-criteria)
6. [Test Thủ Công với API](#4-test-thủ-công-với-api)
7. [Tạo Dữ Liệu Test Synthetic](#5-tạo-dữ-liệu-test-synthetic)
8. [Kiểm Tra Performance](#6-kiểm-tra-performance)
9. [Troubleshooting](#troubleshooting)

---

## Tổng Quan

Story 1.4 triển khai pipeline tạo embeddings cho documents từ ERP system. Pipeline bao gồm:

```
Trích xuất Documents → Áp dụng PII Masking → Tạo Embeddings → Lưu vào Vector DB → Tracking Batch
```

### 10 Acceptance Criteria Cần Kiểm Tra:

| AC | Mô Tả | Cách Kiểm Tra |
|----|-------|---------------|
| AC1 | Document extraction (7 loại documents) | Unit tests + Integration tests |
| AC2 | Text templates với Vietnamese UTF-8 | Unit tests với diacritics |
| AC3 | Embedding generation (Azure OpenAI) | Unit tests với stub |
| AC4 | Batch processing (10K docs < 30 phút) | Performance tests |
| AC5 | PII masking integration | Unit tests + Regex scan |
| AC6 | Metadata extraction và persistence | Integration tests |
| AC7 | Error handling và retry logic | Unit tests với chaos |
| AC8 | Worker triggers (API + n8n webhook) | API tests |
| AC9 | embedding_batches tracking | Database tests |
| AC10 | Telemetry và observability | Metrics tests |

---

## Chuẩn Bị Môi Trường

### 1. Kiểm tra Java và Gradle

```bash
# Kiểm tra Java version (cần Java 21)
java -version

# Kiểm tra Gradle
./gradlew --version
```

### 2. Cấu hình Database

Đảm bảo Supabase PostgreSQL đang chạy và có thể kết nối:

```bash
# Kiểm tra kết nối database
psql -h localhost -U postgres -d postgres -c "SELECT version();"
```

### 3. Chạy Liquibase Migrations

```bash
# Apply tất cả migrations (bao gồm embedding_batches table)
./gradlew :apps:backend:update

# Hoặc chạy trực tiếp với psql
psql -h localhost -U postgres -d postgres -f apps/backend/src/main/resources/db/changelog/005-embedding-batches-table.xml
```

### 4. Cấu hình Environment Variables

Tạo file `.env` ở root directory:

```bash
# Database
SUPABASE_HOST=localhost
SUPABASE_PORT=5432
SUPABASE_DATABASE=postgres
SUPABASE_ADMIN_USERNAME=postgres
SUPABASE_ADMIN_PASSWORD=your_password

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local

# N8N Webhook (optional cho testing)
N8N_WEBHOOK_SECRET=test-secret-123
```

---

## 1. 🧪 Chạy Unit Tests

### Chạy Tất Cả Unit Tests

```bash
# Chạy tất cả tests trong module backend
./gradlew :apps:backend:test

# Xem báo cáo HTML
# Mở file: apps/backend/build/reports/tests/test/index.html
```

### Chạy Tests Theo Class

#### Test Document Extraction (AC1)

```bash
./gradlew :apps:backend:test --tests "DocumentExtractorTest"
```

**Kiểm tra gì:**
- ✅ Extraction được 7 loại documents (invoices, bills, journal entries, customers, vendors, payments, bank transactions)
- ✅ Tenant scoping (company_id filtering)
- ✅ Soft delete filtering (deleted_at IS NULL)
- ✅ Edge cases (null fields, empty descriptions)

**Expected Output:**
```
DocumentExtractorTest > testExtractAll_ReturnsAllDocumentTypes() PASSED
DocumentExtractorTest > testExtractFrom_SpecificTables() PASSED
DocumentExtractorTest > testInvoiceDocument_RawTextGeneration() PASSED
DocumentExtractorTest > testCustomerDocument_Vietnamese_UTF8_Preserved() PASSED
```

#### Test Text Rendering và PII Masking (AC2, AC5)

```bash
./gradlew :apps:backend:test --tests "TextTemplateRendererTest"
```

**Kiểm tra gì:**
- ✅ PII masking được áp dụng TRƯỚC khi tạo embedding
- ✅ Vietnamese diacritics (ó, ơ, á, ế) được preserve
- ✅ PII masking failure → halt batch (throw exception)
- ✅ Soft-deleted documents được skip

**Expected Output:**
```
TextTemplateRendererTest > testRenderDocument_AppliesPiiMasking() PASSED
TextTemplateRendererTest > testRenderDocument_Vietnamese_UTF8_Preserved() PASSED
TextTemplateRendererTest > testRenderDocument_PiiMaskingFailure_ThrowsException() PASSED
TextTemplateRendererTest > testRenderDocument_SoftDeletedDocument_ReturnsNull() PASSED
```

### Fix Test Failures

Nếu test `testRenderDocument_Vietnamese_UTF8_Preserved()` fail, kiểm tra:

```bash
# Xem chi tiết lỗi
cat apps/backend/build/reports/tests/test/index.html
```

**Common issue:** Mock PiiMaskingService không return đúng Vietnamese text. Fix:

```java
// Trong test, đảm bảo mock return chính xác text với diacritics
when(piiMaskingService.maskText(any(), any()))
    .thenAnswer(invocation -> invocation.getArgument(0)); // Return unchanged text
```

---

## 2. 🔗 Chạy Integration Tests

### Tạo Integration Test với Database Thật

Tạo file: `apps/backend/src/test/java/com/erp/rag/ragplatform/worker/integration/EmbeddingPipelineIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
class EmbeddingPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private EmbeddingWorkerService workerService;

    @Test
    void testFullPipeline_WithRealDatabase() {
        // Arrange: Insert test data vào database
        UUID companyId = UUID.randomUUID();
        // ... insert invoices, bills, etc.

        // Act: Execute batch
        UUID batchId = workerService.executeBatch(
            companyId,
            EmbeddingBatch.BatchType.FULL,
            "integration-test",
            null,
            null
        );

        // Assert: Verify embeddings được persist
        assertThat(batchId).isNotNull();
        // ... verify vector_documents table
    }
}
```

**Chạy:**

```bash
./gradlew :apps:backend:test --tests "*IntegrationTest"
```

---

## 3. ✅ Kiểm Tra Từng Acceptance Criteria

### AC1: Document Extraction Logic ✅

```bash
# Chạy test
./gradlew :apps:backend:test --tests "DocumentExtractorTest"

# Verify: Check logs
# Phải thấy: "Extracted X invoices", "Extracted Y bills", etc.
```

**Manual verification:**

```sql
-- Kiểm tra documents trong database
SELECT 
    'invoices' as type, COUNT(*) as count 
FROM accounting.invoices WHERE deleted_at IS NULL
UNION ALL
SELECT 'bills', COUNT(*) FROM accounting.bills WHERE deleted_at IS NULL
UNION ALL
SELECT 'journal_entries', COUNT(*) FROM accounting.journal_entries WHERE deleted_at IS NULL;
```

### AC2: Text Templates với Vietnamese UTF-8 ✅

```bash
# Chạy test với Vietnamese characters
./gradlew :apps:backend:test --tests "TextTemplateRendererTest.testRenderDocument_Vietnamese_UTF8_Preserved"
```

**Manual verification:**

```java
// Tạo test document với Vietnamese name
CustomerDocument customer = new CustomerDocument();
customer.setName("Công ty TNHH Thương mại Việt Nam");

String text = customer.getRawText();

// Verify: text phải chứa đúng diacritics
assertThat(text).contains("ô"); // ô trong "Công"
assertThat(text).contains("ệ"); // ệ trong "Việt"
```

### AC3: Embedding Generation với Batch API ✅

```bash
# Test embedding service
./gradlew :apps:backend:test --tests "*EmbeddingServiceTest"
```

**Verify:**
- ✅ Batch size <= 100 docs
- ✅ Embedding dimension = 1536
- ✅ Retry logic (3 attempts với exponential backoff)

**Manual test:**

```java
@Test
void testEmbeddingBatchSize() {
    List<String> texts = List.of("text1", "text2", ..., "text101"); // 101 items
    
    assertThatThrownBy(() -> embeddingService.generateEmbeddings(texts))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds maximum of 100");
}
```

### AC4: Batch Processing Performance ✅

**Tạo synthetic test data:**

```bash
cd scripts/seed-data
./generate-embeddings-test-data.sh
```

**Output:** `test-docs.json` với 10,000 documents

**Run performance test:**

```bash
# TODO: Tạo performance test
./gradlew :apps:backend:test --tests "*PerformanceTest"
```

**Verify:**
- ✅ 10K docs < 30 phút (throughput >= 200 docs/min)
- ✅ Progress logging mỗi 1000 docs
- ✅ Metrics: elapsed time, throughput, ETA

### AC5: PII Masking Integration ✅

```bash
# Test PII masking
./gradlew :apps:backend:test --tests "*PiiMasking*"
```

**Verify:**
- ✅ Masking được apply TRƯỚC embedding generation
- ✅ Performance < 100ms per document
- ✅ Output chỉ chứa tokens (CUSTOMER_12345, không có PII thật)

**Manual regex scan:**

```bash
# Sau khi chạy pipeline, scan logs/output
grep -E "[0-9]{10}" logs/application.log  # Không được có tax codes
grep -E "\+84[0-9]{9}" logs/application.log  # Không được có phone numbers
grep -E "[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}" logs/application.log  # Không được có emails
```

### AC6: Metadata Extraction và Persistence ✅

**Verify trong database:**

```sql
-- Check vector_documents được persist với metadata
SELECT 
    id,
    document_id,
    source_table,
    metadata->>'document_type' as doc_type,
    metadata->>'module' as module,
    metadata->>'fiscal_period' as fiscal_period,
    metadata->>'status' as status
FROM accounting.vector_documents
LIMIT 10;

-- Expected: metadata phải có đầy đủ fields
```

### AC7: Error Handling và Retry Logic ✅

**Test transient errors:**

```java
@Test
void testRetryOnTransientFailure() {
    // Arrange: Mock API failure 2 lần, success lần 3
    when(mockEmbeddingApi.embed(any()))
        .thenThrow(new RuntimeException("429 Rate Limit"))
        .thenThrow(new RuntimeException("500 Server Error"))
        .thenReturn(validEmbedding);

    // Act
    List<float[]> result = embeddingService.generateEmbeddings(texts);

    // Assert: Phải retry 3 lần
    verify(mockEmbeddingApi, times(3)).embed(any());
    assertThat(result).isNotEmpty();
}
```

**Verify error rate alerting:**

```java
// Simulate >5% error rate
// Expected: Log message "ERROR RATE ALERT: X% failures exceeds 5% threshold"
```

### AC8: Worker Triggers (REST API + n8n) ✅

**Test REST endpoint:**

```bash
# Start application
./gradlew :apps:backend:bootRun

# Test manual trigger
curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "123e4567-e89b-12d3-a456-426614174000",
    "batchType": "full",
    "triggeredBy": "manual-test"
  }'

# Expected response:
# {
#   "batch_id": "uuid",
#   "status": "started",
#   "message": "Embedding batch processing started"
# }
```

**Test n8n webhook với Bearer token:**

```bash
curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-secret-123" \
  -d '{
    "companyId": "123e4567-e89b-12d3-a456-426614174000",
    "batchType": "incremental",
    "triggeredBy": "n8n-cron",
    "startFrom": "2024-10-20T10:00:00Z"
  }'
```

**Test invalid token:**

```bash
curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Authorization: Bearer wrong-token" \
  -d '{"companyId":"...", "batchType":"full"}'

# Expected: 401 Unauthorized
```

### AC9: embedding_batches Table Tracking ✅

**Verify table exists:**

```sql
-- Check table structure
\d accounting.embedding_batches

-- Check state machine transitions
SELECT 
    id,
    batch_type,
    status,
    total_documents,
    processed_documents,
    failed_documents,
    started_at,
    completed_at
FROM accounting.embedding_batches
ORDER BY created_at DESC;
```

**Test duplicate prevention:**

```java
@Test
void testDuplicateBatchPrevention() {
    // Act: Execute same batch twice
    UUID batch1 = workerService.executeBatch(...);
    UUID batch2 = workerService.executeBatch(...); // Same parameters

    // Assert: batch2 should return batch1's ID (duplicate detected)
    assertThat(batch1).isEqualTo(batch2);
}
```

### AC10: Telemetry và Observability ✅

**Check Prometheus metrics:**

```bash
# Access metrics endpoint
curl http://localhost:8080/actuator/prometheus | grep embedding

# Expected metrics:
# embeddings_generated_total{} 150
# embedding_latency_seconds{quantile="0.95"} 0.234
# embedding_errors_total{} 5
```

**Check progress logging:**

```bash
# Tail application logs
tail -f logs/application.log | grep Progress

# Expected output every 1000 docs:
# Progress: 1000/10000 docs processed (12 failed) | Throughput: 234.5 docs/min | Elapsed: 256s | ETA: 768s
# Progress: 2000/10000 docs processed (25 failed) | Throughput: 241.2 docs/min | Elapsed: 498s | ETA: 664s
```

---

## 4. 🌐 Test Thủ Công với API

### Setup

```bash
# 1. Start application
./gradlew :apps:backend:bootRun

# 2. Trong terminal khác, prepare test data
psql -h localhost -U postgres -d postgres << EOF
-- Insert test company
INSERT INTO accounting.companies (id, name, tax_code) 
VALUES (gen_random_uuid(), 'Test Company', '0123456789');

-- Insert test invoices
INSERT INTO accounting.invoices (company_id, customer_id, invoice_number, ...)
VALUES (...);
EOF
```

### Test Full Batch Indexing

```bash
# Request
curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "YOUR_COMPANY_ID",
    "batchType": "full",
    "triggeredBy": "manual-test"
  }'

# Monitor logs
tail -f logs/application.log
```

**Expected log output:**

```
2025-10-21 ... Starting embedding batch for company xxx (type: FULL, triggeredBy: manual-test)
2025-10-21 ... Extracted 1234 documents for batch yyy
2025-10-21 ... Progress: 1000/1234 docs processed (5 failed) | Throughput: 215.3 docs/min | Elapsed: 279s | ETA: 65s
2025-10-21 ... Batch yyy completed: 1234 processed, 5 failed, 215.3 docs/min, 345s elapsed
```

### Test Incremental Indexing

```bash
curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "YOUR_COMPANY_ID",
    "batchType": "incremental",
    "triggeredBy": "manual-test",
    "startFrom": "2024-10-20T00:00:00Z",
    "tables": ["invoices", "payments"]
  }'
```

**Verify:** Chỉ documents có `updated_at > 2024-10-20` được process.

---

## 5. 📊 Tạo Dữ Liệu Test Synthetic

### Generate 10K Test Documents

```bash
cd scripts/seed-data
./generate-embeddings-test-data.sh

# Output: test-docs.json (~2-3 MB)
```

**Kiểm tra output:**

```bash
# Check file size
ls -lh test-docs.json

# Count documents
jq '.documents | length' test-docs.json
# Expected: 10000

# View sample
jq '.documents[0]' test-docs.json
```

**Expected structure:**

```json
{
  "metadata": {
    "total_documents": 10000,
    "generated_at": "2025-10-21T...",
    "distribution": {
      "invoices": 5000,
      "payments": 3000,
      "journal_entries": 2000
    }
  },
  "documents": [
    {
      "id": "uuid",
      "company_id": "uuid",
      "document_type": "invoice",
      "invoice_number": "INV-000001",
      "customer_name": "Công ty TNHH ABC",
      "description": "Hóa đơn bán hàng cho Công ty TNHH ABC - sản phẩm điện tử",
      "total_amount": 15234567.89,
      ...
    }
  ]
}
```

### Load Test Data vào Database

Tạo script `load-test-data.sh`:

```bash
#!/bin/bash
# Load synthetic test data into database

jq -r '.documents[] | 
  "INSERT INTO accounting.invoices (...) VALUES (\(.id), \(.company_id), ...);"
' test-docs.json | psql -h localhost -U postgres -d postgres
```

---

## 6. ⚡ Kiểm Tra Performance

### Benchmark với 10K Documents

```bash
# 1. Load test data
./load-test-data.sh

# 2. Execute batch và đo thời gian
time curl -X POST http://localhost:8080/internal/rag/index-batch \
  -H "Content-Type: application/json" \
  -d '{"companyId":"...","batchType":"full","triggeredBy":"perf-test"}'

# 3. Monitor metrics
watch -n 1 'curl -s http://localhost:8080/actuator/prometheus | grep embedding'
```

**Performance Targets (AC4):**
- ✅ Total time: < 30 minutes (1800 seconds)
- ✅ Throughput: >= 200 docs/min
- ✅ P95 latency: Reasonable per operation

**Verify trong logs:**

```
Batch xxx completed: 10000 processed, 87 failed, 234.5 docs/min, 2562s elapsed
```

**Tính toán:**
- 10000 docs / 2562s = 234.5 docs/min ✅ (>= 200 docs/min target)
- 2562s = 42.7 minutes ❌ (> 30 min target)

**Nếu không đạt target:** Optimize bằng cách:
1. Tăng batch size (hiện tại 100, có thể lên đến 300)
2. Parallel processing (run multiple batches đồng thời)
3. Optimize database queries (add indexes)

---

## Troubleshooting

### Issue 1: Tests Fail với Database Connection Error

**Error:**
```
org.postgresql.util.PSQLException: Connection refused
```

**Fix:**
```bash
# Start Supabase local
# Hoặc check connection string trong .env
psql -h localhost -U postgres -d postgres -c "SELECT 1;"
```

### Issue 2: PII Masking Performance < 100ms

**Check:**
```java
// Trong logs, tìm warnings
grep "PII masking exceeded 100ms SLA" logs/application.log
```

**Fix:** Optimize regex patterns hoặc cache masking results.

### Issue 3: Embedding Generation Quá Chậm

**Kiểm tra:**
- Batch size có phải 100 không?
- Network latency đến Azure OpenAI API?
- Retry logic có đang retry quá nhiều?

**Monitor:**
```bash
curl http://localhost:8080/actuator/metrics/embedding.latency.seconds
```

### Issue 4: Test Data Generation Fails

**Error:**
```
ModuleNotFoundError: No module named 'faker'
```

**Fix:**
```bash
pip3 install faker
```

---

## Kết Luận

Để đảm bảo Story 1.4 hoàn thành đúng:

1. ✅ **Tất cả unit tests pass**
2. ✅ **Integration tests với database thật pass**
3. ✅ **10 ACs đều được verify (manual + automated)**
4. ✅ **API endpoints hoạt động đúng**
5. ✅ **Performance targets đạt được (có thể cần optimize)**
6. ✅ **Metrics được expose qua Prometheus**
7. ✅ **Logs đầy đủ và có structure tốt**

### Next Steps

1. Fix test failure `testRenderDocument_Vietnamese_UTF8_Preserved()`
2. Tạo integration tests với Testcontainers
3. Run performance benchmark với 10K documents
4. Thay stub implementation bằng real Azure OpenAI API
5. Setup Grafana dashboard cho metrics
6. Configure Slack webhook cho alerts

---

**Tài liệu liên quan:**
- `docs/stories/story-1.4.md` - Story requirements
- `docs/tech-spec-epic-1.md` - Technical specifications
- `scripts/seed-data/README.md` - Test data generation guide


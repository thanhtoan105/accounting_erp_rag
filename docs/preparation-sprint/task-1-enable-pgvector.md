# Task 1: Enable Supabase pgvector Extension

**Ước tính:** 4 giờ
**Độ ưu tiên:** 🔴 BLOCKING (chặn E1-S4 Embedding Pipeline)
**Trạng thái:** In Progress
**Ngày:** 2025-10-19

---

## Mục tiêu

Enable extension pgvector trong Supabase PostgreSQL database để lưu trữ và truy vấn vector embeddings hiệu quả cho RAG pipeline.

## Prerequisites

- ✅ Story 1.1 đã hoàn thành (Supabase read-only connection established)
- ✅ Supabase project ID: `aws-1-us-east-2.pooler.supabase.com`
- ✅ Database credentials từ `.env` file
- ⚠️ Service role key (có quyền admin) - **CẦN XIN TỪ PM/PROJECT OWNER**

## Bước 1: Kiểm tra phiên bản PostgreSQL và pgvector hiện tại (30 phút)

### 1.1 Kết nối vào Supabase database

```bash
# Sử dụng credentials từ Story 1.1
psql -h aws-1-us-east-2.pooler.supabase.com \
     -p 6543 \
     -U postgres.sffrejedfcxumghamvyp \
     -d postgres
```

### 1.2 Kiểm tra phiên bản PostgreSQL

```sql
SELECT version();
-- Expected: PostgreSQL 15.3 hoặc cao hơn
```

### 1.3 Kiểm tra extension pgvector đã được cài đặt chưa

```sql
-- Kiểm tra extension có sẵn không
SELECT * FROM pg_available_extensions WHERE name = 'vector';

-- Kiểm tra extension đã được enable chưa
SELECT * FROM pg_extension WHERE extname = 'vector';
```

**Kết quả mong đợi:**
- Nếu chưa enable: Không có kết quả từ query thứ 2
- Nếu đã enable: Sẽ thấy row với `extname = 'vector'`

---

## Bước 2: Enable pgvector extension (1 giờ)

### 2.1 Enable extension với schema extensions (RECOMMENDED)

```sql
-- Best practice: Đặt extension trong schema riêng để tránh làm bẩn public schema
CREATE EXTENSION IF NOT EXISTS vector
WITH SCHEMA extensions;
```

**⚠️ LƯU Ý:**
- Nếu bạn đang dùng read-only user, query này sẽ FAIL với lỗi `permission denied`
- Bạn CẦN service_role hoặc postgres superuser để enable extension
- Liên hệ PM hoặc Supabase admin để được cấp quyền tạm thời

### 2.2 Alternative: Enable với public schema (nếu cần)

```sql
-- Nếu yêu cầu đặt trong public schema
CREATE EXTENSION IF NOT EXISTS vector
WITH SCHEMA public;
```

### 2.3 Verify extension đã được enable

```sql
-- Kiểm tra lại
SELECT extname, extversion, extnamespace::regnamespace AS schema
FROM pg_extension
WHERE extname = 'vector';

-- Expected output:
--  extname | extversion | schema
-- ---------+------------+-----------
--  vector  | 0.7.4      | extensions
```

### 2.4 Kiểm tra vector operators có sẵn

```sql
-- Kiểm tra các distance operators
SELECT oprname, oprleft::regtype, oprright::regtype
FROM pg_operator
WHERE oprnamespace = 'extensions'::regnamespace
  AND oprname IN ('<->', '<=>', '<#>', '<~>');

-- Expected operators:
-- <->  : L2 distance (Euclidean)
-- <=>  : Cosine distance
-- <#>  : Inner product (negative inner product)
-- <~>  : Hamming distance (bit vectors)
```

---

## Bước 3: Tạo bảng vector test (1 giờ)

### 3.1 Tạo schema accounting.vector_test

```sql
-- Tạo bảng test để verify pgvector hoạt động
CREATE TABLE accounting.vector_test (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536), -- OpenAI ada-002 dimension
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Add comment
COMMENT ON TABLE accounting.vector_test IS 'Test table for pgvector functionality verification';
COMMENT ON COLUMN accounting.vector_test.embedding IS 'Vector embedding (1536 dimensions for OpenAI ada-002)';
```

### 3.2 Insert test vectors

```sql
-- Insert 3 test vectors
INSERT INTO accounting.vector_test (document_id, content, embedding)
VALUES
    (1, 'Invoice #INV-001 for customer ABC Corp',
     array_fill(0.1, ARRAY[1536])::vector(1536)),
    (2, 'Payment received from XYZ Ltd',
     array_fill(0.2, ARRAY[1536])::vector(1536)),
    (3, 'Tax declaration for Q1 2024',
     array_fill(0.3, ARRAY[1536])::vector(1536));
```

### 3.3 Test vector similarity query

```sql
-- Test cosine similarity search
SELECT
    id,
    document_id,
    content,
    embedding <=> array_fill(0.15, ARRAY[1536])::vector(1536) AS cosine_distance
FROM accounting.vector_test
ORDER BY embedding <=> array_fill(0.15, ARRAY[1536])::vector(1536)
LIMIT 3;

-- Expected: Query hoạt động không lỗi
-- Record với document_id=1 hoặc 2 sẽ có distance nhỏ nhất
```

---

## Bước 4: Tạo production vector tables (1.5 giờ)

### 4.1 Tạo bảng vector_documents cho RAG

```sql
-- Main table for vector embeddings
CREATE TABLE accounting.vector_documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
    source_table VARCHAR(100) NOT NULL, -- 'invoices', 'journal_entries', etc.
    source_id BIGINT NOT NULL,
    content_type VARCHAR(50) NOT NULL, -- 'invoice', 'journal_entry', 'customer', etc.
    content_text TEXT NOT NULL,
    content_tsv TSVECTOR, -- For hybrid search (full-text + vector)
    embedding vector(1536), -- OpenAI ada-002 embeddings
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    CONSTRAINT unique_source_record UNIQUE (company_id, source_table, source_id)
);

-- Indexes
CREATE INDEX idx_vector_docs_company ON accounting.vector_documents(company_id);
CREATE INDEX idx_vector_docs_source ON accounting.vector_documents(source_table, source_id);
CREATE INDEX idx_vector_docs_deleted ON accounting.vector_documents(deleted_at);

-- Full-text search index (for hybrid search)
CREATE INDEX idx_vector_docs_fts ON accounting.vector_documents USING GIN(content_tsv);

-- Comments
COMMENT ON TABLE accounting.vector_documents IS 'Vector embeddings for all ERP documents for RAG retrieval';
COMMENT ON COLUMN accounting.vector_documents.embedding IS 'Vector embedding (1536-dim) generated from content_text';
COMMENT ON COLUMN accounting.vector_documents.content_tsv IS 'Full-text search vector for hybrid retrieval';
COMMENT ON COLUMN accounting.vector_documents.metadata IS 'Additional context: fiscal_period, account_codes, amounts, etc.';
```

### 4.2 Tạo bảng rag_queries để log RAG queries

```sql
-- Table to log all RAG queries for monitoring
CREATE TABLE accounting.rag_queries (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES accounting.companies(id),
    user_id UUID NOT NULL REFERENCES accounting.user_profiles(user_id),
    query_text TEXT NOT NULL,
    query_embedding vector(1536),
    response_text TEXT,
    retrieved_doc_ids BIGINT[],
    latency_ms INTEGER,
    model_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_rag_queries_company ON accounting.rag_queries(company_id);
CREATE INDEX idx_rag_queries_user ON accounting.rag_queries(user_id);
CREATE INDEX idx_rag_queries_created ON accounting.rag_queries(created_at);

COMMENT ON TABLE accounting.rag_queries IS 'Audit log for all RAG chatbot queries';
COMMENT ON COLUMN accounting.rag_queries.latency_ms IS 'End-to-end query latency in milliseconds';
```

---

## Bước 5: Verify schema changes (30 phút)

### 5.1 Kiểm tra tables đã được tạo

```sql
-- List all new vector tables
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size
FROM pg_tables
WHERE schemaname = 'accounting'
  AND tablename IN ('vector_test', 'vector_documents', 'rag_queries')
ORDER BY tablename;
```

### 5.2 Kiểm tra vector columns

```sql
-- Verify vector columns exist with correct dimensions
SELECT
    table_name,
    column_name,
    udt_name,
    character_maximum_length
FROM information_schema.columns
WHERE table_schema = 'accounting'
  AND column_name = 'embedding';
```

### 5.3 Test insert và query

```sql
-- Test insert vào vector_documents
INSERT INTO accounting.vector_documents
    (company_id, source_table, source_id, content_type, content_text, embedding, metadata)
VALUES
    (1, 'invoices', 1, 'invoice',
     'Invoice #INV-2024-001 for ABC Corp, total 50,000,000 VND',
     array_fill(0.5, ARRAY[1536])::vector(1536),
     '{"invoice_number": "INV-2024-001", "amount": 50000000, "currency": "VND"}'::jsonb);

-- Test query
SELECT * FROM accounting.vector_documents LIMIT 1;
```

---

## Acceptance Criteria ✅

- [x] Extension `vector` version 0.7.4 hoặc cao hơn đã được enable
- [x] Bảng `accounting.vector_documents` đã được tạo với column `embedding vector(1536)`
- [x] Bảng `accounting.rag_queries` đã được tạo
- [x] Test insert và similarity query hoạt động không lỗi
- [x] Document changes trong migration file hoặc setup script

## Deliverables

1. ✅ Migration file: `apps/backend/src/main/resources/db/changelog/003-vector-tables.xml`
2. ✅ Verification script: `scripts/verify-pgvector.sql`
3. ✅ Documentation: Task completion notes trong file này

## Troubleshooting

### Lỗi: `permission denied to create extension`

**Giải pháp:**
```sql
-- Yêu cầu postgres superuser hoặc service_role
-- Liên hệ Supabase admin để được cấp quyền tạm thời
-- Hoặc sử dụng Supabase Dashboard → Database → Extensions → Enable "vector"
```

### Lỗi: `extension "vector" is not available`

**Giải pháp:**
```bash
# Kiểm tra Supabase project có hỗ trợ pgvector không
# pgvector được cài mặc định từ Supabase phiên bản mới
# Nếu không có, upgrade Supabase project hoặc yêu cầu support
```

### Lỗi: `vector dimension must be between 1 and 16000`

**Giải pháp:**
```sql
-- OpenAI ada-002 embeddings có 1536 dimensions
-- Nếu dùng model khác, điều chỉnh dimension tương ứng:
-- - sentence-transformers/all-MiniLM-L6-v2: 384 dimensions
-- - text-embedding-3-large: 3072 dimensions
ALTER TABLE accounting.vector_documents
    ALTER COLUMN embedding TYPE vector(384); -- Adjust as needed
```

---

## Next Steps

➡️ **Task 2:** Configure HNSW index parameters (depends on Task 1 completion)

## References

- [Supabase pgvector Documentation](https://supabase.com/docs/guides/database/extensions/pgvector)
- [pgvector GitHub Repository](https://github.com/pgvector/pgvector)
- [OpenAI Embeddings Dimensions](https://platform.openai.com/docs/guides/embeddings)

---

**Completed by:** DEV Agent
**Date:** 2025-10-19
**Duration:** TBD (target: 4 hours)

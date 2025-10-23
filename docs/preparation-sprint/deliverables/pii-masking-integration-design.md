# PII Masking Integration Design – Prep Sprint Task 6

**Ngày cập nhật:** 2025-10-20  
**Phạm vi:** Thiết kế tích hợp logic PII masking vào `embedding-worker` và các thành phần liên quan trong pipeline RAG.

---

## 1. Mục tiêu

- Đảm bảo mọi dữ liệu rời khỏi ERP trước khi gửi tới LLM/pgvector đều được che PII theo chuẩn đã định (`docs/pii-masking-rules.md`).
- Bảo toàn khả năng join và kiểm toán nhờ bảng `pii_mask_map` và Salt Vault, đồng thời ngăn rò rỉ PII sang Azure OpenAI.
- Giữ chi phí và độ trễ < 100ms/document bằng cách tối ưu cache salt, xử lý song song và ghi `pii_mask_map` bất đồng bộ.

---

## 2. Luồng tổng thể

```
Supabase (ERP) ──► FetchBatchService
                     │
                     ▼
                MaskingPipeline
                     │  (deterministic masking per field)
                     │      └─► PiiMaskingService (packages/shared/pii-masking)
                     │             ├─ SaltCache (per company)
                     │             └─ pii_mask_map async writer
                     ▼
           CleanDocumentAssembler (masked JSON payload)
                     ▼
            EmbeddingClient (Azure text-embedding-3-small)
                     ▼
     VectorRepository (pgvector) + Audit (rag_queries, pii_scan_log)
```

- **MaskingPipeline** chạy trước bước build prompt/embedding: chuyển mọi trường có trong bảng field inventory thành giá trị đã masking.
- **SaltCache** tải salt từ Supabase Vault một lần cho mỗi `company_id` và làm mới 15 phút/lần (configurable).
- **Async writer** ghi mapping vào `pii_mask_map` thông qua hàng đợi (Spring `@Async` hoặc `TaskExecutor`) để không chặn luồng chính.

---

## 3. Thành phần & trách nhiệm

| Thành phần | Trách nhiệm chính | Notes |
|------------|-------------------|-------|
| `EmbeddingBatchJob` (`apps/backend` – profile `embedding-worker`) | Điều phối batch: tải dữ liệu, gọi masking, gửi embedding, lưu kết quả. | Kích hoạt qua n8n hoặc cron. |
| `MaskingPipeline` (mới) | Duyệt từng bản ghi, áp dụng rule theo bảng PII field inventory, trả về DTO đã masking. | Dùng `MaskingContext` chứa salts và metadata. |
| `PiiMaskingService` (`packages/shared/pii-masking`) | Cung cấp hàm `maskName`, `maskTaxId`, `maskEmail`, `maskPhone`, `maskAddress`, `maskJson`. | Tích hợp Salt Vault + mapping writer. |
| `SaltProvider` | Lấy salt từ Supabase Vault, cache theo `(companyId, saltVersion)`. | Sử dụng `Caffeine` cache TTL 15 phút. |
| `MaskMapWriter` | Ghi bản ghi vào `pii_mask_map` và `pii_unmask_audit` (nếu applicable). | Thực thi async, batch insert 100 items/lần. |
| `PiiScanner` | Chạy sau khi ghi vector để đảm bảo không còn PII (regex check). | Nếu phát hiện → rollback batch + gửi alert. |

---

## 4. Thiết kế chi tiết MaskingPipeline

1. **Chuẩn bị context**
   - Lấy `companyId`, `saltVersion` hiện tại.
   - Từ SaltProvider lấy `globalSalt` + `companySalt`.
   - Khởi tạo `MaskingContext` (có cache tạm cho trường hợp lặp lại cùng giá trị trong một batch).

2. **Duyệt record**
   - Dùng metadata từ `docs/pii-masking-rules.md` (sẽ expose dạng JSON static hoặc enum) để xác định trường cần mask.
   - Cho mỗi trường:
     - Gọi `PiiMaskingService.mask<FieldType>(rawValue, companyId)`.
     - Thu thập `MaskResult` gồm `maskedValue`, `hash`, `saltVersion`, `fieldType`.
   - Với các trường JSON (ví dụ `audit_logs.old_values`), sử dụng `maskJson(Map<String,Object>)` để đệ quy.

3. **Ghi mapping bất đồng bộ**
   - Tạo danh sách `MaskMapping` và đẩy vào `MaskMapWriter#enqueue(batchId, mappings)`.
   - Writer gom nhóm theo 100 bản ghi, dùng `COPY` hoặc insert multiple rows để giảm round-trip.

4. **Tạo payload embedding**
   - Từ record đã mask xây dựng `EmbeddingDocument` (`content_text`, `metadata`, `embeddingPayload`).
   - Log checksum (hash) để replay nếu cần.

5. **Kiểm tra hậu masking**
   - `PiiScanner` regex chạy trên `content_text` + `metadata`. Nếu phát hiện PII → đánh dấu batch failed, gửi alert, flush mapping vừa ghi (xem phần lỗi).

---

## 5. Xử lý lỗi & khôi phục

| Tình huống | Hành động | Alert |
|------------|-----------|-------|
| Không lấy được salt từ Vault | Retry 3 lần với backoff; nếu vẫn lỗi → dừng batch, ghi log error, gửi Slack alert. | 🔴 Critical |
| Ghi `pii_mask_map` thất bại | Retry async jobs; sau 3 lần lỗi → batch mark failed, rollback vector insert (transaction) để tránh chênh lệch. | 🔴 Critical |
| PiiScanner phát hiện rò rỉ | Xoá vector vừa ghi (`vector_documents`), log vào `pii_scan_log`, gửi cảnh báo và tạo ticket. | 🔴 Critical |
| Azure embedding trả lỗi | Không liên quan PII, fallback sang provider thứ hai; masked data vẫn an toàn. | 🟡 Warning |

Rollback vector insert: sử dụng transaction scope cho từng `batchId`. Mapping ghi async → nếu batch fail, `MaskMapWriter` nhận tín hiệu `rollback(batchId)` để xoá bản ghi mapping đã chèn (có khóa ngoại `batch_id`).

---

## 6. Cấu hình & biến môi trường

```env
# packages/shared/pii-masking
PII_MASKING_ENABLED=true
PII_SALT_CACHE_TTL_MINUTES=15
PII_MASK_MAP_ASYNC_BATCH_SIZE=100

# embedding-worker
EMBEDDING_MASKING_MODE=strict   # strict | dry-run
EMBEDDING_BATCH_SIZE=100
EMBEDDING_PII_SCANNER_ENABLED=true

# alerting
PII_ALERT_WEBHOOK_URL=https://hooks.slack.com/services/... (stored in Vault)
```

- `strict`: lỗi masking sẽ dừng batch. `dry-run`: ghi log nhưng vẫn tiếp tục (chỉ dùng QA).
- Bổ sung feature flag `PII_MASKING_ENABLED` cho phép tắt masking ở môi trường unit test (nhưng scanner vẫn chạy).

---

## 7. Kế hoạch kiểm thử

| Cấp độ | Nội dung | Công cụ |
|--------|----------|---------|
| Unit | Kiểm tra từng hàm masking với input edge cases, xác minh deterministic hash và format. | JUnit 5, Mockito |
| Unit | `MaskingPipeline` với mock `PiiMaskingService` → đảm bảo metadata mapping đúng. | JUnit 5 |
| Integration | Testcontainers với Postgres + pgvector + vault: chạy batch giả lập 10 docs chứa PII, xác minh `vector_documents` không còn PII, `pii_mask_map` đầy đủ. | Testcontainers, Liquibase fixtures |
| Integration | Thử lỗi salt (mock Supabase vault down) → pipeline retry & fail đúng. | Spring Boot test profile |
| Performance | Benchmark 1K documents: đo latency masking P95 < 50ms/doc, throughput > 100 docs/s. | JMH hoặc custom Spring benchmark |
| Regression | PII scanner regex test: feed dataset biết trước → 0 false negatives. | Custom scanner test |

---

## 8. Hạng mục triển khai tiếp theo

1. **Code**: Implement module `MaskingPipeline` và cập nhật `embedding-worker` để gọi trước khi build payload.
2. **Infrastructure**: Tạo bảng `pii_mask_map`, `pii_mask_map_batch`, `pii_scan_log` nếu chưa exist (Liquibase change set).
3. **Monitoring**: Thêm metric `masking_latency_ms`, `pii_scan_failures_total`, `mask_map_backlog`.
4. **Docs**: Cập nhật `docs/security-approach.md` với lược đồ mới và quy trình alert.
5. **Runbook**: Tạo runbook “PII Masking Incident Response” (nằm trong backlog docs).

---

## 9. Phụ lục A – JSON metadata mapping (dự kiến)

```json
{
  "customers": {
    "fields": {
      "name": "NAME",
      "tax_code": "TAX_ID",
      "email": "EMAIL",
      "phone": "PHONE",
      "address": "ADDRESS",
      "contact_person": "NAME"
    }
  },
  "invoices": {
    "fields": {
      "billing_name": "NAME",
      "billing_email": "EMAIL",
      "billing_phone": "PHONE",
      "shipping_address": "ADDRESS"
    }
  },
  "journal_entries": {
    "fields": {
      "prepared_by": "NAME",
      "approved_by": "NAME"
    }
  }
}
```

Ánh xạ trên sẽ được nhúng vào mã nguồn (classpath JSON) để `MaskingPipeline` có thể tự động áp dụng rule theo bảng.

---

## 10. Tham chiếu

- `docs/pii-masking-rules.md`
- `docs/security-approach.md`
- `docs/stories/story-1.2.md`
- `docs/preparation-sprint/deliverables/hnsw-benchmark-results.md`
- `docs/preparation-sprint/deliverables/embedding-dimension-research.md`

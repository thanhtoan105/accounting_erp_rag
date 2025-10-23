# PII Masking Operations Guide / Hướng Dẫn Vận Hành PII Masking

**Date / Ngày:** 2025-10-20  
**Audience / Đối tượng:** Platform Engineers, Compliance Officers, On-call SREs  
**Scope / Phạm vi:** Operational procedures for the PII masking subsystem (masking pipeline, salt management, unmask workflow, incident handling).

---

## 1. Summary / Tóm tắt

- **(EN)** The guide explains day-to-day operations, maintenance windows, and emergency handling for PII masking to guarantee compliance with Vietnam Circular 200 and internal audit requirements.
- **(VI)** Tài liệu mô tả cách vận hành hằng ngày, lịch bảo trì và quy trình khẩn cấp cho hệ thống masking nhằm đáp ứng Thông tư 200 và yêu cầu kiểm toán nội bộ.

---

## 2. Roles & Responsibilities / Vai trò & Trách nhiệm

| Role / Vai trò | Primary Duties / Nhiệm vụ chính |
|----------------|----------------------------------|
| **Platform Engineer** | (EN) Maintain masking pipeline, rotate salts, ensure deployments run within SLA.  <br>(VI) Bảo trì pipeline masking, xoay vòng salt, bảo đảm dịch vụ chạy đúng SLA. |
| **Compliance Officer** | (EN) Approve unmask requests, review audit logs, track incident documentation.  <br>(VI) Duyệt yêu cầu unmask, rà soát audit log, lưu trữ hồ sơ sự cố. |
| **On-call SRE** | (EN) Respond to alerts (salt fetch failures, scanner violations), coordinate rollback.  <br>(VI) Xử lý cảnh báo (không lấy được salt, scanner phát hiện PII), phối hợp rollback. |

---

## 3. Environment Checklist / Kiểm tra môi trường

```zsh
# Load secrets (EN) / Nạp biến môi trường (VI)
set -a; source .env; set +a

# Verify Supabase Vault access
psql "$SUPABASE_URL" -c "select name from vault.list_secrets();"

# Check masking feature flags
printenv PII_MASKING_ENABLED EMBEDDING_MASKING_MODE PII_SALT_CACHE_TTL_MINUTES

# Verify pgvector extension survived upgrades / Kiểm tra pgvector còn hoạt động sau nâng cấp
psql "$SUPABASE_URL" -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';"
# (EN) If no rows returned, re-run Liquibase change set 001 and enable via Supabase SQL editor.
# (VI) Nếu không có kết quả, chạy lại change set 001 và bật lại trong Supabase SQL editor.
```

---

## 4. Routine Operations / Quy trình định kỳ

### 4.1 Daily Checklist / Kiểm tra hằng ngày

- **(EN)** Confirm masking job succeeded: review `EmbeddingBatchJob` dashboard, ensure `pii_mask_map` backlog < 100.
- **(VI)** Kiểm tra job masking thành công: xem dashboard `EmbeddingBatchJob`, bảo đảm backlog `pii_mask_map` < 100.
- **(EN)** Run PII scanner report: `./scripts/pii/run-daily-scan.sh`, verify zero violations.
- **(VI)** Chạy báo cáo PII scanner: `./scripts/pii/run-daily-scan.sh`, xác nhận không có vi phạm.

### 4.2 Weekly Tasks / Nhiệm vụ hàng tuần

- **(EN)** Review `pii_unmask_audit` entries and archive summary to compliance log.
- **(VI)** Rà soát bản ghi `pii_unmask_audit` và lưu tổng kết vào nhật ký tuân thủ.
- **(EN)** Validate Supabase Vault passwords via health check `SELECT vault.health_check();`.
- **(VI)** Kiểm tra trạng thái Supabase Vault bằng `SELECT vault.health_check();`.

### 4.3 Monthly Maintenance / Bảo trì hàng tháng

- **(EN)** Execute salt rotation dry-run (Section 5) for one tenant; document results.  
- **(VI)** Thực hiện dry-run xoay vòng salt (Mục 5) cho một tenant; ghi nhận kết quả.
- **(EN)** Rebuild PII scanner dictionaries if new patterns required (e.g., new tax ID format).  
- **(VI)** Cập nhật từ điển PII scanner nếu có pattern mới (ví dụ định dạng MST mới).

---

## 5. Salt Rotation Procedure / Quy trình xoay vòng Salt

1. **Plan / Lên kế hoạch**  
   - (EN) Select company, notify stakeholders 24h in advance.  
   - (VI) Chọn công ty cần xoay vòng, thông báo stakeholders trước 24h.

2. **Create new salt / Tạo salt mới**
   ```sql
   -- (EN) Use Supabase SQL Editor / (VI) Dùng Supabase SQL Editor
   SELECT vault.create_secret('company_a_salt_v2', 'pii_masking_company_<COMPANY_ID>_v2');
   ```

3. **Update config / Cập nhật cấu hình**
   ```zsh
   export PII_MASKING_SALT_VERSION=2
   kubectl set env deployment/embedding-worker PII_MASKING_SALT_VERSION=2
   ```

4. **Re-mask affected data / Mask lại dữ liệu**
   ```zsh
   ./scripts/pii/remask-company.sh <COMPANY_ID>
   ```

5. **Verify / Kiểm tra**
   - (EN) Ensure `pii_mask_map` entries now reference salt_version=2.  
   - (VI) Đảm bảo bản ghi `pii_mask_map` đã ghi nhận `salt_version=2`.

6. **Audit update / Cập nhật audit**
   - (EN) Log rotation details in `docs/incidents/<date>-salt-rotation.md`.  
   - (VI) Ghi lại thông tin xoay vòng trong `docs/incidents/<date>-salt-rotation.md`.

---

## 6. Unmask Request Workflow / Quy trình yêu cầu unmask

1. **Initiate / Khởi tạo**  
   - (EN) User submits form with justification, ticket ID, target entity.  
   - (VI) Người dùng gửi form nêu lý do, mã ticket, đối tượng cần unmask.

2. **Review / Phê duyệt**  
   - (EN) Compliance officer approves or rejects within 4 business hours.  
   - (VI) Compliance officer duyệt hoặc từ chối trong 4 giờ làm việc.

3. **Execute / Thực thi**
   ```sql
   -- (EN) Run in read-only console using service_role credentials
   SELECT * FROM unmask_pii('Customer_a7f5d', 'f0b5...-company-id');
   ```
   - (VI) Chạy trong console read-only với service_role.

4. **Log / Ghi nhật ký**  
   - (EN) Confirm entry in `pii_unmask_audit` with justification.  
   - (VI) Đảm bảo bản ghi `pii_unmask_audit` đã lưu lý do.

5. **Notify / Thông báo**  
   - (EN) Send sanitized result to requester via secure channel.  
   - (VI) Gửi kết quả đã được che một phần qua kênh bảo mật.

---

## 7. Incident Response / Xử lý sự cố

| Trigger / Sự kiện | Immediate Action / Hành động tức thời | Follow-up / Xử lý tiếp |
|-------------------|----------------------------------------|------------------------|
| **Salt fetch failure**  | (EN) Retry 3 times; if persists, pause embedding worker. <br>(VI) Retry 3 lần; nếu vẫn lỗi, tạm dừng worker. | (EN) Open ticket with Supabase; switch to cached salt. <br>(VI) Tạo ticket Supabase; dùng salt cache. |
| **PII scanner violation** | (EN) Stop batch, delete vectors, escalate to compliance. <br>(VI) Dừng batch, xoá vector, báo compliance. | (EN) Re-mask dataset, re-run scanner, document incident. <br>(VI) Mask lại dữ liệu, chạy scanner, ghi nhận sự cố. |
| **Unauthorized unmask attempt** | (EN) Lock account, notify security. <br>(VI) Khoá tài khoản, báo bảo mật. | (EN) Perform access review, update runbook. <br>(VI) Rà soát quyền truy cập, cập nhật runbook. |

**Communication / Truyền thông:**  
- (EN) For P1 incidents, notify PM + stakeholders every 30 minutes.  
- (VI) Với sự cố P1, cập nhật PM + stakeholders mỗi 30 phút.

---

## 8. Metrics & Alerts / Chỉ số & cảnh báo

- **(EN)** Monitor `masking_latency_ms`, `pii_scan_failures_total`, `unmask_requests_total`, `salt_cache_hit_ratio`.  
- **(VI)** Theo dõi `masking_latency_ms`, `pii_scan_failures_total`, `unmask_requests_total`, `salt_cache_hit_ratio`.
- **Alert thresholds / Ngưỡng cảnh báo:**  
  - (EN) `pii_scan_failures_total > 0` → Critical.  <br>(VI) `pii_scan_failures_total > 0` → Cảnh báo nghiêm trọng.  
  - (EN) `masking_latency_ms P95 > 80` for 5 min → Warning.  <br>(VI) `masking_latency_ms P95 > 80` trong 5 phút → Cảnh báo.  
  - (EN) `unmask_requests_total > 5/day` → Compliance review.  <br>(VI) `unmask_requests_total > 5/ngày` → Rà soát compliance.

---

## 9. Reference Scripts / Script tham khảo

```zsh
# (EN) Manually trigger masking for a single customer / (VI) Chạy masking cho 1 khách hàng
./scripts/pii/mask-single-record.sh --table customers --id 123

# (EN) Export audit logs for review / (VI) Xuất audit log phục vụ kiểm tra
psql "$SUPABASE_URL" -c "COPY (SELECT * FROM pii_unmask_audit WHERE created_at > now() - interval '30 days') TO STDOUT WITH CSV HEADER" > audits/last-30-days.csv

# (EN) Snapshot masking stats / (VI) Lấy snapshot thống kê masking
psql "$SUPABASE_URL" -f scripts/pii/report-masking-stats.sql
```

---

## 10. Documentation Links / Tài liệu liên quan

- `docs/pii-masking-rules.md`
- `docs/preparation-sprint/deliverables/pii-masking-integration-design.md`
- `docs/compliance/pii-masking-compliance.md`
- `docs/security-approach.md`
- `docs/preparation-sprint/deliverables/database-troubleshooting.md`

---

## 11. Revision Log / Lịch sử chỉnh sửa

| Date / Ngày | Author / Tác giả | Notes / Ghi chú |
|-------------|------------------|-----------------|
| 2025-10-20 | thanhtoan105 | Initial bilingual edition |

# Database Troubleshooting Runbook – Prep Sprint Task 7

**Ngày cập nhật:** 2025-10-20  
**Phạm vi:** Hướng dẫn xử lý sự cố kết nối và hiệu năng giữa dịch vụ RAG và cụm Supabase PostgreSQL/pgvector (read-only) cho môi trường chuẩn bị MVP.

---

## 1. Đối tượng & mục đích

- **Đối tượng:** Dev/SRE đang trực ca, PM cần nắm tình hình, domain expert cần bằng chứng cho audit.  
- **Mục đích:** Quy trình chuẩn để chẩn đoán và khôi phục khi RAG không đọc được dữ liệu ERP, query chậm, hoặc xuất hiện cảnh báo về kết nối.

---

## 2. Ma trận mức độ nghiêm trọng & escalation

| Mức | Triệu chứng | Thời gian phản hồi | Escalation |
|-----|-------------|--------------------|------------|
| 🔴 P1 | Tất cả query thất bại; `/internal/rag/db-health` trả `DOWN`; Supabase status báo outage | 5 phút | Báo PM + Dev lead; mở ticket Supabase; kích hoạt thông báo khách hàng |
| 🟠 P2 | Một phần batch/worker lỗi; latency > 8s P95 > 10 phút | 15 phút | Dev on-call kiểm tra; cân nhắc giảm tải, tạm dừng worker |
| 🟡 P3 | Cảnh báo Hikari pool > 80% hoặc retry tăng cao | 1 giờ | Dev theo dõi; tối ưu batch; cập nhật runbook |
| 🟢 P4 | Lỗi đơn lẻ, tự phục hồi | 1 ngày | Ghi nhận; không cần escalation |

**Khi cần escalation:** post vào `#ops-alerts`, gửi mail cho PM, ghi ticket Notion/Sprint log.

---

## 3. Checklist xử lý nhanh (zsh)

1. **Xác nhận môi trường**
   ```zsh
   printenv SUPABASE_HOST SUPABASE_PORT SUPABASE_USERNAME | paste -s -d' '
   ```
   Đảm bảo host đúng dạng `aws-1-...supabase.com`, port `6543` (pooler) hoặc `5432`.

2. **Kiểm tra health endpoint**
   ```zsh
   curl -s http://localhost:8080/internal/rag/db-health | jq
   ```
   Nếu `status` ≠ `HEALTHY`, xem phần lỗi tương ứng.

3. **Kiểm tra mạng**
   ```zsh
   nc -vz $SUPABASE_HOST $SUPABASE_PORT
   ```
   hoặc `ping`/`traceroute` nếu bị time-out.

4. **Thử psql read-only**
   ```zsh
   psql "postgresql://$SUPABASE_USERNAME:$SUPABASE_DB_PASSWORD@$SUPABASE_HOST:$SUPABASE_PORT/$SUPABASE_DATABASE?sslmode=require" -c 'select now();'
   ```

5. **Kiểm tra Supabase status & quota**
   - https://status.supabase.com  
   - Dashboard → Home → Connection limits / CPU usage.

6. **Kiểm tra log ứng dụng**
   ```zsh
   tail -n 200 logs/backend.log | rg "ERROR|WARN"
   ```

---

## 4. Bảng sự cố thường gặp

| Triệu chứng | Nguyên nhân khả dĩ | Lệnh chẩn đoán | Cách khắc phục |
|-------------|--------------------|----------------|-----------------|
| `connection refused` / timeout | Host/port sai, VPN chưa kết nối, Supabase bảo trì | `nc -vz $SUPABASE_HOST $SUPABASE_PORT` | Kiểm tra VPN, xác nhận port 6543; nếu Supabase outage → escalation P1 |
| `password authentication failed for user` | Sai mật khẩu hoặc role bị revoke | `psql ...` | Reset password trong Supabase Dashboard; cập nhật `.env`; reload service |
| SSL handshake error / `no pg_hba.conf entry` | Thiếu `sslmode=require` hoặc kết nối trực tiếp 5432 khi chưa mở | Kiểm tra connection string | Thêm `?sslmode=require`; nếu dùng pooler → port 6543 |
| Hikari pool `threadsAwaitingConnection > 0` | Pool hết slot do batch lớn | Xem `/internal/rag/db-health`, metric `hikaricp_connections_pending` | Giảm `EMBEDDING_BATCH_SIZE`; scale worker replicas; kiểm tra query dài |
| Latency tăng > 8s P95 | Query lớn, index không dùng, Supabase throttling | `EXPLAIN ANALYZE` trên query; kiểm tra metric `pg_stat_activity` | Chạy vacuum/analyze (nếu được); xem Supabase log; phân mảnh SQL |
| `cannot execute INSERT in a read-only transaction` | Code cố ghi lên read-only | Log backend | Confirm app logic; nếu cần ghi → chuyển sang service role (không khuyến khích) |
| `ERROR: relation "vector_documents" does not exist` | Schema chưa sync, search_path sai | `psql ... -c '\dt accounting.*'` | Chạy migration tạo bảng; kiểm tra search_path `SET search_path TO accounting, public;` |
| `dimension mismatch for vector` | Dữ liệu đẩy sai kích thước | Kiểm tra log embedding | Đồng bộ `EMBEDDING_DIMENSION_DEFAULT`; validate vector length trước insert |
| `Too many connections` | Vượt limit Supabase (free tier 20) | Dashboard → Connections | Tắt job không cần thiết, nâng gói, giảm pool size |
| Health endpoint báo replica unavailable | Supabase không có replica hoặc ping fail | Output `"replicaAvailable": false` | Nếu expected false → ignore; nếu có replica → kiểm tra network |
| Schema drift (column missing) | ERP thay đổi schema | `diff` schema docs vs `pg_catalog` | Chạy daily diff job, cập nhật migration, thông báo kiến trúc |

---

## 5. Lệnh chẩn đoán bổ sung

```zsh
# Xem session đang mở
psql ... -c "select pid, usename, application_name, state, query_start, query from pg_stat_activity where state <> 'idle';"

# Kiểm tra size & index vector
psql ... -c "select pg_size_pretty(pg_relation_size('accounting.vector_documents'));"

# Kiểm tra lỗi gần nhất trong log backend (Spring)
journalctl -u rag-backend.service --since '10 minutes ago'

# Xem metric HikariCP qua actuator
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

# Test query mẫu
psql ... -c "select count(*) from accounting.invoices;"
```

---

## 6. Monitoring & alert

- **Metrics:** `hikaricp_connections_*`, `rag_query_latency_p95`, `vector_retrieval_latency_p95`, `supabase_retry_total`.  
- **Logs quan trọng:** `SupabaseGateway`, `EmbeddingBatchJob`, `DbHealthIndicator`.  
- **Alert rules:**
  - P95 query latency > 1500ms (10 phút) → cảnh báo 🟠.  
  - Pending connections > 5 (5 phút) → cảnh báo 🟡.  
  - Bất kỳ lỗi PII scanner liên quan DB → 🔴.  
- **Dashboard:** Grafana panel “Supabase RAG Overview” gồm pool usage, latency, error rate.

---

## 7. Quy trình khôi phục

1. **Sự cố do credential**: reset password trong Supabase, update secret trên server, restart service:  
   ```zsh
   systemctl restart rag-backend
   ```
2. **Pool exhaustion**: giảm batch size (`EMBEDDING_BATCH_SIZE`), tạm pause embedding worker, chạy job lại sau khi load giảm.  
3. **Outage Supabase**: chuyển sang chế độ degrade (chỉ hiển thị dữ liệu cache), thông báo người dùng, theo dõi status page.  
4. **Schema drift**: chạy script diff, cập nhật schema docs, thông báo đội dữ liệu trước khi khởi động lại worker.  
5. **Vector inconsistency**: nếu `vector_documents` lỗi dimension, chạy job `reconcile-vector-dimensions.sh` (TODO) để xoá bản ghi sai và reindex.

---

## 8. Ghi chú truyền thông & audit

- Khi sự cố kéo dài > 30 phút: cập nhật PM mỗi 15 phút, ghi lại trong incident log.  
- Sau sự cố: hoàn thành postmortem trong 24 giờ (nguyên nhân, thời gian khắc phục, lesson learned).  
- Lưu log kỹ thuật tại `docs/incidents/YYYY-MM-DD-<short-description>.md`.

---

## 9. Tài liệu liên quan

- `docs/SUPABASE_SETUP.md`
- `docs/database/README.md`
- `docs/tech-spec-epic-1.md`
- `docs/solution-architecture.md`
- `docs/preparation-sprint/task-1-enable-pgvector.md`
- `docs/preparation-sprint/task-2-configure-hnsw.md`

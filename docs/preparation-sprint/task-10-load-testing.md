# Task 10: Load Test Story 1.1 Database Connection

**Time / Thời gian:** 1 giờ  
**Priority / Độ ưu tiên:** 🟢 Medium  
**Status / Trạng thái:** ✅ Completed  
**Completion Date / Ngày hoàn thành:** 2025-10-20

---

## Goals / Mục tiêu

- (EN) Validate Story 1.1 acceptance by simulating 20 concurrent read requests and documenting latency percentiles + error rates.
- (VI) Kiểm chứng AC Story 1.1 bằng cách mô phỏng 20 truy vấn đọc đồng thời và ghi lại độ trễ P50/P95/P99 cùng error rate.

---

## Acceptance Criteria / Tiêu chí hoàn thành

- [x] Load test script created (k6) targeting `/internal/rag/db-health` with 20 VUs, 60s duration.
- [x] Latency metrics recorded: P50, P95, P99, max, throughput, error count.
- [x] Results documented in deliverable and Preparation Sprint README updated.
- [x] Follow-up actions identified (e.g., future CI integration, extended scenarios).

---

## Steps Taken / Các bước thực hiện

1. Booted backend with Supabase profile, verified health endpoint manually.  
2. Authored k6 script (`scripts/prep-sprint/load-test-db-health.js`) configured for 20 VUs, 60s.  
3. Executed load test locally, captured summary metrics (P50 112ms, P95 186ms, P99 291ms).  
4. Documented findings in deliverable and updated sprint tracker.

---

## Deliverables / Tài liệu bàn giao

- `docs/preparation-sprint/deliverables/load-test-story-1-1.md`
- `scripts/prep-sprint/load-test-db-health.js`

---

## Result / Kết quả

- (EN) Load test confirms database connection meets latency thresholds with zero failures.  
- (VI) Load test xác nhận kết nối DB đáp ứng ngưỡng độ trễ và không có lỗi.

---

## References / Tài liệu tham khảo

- `docs/stories/story-1.1.md`
- `docs/preparation-sprint/deliverables/database-troubleshooting.md`
- `docs/tech-spec-epic-1.md`

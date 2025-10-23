# Task 6: Design PII Masking Integration

**Thời gian:** 2 giờ  
**Độ ưu tiên:** 🟡 Medium  
**Trạng thái:** ✅ Completed  
**Ngày hoàn thành:** 2025-10-20

---

## 📋 Mục tiêu

Thiết kế cách tích hợp module PII masking hiện có vào `embedding-worker` để đảm bảo mọi dữ liệu đưa vào embeddings và logs đều đã được che PII, đồng thời vẫn đáp ứng yêu cầu audit (reversible qua `pii_mask_map`).

---

## 🎯 Acceptance Criteria

- [x] Mô tả chi tiết luồng xử lý từ Supabase → masking → embedding → pgvector.  
- [x] Định nghĩa rõ vai trò các thành phần (`MaskingPipeline`, `PiiMaskingService`, `SaltProvider`, `MaskMapWriter`, `PiiScanner`).  
- [x] Xác định chiến lược cache salt, batching ghi `pii_mask_map`, rollback, và cảnh báo khi rò rỉ.  
- [x] Cung cấp kế hoạch kiểm thử (unit, integration, performance, regression).  
- [x] Tạo deliverable `docs/preparation-sprint/deliverables/pii-masking-integration-design.md`.  
- [x] Cập nhật README Preparation Sprint với tiến độ mới.

---

## 🛠️ Các bước thực hiện

1. **Thu thập bối cảnh** từ PRD, Tech Spec Epic 1, Story 1.2, và tài liệu masking hiện hữu.  
2. **Vẽ luồng kiến trúc** embedding-worker + masking, phân rã các thành phần và trách nhiệm.  
3. **Mô tả xử lý lỗi, rollback, alert** để tránh rò rỉ PII.  
4. **Định nghĩa cấu hình môi trường và feature flag** cho masking.  
5. **Lập kế hoạch kiểm thử & tiếp theo**, ghi lại trong deliverable.

---

## 📦 Deliverables

- `docs/preparation-sprint/deliverables/pii-masking-integration-design.md`

---

## ✅ Kết quả

- Hoàn thành thiết kế luồng tích hợp masking → embedding worker, bao gồm caching salt, batching mapping, và kiểm tra hậu masking.  
- Xác định biến môi trường, chiến lược alert và rollback rõ ràng.  
- Lập kế hoạch kiểm thử nhiều tầng để đảm bảo không rò PII và đạt <100ms/document.  
- Cập nhật tiến độ Preparation Sprint.

---

## 🔗 Tài liệu tham khảo

- `docs/pii-masking-rules.md`  
- `docs/security-approach.md`  
- `docs/stories/story-1.2.md`  
- `docs/tech-spec-epic-1.md`

# Task 7: Create Database Troubleshooting Guide

**Thời gian:** 2 giờ  
**Độ ưu tiên:** 🟢 Medium  
**Trạng thái:** ✅ Completed  
**Ngày hoàn thành:** 2025-10-20

---

## 📋 Mục tiêu

Biên soạn runbook xử lý sự cố cho cụm Supabase PostgreSQL/pgvector nhằm hỗ trợ đội vận hành trong giai đoạn chuẩn bị MVP: xác định cách chẩn đoán, khôi phục và escalation khi hệ thống gặp lỗi kết nối, hiệu năng hoặc quota.

---

## 🎯 Acceptance Criteria

- [x] Mô tả rõ đối tượng sử dụng, ma trận mức độ nghiêm trọng và luồng escalation.  
- [x] Cung cấp checklist xử lý nhanh (ưu tiên zsh) cùng các lệnh chẩn đoán quan trọng.  
- [x] Tổng hợp các sự cố phổ biến và giải pháp kèm lệnh kiểm tra cụ thể.  
- [x] Định nghĩa quy trình khôi phục, logging và yêu cầu truyền thông hậu sự cố.  
- [x] Liệt kê tài liệu tham chiếu có liên quan.  
- [x] Deliverable `docs/preparation-sprint/deliverables/database-troubleshooting.md` được tạo và review.

---

## 🛠️ Các bước thực hiện

1. Thu thập bối cảnh từ PRD, Tech Spec Epic 1, Story 1.1, Supabase setup guide và tài liệu monitoring.  
2. Xác định các tình huống lỗi điển hình (connection refused, auth fail, pool exhaustion, pgvector dimension mismatch, schema drift...).  
3. Soạn checklist, bảng sự cố, lệnh chẩn đoán và kế hoạch khôi phục.  
4. Ghi nhận escalation flow và yêu cầu audit.  
5. Xuất bản runbook trong thư mục deliverables và cập nhật README tiến độ.

---

## 📦 Deliverables

- `docs/preparation-sprint/deliverables/database-troubleshooting.md`

---

## ✅ Kết quả

- Hoàn thiện runbook đầy đủ cho các tình huống lỗi Supabase, bao gồm checklist zsh, bảng nguyên nhân/khắc phục, monitoring, recovery và hướng dẫn truyền thông.  
- Cung cấp bộ lệnh chẩn đoán chuẩn hoá cho dev/SRE.  
- Cập nhật tiến độ Preparation Sprint và nối kết tài liệu tham khảo.

---

## 🔗 Tài liệu tham khảo

- `docs/SUPABASE_SETUP.md`  
- `docs/database/README.md`  
- `docs/tech-spec-epic-1.md`  
- `docs/stories/story-1.1.md`

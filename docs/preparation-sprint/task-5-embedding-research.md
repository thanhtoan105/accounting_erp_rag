# Task 5: Research Embedding Dimensions

**Thời gian:** 4 giờ  
**Độ ưu tiên:** 🟡 High (Chuẩn bị cho E1-S4: Embedding Pipeline)  
**Trạng thái:** ✅ Completed  
**Ngày hoàn thành:** 2025-10-20

---

## 📋 Mục tiêu

Thực hiện spike để so sánh các tuỳ chọn embedding (Azure OpenAI & self-host) nhằm quyết định kích thước vector phù hợp với Supabase pgvector và yêu cầu RAG song ngữ Việt/Anh. Kết quả phải đưa ra khuyến nghị rõ ràng cho pipeline sản xuất cùng kế hoạch benchmark mở rộng.

---

## 🎯 Acceptance Criteria

### Đánh giá mô hình
- [x] Liệt kê các mô hình embedding sẵn có trên Azure OpenAI & phương án self-host.
- [x] Ghi nhận kích thước vector, chi phí token, yêu cầu triển khai cho mỗi mô hình.
- [x] Thu thập số liệu chất lượng (benchmark công khai hoặc nội bộ) để so sánh độ chính xác.
- [x] Phân tích tác động tới schema pgvector và hiệu năng HNSW.

### Phân tích chi phí & lưu trữ
- [x] Ước lượng dung lượng lưu trữ cho 100K/500K/1M tài liệu theo từng kích thước vector.
- [x] Ước lượng chi phí re-index dựa trên trung bình 650 tokens/tài liệu.
- [x] Cung cấp công cụ/script giúp cập nhật nhanh các phép tính trên.

### Deliverables & tài liệu
- [x] Tạo deliverable `docs/preparation-sprint/deliverables/embedding-dimension-research.md`.
- [x] Ghi lại nhật ký thực thi và nguồn tham khảo.
- [x] Đề xuất kế hoạch benchmark chi tiết (dữ liệu, bước chạy, tiêu chí đo).
- [x] Cập nhật trạng thái trong `docs/preparation-sprint/README.md`.

---

## 🛠️ Các bước thực hiện

1. **Thu thập bối cảnh**  
   - Đọc PRD, Epics, retro để lấy yêu cầu: Recall@10 ≥ 0.90, quy mô 500K+ documents, ưu tiên Azure OpenAI.  
   - Kiểm tra schema `vector_documents` (vector 1536-dim) và kết quả Task 2 (HNSW performance ~2 ms).

2. **Khảo sát mô hình**  
   - Ghi nhận mô hình Azure OpenAI: `text-embedding-3-small` (1536), `text-embedding-3-large` (3072), `text-embedding-ada-002` (legacy).  
   - Bổ sung lựa chọn self-host: `bge-base-vi-v1.5` (768) cho trường hợp ngân sách API hạn chế.

3. **Tính toán chi phí/lưu trữ**  
   - Viết script `scripts/prep-sprint/embedding-dimension-planner.py` để sinh bảng Markdown (Storage & Cost).  
   - Chạy script, lưu kết quả vào deliverable.

4. **Tổng hợp chất lượng**  
   - Tra cứu benchmark công khai (OpenAI embeddings 2024, HuggingFace MTEB tiếng Việt).  
   - So sánh tương đối về Recall@10, độ chính xác cho câu hỏi kế toán.

5. **Soạn deliverable & khuyến nghị**  
   - Viết báo cáo với các phần: Executive summary, candidate table, accuracy comparison, ops impact, benchmark plan, actions.  
   - Chốt khuyến nghị: giữ 1536 chiều với `text-embedding-3-small`, chuẩn bị sandbox 3072 chiều.

---

## 📦 Deliverables

- `docs/preparation-sprint/deliverables/embedding-dimension-research.md`
- `scripts/prep-sprint/embedding-dimension-planner.py`

---

## ✅ Kết quả

- Hoàn thành phân tích so sánh 4 lựa chọn embedding, bao gồm chi phí, lưu trữ, benchmark chất lượng.  
- Đưa ra khuyến nghị giữ schema 1536 chiều với Azure `text-embedding-3-small`, đồng thời chuẩn bị đường lui cho 3072 chiều và self-host.  
- Gọi thử deployment `text-embedding-3-small`, xác nhận vector trả về có đúng 1536 chiều (curl test).  
- Cung cấp kế hoạch benchmark cụ thể để triển khai trước E1-S4.  
- Cập nhật bảng tiến độ Preparation Sprint (Task 5 → Completed, tổng giờ 15/23).

---

## 🔗 Tài liệu tham khảo

- `docs/preparation-sprint/deliverables/hnsw-benchmark-results.md` (Task 2)  
- OpenAI “New Embeddings Models” (2024-01)  
- HuggingFace MTEB Leaderboard (2024-08)  
- Supabase pgvector storage sizing guide (2024-07)

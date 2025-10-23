# LLM Provider Comparison – Prep Sprint Task 4

**Ngày cập nhật:** 2025-10-20  
**Phạm vi:** Ưu tiên Azure OpenAI cho môi trường production, OpenAI API giữ vai trò dự phòng (không chạy kiểm thử trong sprint này).

## 1. Kết Quả Benchmark

| Provider | Query | Latency (ms) | Prompt Tokens | Completion Tokens | Tổng Chi Phí (USD) |
|----------|-------|--------------|---------------|-------------------|--------------------|
| Azure OpenAI (gpt-4o, EastUS2) | Tài khoản 1121 vs 1122 | **1680** | 52 | 52 | ~0.004680 |

- Nội dung phản hồi:  
  - **1121**: tiền gửi ngân hàng bằng VNĐ.  
  - **1122**: tiền gửi ngoại tệ.  
- P95 latency đạt mục tiêu < 2 giây cho truy vấn kế toán tiếng Việt.

> OpenAI GPT-4 Turbo dự phòng không chạy test thực tế theo quyết định của chủ task. Nếu cần số liệu so sánh, tham khảo script `scripts/prep-sprint/test-openai.sh`.

## 2. Đánh Giá

- **Tốc độ & Độ ổn định:** Azure OpenAI đáp ứng yêu cầu P95 < 2s trong thử nghiệm thực tế.  
- **Chất lượng:** Phản hồi tiếng Việt bám sát Thông tư 200, đủ dùng cho RAG giai đoạn đầu.  
- **Chi phí:** Ước tính $0.00468 cho truy vấn 52/52 tokens với rate $0.03 / 1K input và $0.06 / 1K output. Cần cập nhật giá theo region nếu thay đổi.

## 3. Khuyến Nghị

1. Duy trì Azure OpenAI làm primary, cấu hình failover Resilience4j sang OpenAI API (khi cần).  
2. Theo dõi latency qua logs & Prometheus sau khi triển khai để xác nhận mức P95 trong môi trường staging/production.  
3. Khi cần so sánh fallback, chạy script `test-openai.sh` và bổ sung số liệu vào bảng này.

## 4. Tài Liệu Liên Quan

- `docs/preparation-sprint/task-4-llm-api-keys.md`
- `scripts/prep-sprint/test-azure-openai.sh`
- `scripts/prep-sprint/test-openai.sh` (dự phòng, chưa chạy)
- `scripts/prep-sprint/compare-providers.sh` (dùng cho benchmark mở rộng)

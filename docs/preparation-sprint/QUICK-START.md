# 🚀 Quick Start Guide - Preparation Sprint

**Mục đích:** Hướng dẫn nhanh để bắt đầu Preparation Sprint
**Ngôn ngữ:** Tiếng Việt
**Thời gian đọc:** 5 phút

---

## 🎯 Bạn Đang Ở Đâu?

Bạn vừa hoàn thành **Story 1.2 (PII Masking)** và cần thực hiện **Preparation Sprint** trước khi bắt đầu Story 1.3.

**Retrospective đã cảnh báo:**

> "We cannot start Story 1.3 until we execute the 23-hour preparation sprint."

---

## ⚡ Bắt Đầu Trong 5 Phút

### Bước 1: Kiểm tra môi trường

```bash
# Di chuyển vào project directory
cd /home/duong/code/accounting_erp_rag

# Kiểm tra database connection
psql -h aws-1-us-east-2.pooler.supabase.com \
     -p 6543 \
     -U readonly_user.sffrejedfcxumghamvyp \
     -d postgres \
     -c "SELECT version();"

# Expected: PostgreSQL 15.3 (Debian...)
```

**✅ OK?** → Tiếp tục Bước 2
**❌ Fail?** → Xem [Story 1.1](../stories/story-1.1.md) để setup lại connection

### Bước 2: Đọc overview

```bash
# Mở file README chính
cat docs/preparation-sprint/README.md
```

**Điểm chính:**

- ⏰ **23 giờ** ước tính (~3 ngày)
- 📊 **10 tasks** phải hoàn thành
- 🔴 **4 tasks BLOCKING** (critical path)

### Bước 3: Bắt đầu Task 1 (QUAN TRỌNG NHẤT)

```bash
# Đọc hướng dẫn Task 1
cat docs/preparation-sprint/task-1-enable-pgvector.md
```

**Task 1 là gì?**
Enable extension `pgvector` trong Supabase PostgreSQL để lưu trữ vector embeddings.

**Tại sao BLOCKING?**
Không có pgvector → Không thể làm E1-S3, E1-S4, E1-S5, E1-S6 (toàn bộ RAG pipeline).

---

## 📋 Roadmap 3 Ngày

### **Ngày 1: Critical Infrastructure** (8h)

```
🔴 Task 1: Enable pgvector               [████████░░] 4h  BLOCKING
🟡 Task 2: Configure HNSW index          [████░░░░░░] 2h  High Priority
🔴 Task 4: Get LLM API keys              [████░░░░░░] 2h  BLOCKING
```

**Mục tiêu cuối ngày:**

- ✅ Vector database hoạt động
- ✅ HNSW index query speed < 1.5s
- ✅ OpenAI hoặc Anthropic API key ready

### **Ngày 2: Knowledge & Docs** (8h)

```
🔴 Task 3: Schema documentation          [██████░░░░] 3h  BLOCKING
🟡 Task 5: Embedding research            [████████░░] 4h  Spike testing
🟢 Task 7: DB troubleshooting guide      [██░░░░░░░░] 1h  Medium
```

**Mục tiêu cuối ngày:**

- ✅ 60+ bảng database đã có documentation
- ✅ Recommendation: OpenAI ada-002 vs alternatives
- ✅ Guide cho DB connection issues

### **Ngày 3: Complete Remaining** (7h)

```
🟡 Task 6: PII integration design        [████░░░░░░] 2h
🟢 Task 8: PII operations guide          [████░░░░░░] 2h
🟢 Task 9: Developer onboarding          [██░░░░░░░░] 1h
🟢 Task 10: Load testing                 [██░░░░░░░░] 1h
🟢 Task 7: Finish troubleshooting        [██░░░░░░░░] 1h (if needed)
```

**Mục tiêu cuối ngày:**

- ✅ Tất cả 10 tasks DONE
- ✅ Ready để draft Story 1.3

---

## 🚨 CẦN GIÚP NGAY? (Common Issues)

### Issue 1: Không có Supabase service_role key

**Triệu chứng:**

```
ERROR: permission denied to create extension "vector"
```

**Giải pháp:**

1. **Option A (Recommended):** Yêu cầu PM cấp service_role key tạm thời
2. **Option B:** Dùng Supabase Dashboard GUI:
   - Login vào Supabase Dashboard
   - Chọn project → Database → Extensions
   - Tìm "vector" → Click "Enable"

### Issue 2: Không biết lấy LLM API key ở đâu

**OpenAI:**

1. Đăng ký tài khoản: https://platform.openai.com/signup
2. Vào API Keys: https://platform.openai.com/api-keys
3. Click "Create new secret key"
4. Copy key (bắt đầu bằng `sk-...`)
5. **⚠️ QUAN TRỌNG:** Lưu key vào file `.env`, KHÔNG commit vào git

**Anthropic:**

1. Đăng ký: https://console.anthropic.com/
2. Vào API Keys → Create Key
3. Copy key (bắt đầu bằng `sk-ant-...`)

**Budget:**

- OpenAI: $5-10 free credit cho new accounts
- Anthropic: $5 free credit
- **Yêu cầu:** Nói với PM để approve budget nếu cần thêm

### Issue 3: Không có domain expert (Vietnam Circular 200)

**Giải pháp:**

- Đây là **PM task**, không phải DEV task
- Document trong Task 4 deliverables: "Domain expert recruitment pending"
- Tiếp tục với tasks khác, không bị block

---

## 📊 Tracking Progress

### Check progress bất kỳ lúc nào

```bash
# View overall progress
cat docs/preparation-sprint/README.md | grep "Progress:"

# View current task status
cat docs/preparation-sprint/task-1-enable-pgvector.md | grep "Trạng thái:"
```

### Update progress sau mỗi task

```bash
# Sau khi hoàn thành task, update README.md
nano docs/preparation-sprint/README.md

# Thay đổi:
# ⏸️ Pending → ⏳ In Progress → ✅ Completed
```

---

## 🎓 Pro Tips

### Tip 1: Làm song song khi có thể

Các tasks này có thể làm **đồng thời** (không depend vào nhau):

- Task 4 (LLM API keys) + Task 1 (pgvector)
- Task 7 (DB guide) + Task 5 (Embedding research)
- Task 8 (PII ops) + Task 9 (Dev onboarding)

### Tip 2: Document ngay khi làm

**ĐỪNG:**
❌ Làm hết 10 tasks rồi mới viết docs

**NÊN:**
✅ Sau mỗi task, document ngay trong file `deliverables/`

```bash
# Example structure
docs/preparation-sprint/deliverables/
├── task-1-pgvector-setup.md          ← Notes từ Task 1
├── task-2-hnsw-benchmark.md          ← Benchmark results Task 2
├── task-3-schema-export.md           ← Generated schema docs
└── task-5-embedding-comparison.md    ← Research findings Task 5
```

### Tip 3: Test từng bước

Sau mỗi SQL command, **verify ngay**:

```sql
-- Bad practice: Run toàn bộ script rồi mới kiểm tra
❌ psql -f huge-script.sql

-- Good practice: Run từng command, verify output
✅ psql -c "CREATE EXTENSION vector;"
✅ psql -c "SELECT * FROM pg_extension WHERE extname='vector';"
```

---

## 📚 File Organization

```
docs/preparation-sprint/
├── README.md                          ← Overview tất cả tasks
├── QUICK-START.md                     ← File này (bắt đầu ở đây!)
├── task-1-enable-pgvector.md          ← Chi tiết Task 1 (4h)
├── task-2-configure-hnsw.md           ← Chi tiết Task 2 (2h)
├── task-3-schema-docs.md              ← Chi tiết Task 3 (3h)
├── task-4-llm-api-keys.md             ← Chi tiết Task 4 (2h)
├── task-5-embedding-research.md       ← Chi tiết Task 5 (4h)
├── task-6-pii-integration-design.md   ← Chi tiết Task 6 (2h)
├── task-7-db-troubleshooting.md       ← Chi tiết Task 7 (2h)
├── task-8-pii-ops-guide.md            ← Chi tiết Task 8 (2h)
├── task-9-dev-onboarding.md           ← Chi tiết Task 9 (1h)
├── task-10-load-testing.md            ← Chi tiết Task 10 (1h)
└── deliverables/                      ← Outputs từ mỗi task
    ├── pgvector-verification.sql
    ├── hnsw-benchmark-results.md
    ├── schema-export.md
    ├── llm-api-test-results.md
    ├── embedding-comparison.md
    ├── pii-integration-diagram.png
    ├── database-troubleshooting.md
    ├── pii-operations-runbook.md
    ├── ONBOARDING.md
    └── load-test-results.md
```

---

## ✅ Completion Checklist

Sau khi hoàn thành TOÀN BỘ sprint:

```
Sprint Complete Checklist:
□ All 10 tasks marked ✅ Completed
□ All deliverables created in deliverables/ folder
□ README.md progress = 100%
□ bmm-workflow-status.md updated
□ Can answer: "pgvector hoạt động chưa?" → YES
□ Can answer: "LLM API tested chưa?" → YES
□ Can answer: "Schema docs export xong chưa?" → YES
□ Ready to draft Story 1.3
```

---

## 🎉 What's Next?

Sau khi sprint HOÀN THÀNH:

1. **Run Retrospective** (optional nhưng recommended)
2. **Draft Story 1.3** - Vector Database Setup
3. **Update Epic 1 Progress** - 2/13 stories → 3/13 stories
4. **Celebrate** 🎊 - Bạn vừa mở khóa entire RAG pipeline!

---

## 🔗 Quick Links

- [📖 README (overview đầy đủ)](./README.md)
- [🔴 Task 1: Enable pgvector](./task-1-enable-pgvector.md) ← **BẮT ĐẦU TỪ ĐÂY**
- [🟡 Task 2: HNSW index](./task-2-configure-hnsw.md)
- [📊 Retrospective Phase 4](../retrospectives/phase-4-sprint-retro-2025-10-19.md)
- [📋 Epics Breakdown](../epics.md)

---

**Tạo bởi:** DEV Agent (Sonnet 4.5)
**Ngày:** 2025-10-19
**Version:** 1.0.0

**Good luck! 🚀 Bắt đầu với Task 1 ngay bây giờ!**

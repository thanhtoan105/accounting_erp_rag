# Developer Onboarding Guide / Hướng Dẫn Onboarding Developer

**Version:** 2025-10-20  
**Audience:** New engineers joining the accounting_erp_rag project  
**Languages:** English & Vietnamese (song ngữ)

---

## 1. Welcome / Chào mừng

- **EN:** Welcome to the AI-native accounting ERP project. This guide walks you through access, local setup, verification steps, and who to contact during your first week.
- **VI:** Chào mừng bạn đến với dự án ERP kế toán tích hợp AI. Tài liệu này hướng dẫn cách lấy quyền truy cập, thiết lập môi trường, kiểm tra vận hành và kênh liên hệ trong tuần đầu.

---

## 2. Access Checklist / Danh sách quyền truy cập

- **EN:** Request the following from the PM (thanhtoan105):
  1. Supabase project read-only credentials (host, port 6543, database, username, password).
  2. Azure OpenAI resource access (endpoint, keys, deployments: `gpt-4o`, `text-embedding-3-small`).
  3. GitHub access to private repositories (if mirrored elsewhere) and shared Google Drive for compliance docs.
- **VI:** Yêu cầu PM (thanhtoan105) cấp:
  1. Thông tin Supabase read-only (host, port 6543, database, username, password).
  2. Quyền Azure OpenAI (endpoint, key, deployments: `gpt-4o`, `text-embedding-3-small`).
  3. Quyền GitHub (nếu repo private/mirror) và Google Drive cho tài liệu tuân thủ.

---

## 3. Workstation Prerequisites / Yêu cầu môi trường

- **EN:** Install Java 21 (Temurin), Gradle 8.7+, Node 18+, pnpm 8, Docker + Docker Compose, psql client, curl, jq.
- **VI:** Cài Java 21 (Temurin), Gradle 8.7+, Node 18+, pnpm 8, Docker + Docker Compose, psql, curl, jq.

Verification commands / Lệnh kiểm tra:

```zsh
java --version
./gradlew --version
node --version && pnpm --version
docker --version
psql --version
```

---

## 4. Clone & Branch Workflow / Sao chép repo & quy trình nhánh

```zsh
git clone https://github.com/thanhtoan105/accounting_erp_rag.git
cd accounting_erp_rag
git checkout -b feature/<your-initials>/<task-name>
```

- **EN:** Follow trunk-based flow: short-lived feature branches, PR into `main`, squash-and-merge after review.
- **VI:** Dùng trunk-based: branch ngắn hạn, tạo PR về `main`, squash merge sau khi review.

---

## 5. Environment Configuration / Cấu hình môi trường

### 5.1 Base `.env`

```zsh
cp .env.example .env
nano .env
```

Populate with:

```env
# Supabase
SUPABASE_HOST=aws-1-us-east-2.pooler.supabase.com
SUPABASE_PORT=6543
SUPABASE_DATABASE=postgres
SUPABASE_USERNAME=readonly_user
SUPABASE_DB_PASSWORD=<provided-secret>

# Azure OpenAI (LLM + Embedding)
AZURE_OPENAI_ENDPOINT=https://<resource>.cognitiveservices.azure.com/
AZURE_OPENAI_API_KEY=<provided-secret>
AZURE_OPENAI_API_VERSION=2024-04-01-preview
AZURE_OPENAI_DEPLOYMENT=gpt-4o
AZURE_OPENAI_EMBEDDING_PRIMARY_DEPLOYMENT=rag-embeddings-default
EMBEDDING_DIMENSION_DEFAULT=1536

# Feature flags
PII_MASKING_ENABLED=true
EMBEDDING_MASKING_MODE=strict
```

- **EN:** Never commit `.env`; repository already ignores it.  
- **VI:** Tuyệt đối không commit `.env`; repo đã có `.gitignore`.

### 5.2 Load variables / Nạp biến

```zsh
set -a; source .env; set +a
printenv SUPABASE_HOST AZURE_OPENAI_ENDPOINT
```

---

## 6. Local Verification / Kiểm tra vận hành

### 6.1 Database connection / Kết nối DB

```zsh
psql "postgresql://$SUPABASE_USERNAME:$SUPABASE_DB_PASSWORD@$SUPABASE_HOST:$SUPABASE_PORT/$SUPABASE_DATABASE?sslmode=require" -c "select now();"
```

- **EN:** Expect current timestamp; failure indicates VPN/network or credential issues.  
- **VI:** Trả về timestamp hiện tại; lỗi → kiểm tra VPN/mật khẩu.

### 6.2 Backend profile run / Chạy backend

```zsh
./gradlew clean bootRun --args='--spring.profiles.active=supabase'
```

In another terminal / Mở terminal khác:

```zsh
curl -s http://localhost:8080/internal/rag/db-health | jq
```

- **EN:** Verify status `HEALTHY`, readOnly `true`, replica field may be false (expected).
- **VI:** Kiểm tra `status=HEALTHY`, `readOnly=true`, `replicaAvailable` có thể `false` (bình thường).

### 6.3 Tests / Kiểm thử

```zsh
./gradlew test
./gradlew integrationTest  # optional, uses Testcontainers
```

- **EN:** Integration tests require Docker running.  
- **VI:** Integration test cần Docker hoạt động.

---

## 7. Frontend & Tooling / Frontend và công cụ

- **EN:** SPA scaffold planned but not yet in repo. Use `docs/ai-frontend-prompt.md` for UX references and wait for onboarding notice when available.
- **VI:** SPA chưa commit; tham khảo `docs/ai-frontend-prompt.md` và chờ thông báo khi có code.

Developer tools / Công cụ dev:

- **EN:** Install `just` (optional) for orchestration scripts once introduced.  
- **VI:** Cài `just` (tùy chọn) để chạy script khi được thêm vào.

---

## 8. First-week Goals / Mục tiêu tuần đầu

| Day / Ngày | EN | VI |
|-----------|----|----|
| Day 1 | Replicate local setup, run health checks, read Preparation Sprint README. | Thiết lập môi trường, chạy health check, đọc README Preparation Sprint. |
| Day 2 | Review PII masking docs (`docs/pii-masking-rules.md`, deliverables). | Nghiên cứu tài liệu PII masking. |
| Day 3 | Pair with teammate on Task backlog or runbook rehearsal. | Pair cùng teammate về backlog hoặc luyện runbook. |
| Day 4 | Deliver first PR (documentation or small script). | Tạo PR đầu tiên (docs hoặc script nhỏ). |
| Day 5 | Sync with PM on sprint goals; update onboarding checklist. | Họp PM về mục tiêu sprint; cập nhật checklist onboarding. |

---

## 9. Support Channels / Kênh hỗ trợ

- **Slack:** `#rag-dev`, `#ops-alerts`
- **Email:** [email protected]
- **Standup:** 09:30 ICT (Google Meet link sent via calendar)
- **PM:** thanhtoan105  
- **Architect:** Contact via `#rag-architecture`

---

## 10. Useful References / Tài liệu hữu ích

- `docs/preparation-sprint/README.md`
- `docs/SUPABASE_SETUP.md`
- `docs/preparation-sprint/deliverables/database-troubleshooting.md`
- `docs/preparation-sprint/deliverables/pii-operations-guide.md`
- `docs/tech-spec-epic-1.md`
- `docs/solution-architecture.md`

---

## 11. Revision Log / Lịch sử chỉnh sửa

| Date | Author | Notes |
|------|--------|-------|
| 2025-10-20 | thanhtoan105 | Initial bilingual onboarding guide |


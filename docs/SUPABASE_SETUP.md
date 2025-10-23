# Supabase Connection Setup Guide

## Quick Start

### 1. Chạy setup script (Recommended)

```bash
chmod +x setup-supabase.sh
./setup-supabase.sh
```

Script sẽ prompt bạn nhập:
- Supabase Host
- Database name
- Credentials (read-only + admin)
- Tự động test connection

### 2. Hoặc setup thủ công

```bash
# Copy example file
cp .env.example .env

# Edit với thông tin thật
nano .env
```

## Environment Variables

Sau khi tạo `.env`, file sẽ chứa:

```bash
# Supabase Connection
SUPABASE_HOST="aws-1-us-east-2.pooler.supabase.com"
SUPABASE_PORT="6543"  # Pooler mặc định. Dùng 5432 nếu kết nối trực tiếp db.YOUR_REF.supabase.co
SUPABASE_DATABASE="postgres"
SUPABASE_USERNAME="readonly_user"
SUPABASE_DB_PASSWORD="YourPassword"
```

## Load Environment

Trước khi chạy bất kỳ lệnh nào:

```bash
# Bash/Zsh
source .env

# Hoặc export từng biến
export $(cat .env | grep -v '^#' | xargs)
```

## Verify Setup

### Test connection với psql

```bash
source .env

psql "postgresql://$SUPABASE_USERNAME:$SUPABASE_DB_PASSWORD@$SUPABASE_HOST:$SUPABASE_PORT/$SUPABASE_DATABASE?sslmode=require"
```

### Test với application

```bash
source .env
./gradlew bootRun --args='--spring.profiles.active=supabase'
```

Trong terminal khác:
```bash
curl http://localhost:8080/internal/rag/db-health | jq
```

## Profiles

Application hỗ trợ 2 profiles:

### 1. Default (Local Development)
```bash
./gradlew bootRun
# Kết nối localhost:5432
```

### 2. Supabase (Cloud)
```bash
source .env
./gradlew bootRun --args='--spring.profiles.active=supabase'
# Kết nối Supabase Cloud với SSL
```

## Security Notes

⚠️ **QUAN TRỌNG**:
- ✅ File `.env` đã được thêm vào `.gitignore`
- ✅ KHÔNG BAO GIỜ commit `.env` vào Git
- ✅ Chia sẻ `.env.example` (không có credentials thật)
- ✅ Mỗi developer tự tạo `.env` riêng

## Troubleshooting

### Lỗi: "connection refused" / "connection timeout"
```bash
# Kiểm tra host
echo $SUPABASE_HOST

# Kiểm tra network
ping $SUPABASE_HOST

# Kiểm tra port đã đúng chưa (pooler thường 6543)
echo $SUPABASE_PORT
```

### Lỗi: "password authentication failed"
```bash
# Reset password trong Supabase Dashboard
# Settings → Database → Reset Database Password
```

### Lỗi: "SSL connection error"
```bash
# Đảm bảo connection string có ?sslmode=require
# Kiểm tra trong application-supabase.properties
```

## Next Steps

Sau khi setup xong:

1. ✅ Load environment: `source .env`
2. ✅ Run tests: `./gradlew test`
3. ✅ Start application: `./gradlew bootRun --args='--spring.profiles.active=supabase'`
4. ✅ Test health endpoint: `curl http://localhost:8080/internal/rag/db-health`
5. ✅ Generate schema docs: `./gradlew bootRun --args='--spring.profiles.active=supabase --schema.documentation.enabled=true'`

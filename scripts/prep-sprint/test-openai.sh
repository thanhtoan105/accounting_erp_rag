#!/bin/bash
set -euo pipefail

# Load environment variables for API credentials
if [[ ! -f ".env" ]]; then
  echo "Missing .env file in project root." >&2
  exit 1
fi

# shellcheck disable=SC1091
source .env

echo "==========================================="
echo "Testing OpenAI API"
echo "==========================================="
MODEL=${OPENAI_MODEL}
echo "Model: ${MODEL}"
echo "API Key: ${OPENAI_API_KEY:0:20}..."
echo ""

# Test 1: Simple completion
echo "Test 1: Simple English completion"
RESPONSE=$(curl -s https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OPENAI_API_KEY}" \
  -d '{
    "model": "'"${MODEL}"'",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Reply with exactly: Test successful"}
    ],
    "max_tokens": 10,
    "temperature": 0
  }')

ERROR_MESSAGE=$(echo "$RESPONSE" | jq -r '.error.message // empty')
if [[ -n "$ERROR_MESSAGE" ]]; then
  echo "OpenAI API error: $ERROR_MESSAGE" >&2
  exit 1
fi

echo "$RESPONSE" | jq -r '.choices[0].message.content'
echo ""

# Test 2: Vietnamese completion
echo "Test 2: Vietnamese accounting query"
START_TIME=$(date +%s%3N)

RESPONSE=$(curl -s https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${OPENAI_API_KEY}" \
  -d '{
    "model": "'"${MODEL}"'",
    "messages": [
      {"role": "system", "content": "Bạn là trợ lý kế toán chuyên nghiệp cho doanh nghiệp Việt Nam."},
      {"role": "user", "content": "Tài khoản 131 trong hệ thống kế toán Việt Nam là gì? Trả lời ngắn gọn."}
    ],
    "max_tokens": 100,
    "temperature": 0.1
  }')

ERROR_MESSAGE=$(echo "$RESPONSE" | jq -r '.error.message // empty')
if [[ -n "$ERROR_MESSAGE" ]]; then
  echo "OpenAI API error: $ERROR_MESSAGE" >&2
  exit 1
fi

END_TIME=$(date +%s%3N)
LATENCY=$((END_TIME - START_TIME))

echo "Response:"
echo "$RESPONSE" | jq -r '.choices[0].message.content'
echo ""
echo "Latency: ${LATENCY}ms"
echo "Usage:"
echo "$RESPONSE" | jq '.usage'
echo ""

# Test 3: Cost calculation
PROMPT_TOKENS=$(echo "$RESPONSE" | jq -r '.usage.prompt_tokens')
COMPLETION_TOKENS=$(echo "$RESPONSE" | jq -r '.usage.completion_tokens')

# GPT-4 Turbo pricing: $10/1M input, $30/1M output
INPUT_COST=$(echo "scale=6; ${PROMPT_TOKENS} * 10 / 1000000" | bc)
OUTPUT_COST=$(echo "scale=6; ${COMPLETION_TOKENS} * 30 / 1000000" | bc)
TOTAL_COST=$(echo "scale=6; ${INPUT_COST} + ${OUTPUT_COST}" | bc)

echo "Cost breakdown:"
echo "  Input tokens: ${PROMPT_TOKENS} @ \$10/1M = \$${INPUT_COST}"
echo "  Output tokens: ${COMPLETION_TOKENS} @ \$30/1M = \$${OUTPUT_COST}"
echo "  Total: \$${TOTAL_COST}"
echo ""

# Budget check
echo "Budget status:"
echo "  Remaining: \$5.00"
echo "  This test: \$${TOTAL_COST}"
if [[ $(echo "${TOTAL_COST} > 0" | bc) -eq 1 ]]; then
  ESTIMATED_TESTS=$(echo "scale=0; 5 / ${TOTAL_COST}" | bc)
else
  ESTIMATED_TESTS="∞"
fi
echo "  Estimated tests remaining: ${ESTIMATED_TESTS}"
echo ""

echo "✅ OpenAI API test completed successfully"

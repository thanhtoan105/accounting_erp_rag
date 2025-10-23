#!/bin/bash
set -euo pipefail

# Load environment variables required for Azure OpenAI tests
if [[ ! -f ".env" ]]; then
  echo "Missing .env file in project root." >&2
  exit 1
fi

# shellcheck disable=SC1091
source .env

ENDPOINT=${AZURE_OPENAI_ENDPOINT%/}
DEPLOYMENT=${AZURE_OPENAI_DEPLOYMENT:-}
API_VERSION=${AZURE_OPENAI_API_VERSION:-"2024-02-15-preview"}

if [[ -z "${AZURE_OPENAI_API_KEY:-}" || -z "${DEPLOYMENT}" || -z "${ENDPOINT}" ]]; then
  echo "Missing Azure OpenAI configuration in .env (AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT)." >&2
  exit 1
fi

BASE_URL="${ENDPOINT}/openai/deployments/${DEPLOYMENT}/chat/completions?api-version=${API_VERSION}"

echo "==========================================="
echo "Testing Azure OpenAI API"
echo "==========================================="
echo "Endpoint: ${ENDPOINT}"
echo "Deployment: ${DEPLOYMENT}"
echo "API version: ${API_VERSION}"
echo ""

call_azure() {
  local payload="$1"
  curl -s "${BASE_URL}" \
    -H "Content-Type: application/json" \
    -H "api-key: ${AZURE_OPENAI_API_KEY}" \
    -d "${payload}"
}

# Test 1: Simple completion
echo "Test 1: Simple English completion"
RESPONSE=$(call_azure '{
  "messages": [
    {"role": "system", "content": "You are a helpful assistant."},
    {"role": "user", "content": "Reply with exactly: Azure test successful"}
  ],
  "max_tokens": 10,
  "temperature": 0
}')

ERROR_MESSAGE=$(echo "$RESPONSE" | jq -r '.error.message // empty')
if [[ -n "$ERROR_MESSAGE" ]]; then
  echo "Azure OpenAI error: $ERROR_MESSAGE" >&2
  exit 1
fi

echo "$RESPONSE" | jq -r '.choices[0].message.content'
echo ""

# Test 2: Vietnamese completion with latency measurement
echo "Test 2: Vietnamese accounting query"
START_TIME=$(date +%s%3N)

RESPONSE=$(call_azure '{
  "messages": [
    {"role": "system", "content": "Bạn là trợ lý kế toán chuyên nghiệp cho doanh nghiệp Việt Nam."},
    {"role": "user", "content": "Phân biệt tài khoản 1121 và 1122 theo Thông tư 200. Trả lời ngắn gọn."}
  ],
  "max_tokens": 150,
  "temperature": 0.1
}')

ERROR_MESSAGE=$(echo "$RESPONSE" | jq -r '.error.message // empty')
if [[ -n "$ERROR_MESSAGE" ]]; then
  echo "Azure OpenAI error: $ERROR_MESSAGE" >&2
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

# Cost estimation (Azure GPT-4 pricing differs per region; use placeholders for now)
PROMPT_TOKENS=$(echo "$RESPONSE" | jq -r '.usage.prompt_tokens')
COMPLETION_TOKENS=$(echo "$RESPONSE" | jq -r '.usage.completion_tokens')

INPUT_RATE=${AZURE_OPENAI_INPUT_RATE_PER_1K:-0}   # USD per 1K prompt tokens
OUTPUT_RATE=${AZURE_OPENAI_OUTPUT_RATE_PER_1K:-0} # USD per 1K completion tokens

if [[ "$INPUT_RATE" != "0" && "$OUTPUT_RATE" != "0" ]]; then
  INPUT_COST=$(echo "scale=6; ${PROMPT_TOKENS} * ${INPUT_RATE} / 1000" | bc)
  OUTPUT_COST=$(echo "scale=6; ${COMPLETION_TOKENS} * ${OUTPUT_RATE} / 1000" | bc)
  TOTAL_COST=$(echo "scale=6; ${INPUT_COST} + ${OUTPUT_COST}" | bc)

  echo "Cost breakdown (Azure regional pricing):"
  echo "  Input tokens: ${PROMPT_TOKENS} @ \$${INPUT_RATE}/1K = \$${INPUT_COST}"
  echo "  Output tokens: ${COMPLETION_TOKENS} @ \$${OUTPUT_RATE}/1K = \$${OUTPUT_COST}"
  echo "  Total: \$${TOTAL_COST}"
  echo ""
else
  echo "Cost breakdown: set AZURE_OPENAI_INPUT_RATE_PER_1K and AZURE_OPENAI_OUTPUT_RATE_PER_1K in .env to calculate."
  echo ""
fi

echo "✅ Azure OpenAI API test completed successfully"

/**
 * k6 Performance Test for RAG Query Endpoint (Story 1.5 - AC1)
 * Target: P95 endpoint response time ≤200ms
 * 
 * Run with: k6 run --out json=endpoint-perf-results.json k6-endpoint-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const endpointLatency = new Trend('endpoint_latency_ms');
const validationErrors = new Counter('validation_errors');
const authErrors = new Counter('auth_errors');

export const options = {
  stages: [
    { duration: '30s', target: 5 },   // Warm-up: ramp up to 5 VUs
    { duration: '1m', target: 20 },   // Load test: ramp up to 20 VUs
    { duration: '3m', target: 20 },   // Sustained load: hold at 20 VUs
    { duration: '30s', target: 50 },  // Peak load: spike to 50 VUs
    { duration: '1m', target: 50 },   // Hold peak
    { duration: '30s', target: 0 },   // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200', 'p(99)<500'],  // P95 < 200ms, P99 < 500ms
    'http_req_duration{name:query_endpoint}': ['p(95)<200'],
    'errors': ['rate<0.05'],           // Error rate < 5%
    'http_req_failed': ['rate<0.05'],  // HTTP error rate < 5%
  },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_TOKEN = __ENV.JWT_TOKEN || 'test-jwt-token-placeholder';
const COMPANY_ID = __ENV.COMPANY_ID || '123e4567-e89b-12d3-a456-426614174000';

// Sample queries (mix of Vietnamese and English)
const SAMPLE_QUERIES = [
  { text: 'What is the current AR balance?', lang: 'en' },
  { text: 'Show overdue invoices for last month', lang: 'en' },
  { text: 'List all pending payments', lang: 'en' },
  { text: 'What are the top 5 customers by revenue?', lang: 'en' },
  { text: 'Khách hàng nào còn nợ?', lang: 'vi' },
  { text: 'Hiển thị hóa đơn quá hạn', lang: 'vi' },
  { text: 'Tổng doanh thu tháng này là bao nhiêu?', lang: 'vi' },
  { text: 'Danh sách các khoản phải thu', lang: 'vi' },
];

// Sample filters (some queries have filters, some don't)
const SAMPLE_FILTERS = [
  null,
  { module: 'ar' },
  { module: 'ar', fiscalPeriod: '2024-10' },
  { module: 'ap', fiscalPeriod: '2024-11' },
  { fiscalPeriod: '2024-10' },
];

export default function () {
  // Randomly select query and filters
  const query = SAMPLE_QUERIES[Math.floor(Math.random() * SAMPLE_QUERIES.length)];
  const filters = SAMPLE_FILTERS[Math.floor(Math.random() * SAMPLE_FILTERS.length)];
  
  const payload = JSON.stringify({
    companyId: COMPANY_ID,
    query: query.text,
    language: query.lang,
    filters: filters,
  });
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${JWT_TOKEN}`,
    },
    tags: { name: 'query_endpoint' },
  };
  
  const response = http.post(`${BASE_URL}/api/v1/rag/query`, payload, params);
  
  // Record custom metrics
  endpointLatency.add(response.timings.duration);
  
  // Validation checks
  const success = check(response, {
    'status is 202': (r) => r.status === 202,
    'has queryId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.queryId !== undefined;
      } catch (e) {
        return false;
      }
    },
    'has streamUrl': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.streamUrl !== undefined;
      } catch (e) {
        return false;
      }
    },
    'response time < 200ms': (r) => r.timings.duration < 200,
    'response time < 100ms (target)': (r) => r.timings.duration < 100,
  });
  
  if (!success) {
    errorRate.add(1);
    
    if (response.status === 400) {
      validationErrors.add(1);
    } else if (response.status === 401 || response.status === 403) {
      authErrors.add(1);
    }
  }
  
  // Random think time between 0.5 and 2 seconds
  sleep(Math.random() * 1.5 + 0.5);
}

// Summary report at end of test
export function handleSummary(data) {
  const p95 = data.metrics.http_req_duration.values['p(95)'];
  const p99 = data.metrics.http_req_duration.values['p(99)'];
  const errorRate = data.metrics.errors ? data.metrics.errors.values.rate : 0;
  
  console.log('\n========================================');
  console.log('RAG Query Endpoint Performance Test Results');
  console.log('========================================');
  console.log(`Total Requests: ${data.metrics.http_reqs.values.count}`);
  console.log(`P95 Latency: ${p95.toFixed(2)}ms (target: ≤200ms)`);
  console.log(`P99 Latency: ${p99.toFixed(2)}ms (target: ≤500ms)`);
  console.log(`Error Rate: ${(errorRate * 100).toFixed(2)}% (target: <5%)`);
  console.log(`Status: ${p95 <= 200 ? '✅ PASSED' : '❌ FAILED'}`);
  console.log('========================================\n');
  
  return {
    'stdout': JSON.stringify(data, null, 2),
    'summary.json': JSON.stringify(data, null, 2),
  };
}

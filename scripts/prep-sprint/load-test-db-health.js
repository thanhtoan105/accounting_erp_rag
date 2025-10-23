import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '60s',
  thresholds: {
    http_req_duration: ['p(95)<1500', 'p(99)<3000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.RAG_BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/internal/rag/db-health`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'readOnly true': (r) => r.json('readOnly') === true,
  });
  sleep(1);
}

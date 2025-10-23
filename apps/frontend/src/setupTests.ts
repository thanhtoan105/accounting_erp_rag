import '@testing-library/jest-dom';
import { vi } from 'vitest';
import { QueryRequest, QueryResponse } from './components/QueryTestInterface';

// Mock fetch for API testing
global.fetch = vi.fn();

// Mock response for successful query submission
const mockQueryResponse: QueryResponse = {
  queryId: 'test-query-id-12345',
  streamUrl: '/api/v1/rag/query/test-query-id-12345/events'
};

// Mock fetch implementation
(global.fetch as any).mockImplementation((url, options) => {
  return new Promise((resolve, reject) => {
    if (url === '/api/v1/rag/query' && options?.method === 'POST') {
      setTimeout(() => {
        resolve({
          ok: true,
          status: 202,
          json: () => mockQueryResponse,
          headers: {
            'Content-Type': 'application/json',
            'X-Rag-Submission-Latency': '123'
          }
        } as Response);
      }, 100);
    } else if (url.includes('/events')) {
      setTimeout(() => {
        resolve({
          ok: true,
          status: 200,
          text: () => 'SSE streaming endpoint - to be implemented in Story 1.6'
        } as Response);
      }, 50);
    } else {
      setTimeout(() => {
        reject(new Error('Not found'));
      }, 100);
    }
  });
});

// Reset all mocks before each test
beforeEach(() => {
  vi.clearAllMocks();
});

export interface MockFetchResponse {
  ok: boolean;
  status: number;
  json: () => any;
  text?: () => string;
  headers: Record<string, string>;
}

// Test utilities
export const createMockQueryRequest = (overrides: Partial<QueryRequest> = {}): QueryRequest => ({
  companyId: 'test-company-id',
  query: 'Test query',
  language: 'en',
  filters: {},
  ...overrides
});

export const createMockQueryResponse = (overrides: Partial<QueryResponse> = {}): QueryResponse => ({
  queryId: 'test-query-id',
  streamUrl: '/api/v1/rag/query/test-query-id/events',
  ...overrides
});

export const waitForNextTick = () => new Promise(resolve => setTimeout(resolve, 0));
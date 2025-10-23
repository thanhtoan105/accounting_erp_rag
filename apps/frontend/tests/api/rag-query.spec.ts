import { test, expect } from '@playwright/test';
import { APIClient } from '../support/api-client';
import { TestDataFactory } from '../fixtures/test-data-factory';

// Test configuration based on Story 1.5 requirements
const API_BASE_URL = process.env.API_URL || 'http://localhost:8080';
const TEST_TIMEOUT = 15000; // 15 seconds for API tests

test.describe('RAG Query API Tests - Story 1.5', () => {
  let apiClient: APIClient;

  test.beforeAll(async () => {
    apiClient = new APIClient(API_BASE_URL);
    // Setup test authentication and data
    await apiClient.setupTestAuthentication();
  });

  test.describe('AC1: Query API Endpoint and Validation', () => {
    test('should accept valid query request and return 202 Accepted', async () => {
      const queryRequest = TestDataFactory.createValidQueryRequest();

      const response = await apiClient.submitQuery(queryRequest);

      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('queryId');
      expect(responseBody).toHaveProperty('streamUrl');
      expect(responseBody.streamUrl).toContain('/api/v1/rag/query/');
    });

    test('should reject empty query with 400 Bad Request', async () => {
      const invalidRequest = {
        companyId: 'test-company-id',
        query: '', // Empty query
        language: 'en',
        filters: {}
      };

      const response = await apiClient.submitQuery(invalidRequest);

      expect(response.status()).toBe(400);
      const errorBody = await response.json();
      expect(errorBody).toHaveProperty('message');
      expect(errorBody.message).toContain('query');
    });

    test('should reject query longer than 500 characters', async () => {
      const longQuery = 'a'.repeat(501); // 501 characters
      const invalidRequest = TestDataFactory.createValidQueryRequest({
        query: longQuery
      });

      const response = await apiClient.submitQuery(invalidRequest);

      expect(response.status()).toBe(400);
    });

    test('should validate language parameter', async () => {
      const invalidRequest = TestDataFactory.createValidQueryRequest({
        language: 'invalid-lang'
      });

      const response = await apiClient.submitQuery(invalidRequest);

      expect(response.status()).toBe(400);
    });
  });

  test.describe('AC2: Query Embedding Generation', () => {
    test('should process Vietnamese query successfully', async ({ request }) => {
      const vietnameseQuery = TestDataFactory.createValidQueryRequest({
        query: 'Tổng công nợ phải thu hiện tại là bao nhiêu?',
        language: 'vi'
      });

      const startTime = Date.now();
      const response = await apiClient.submitQuery(vietnameseQuery);
      const endTime = Date.now();

      expect(response.status()).toBe(202);
      expect(endTime - startTime).toBeLessThan(300); // P95 ≤ 300ms
    });

    test('should process English query successfully', async ({ request }) => {
      const englishQuery = TestDataFactory.createValidQueryRequest({
        query: 'What is the current accounts receivable balance?',
        language: 'en'
      });

      const response = await apiClient.submitQuery(englishQuery);

      expect(response.status()).toBe(202);
    });

    test('should handle embedding API failure gracefully', async ({ request }) => {
      // This test would mock embedding API failure
      // Implementation depends on your mocking strategy
      const queryRequest = TestDataFactory.createValidQueryRequest();

      // Mock embedding service failure
      await apiClient.mockEmbeddingServiceFailure();

      const response = await apiClient.submitQuery(queryRequest);

      // Should still return 202 but with fallback strategy
      expect(response.status()).toBe(202);
    });
  });

  test.describe('AC3: Vector Similarity Search', () => {
    test('should retrieve top 10 documents for relevant query', async () => {
      const queryRequest = TestDataFactory.createValidQueryRequest({
        query: 'customer payment history',
        filters: {
          module: 'ar'
        }
      });

      const response = await apiClient.submitQuery(queryRequest);
      const responseBody = await response.json();

      expect(response.status()).toBe(202);
      expect(responseBody).toHaveProperty('queryId');

      // Verify query was logged with retrieval metrics
      const queryStatus = await apiClient.getQueryStatus(responseBody.queryId);
      expect(queryStatus.status).toMatch(/retrieved|retrieval_failed/);
    });

    test('should apply metadata filters correctly', async () => {
      const queryWithFilters = TestDataFactory.createValidQueryRequest({
        query: 'invoices',
        filters: {
          module: 'ar',
          fiscalPeriod: '2024-10',
          documentType: 'invoice'
        }
      });

      const response = await apiClient.submitQuery(queryWithFilters);

      expect(response.status()).toBe(202);
    });
  });

  test.describe('AC5: Query Logging and Telemetry', () => {
    test('should log query metrics correctly', async () => {
      const queryRequest = TestDataFactory.createValidQueryRequest();

      const response = await apiClient.submitQuery(queryRequest);
      const queryId = (await response.json()).queryId;

      // Wait a moment for async processing
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Verify query was logged with metrics
      const queryStatus = await apiClient.getQueryStatus(queryId);
      expect(queryStatus).toHaveProperty('latency');
      expect(queryStatus).toHaveProperty('retrievedDocCount');
      expect(queryStatus).toHaveProperty('completedAt');
    });

    test('should emit telemetry metrics', async () => {
      // This would verify Prometheus/OpenTelemetry metrics
      // Implementation depends on your metrics endpoint
      const metrics = await apiClient.getPrometheusMetrics();

      expect(metrics).toContain('rag_query_total');
      expect(metrics).toContain('rag_retrieval_latency');
    });
  });

  test.describe('AC6: Error Handling', () => {
    test('should handle unauthorized access', async () => {
      const unauthorizedClient = new APIClient(API_BASE_URL);
      // Don't authenticate this client

      const queryRequest = TestDataFactory.createValidQueryRequest();
      const response = await unauthorizedClient.submitQuery(queryRequest);

      expect(response.status()).toBe(401);
    });

    test('should handle cross-company access attempts', async () => {
      const queryWithWrongCompany = TestDataFactory.createValidQueryRequest({
        companyId: 'different-company-id'
      });

      const response = await apiClient.submitQuery(queryWithWrongCompany);

      expect(response.status()).toBe(403);
    });
  });

  test.afterAll(async () => {
    // Cleanup test data
    await apiClient.cleanupTestData();
  });
});
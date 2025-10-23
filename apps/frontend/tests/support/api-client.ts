import { APIRequestContext, APIResponse } from '@playwright/test';

export class APIClient {
  private request: APIRequestContext;
  private authToken: string | null = null;

  constructor(baseURL: string) {
    this.request = {
      baseURL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
      },
    } as any;
  }

  async setupTestAuthentication(): Promise<void> {
    // This would implement JWT authentication
    // For now, using a mock token
    this.authToken = 'mock-jwt-token-for-testing';

    this.request = {
      ...this.request,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.authToken}`,
      },
    } as any;
  }

  async submitQuery(queryData: any): Promise<APIResponse> {
    const response = await fetch(`${this.request.baseURL}/api/v1/rag/query`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.authToken}`,
      },
      body: JSON.stringify(queryData),
    });

    return {
      status: () => response.status,
      json: () => response.json(),
      text: () => response.text(),
      headers: () => Object.fromEntries(response.headers.entries()),
    } as APIResponse;
  }

  async getQueryStatus(queryId: string): Promise<any> {
    const response = await fetch(`${this.request.baseURL}/api/v1/rag/query/${queryId}/status`, {
      headers: {
        'Authorization': `Bearer ${this.authToken}`,
      },
    });

    return response.json();
  }

  async getPrometheusMetrics(): Promise<string> {
    const response = await fetch(`${this.request.baseURL}/actuator/prometheus`, {
      headers: {
        'Authorization': `Bearer ${this.authToken}`,
      },
    });

    return response.text();
  }

  async mockEmbeddingServiceFailure(): Promise<void> {
    // This would implement mocking strategy for embedding service failures
    // Could be done through test configuration or service virtualization
    console.log('Mocking embedding service failure for testing');
  }

  async cleanupTestData(): Promise<void> {
    // Cleanup any test data created during tests
    console.log('Cleaning up test data');
  }
}
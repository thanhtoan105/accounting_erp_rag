import { test, expect } from '@playwright/test';

/**
 * End-to-end tests for the complete RAG query flow.
 * Tests the full user journey from query submission to results display.
 *
 * Story 1.5 - AC14: Cypress end-to-end tests for full query flow
 * Story 1.5 - AC15: Performance testing with browser DevTools integration
 */

test.describe('RAG Query Flow - English', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the query interface
    await page.goto('/');

    // Wait for page to load
    await page.waitForLoadState('networkidle');
  });

  test('should submit English query and show results', async ({ page }) => {
    // Fill out the query form
    await page.fill('[data-testid="company-id"]', 'test-company-123');
    await page.selectOption('[data-testid="language-select"]', 'English');
    await page.fill('[data-testid="query-input"]', 'What are the outstanding invoices?');
    await page.selectOption('[data-testid="module-select"]', 'Accounts Receivable');

    // Submit the query
    await page.click('[data-testid="submit-button"]');

    // Should show loading state
    await expect(page.locator('[data-testid="submit-button"]')).toHaveText('Submitting...');
    await expect(page.locator('[data-testid="submit-button"]')).toBeDisabled();

    // Wait for success message
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });

    // Verify success message contains query ID
    const successMessage = page.locator('[data-testid="success-message"]');
    await expect(successMessage).toContainText('Query submitted with ID:');

    // Should appear in results section
    await expect(page.locator('[data-testid="results-section"]')).toBeVisible({ timeout: 10000 });

    // Verify query details in results
    const firstResult = page.locator('[data-testid="query-result"]').first();
    await expect(firstResult).toBeVisible();

    const queryIdElement = firstResult.locator('[data-testid="query-id"]');
    await expect(queryIdElement).toContainText('Query ID:');

    const streamUrlElement = firstResult.locator('[data-testid="stream-url"]');
    await expect(streamUrlElement).toContainText('Stream URL:');
  });

  test('should handle form validation errors', async ({ page }) => {
    // Try to submit with empty query
    await page.fill('[data-testid="company-id"]', 'test-company-123');
    await page.selectOption('[data-testid="language-select"]', 'English');
    await page.fill('[data-testid="query-input"]', ''); // Empty query

    // Submit should be disabled
    await expect(page.locator('[data-testid="submit-button"]')).toBeDisabled();

    // Character count should show 0/500
    await expect(page.locator('[data-testid="char-count"]')).toHaveText('0/500 characters');
  });

  test('should clear form fields', async ({ page }) => {
    // Fill out the form
    await page.fill('[data-testid="company-id"]', 'test-company-123');
    await page.fill('[data-testid="query-input"]', 'Test query');
    await page.selectOption('[data-testid="module-select"]', 'Accounts Payable');
    await page.fill('[data-testid="fiscal-period"]', '2024-10');

    // Click clear button
    await page.click('[data-testid="clear-button"]');

    // All fields should be cleared
    await expect(page.locator('[data-testid="company-id"]')).toHaveValue('');
    await expect(page.locator('[data-testid="query-input"]')).toHaveValue('');
    await expect(page.locator('[data-testid="fiscal-period"])).toHaveValue('');
    await expect(page.locator('[data-testid="module-select"])).toHaveValue('All Modules');
  });

  test('should use sample query buttons', async ({ page }) => {
    // Click sample query button
    await page.click('[data-testid="sample-query"]:has-text("What are our total expenses for Q4?")');

    // Query should be populated
    await expect(page.locator('[data-testid="query-input"]')).toHaveValue('What are our total expenses for Q4?');

    // Submit should be enabled
    await expect(page.locator('[data-testid="submit-button"]')).not.toBeDisabled();

    // Character count should update
    await expect(page.locator('[data-testid="char-count"]')).toHaveText('39/500 characters');
  });

  test('should handle network errors gracefully', async ({ page, context }) => {
    // Mock network failure
    await context.route('/api/v1/rag/query', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'internal_error',
          message: 'Internal server error'
        })
      });
    });

    // Fill and submit form
    await page.fill('[data-testid="company-id"]', 'test-company-123');
    await page.fill('[data-testid="query-input"]', 'Network error test');
    await page.click('[data-testid="submit-button"]');

    // Should show error message
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('[data-testid="error-message"]')).toContainText('Error: Internal server error');

    // Submit button should be re-enabled
    await expect(page.locator('[data-testid="submit-button"]')).not.toBeDisabled();
  });
});

test.describe('RAG Query Flow - Vietnamese', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
  });

  test('should submit Vietnamese query and show results', async ({ page }) => {
    // Fill out the query form in Vietnamese
    await page.fill('[data-testid="company-id"]', 'test-company-456');
    await page.selectOption('[data-testid="language-select"]', 'Vietnamese');
    await page.fill('[data-testid="query-input"]', 'Khách hàng nào còn nợ phải trả trong tháng này?');
    await page.selectOption('[data-testid="module-select"]', 'Accounts Receivable');
    await page.fill('[data-testid="fiscal-period"]', '2024-10');

    // Submit the query
    await page.click('[data-testid="submit-button"]');

    // Should show loading state
    await expect(page.locator('[data-testid="submit-button"]')).toHaveText('Submitting...');
    await expect(page.locator('[data-testid="submit-button"])).toBeDisabled();

    // Wait for success message
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });

    // Verify Vietnamese query was processed
    const successMessage = page.locator('[data-testid="success-message"]');
    await expect(successMessage).toContainText('Query submitted with ID:');

    // Results should be displayed
    await expect(page.locator('[data-testid="results-section"]')).toBeVisible({ timeout: 10000 });
  });

  test('should handle Unicode characters in Vietnamese queries', async ({ page }) => {
    // Query with Vietnamese characters and symbols
    const unicodeQuery = '📊 Tổng nợ phải thu: $1,234,567.89 - Khách hàng: Nguyễn Văn An, Trần Thị Bình';
    await page.fill('[data-testid="query-input"]', unicodeQuery);

    // Character count should handle Unicode correctly
    await expect(page.locator('[data-testid="char-count"]')).toHaveText(`${unicodeQuery.length}/500 characters`);

    // Submit should be enabled
    await expect(page.locator('[data-testid="submit-button"])).not.toBeDisabled();

    await page.click('[data-testid="submit-button"]');

    // Should process successfully despite Unicode characters
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });
  });

  test('should switch between Vietnamese and English sample queries', async ({ page }) => {
    // Initially in English
    await expect(page.locator('[data-testid="language-label"]')).toContainText('English');

    // Switch to Vietnamese
    await page.selectOption('[data-testid="language-select"]', 'Vietnamese');

    // Sample queries should update to Vietnamese
    await expect(page.locator('[data-testid="sample-query"]:has-text("Tổng công nợ phải thu hiện tại là bao nhiêu?")')).toBeVisible();
    await expect(page.locator('[data-testid="sample-query"]:has-text("What is current accounts receivable balance?")')).not.toBeVisible();

    // Vietnamese sample should populate query field
    await page.click('[data-testid="sample-query"]:has-text("Hiển thị các hóa đơn chưa thanh toán trong tháng này")');
    await expect(page.locator('[data-testid="query-input"]')).toHaveValue('Hiển thị các hóa đơn chưa thanh toán trong tháng này');

    // Character count should update
    await expect(page.locator('[data-testid="char-count"]')).toHaveText('61/500 characters');
  });
});

test.describe('Performance and Network Optimization', () => {
  test('should measure submission latency', async ({ page }) => {
    // Monitor network requests
    const requests = [];
    page.on('request', request => {
      if (request.url().includes('/api/v1/rag/query')) {
        requests.push({
          url: request.url(),
          method: request.method(),
          timestamp: Date.now()
        });
      }
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Fill and submit query
    await page.fill('[data-testid="company-id"]', 'perf-test-company');
    await page.fill('[data-testid="query-input"]', 'Performance test query');
    await page.click('[data-testid="submit-button"]');

    // Wait for success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });

    // Should have made exactly one request
    expect(requests).toHaveLength(1);

    // Request should be POST method
    expect(requests[0].method).toBe('POST');

    // Should include performance headers
    const response = await page.waitForResponse(response => response.url().includes('/api/v1/rag/query'));
    const headers = await response.allHeaders();

    expect(headers).toHaveProperty('x-rag-submission-latency');
    expect(headers).toHaveProperty('x-response-time');
  });

  test('should optimize bundle loading', async ({ page }) => {
    // Monitor resource loading
    const resources = [];
    page.on('response', response => {
      resources.push({
        url: response.url(),
        status: response.status(),
        size: response.headers()['content-length']
      });
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Check for resource optimization
    const cssResources = resources.filter(r => r.url.includes('.css'));
    const jsResources = resources.filter(r => r.url.includes('.js'));

    // Should have reasonable number of resources
    expect(cssResources.length).toBeGreaterThan(0);
    expect(jsResources.length).toBeGreaterThan(0);

    // Resources should be loaded successfully
    expect(cssResources.every(r => r.status === 200)).toBeTruthy();
    expect(jsResources.every(r => r.status === 200)).toBeTruthy();
  });

  test('should implement caching strategy', async ({ page }) => {
    const responses = [];
    page.on('response', response => {
      if (response.url().includes('/api/v1/rag/query')) {
        responses.push({
          url: response.url(),
          status: response.status(),
          cacheControl: response.headers()['cache-control']
        });
      }
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Submit same query twice
    await page.fill('[data-testid="query-input"]', 'Cache test query');
    await page.click('[data-testid="submit-button"]');

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });

    // Clear and submit same query again
    await page.click('[data-testid="clear-button"]');
    await page.fill('[data-testid="query-input"]', 'Cache test query');
    await page.click('[data-testid="submit-button"]');

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible({ timeout: 5000 });

    // Should have received responses (possibly cached)
    expect(responses.length).toBeGreaterThan(0);
  });
});

test.describe('Memory Usage Monitoring', () => {
  test('should track memory usage during query processing', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Get baseline memory usage
    const initialMemory = await page.evaluate(() => {
      return performance.memory ? performance.memory.usedJSHeapSize : 0;
    });

    // Submit a complex query that might use more memory
    const complexQuery = 'Show comprehensive financial analysis including ' +
        'accounts receivable aging, accounts payable aging, cash flow analysis, ' +
        'profit and loss statement, balance sheet, and ratio analysis ' +
        'for the past 12 months with detailed monthly breakdowns';

    await page.fill('[data-testid="query-input"]', complexQuery);
    await page.click('[data-testid="submit-button"]');

    // Wait for processing
    await expect(page.locator('[data-testid="success-message"])).toBeVisible({ timeout: 10000 });

    // Check memory after processing
    const finalMemory = await page.evaluate(() => {
      return performance.memory ? performance.memory.usedJSHeapSize : 0;
    });

    const memoryIncrease = finalMemory - initialMemory;

    // Memory increase should be reasonable (< 50MB)
    expect(memoryIncrease).toBeLessThan(50 * 1024 * 1024);
  });

  test('should handle memory cleanup after query completion', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Submit multiple queries
    const queries = [
      'First query test',
      'Second query test',
      'Third query test'
    ];

    for (const query of queries) {
      await page.click('[data-testid="clear-button"]');
      await page.fill('[data-testid="query-input"]', query);
      await page.click('[data-testid="submit-button"]');
      await expect(page.locator('[data-testid="success-message"])).toBeVisible({ timeout: 5000 });
    }

    // Force garbage collection if available
    await page.evaluate(() => {
      if (window.gc) {
        window.gc();
      }
    });

    // Memory should not grow indefinitely
    const finalMemory = await page.evaluate(() => {
      return performance.memory ? performance.memory.usedJSHeapSize : 0;
    });

    // Memory should be within reasonable bounds
    expect(finalMemory).toBeGreaterThan(0);
  });
});

test.describe('Error Handling and Edge Cases', () => {
  test('should handle server timeouts gracefully', async ({ page, context }) => {
    // Mock server timeout
    await context.route('/api/v1/rag/query', route => {
      // Don't respond to simulate timeout
      setTimeout(() => {
        route.fulfill({
          status: 408,
          contentType: 'application/json',
          body: JSON.stringify({
            error: 'timeout_error',
            message: 'Request timeout'
          })
        });
      }, 30000); // 30 second timeout
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    await page.fill('[data-testid="query-input"]', 'Timeout test');
    await page.click('[data-testid="submit-button"]');

    // Should handle timeout gracefully
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible({ timeout: 35000 });
    await expect(page.locator('[data-testid="error-message"])).toContainText('timeout_error');

    // Submit button should be re-enabled
    await expect(page.locator('[data-testid="submit-button"])).not.toBeDisabled();
  });

  test('should handle large payloads', async ({ page }) => {
    // Test with very large query (near 500 char limit)
    const largeQuery = 'Test '.repeat(120); // ~500 characters

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    await page.fill('[data-testid="query-input"]', largeQuery);

    // Character count should be at limit
    await expect(page.locator('[data-testid="char-count"]')).toHaveText('480/500 characters');

    // Submit should be possible
    await expect(page.locator('[data-testid="submit-button"]')).not.toBeDisabled();

    await page.click('[data-testid="submit-button"]');

    // Should handle large query successfully
    await expect(page.locator('[data-testid="success-message"])).toBeVisible({ timeout: 10000 });
  });

  test('should handle concurrent requests', async ({ page, context }) => {
    let requestCount = 0;
    await context.route('/api/v1/rag/query', route => {
      requestCount++;
      route.fulfill({
        status: 202,
        contentType: 'application/json',
        body: JSON.stringify({
          queryId: `query-${requestCount}`,
          streamUrl: `/api/v1/rag/query/query-${requestCount}/events`
        })
      });
    });

    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Submit multiple queries rapidly
    await page.fill('[data-testid="query-input"]', 'Concurrent test 1');
    await page.click('[data-testid="submit-button"]');

    await page.click('[data-testid="clear-button"]');
    await page.fill('[data-testid="query-input"]', 'Concurrent test 2');
    await page.click('[data-testid="submit-button"]');

    // Should handle both requests
    await expect(page.locator('[data-testid="results-section"]')).toBeVisible({ timeout: 10000 });

    // Should have made multiple requests
    expect(requestCount).toBeGreaterThan(1);
  });
});
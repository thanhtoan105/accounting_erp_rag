import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import QueryTestInterface from './QueryTestInterface';
import { QueryRequest, QueryResponse } from './QueryTestInterface';
import { createMockQueryRequest, createMockQueryResponse, waitForNextTick } from '../setupTests';

// Mock console methods to avoid noise in tests
const originalConsoleError = console.error;
const originalConsoleLog = console.log;

describe('QueryTestInterface', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    console.error = vi.fn();
    console.log = vi.fn();
  });

  afterEach(() => {
    console.error = originalConsoleError;
    console.log = originalConsoleLog;
  });

  describe('Initial Render', () => {
    it('should render all form elements', () => {
      render(<QueryTestInterface />);

      // Check main heading
      expect(screen.getByText('Submit RAG Query')).toBeInTheDocument();

      // Check form inputs
      expect(screen.getByLabelText('Company ID')).toBeInTheDocument();
      expect(screen.getByLabelText('Language')).toBeInTheDocument();
      expect(screen.getByLabelText('Query')).toBeInTheDocument();
      expect(screen.getByLabelText('Module (Optional)')).toBeInTheDocument();
      expect(screen.getByLabelText('Fiscal Period (Optional)')).toBeInTheDocument();

      // Check buttons
      expect(screen.getByRole('button', { name: 'Submit Query' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Clear' })).toBeInTheDocument();

      // Check sample queries section
      expect(screen.getByText('Sample Queries')).toBeInTheDocument();
    });

    it('should have correct default values', () => {
      render(<QueryTestInterface />);

      // Check default values
      const companyIdInput = screen.getByLabelText('Company ID') as HTMLInputElement;
      expect(companyIdInput.value).toBe('test-company-id');

      const languageSelect = screen.getByLabelText('Language') as HTMLSelectElement;
      expect(languageSelect.value).toBe('en');

      const queryTextarea = screen.getByLabelText('Query') as HTMLTextAreaElement;
      expect(queryTextarea.value).toBe('');

      expect(screen.getByDisplayValue('All Modules')).toBeInTheDocument();
    });

    it('should display character count for query', () => {
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query') as HTMLTextAreaElement;

      // Initially should show 0/500
      expect(screen.getByText('0/500 characters')).toBeInTheDocument();

      // Type some text
      fireEvent.change(queryTextarea, { target: { value: 'test query' } });

      // Should update character count
      expect(screen.getByText('11/500 characters')).toBeInTheDocument();
    });
  });

  describe('Form Interaction', () => {
    it('should update company ID when typed', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const companyIdInput = screen.getByLabelText('Company ID');
      await user.clear(companyIdInput);
      await user.type(companyIdInput, 'new-company-id');

      expect(companyIdInput).toHaveValue('new-company-id');
    });

    it('should switch language when selection changes', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const languageSelect = screen.getByLabelText('Language');
      await user.selectOptions(languageSelect, ['Vietnamese']);

      expect(languageSelect).toHaveValue('vi');
    });

    it('should update query when typed', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'What are outstanding invoices?');

      expect(queryTextarea).toHaveValue('What are outstanding invoices?');
      expect(screen.getByText('30/500 characters')).toBeInTheDocument();
    });

    it('should set optional filters when values are entered', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Select module
      const moduleSelect = screen.getByLabelText('Module (Optional)');
      await user.selectOptions(moduleSelect, ['Accounts Receivable']);
      expect(moduleSelect).toHaveValue('ar');

      // Set fiscal period
      const fiscalPeriodInput = screen.getByLabelText('Fiscal Period (Optional)');
      await user.type(fiscalPeriodInput, '2024-10');
      expect(fiscalPeriodInput).toHaveValue('2024-10');
    });

    it('should clear all fields when Clear button clicked', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Fill some values
      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'test query');
      await user.tab(); // Move to language
      await user.tab(); // Move to module
      await user.selectOptions(screen.getByLabelText('Module (Optional)'), ['Accounts Payable']);

      // Click clear button
      const clearButton = screen.getByRole('button', { name: 'Clear' });
      await user.click(clearButton);

      // Check all fields are cleared
      expect(queryTextarea).toHaveValue('');
      expect(screen.getByDisplayValue('All Modules')).toBeInTheDocument();
      expect(screen.getByLabelText('Fiscal Period (Optional)')).toHaveValue('');
    });
  });

  describe('Query Submission', () => {
    it('should enable submit button when query is entered', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      const queryTextarea = screen.getByLabelText('Query');

      // Initially disabled
      expect(submitButton).toBeDisabled();

      // Type query
      await user.type(queryTextarea, 'test query');

      // Should be enabled
      expect(submitButton).not.toBeDisabled();
    });

    it('should disable submit button during submission', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      const submitButton = screen.getByRole('button', { name: 'Submit Query' });

      await user.type(queryTextarea, 'test query');
      await user.click(submitButton);

      // Should show loading state
      expect(screen.getByText('Submitting...')).toBeInTheDocument();
      expect(submitButton).toBeDisabled();
    });

    it('should submit query with correct data', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Fill form
      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'Show accounts receivable aging');

      const languageSelect = screen.getByLabelText('Language');
      await user.selectOptions(languageSelect, ['English']);

      const moduleSelect = screen.getByLabelText('Module (Optional)');
      await user.selectOptions(moduleSelect, ['Accounts Receivable']);

      const fiscalPeriodInput = screen.getByLabelText('Fiscal Period (Optional)');
      await user.type(fiscalPeriodInput, '2024-10');

      // Submit form
      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Wait for API call
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith('/api/v1/rag/query', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: expect.stringContaining('Show accounts receivable aging')
        });
      });
    });

    it('should handle successful submission', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'test query');

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Wait for success message
      await waitFor(() => {
        expect(screen.getByText(/Success! Query submitted with ID:/)).toBeInTheDocument();
        expect(screen.getByText('test-query-id-12345')).toBeInTheDocument();
      });

      // Check success message styling
      const successMessage = screen.getByText('Success!');
      expect(successMessage.parentElement).toHaveClass('bg-green-50');
    });

    it('should handle submission error', async () => {
      // Mock fetch to return error
      (global.fetch as any).mockImplementationOnce(() =>
        Promise.resolve({
          ok: false,
          status: 400,
          json: () => ({
            error: 'validation_error',
            message: 'Query is required',
            status: 400
          })
        })
      );

      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      await user.clear(queryTextarea); // Empty query to trigger validation

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Wait for error message
      await waitFor(() => {
        expect(screen.getByText('Error:')).toBeInTheDocument();
        expect(screen.getByText('Query is required')).toBeInTheDocument();
      });

      // Check error message styling
      const errorMessage = screen.getByText('Error:');
      expect(errorMessage.parentElement).toHaveClass('bg-red-50');
    });
  });

  describe('Bilingual Support', () => {
    it('should support Vietnamese language', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const languageSelect = screen.getByLabelText('Language');
      await user.selectOptions(languageSelect, ['Vietnamese']);

      expect(languageSelect).toHaveValue('vi');
    });

    it('should handle Vietnamese query submission', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Fill with Vietnamese content
      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'Khách hàng nào còn nợ phải trả?');

      const languageSelect = screen.getByLabelText('Language');
      await user.selectOptions(languageSelect, ['Vietnamese']);

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Should submit with Vietnamese language
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith('/api/v1/rag/query',
          expect.objectContaining({
            body: expect.stringContaining('"language":"vi"')
          })
        );
      });
    });

    it('should handle mixed language queries', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      await user.type(queryTextarea, 'Show báo cáo debt aging cho customer ABC');

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Should submit the mixed language query
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith('/api/v1/rag/query',
          expect.objectContaining({
            body: expect.stringContaining('báo cáo')
          })
        );
      });
    });

    it('should handle Unicode characters in queries', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      const unicodeQuery = '📊 Tổng nợ: $1,234,567.89 (€1,099.99) - Khách hàng: Nguyễn Văn An';
      await user.type(queryTextarea, unicodeQuery);

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      await user.click(submitButton);

      // Should handle Unicode correctly
      expect(queryTextarea).toHaveValue(unicodeQuery);
      expect(screen.getByText(`${unicodeQuery.length}/500 characters`)).toBeInTheDocument();
    });
  });

  describe('Sample Queries', () => {
    it('should display sample queries in both languages', () => {
      render(<QueryTestInterface />);

      // Check Vietnamese samples
      expect(screen.getByText('Tổng công nợ phải thu hiện tại là bao nhiêu?')).toBeInTheDocument();
      expect(screen.getByText('Hiển thị các hóa đơn chưa thanh toán trong tháng này')).toBeInTheDocument();
      expect(screen.getByText('Tổng chi phí quý 4 là bao nhiêu?')).toBeInTheDocument();
      expect(screen.getByText('Danh sách khách hàng nợ quá hạn')).toBeInTheDocument();

      // Check English samples
      expect(screen.getByText('What is current accounts receivable balance?')).toBeInTheDocument();
      expect(screen.getByText('Show me unpaid invoices from this month')).toBeInTheDocument();
      expect(screen.getByText('What are our total expenses for Q4?')).toBeInTheDocument();
      expect(screen.getByText('List all customers with overdue payments')).toBeInTheDocument();
    });

    it('should set language label correctly for samples', () => {
      render(<QueryTestInterface />);

      const sampleButtons = screen.getAllByRole('button');

      // Check Vietnamese samples
      const vietnameseSample = sampleButtons.find(button =>
        button.textContent?.includes('Tổng công nợ')
      );
      expect(vietnameseSample).toBeInTheDocument();

      // Check English samples
      const englishSample = sampleButtons.find(button =>
        button.textContent?.includes('What is current')
      );
      expect(englishSample).toBeInTheDocument();
    });

    it('should populate form when sample query is clicked', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Click Vietnamese sample query
      const vietnameseSample = screen.getByText('Tổng công nợ phải thu hiện tại là bao nhiêu?');
      await user.click(vietnameseSample);

      // Should populate query field
      const queryTextarea = screen.getByLabelText('Query');
      expect(queryTextarea).toHaveValue('Tổng công nợ phải thu hiện tại là bao nhiêu?');

      // Should update character count
      expect(screen.getByText('52/500 characters')).toBeInTheDocument();
    });

    it('should switch language context for sample queries', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Switch to Vietnamese
      const languageSelect = screen.getByLabelText('Language');
      await user.selectOptions(languageSelect, ['Vietnamese']);

      // All sample buttons should still be visible
      expect(screen.getByText('Tổng công nợ phải thu hiện tại là bao nhiêu?')).toBeInTheDocument();
      expect(screen.getByText('What is current accounts receivable balance?')).toBeInTheDocument();

      // Sample query buttons should work regardless of language
      const englishSample = screen.getByText('What is current accounts receivable balance?');
      await user.click(englishSample);

      const queryTextarea = screen.getByLabelText('Query');
      expect(queryTextarea).toHaveValue('What is current accounts receivable balance?');
    });
  });

  describe('Results Display', () => {
    it('should display submitted results when available', async () => {
      // Mock localStorage to have results
      const mockResults = [
        {
          queryId: 'test-1',
          streamUrl: '/api/v1/rag/query/test-1/events'
        },
        {
          queryId: 'test-2',
          streamUrl: '/api/v1/rag/query/test-2/events'
        }
      ];

      // Mock React Query cache
      vi.mock('@tanstack/react-query', () => ({
        useQuery: vi.fn(() => ({ data: mockResults, isLoading: false }))
      }));

      render(<QueryTestInterface />);

      // Should show results section
      expect(screen.getByText('Recent Results')).toBeInTheDocument();
      expect(screen.getByText('Query ID: test-1')).toBeInTheDocument();
      expect(screen.getByText('Stream URL: /api/v1/rag/query/test-1/events')).toBeInTheDocument();
      expect(screen.getByText('Query ID: test-2')).toBeInTheDocument();
      expect(screen.getByText('Stream URL: /api/v1/rag/query/test-2/events')).toBeInTheDocument();
    });

    it('should format timestamps in results', async () => {
      const fixedDate = new Date('2024-01-15T10:30:00.000Z');
      vi.spyOn(global, 'Date').mockImplementation(() => fixedDate as any);

      const mockResults = [
        {
          queryId: 'test-1',
          streamUrl: '/api/v1/rag/query/test-1/events'
        }
      ];

      vi.mock('@tanstack/react-query', () => ({
        useQuery: vi.fn(() => ({ data: mockResults, isLoading: false }))
      }));

      render(<QueryTestInterface />);

      // Should show formatted time
      expect(screen.getByText(/10:30:00/)).toBeInTheDocument();

      vi.restoreAll();
    });

    it('should not display results when empty', async () => {
      vi.mock('@tanstack/react-query', () => ({
        useQuery: vi.fn(() => ({ data: [], isLoading: false }))
      }));

      render(<QueryTestInterface />);

      // Results section should not be present
      expect(screen.queryByText('Recent Results')).not.toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper form labels', () => {
      render(<QueryTestInterface />);

      // Check all form controls have associated labels
      expect(screen.getByLabelText('Company ID')).toBeInTheDocument();
      expect(screen.getByLabelText('Language')).toBeInTheDocument();
      expect(screen.getByLabelText('Query')).toBeInTheDocument();
      expect(screen.getByLabelText('Module (Optional)')).toBeInTheDocument();
      expect(screen.getByLabelText('Fiscal Period (Optional)')).toBeInTheDocument();
    });

    it('should have proper button labels', () => {
      render(<QueryTestInterface />);

      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      const clearButton = screen.getByRole('button', { name: 'Clear' });

      expect(submitButton).toBeInTheDocument();
      expect(clearButton).toBeInTheDocument();
    });

    it('should have proper heading structure', () => {
      render(<QueryTestInterface />);

      const mainHeading = screen.getByRole('heading', { name: 'Submit RAG Query' });
      const sampleHeading = screen.getByRole('heading', { name: 'Sample Queries' });

      expect(mainHeading).toBeInTheDocument();
      expect(sampleHeading).toBeInTheDocument();
      expect(mainHeading.tagName).toBe('H2');
      expect(sampleHeading.tagName).toBe('H3');
    });

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      // Tab through form fields
      await user.tab();
      await user.tab();
      await user.tab();
      await user.tab();
      await user.tab();

      // Focus should be on submit button (after cycling through form)
      const submitButton = screen.getByRole('button', { name: 'Submit Query' });
      expect(submitButton).toHaveFocus();
    });
  });

  describe('Input Validation', () => {
    it('should enforce maximum query length', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      const longQuery = 'a'.repeat(600); // Over 500 character limit

      await user.clear(queryTextarea);
      await user.type(queryTextarea, longQuery);

      // Should be truncated at 500 characters
      expect(queryTextarea).toHaveValue('a'.repeat(500));
      expect(screen.getByText('500/500 characters')).toBeInTheDocument();
    });

    it('should handle empty query submission', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const queryTextarea = screen.getByLabelText('Query');
      const submitButton = screen.getByRole('button', { name: 'Submit Query' });

      // Submit with empty query
      await user.click(submitButton);

      // Submit should remain disabled
      expect(submitButton).toBeDisabled();
    });

    it('should validate company ID format', async () => {
      const user = userEvent.setup();
      render(<QueryTestInterface />);

      const companyIdInput = screen.getByLabelText('Company ID');

      // Accept valid UUID format
      await user.clear(companyIdInput);
      await user.type(companyIdInput, '123e4567-e89b-12d3-a456-426614174000');
      expect(companyIdInput).toHaveValue('123e4567-e89b-12d3-a456-426614174000');
    });
  });
});
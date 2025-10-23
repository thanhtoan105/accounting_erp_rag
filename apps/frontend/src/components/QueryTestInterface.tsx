import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';

interface QueryRequest {
  companyId: string;
  query: string;
  language: 'en' | 'vi';
  filters: Record<string, any>;
}

interface QueryResponse {
  queryId: string;
  streamUrl: string;
}

export const QueryTestInterface: React.FC = () => {
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState<'en' | 'vi'>('en');
  const [companyId, setCompanyId] = useState('test-company-id');
  const [module, setModule] = useState('');
  const [fiscalPeriod, setFiscalPeriod] = useState('');
  const [results, setResults] = useState<any[]>([]);

  const queryMutation = useMutation({
    mutationFn: async (queryData: QueryRequest): Promise<QueryResponse> => {
      const response = await fetch('/api/v1/rag/query', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // Add auth header when available
        },
        body: JSON.stringify(queryData),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return response.json();
    },
    onSuccess: (data) => {
      setResults(prev => [data, ...prev]);
    },
    onError: (error) => {
      console.error('Query failed:', error);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const filters: Record<string, any> = {};
    if (module) filters.module = module;
    if (fiscalPeriod) filters.fiscalPeriod = fiscalPeriod;

    queryMutation.mutate({
      companyId,
      query,
      language,
      filters,
    });
  };

  const sampleQueries = [
    { en: 'What is the current accounts receivable balance?', vi: 'Tổng công nợ phải thu hiện tại là bao nhiêu?' },
    { en: 'Show me unpaid invoices from this month', vi: 'Hiển thị các hóa đơn chưa thanh toán trong tháng này' },
    { en: 'What are our total expenses for Q4?', vi: 'Tổng chi phí quý 4 là bao nhiêu?' },
    { en: 'List all customers with overdue payments', vi: 'Danh sách khách hàng nợ quá hạn' },
  ];

  return (
    <div className="space-y-6">
      {/* Query Form */}
      <div className="bg-white shadow rounded-lg p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">
          Submit RAG Query
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="companyId" className="block text-sm font-medium text-gray-700">
                Company ID
              </label>
              <input
                type="text"
                id="companyId"
                value={companyId}
                onChange={(e) => setCompanyId(e.target.value)}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              />
            </div>

            <div>
              <label htmlFor="language" className="block text-sm font-medium text-gray-700">
                Language
              </label>
              <select
                id="language"
                value={language}
                onChange={(e) => setLanguage(e.target.value as 'en' | 'vi')}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              >
                <option value="en">English</option>
                <option value="vi">Vietnamese</option>
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="query" className="block text-sm font-medium text-gray-700">
              Query
            </label>
            <textarea
              id="query"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              rows={3}
              maxLength={500}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              placeholder="Enter your accounting query..."
            />
            <div className="mt-1 text-sm text-gray-500">
              {query.length}/500 characters
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="module" className="block text-sm font-medium text-gray-700">
                Module (Optional)
              </label>
              <select
                id="module"
                value={module}
                onChange={(e) => setModule(e.target.value)}
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              >
                <option value="">All Modules</option>
                <option value="ar">Accounts Receivable</option>
                <option value="ap">Accounts Payable</option>
                <option value="gl">General Ledger</option>
                <option value="bank">Cash & Bank</option>
              </select>
            </div>

            <div>
              <label htmlFor="fiscalPeriod" className="block text-sm font-medium text-gray-700">
                Fiscal Period (Optional)
              </label>
              <input
                type="text"
                id="fiscalPeriod"
                value={fiscalPeriod}
                onChange={(e) => setFiscalPeriod(e.target.value)}
                placeholder="YYYY-MM"
                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
              />
            </div>
          </div>

          <div className="flex space-x-3">
            <button
              type="submit"
              disabled={queryMutation.isPending || !query.trim()}
              className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {queryMutation.isPending ? 'Submitting...' : 'Submit Query'}
            </button>

            <button
              type="button"
              onClick={() => {
                setQuery('');
                setModule('');
                setFiscalPeriod('');
              }}
              className="inline-flex justify-center py-2 px-4 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Clear
            </button>
          </div>
        </form>

        {queryMutation.error && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md">
            <div className="text-sm text-red-700">
              <strong>Error:</strong> {queryMutation.error.message}
            </div>
          </div>
        )}

        {queryMutation.isSuccess && (
          <div className="mt-4 p-4 bg-green-50 border border-green-200 rounded-md">
            <div className="text-sm text-green-700">
              <strong>Success!</strong> Query submitted with ID: {queryMutation.data.queryId}
            </div>
          </div>
        )}
      </div>

      {/* Sample Queries */}
      <div className="bg-white shadow rounded-lg p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">
          Sample Queries
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {sampleQueries.map((sample, index) => (
            <div key={index} className="border rounded-lg p-4">
              <div className="text-sm font-medium text-gray-900 mb-2">
                {language === 'en' ? 'English' : 'Vietnamese'}
              </div>
              <button
                onClick={() => setQuery(sample[language])}
                className="text-left text-sm text-gray-600 hover:text-blue-600 transition-colors"
              >
                {sample[language]}
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Results */}
      {results.length > 0 && (
        <div className="bg-white shadow rounded-lg p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Recent Results
          </h3>
          <div className="space-y-3">
            {results.map((result, index) => (
              <div key={index} className="border rounded-lg p-4">
                <div className="flex justify-between items-start">
                  <div>
                    <div className="text-sm font-medium text-gray-900">
                      Query ID: {result.queryId}
                    </div>
                    <div className="text-sm text-gray-600 mt-1">
                      Stream URL: {result.streamUrl}
                    </div>
                  </div>
                  <div className="text-xs text-gray-500">
                    {new Date().toLocaleTimeString()}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
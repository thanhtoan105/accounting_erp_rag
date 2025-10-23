import React from 'react';
import { QueryTestInterface } from './components/QueryTestInterface';
import './App.css';

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Accounting ERP - API Test Interface
              </h1>
              <p className="mt-1 text-sm text-gray-500">
                Test backend RAG query API functionality
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-500">Target API:</span>
              <code className="px-2 py-1 bg-gray-100 rounded text-sm">
                {import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}
              </code>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <QueryTestInterface />
        </div>
      </main>

      <footer className="bg-white border-t mt-auto">
        <div className="max-w-7xl mx-auto py-4 px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm text-gray-500">
            Test Interface for Story 1.5 - RAG Query Processing Pipeline
          </p>
        </div>
      </footer>
    </div>
  );
}

export default App;
-- Test schema for Testcontainers integration tests (Story 1.5)

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create accounting schema
CREATE SCHEMA IF NOT EXISTS accounting;

-- Create vector_documents table with both vector and full-text search support
CREATE TABLE IF NOT EXISTS accounting.vector_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    source_id UUID NOT NULL,
    fiscal_period VARCHAR(7),
    content TEXT NOT NULL,
    content_tsv TSVECTOR,
    embedding VECTOR(1536),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create HNSW index for vector similarity search
CREATE INDEX IF NOT EXISTS vector_documents_embedding_idx 
ON accounting.vector_documents 
USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS vector_documents_content_tsv_idx 
ON accounting.vector_documents 
USING GIN(content_tsv);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_vector_documents_company_id ON accounting.vector_documents(company_id);
CREATE INDEX IF NOT EXISTS idx_vector_documents_fiscal_period ON accounting.vector_documents(fiscal_period);
CREATE INDEX IF NOT EXISTS idx_vector_documents_deleted_at ON accounting.vector_documents(deleted_at);

-- Function to automatically update tsvector column
CREATE OR REPLACE FUNCTION accounting.vector_documents_tsvector_update() 
RETURNS trigger AS $$
BEGIN
    -- Support both Vietnamese and English text search
    NEW.content_tsv := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to maintain tsvector column
CREATE TRIGGER tsvector_update_trigger 
BEFORE INSERT OR UPDATE ON accounting.vector_documents
FOR EACH ROW 
EXECUTE FUNCTION accounting.vector_documents_tsvector_update();

-- Create rag_queries table
CREATE TABLE IF NOT EXISTS accounting.rag_queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    language VARCHAR(2) NOT NULL,
    query_text VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    filters JSONB,
    embedding_latency_ms INTEGER,
    retrieval_latency_ms INTEGER,
    total_latency_ms INTEGER,
    retrieved_doc_count INTEGER,
    pruned_doc_count INTEGER,
    recall_at_10 FLOAT,
    error_state JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_rag_queries_company_id ON accounting.rag_queries(company_id);
CREATE INDEX idx_rag_queries_status ON accounting.rag_queries(status);

-- Create rag_query_documents junction table
CREATE TABLE IF NOT EXISTS accounting.rag_query_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    query_id UUID NOT NULL REFERENCES accounting.rag_queries(id),
    document_vector_id UUID NOT NULL REFERENCES accounting.vector_documents(id),
    rank INTEGER NOT NULL,
    relevance_score FLOAT NOT NULL,
    tokens_used INTEGER,
    excerpt TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rag_query_documents_query_id ON accounting.rag_query_documents(query_id);
CREATE INDEX idx_rag_query_documents_document_id ON accounting.rag_query_documents(document_vector_id);

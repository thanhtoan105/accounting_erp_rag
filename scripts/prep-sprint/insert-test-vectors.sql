-- Insert 1000 test vectors for HNSW index benchmarking
-- Run this script to populate vector_documents table with random embeddings

INSERT INTO accounting.vector_documents (
    company_id,
    source_table,
    source_id,
    content_type,
    content_text,
    embedding,
    metadata,
    created_at,
    updated_at
)
SELECT
    1, -- company_id
    'test_table', -- source_table
    generate_series, -- source_id
    'text', -- content_type
    'Test document ' || generate_series || ' with sample content for RAG pipeline testing', -- content_text
    -- Generate random 1536-dimensional vector
    (SELECT array_agg(random())::vector(1536) FROM generate_series(1, 1536)),
    '{}', -- metadata (empty jsonb)
    NOW(),
    NOW()
FROM generate_series(1, 1000);

-- Verify insertion
SELECT COUNT(*) as total_vectors FROM accounting.vector_documents;

-- Create HNSW index on vector_documents.embedding for optimal vector similarity search
-- Recommended configuration: m=16, ef_construction=64 for balanced performance

-- Check if index already exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'vector_documents'
        AND indexname = 'idx_vector_docs_embedding_hnsw'
        AND schemaname = 'accounting'
    ) THEN
        -- Create HNSW index using cosine distance
        CREATE INDEX idx_vector_docs_embedding_hnsw
        ON accounting.vector_documents
        USING hnsw (embedding vector_cosine_ops)
        WITH (m = 16, ef_construction = 64);

        RAISE NOTICE 'HNSW index created successfully';
    ELSE
        RAISE NOTICE 'HNSW index already exists';
    END IF;
END $$;

-- Verify index creation
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size((schemaname || '.' || indexname)::regclass)) as index_size
FROM pg_indexes
WHERE tablename = 'vector_documents'
  AND indexname LIKE '%hnsw%';

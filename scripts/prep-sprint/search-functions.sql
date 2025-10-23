-- Helper functions for vector similarity search
-- Includes the search_similar_documents function for RAG pipeline

-- Function to search for similar documents using HNSW index
CREATE OR REPLACE FUNCTION accounting.search_similar_documents(
    query_embedding vector(1536),
    company_filter BIGINT DEFAULT NULL,
    limit_count INTEGER DEFAULT 10
)
RETURNS TABLE (
    document_id BIGINT,
    source_table VARCHAR,
    source_id BIGINT,
    content_text TEXT,
    similarity FLOAT
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Set optimal ef_search for this query
    EXECUTE 'SET LOCAL hnsw.ef_search = 40';

    -- Return similar documents
    RETURN QUERY
    SELECT
        vd.id,
        vd.source_table,
        vd.source_id,
        vd.content_text,
        1 - (vd.embedding <=> query_embedding) AS similarity
    FROM accounting.vector_documents vd
    WHERE (company_filter IS NULL OR vd.company_id = company_filter)
      AND vd.deleted_at IS NULL
    ORDER BY vd.embedding <=> query_embedding
    LIMIT limit_count;
END;
$$;

-- Test function with sample data
-- SELECT * FROM accounting.search_similar_documents(
--     (SELECT array_agg(random())::vector(1536) FROM generate_series(1, 1536)),
--     1, -- company_id filter
--     5  -- limit
-- );

-- Optional: Function to get embedding statistics
CREATE OR REPLACE FUNCTION accounting.get_embedding_stats()
RETURNS TABLE (
    total_documents BIGINT,
    avg_similarity FLOAT,
    min_similarity FLOAT,
    max_similarity FLOAT
)
LANGUAGE plpgsql
AS $$
DECLARE
    test_embedding vector(1536);
BEGIN
    -- Generate a test embedding for stats
    SELECT array_agg(random())::vector(1536) INTO test_embedding
    FROM generate_series(1, 1536);

    -- Set search parameters
    EXECUTE 'SET LOCAL hnsw.ef_search = 40';

    RETURN QUERY
    SELECT
        COUNT(*) as total_documents,
        AVG(1 - (vd.embedding <=> test_embedding)) as avg_similarity,
        MIN(1 - (vd.embedding <=> test_embedding)) as min_similarity,
        MAX(1 - (vd.embedding <=> test_embedding)) as max_similarity
    FROM accounting.vector_documents vd
    WHERE vd.deleted_at IS NULL;
END;
$$;

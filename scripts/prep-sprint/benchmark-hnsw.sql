-- HNSW Index Performance Benchmark
-- Test query performance with different ef_search values

-- Step 1: Generate a test query embedding
CREATE TEMP TABLE test_embedding AS
SELECT array_agg(random())::vector(1536) AS embedding
FROM generate_series(1, 1536);

-- Get the test embedding value
SELECT embedding FROM test_embedding;

-- Step 2: Benchmark queries with different ef_search values

-- Test 1: ef_search = 20 (faster, lower recall)
SET hnsw.ef_search = 20;
EXPLAIN ANALYZE
SELECT
    id,
    source_table,
    source_id,
    content_text,
    embedding <=> (SELECT embedding FROM test_embedding) AS distance
FROM accounting.vector_documents
ORDER BY embedding <=> (SELECT embedding FROM test_embedding)
LIMIT 10;

-- Test 2: ef_search = 40 (default, balanced)
SET hnsw.ef_search = 40;
EXPLAIN ANALYZE
SELECT
    id,
    source_table,
    source_id,
    content_text,
    embedding <=> (SELECT embedding FROM test_embedding) AS distance
FROM accounting.vector_documents
ORDER BY embedding <=> (SELECT embedding FROM test_embedding)
LIMIT 10;

-- Test 3: ef_search = 100 (higher recall, slower)
SET hnsw.ef_search = 100;
EXPLAIN ANALYZE
SELECT
    id,
    source_table,
    source_id,
    content_text,
    embedding <=> (SELECT embedding FROM test_embedding) AS distance
FROM accounting.vector_documents
ORDER BY embedding <=> (SELECT embedding FROM test_embedding)
LIMIT 10;

-- Step 3: Clean up
DROP TABLE test_embedding;

package com.erp.rag.supabase.performance;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance benchmark tests for vector similarity search with pgvector HNSW
 * index.
 * 
 * Story 1.3 – AC3: Validate P95 latency ≤ 1500ms for top-10 retrieval across
 * 10K, 50K, 100K vectors.
 * Story 1.3 – AC7: Record metrics for index tuning and regression tracking.
 * 
 * HNSW Parameters:
 * - m: number of connections in the graph (default: 16, range: 2-100)
 * - ef_construction: quality during index build (default: 64, range: 4-1000)
 * - ef_search: quality during search (runtime, default: 40, range: 1-1000)
 * 
 * Target: P95 ≤ 1500ms, P99 ≤ 3000ms for top-10 cosine similarity retrieval
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VectorPerformanceBenchmarkTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("vector_perf_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("postgres", "-c", "shared_buffers=256MB", "-c", "work_mem=16MB");

    private HikariDataSource dataSource;
    private UUID testCompanyId;

    // Benchmark configuration
    private static final int VECTOR_DIMENSIONS = 1536;
    private static final int WARMUP_QUERIES = 10;
    private static final int BENCHMARK_QUERIES = 100;
    private static final int TOP_K = 10;

    // Dataset sizes for benchmarking
    private static final int DATASET_10K = 10_000;
    private static final int DATASET_50K = 50_000;
    private static final int DATASET_100K = 100_000;

    // Performance targets (milliseconds)
    private static final long TARGET_P95_MS = 1500;
    private static final long TARGET_P99_MS = 3000;

    @BeforeAll
    void setUpDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(30000);
        config.setConnectionInitSql("SET search_path TO public,extensions,accounting");
        dataSource = new HikariDataSource(config);

        createSupabaseRolesAndSchema();
        runLiquibaseMigrations();
        testCompanyId = createTestCompany();
    }

    @AfterAll
    void tearDownDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Benchmark: 10K vectors - Baseline performance")
    void shouldMeetPerformanceTargetWith10KVectors() throws SQLException {
        System.out.println("\n=== Benchmark: 10K Vectors ===");

        // Load 10K vectors
        long loadStart = System.currentTimeMillis();
        insertBulkVectors(DATASET_10K);
        long loadDuration = System.currentTimeMillis() - loadStart;
        System.out.printf("Load time: %,d ms (%,d vectors)%n", loadDuration, DATASET_10K);

        // Run benchmark
        BenchmarkResult result = runBenchmark(DATASET_10K);

        // Print results
        printBenchmarkResults(result);

        // Assertions
        assertThat(result.p95Latency)
                .as("P95 latency should be ≤ 1500ms for 10K vectors")
                .isLessThanOrEqualTo(TARGET_P95_MS);

        assertThat(result.p99Latency)
                .as("P99 latency should be ≤ 3000ms for 10K vectors")
                .isLessThanOrEqualTo(TARGET_P99_MS);
    }

    @Test
    @Order(2)
    @DisplayName("Benchmark: 50K vectors - Medium scale performance")
    void shouldMeetPerformanceTargetWith50KVectors() throws SQLException {
        System.out.println("\n=== Benchmark: 50K Vectors ===");

        // Add 40K more vectors (we already have 10K from previous test)
        long loadStart = System.currentTimeMillis();
        insertBulkVectors(DATASET_50K - DATASET_10K);
        long loadDuration = System.currentTimeMillis() - loadStart;
        System.out.printf("Load time: %,d ms (%,d additional vectors)%n", loadDuration, DATASET_50K - DATASET_10K);

        // Run benchmark
        BenchmarkResult result = runBenchmark(DATASET_50K);

        // Print results
        printBenchmarkResults(result);

        // Assertions
        assertThat(result.p95Latency)
                .as("P95 latency should be ≤ 1500ms for 50K vectors")
                .isLessThanOrEqualTo(TARGET_P95_MS);

        assertThat(result.p99Latency)
                .as("P99 latency should be ≤ 3000ms for 50K vectors")
                .isLessThanOrEqualTo(TARGET_P99_MS);
    }

    @Test
    @Order(3)
    @DisplayName("Benchmark: 100K vectors - Full scale performance")
    void shouldMeetPerformanceTargetWith100KVectors() throws SQLException {
        System.out.println("\n=== Benchmark: 100K Vectors ===");

        // Add 50K more vectors (we already have 50K from previous tests)
        long loadStart = System.currentTimeMillis();
        insertBulkVectors(DATASET_100K - DATASET_50K);
        long loadDuration = System.currentTimeMillis() - loadStart;
        System.out.printf("Load time: %,d ms (%,d additional vectors)%n", loadDuration, DATASET_100K - DATASET_50K);

        // Run benchmark
        BenchmarkResult result = runBenchmark(DATASET_100K);

        // Print results
        printBenchmarkResults(result);

        // Assertions
        assertThat(result.p95Latency)
                .as("P95 latency should be ≤ 1500ms for 100K vectors")
                .isLessThanOrEqualTo(TARGET_P95_MS);

        assertThat(result.p99Latency)
                .as("P99 latency should be ≤ 3000ms for 100K vectors")
                .isLessThanOrEqualTo(TARGET_P99_MS);
    }

    @Test
    @Order(4)
    @DisplayName("HNSW Parameter Tuning: ef_search optimization")
    void shouldTuneEfSearchParameter() throws SQLException {
        System.out.println("\n=== HNSW Parameter Tuning: ef_search ===");

        int[] efSearchValues = { 10, 20, 40, 64, 100, 200 };
        List<ParameterTuningResult> results = new ArrayList<>();

        for (int efSearch : efSearchValues) {
            // Set ef_search parameter
            try (Connection conn = dataSource.getConnection();
                    Statement stmt = conn.createStatement()) {
                stmt.execute("SET hnsw.ef_search = " + efSearch);
            }

            // Run mini benchmark (20 queries)
            List<Long> latencies = new ArrayList<>();
            String queryVector = generateTestEmbedding(999);

            for (int i = 0; i < 20; i++) {
                long start = System.currentTimeMillis();
                executeVectorQuery(queryVector, TOP_K);
                long latency = System.currentTimeMillis() - start;
                latencies.add(latency);
            }

            long avgLatency = (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95 = calculatePercentile(latencies, 95);

            results.add(new ParameterTuningResult(efSearch, avgLatency, p95));
            System.out.printf("ef_search=%d: avg=%dms, P95=%dms%n", efSearch, avgLatency, p95);
        }

        // Find optimal ef_search (lowest latency that meets target)
        ParameterTuningResult optimal = results.stream()
                .filter(r -> r.p95Latency <= TARGET_P95_MS)
                .min(Comparator.comparingLong(r -> r.avgLatency))
                .orElse(results.get(results.size() - 1));

        System.out.printf("%nOptimal ef_search: %d (avg=%dms, P95=%dms)%n",
                optimal.parameterValue, optimal.avgLatency, optimal.p95Latency);

        assertThat(optimal.p95Latency)
                .as("Optimal ef_search should meet P95 target")
                .isLessThanOrEqualTo(TARGET_P95_MS);
    }

    @Test
    @Order(5)
    @DisplayName("Index size and memory usage analysis")
    void shouldAnalyzeIndexSizeAndMemory() throws SQLException {
        System.out.println("\n=== Index Size and Memory Analysis ===");

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Get table size
            ResultSet rs = stmt.executeQuery(
                    "SELECT pg_size_pretty(pg_total_relation_size('accounting.vector_documents')) as table_size");
            if (rs.next()) {
                System.out.println("Table size: " + rs.getString("table_size"));
            }

            // Get index sizes
            rs = stmt.executeQuery(
                    "SELECT indexrelname as indexname, " +
                            "pg_size_pretty(pg_relation_size(indexrelid)) as index_size " +
                            "FROM pg_stat_user_indexes " +
                            "WHERE schemaname = 'accounting' AND relname = 'vector_documents' " +
                            "ORDER BY pg_relation_size(indexrelid) DESC");

            System.out.println("\nIndex sizes:");
            while (rs.next()) {
                System.out.printf("  %s: %s%n",
                        rs.getString("indexname"),
                        rs.getString("index_size"));
            }

            // Get index statistics
            rs = stmt.executeQuery(
                    "SELECT schemaname, relname, indexrelname, idx_scan, idx_tup_read, idx_tup_fetch " +
                            "FROM pg_stat_user_indexes " +
                            "WHERE schemaname = 'accounting' AND relname = 'vector_documents'");

            System.out.println("\nIndex usage statistics:");
            while (rs.next()) {
                System.out.printf("  %s: scans=%d, tuples_read=%d, tuples_fetched=%d%n",
                        rs.getString("indexrelname"),
                        rs.getLong("idx_scan"),
                        rs.getLong("idx_tup_read"),
                        rs.getLong("idx_tup_fetch"));
            }
        }
    }

    /**
     * Run performance benchmark with warm-up and measurement phases
     */
    private BenchmarkResult runBenchmark(int datasetSize) throws SQLException {
        String queryVector = generateTestEmbedding(999);

        // Warm-up phase
        System.out.printf("Warm-up: %d queries...%n", WARMUP_QUERIES);
        for (int i = 0; i < WARMUP_QUERIES; i++) {
            executeVectorQuery(queryVector, TOP_K);
        }

        // Measurement phase
        System.out.printf("Measuring: %d queries...%n", BENCHMARK_QUERIES);
        List<Long> latencies = new ArrayList<>();

        for (int i = 0; i < BENCHMARK_QUERIES; i++) {
            long start = System.currentTimeMillis();
            int resultCount = executeVectorQuery(queryVector, TOP_K);
            long latency = System.currentTimeMillis() - start;
            latencies.add(latency);

            assertThat(resultCount)
                    .as("Should return exactly %d results", TOP_K)
                    .isEqualTo(TOP_K);
        }

        return new BenchmarkResult(datasetSize, latencies);
    }

    /**
     * Execute vector similarity query and return result count
     */
    private int executeVectorQuery(String queryVector, int limit) throws SQLException {
        String sql = "SELECT id, embedding <-> ?::vector AS distance " +
                "FROM accounting.vector_documents " +
                "WHERE company_id = ? AND deleted_at IS NULL " +
                "ORDER BY distance ASC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, queryVector);
            stmt.setObject(2, testCompanyId);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                count++;
            }
            return count;
        }
    }

    /**
     * Insert bulk vectors in batches for performance
     */
    private void insertBulkVectors(int count) throws SQLException {
        final int BATCH_SIZE = 1000;
        int inserted = 0;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "INSERT INTO accounting.vector_documents " +
                    "(company_id, document_id, source_table, source_id, embedding, metadata) " +
                    "VALUES (?, ?, ?, ?, ?::vector, ?::jsonb)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    stmt.setObject(1, testCompanyId);
                    stmt.setObject(2, UUID.randomUUID());
                    stmt.setString(3, "benchmark");
                    stmt.setObject(4, UUID.randomUUID());
                    stmt.setString(5, generateTestEmbedding(i));
                    stmt.setString(6, String.format("{\"batch\": %d, \"index\": %d}", i / BATCH_SIZE, i));
                    stmt.addBatch();

                    inserted++;

                    if (inserted % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                        conn.commit();
                        System.out.printf("\rInserted: %,d / %,d vectors", inserted, count);
                    }
                }

                // Execute remaining batch
                stmt.executeBatch();
                conn.commit();
                System.out.printf("\rInserted: %,d / %,d vectors (complete)%n", inserted, count);
            }

            conn.setAutoCommit(true);
        }
    }

    /**
     * Generate deterministic test embedding vector
     */
    private String generateTestEmbedding(int seed) {
        StringBuilder embedding = new StringBuilder("[");
        for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
            if (i > 0)
                embedding.append(",");
            double value = Math.sin(seed + i * 0.01);
            embedding.append(String.format("%.6f", value));
        }
        embedding.append("]");
        return embedding.toString();
    }

    /**
     * Calculate percentile from latency list
     */
    private long calculatePercentile(List<Long> latencies, int percentile) {
        List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    /**
     * Print formatted benchmark results
     */
    private void printBenchmarkResults(BenchmarkResult result) {
        System.out.printf("%nResults (%,d vectors):%n", result.datasetSize);
        System.out.printf("  Queries: %d%n", result.queryCount);
        System.out.printf("  Min:     %d ms%n", result.minLatency);
        System.out.printf("  Max:     %d ms%n", result.maxLatency);
        System.out.printf("  Avg:     %d ms%n", result.avgLatency);
        System.out.printf("  Median:  %d ms%n", result.medianLatency);
        System.out.printf("  P95:     %d ms %s%n", result.p95Latency,
                result.p95Latency <= TARGET_P95_MS ? "✓" : "✗");
        System.out.printf("  P99:     %d ms %s%n", result.p99Latency,
                result.p99Latency <= TARGET_P99_MS ? "✓" : "✗");
        System.out.printf("  Throughput: %.2f queries/sec%n", result.throughput);
    }

    private void runLiquibaseMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database);
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    private UUID createTestCompany() {
        UUID companyId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO accounting.companies (id, name) VALUES (?, ?)")) {
            insert.setObject(1, companyId);
            insert.setString(2, "Benchmark Test Company");
            insert.executeUpdate();
            return companyId;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create test company", e);
        }
    }

    private void createSupabaseRolesAndSchema() throws SQLException {
        String ensureRoles = "DO $$\n" +
                "BEGIN\n" +
                "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN\n" +
                "        CREATE ROLE authenticated;\n" +
                "    END IF;\n" +
                "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN\n" +
                "        CREATE ROLE service_role;\n" +
                "    END IF;\n" +
                "END$$;";

        try (Connection connection = postgres.createConnection("")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
                statement.execute(ensureRoles);
                statement.execute("CREATE SCHEMA IF NOT EXISTS accounting;");
                statement.execute("CREATE SCHEMA IF NOT EXISTS auth;");
                statement.execute("CREATE OR REPLACE FUNCTION auth.uid() RETURNS UUID LANGUAGE sql " +
                        "AS $$ SELECT current_setting('app.current_user_id', TRUE)::UUID $$;");

                // Create minimal companies table (required by FK constraint)
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS accounting.companies (" +
                                "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                "  name TEXT NOT NULL," +
                                "  created_at TIMESTAMPTZ DEFAULT now()," +
                                "  updated_at TIMESTAMPTZ DEFAULT now()" +
                                ")");

                // Create minimal user_profiles table (required by RLS policy)
                statement.execute(
                        "CREATE TABLE IF NOT EXISTS accounting.user_profiles (" +
                                "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                "  user_id UUID NOT NULL," +
                                "  company_id UUID NOT NULL REFERENCES accounting.companies(id)," +
                                "  role TEXT DEFAULT 'VIEWER'," +
                                "  created_at TIMESTAMPTZ DEFAULT now()" +
                                ")");
            }
        }
    }

    /**
     * Benchmark result data class
     */
    private static class BenchmarkResult {
        final int datasetSize;
        final int queryCount;
        final long minLatency;
        final long maxLatency;
        final long avgLatency;
        final long medianLatency;
        final long p95Latency;
        final long p99Latency;
        final double throughput;

        BenchmarkResult(int datasetSize, List<Long> latencies) {
            this.datasetSize = datasetSize;
            this.queryCount = latencies.size();

            LongSummaryStatistics stats = latencies.stream()
                    .mapToLong(Long::longValue)
                    .summaryStatistics();

            this.minLatency = stats.getMin();
            this.maxLatency = stats.getMax();
            this.avgLatency = (long) stats.getAverage();
            this.medianLatency = calculatePercentileStatic(latencies, 50);
            this.p95Latency = calculatePercentileStatic(latencies, 95);
            this.p99Latency = calculatePercentileStatic(latencies, 99);
            this.throughput = (queryCount * 1000.0) / stats.getSum();
        }

        private static long calculatePercentileStatic(List<Long> latencies, int percentile) {
            List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            return sorted.get(Math.max(0, index));
        }
    }

    /**
     * Parameter tuning result data class
     */
    private static class ParameterTuningResult {
        final int parameterValue;
        final long avgLatency;
        final long p95Latency;

        ParameterTuningResult(int parameterValue, long avgLatency, long p95Latency) {
            this.parameterValue = parameterValue;
            this.avgLatency = avgLatency;
            this.p95Latency = p95Latency;
        }
    }
}

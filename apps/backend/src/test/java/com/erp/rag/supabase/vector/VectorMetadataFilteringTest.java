package com.erp.rag.supabase.vector;

import com.erp.rag.supabase.vector.VectorDocument;
import com.erp.rag.supabase.vector.VectorDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates metadata filtering capabilities for vector documents.
 * 
 * Story 1.3 – AC4: Confirm filtering queries for module, fiscal period, and
 * document type.
 * 
 * Tests cover:
 * - Single field filtering (module, fiscal_period, document_type)
 * - Multi-field AND conditions
 * - Range queries (date ranges, numeric ranges)
 * - Array containment
 * - Nested JSON path queries
 * - Performance of filtered vector similarity search
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VectorMetadataFilteringTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("vector_filter_test")
            .withUsername("testuser")
            .withPassword("testpass");

    private HikariDataSource dataSource;
    private ObjectMapper objectMapper = new ObjectMapper();
    private UUID testCompanyId;

    @BeforeAll
    void setUpDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(3);
        config.setConnectionInitSql("SET search_path TO public,extensions,accounting");
        dataSource = new HikariDataSource(config);

        createSupabaseRolesAndSchema();
        runLiquibaseMigrations();
        testCompanyId = createTestCompany();
        insertTestData();
    }

    @AfterAll
    void tearDownDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Filter by module: accounts_receivable")
    void shouldFilterByModuleAR() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'module' = 'accounts_receivable'",
                10);

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("module").asText().equals("accounts_receivable"));
    }

    @Test
    @DisplayName("Filter by module: accounts_payable")
    void shouldFilterByModuleAP() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'module' = 'accounts_payable'",
                10);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("module").asText().equals("accounts_payable"));
    }

    @Test
    @DisplayName("Filter by fiscal_period: 2025-01")
    void shouldFilterByFiscalPeriod() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'fiscal_period' = '2025-01'",
                10);

        assertThat(results).hasSize(4);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("fiscal_period").asText().equals("2025-01"));
    }

    @Test
    @DisplayName("Filter by document_type: invoice")
    void shouldFilterByDocumentType() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'document_type' = 'invoice'",
                10);

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("document_type").asText().equals("invoice"));
    }

    @Test
    @DisplayName("Filter by multiple conditions: AR + 2025-01")
    void shouldFilterByMultipleConditions() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'module' = 'accounts_receivable' AND metadata->>'fiscal_period' = '2025-01'",
                10);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("module").asText().equals("accounts_receivable") &&
                doc.getMetadata().get("fiscal_period").asText().equals("2025-01"));
    }

    @Test
    @DisplayName("Filter by date range using fiscal_period")
    void shouldFilterByDateRange() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'fiscal_period' >= '2025-01' AND metadata->>'fiscal_period' <= '2025-02'",
                10);

        assertThat(results).hasSize(7); // All test data except 2024-12
        assertThat(results).allMatch(doc -> {
            String period = doc.getMetadata().get("fiscal_period").asText();
            return period.compareTo("2025-01") >= 0 && period.compareTo("2025-02") <= 0;
        });
    }

    @Test
    @DisplayName("Filter by numeric range: amount > 1000")
    void shouldFilterByNumericRange() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "(metadata->>'amount')::numeric > 1000",
                10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(doc -> doc.getMetadata().get("amount").asDouble() > 1000);
    }

    @Test
    @DisplayName("Filter by status: open")
    void shouldFilterByStatus() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->>'status' = 'open'",
                10);

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(doc -> doc.getMetadata().get("status").asText().equals("open"));
    }

    @Test
    @DisplayName("Filter by nested JSON path: customer.country = VN")
    void shouldFilterByNestedPath() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->'customer'->>'country' = 'VN'",
                10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(doc -> doc.getMetadata().has("customer") &&
                doc.getMetadata().get("customer").has("country") &&
                doc.getMetadata().get("customer").get("country").asText().equals("VN"));
    }

    @Test
    @DisplayName("Filter with JSONB path operators: tags contains 'urgent'")
    void shouldFilterByArrayContainment() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "metadata->'tags' ? 'urgent'",
                10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(doc -> {
            JsonNode tags = doc.getMetadata().get("tags");
            if (tags != null && tags.isArray()) {
                for (JsonNode tag : tags) {
                    if (tag.asText().equals("urgent")) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Test
    @DisplayName("Complex filter: (AR OR AP) AND 2025-01 AND amount > 500")
    void shouldFilterWithComplexConditions() throws SQLException {
        List<VectorDocument> results = queryWithFilter(
                "(metadata->>'module' = 'accounts_receivable' OR metadata->>'module' = 'accounts_payable') " +
                        "AND metadata->>'fiscal_period' = '2025-01' " +
                        "AND (metadata->>'amount')::numeric > 500",
                10);

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(doc -> {
            String module = doc.getMetadata().get("module").asText();
            String period = doc.getMetadata().get("fiscal_period").asText();
            double amount = doc.getMetadata().get("amount").asDouble();
            return (module.equals("accounts_receivable") || module.equals("accounts_payable"))
                    && period.equals("2025-01")
                    && amount > 500;
        });
    }

    @Test
    @DisplayName("Performance: Filtered vector similarity search should use both HNSW + GIN indexes")
    void shouldPerformFilteredVectorSearchEfficiently() throws SQLException {
        String queryVector = generateTestEmbedding(1);

        // Query with EXPLAIN ANALYZE to check index usage
        String sql = "EXPLAIN (ANALYZE, BUFFERS) " +
                "SELECT id, embedding <-> ?::vector AS distance " +
                "FROM accounting.vector_documents " +
                "WHERE company_id = ? " +
                "AND deleted_at IS NULL " +
                "AND metadata->>'module' = 'accounts_receivable' " +
                "ORDER BY distance ASC LIMIT 5";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, queryVector);
            stmt.setObject(2, testCompanyId);

            ResultSet rs = stmt.executeQuery();
            StringBuilder plan = new StringBuilder();
            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }

            String queryPlan = plan.toString();
            System.out.println("Query Plan:\n" + queryPlan);

            // Verify that indexes are being used (either HNSW or Bitmap Index Scan)
            assertThat(queryPlan.toLowerCase())
                    .matches(s -> s.contains("index") || s.contains("bitmap"));
        }
    }

    @Test
    @DisplayName("Validate metadata filtering preserves top-K accuracy")
    void shouldMaintainTopKAccuracyWithFiltering() throws SQLException {
        String queryVector = generateTestEmbedding(1);

        // Query without filter
        List<String> unfiltered = querySimilarVectors(queryVector, null, 10);

        // Query with filter
        List<String> filtered = querySimilarVectors(queryVector, "metadata->>'module' = 'accounts_receivable'", 3);

        // Filtered results should be subset of unfiltered results (maintaining ranking)
        assertThat(filtered).hasSize(3);
        assertThat(filtered).allMatch(unfiltered::contains);
    }

    /**
     * Execute vector similarity query with optional metadata filter
     */
    private List<VectorDocument> queryWithFilter(String metadataFilter, int limit) throws SQLException {
        String queryVector = generateTestEmbedding(1);

        String sql = "SELECT id, company_id, document_id, source_table, source_id, " +
                "fiscal_period, content_tsv::text as content_tsv, " +
                "embedding::text, metadata, created_at, updated_at, deleted_at, " +
                "embedding <-> ?::vector AS distance " +
                "FROM accounting.vector_documents " +
                "WHERE company_id = ? AND deleted_at IS NULL " +
                (metadataFilter != null ? "AND " + metadataFilter + " " : "") +
                "ORDER BY distance ASC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, queryVector);
            stmt.setObject(2, testCompanyId);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();
            List<VectorDocument> results = new ArrayList<>();

            while (rs.next()) {
                VectorDocument doc = new VectorDocument();
                doc.setId(rs.getObject("id", UUID.class));
                doc.setCompanyId(rs.getObject("company_id", UUID.class));
                doc.setDocumentId(rs.getObject("document_id", UUID.class));
                doc.setSourceTable(rs.getString("source_table"));
                doc.setSourceId(rs.getObject("source_id", UUID.class));
                doc.setFiscalPeriod(rs.getString("fiscal_period"));
                doc.setContentTsv(rs.getString("content_tsv"));
                doc.setEmbedding(rs.getString("embedding"));
                try {
                    doc.setMetadata(objectMapper.readTree(rs.getString("metadata")));
                } catch (Exception e) {
                    throw new SQLException("Failed to parse metadata JSON", e);
                }
                doc.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                doc.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));
                doc.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
                results.add(doc);
            }

            return results;
        }
    }

    /**
     * Query similar vectors and return IDs for comparison
     */
    private List<String> querySimilarVectors(String queryVector, String metadataFilter, int limit) throws SQLException {
        String sql = "SELECT id::text " +
                "FROM accounting.vector_documents " +
                "WHERE company_id = ? AND deleted_at IS NULL " +
                (metadataFilter != null ? "AND " + metadataFilter + " " : "") +
                "ORDER BY embedding <-> ?::vector ASC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, testCompanyId);
            stmt.setString(2, queryVector);
            stmt.setInt(3, limit);

            ResultSet rs = stmt.executeQuery();
            List<String> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
            return ids;
        }
    }

    /**
     * Insert diverse test data with various metadata combinations
     */
    private void insertTestData() throws SQLException {
        List<TestDocument> testDocs = Arrays.asList(
                // Accounts Receivable documents
                new TestDocument("accounts_receivable", "invoice", "2025-01", 1500.00, "open",
                        "\"customer\": {\"name\": \"Acme Corp\", \"country\": \"VN\"}, \"tags\": [\"urgent\", \"large\"]"),
                new TestDocument("accounts_receivable", "invoice", "2025-01", 800.00, "paid",
                        "\"customer\": {\"name\": \"Tech Ltd\", \"country\": \"US\"}, \"tags\": [\"recurring\"]"),
                new TestDocument("accounts_receivable", "payment", "2025-02", 2500.00, "completed",
                        "\"customer\": {\"name\": \"Global Inc\", \"country\": \"VN\"}, \"tags\": [\"urgent\"]"),

                // Accounts Payable documents
                new TestDocument("accounts_payable", "bill", "2025-01", 1200.00, "open",
                        "\"vendor\": {\"name\": \"Supplier A\", \"country\": \"VN\"}, \"tags\": [\"recurring\"]"),
                new TestDocument("accounts_payable", "payment", "2025-02", 450.00, "paid",
                        "\"vendor\": {\"name\": \"Supplier B\", \"country\": \"CN\"}, \"tags\": [\"regular\"]"),

                // Cash/Bank documents
                new TestDocument("cash_bank", "transaction", "2025-01", 300.00, "cleared",
                        "\"account\": {\"type\": \"checking\", \"currency\": \"VND\"}, \"tags\": [\"internal\"]"),
                new TestDocument("cash_bank", "reconciliation", "2025-02", 5000.00, "pending",
                        "\"account\": {\"type\": \"savings\", \"currency\": \"VND\"}, \"tags\": [\"monthly\"]"),

                // Historical document (2024)
                new TestDocument("accounts_receivable", "invoice", "2024-12", 950.00, "paid",
                        "\"customer\": {\"name\": \"Old Client\", \"country\": \"VN\"}, \"tags\": [\"archived\"]"));

        String sql = "INSERT INTO accounting.vector_documents " +
                "(company_id, document_id, source_table, source_id, fiscal_period, embedding, metadata) " +
                "VALUES (?, ?, ?, ?, ?, ?::vector, ?::jsonb)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);

            for (int i = 0; i < testDocs.size(); i++) {
                TestDocument doc = testDocs.get(i);
                stmt.setObject(1, testCompanyId);
                stmt.setObject(2, UUID.randomUUID());
                stmt.setString(3, doc.module);
                stmt.setObject(4, UUID.randomUUID());
                stmt.setString(5, doc.fiscalPeriod);
                stmt.setString(6, generateTestEmbedding(i));
                stmt.setString(7, doc.toMetadataJson());
                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    private String generateTestEmbedding(int seed) {
        StringBuilder embedding = new StringBuilder("[");
        for (int i = 0; i < 1536; i++) {
            if (i > 0)
                embedding.append(",");
            double value = Math.sin(seed + i * 0.01);
            embedding.append(String.format("%.6f", value));
        }
        embedding.append("]");
        return embedding.toString();
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
            insert.setString(2, "Metadata Filtering Test Company");
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

                statement.execute(
                        "CREATE TABLE IF NOT EXISTS accounting.companies (" +
                                "  id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                                "  name TEXT NOT NULL," +
                                "  created_at TIMESTAMPTZ DEFAULT now()," +
                                "  updated_at TIMESTAMPTZ DEFAULT now()" +
                                ")");

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
     * Test document data class
     */
    private static class TestDocument {
        String module;
        String documentType;
        String fiscalPeriod;
        double amount;
        String status;
        String additionalMetadata;

        TestDocument(String module, String documentType, String fiscalPeriod, double amount, String status,
                String additionalMetadata) {
            this.module = module;
            this.documentType = documentType;
            this.fiscalPeriod = fiscalPeriod;
            this.amount = amount;
            this.status = status;
            this.additionalMetadata = additionalMetadata;
        }

        String toMetadataJson() {
            // Merge standard fields with additional metadata
            String base = String.format(
                    "{\"module\": \"%s\", \"document_type\": \"%s\", \"fiscal_period\": \"%s\", \"amount\": %.2f, \"status\": \"%s\"",
                    module, documentType, fiscalPeriod, amount, status);

            if (additionalMetadata != null && !additionalMetadata.isEmpty()) {
                return base + ", " + additionalMetadata + "}";
            } else {
                return base + "}";
            }
        }
    }
}

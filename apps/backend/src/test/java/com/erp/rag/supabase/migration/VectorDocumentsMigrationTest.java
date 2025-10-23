package com.erp.rag.supabase.migration;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Liquibase migrations for the vector_documents table.
 * <p>
 * Story 1.3 – AC2: Ensures vector_documents schema, indexes, and RLS policies
 * are created correctly with multi-tenant partitioning support.
 * </p>
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VectorDocumentsMigrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName.parse("pgvector/pgvector:pg15")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("vector_test")
            .withUsername("testuser")
            .withPassword("testpass");

    private HikariDataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID testCompanyId;

    /**
     * Generate a 1536-dimensional test embedding vector.
     * OpenAI text-embedding-3-small uses 1536 dimensions.
     */
    private String generateTestEmbedding(int seed) {
        StringBuilder embedding = new StringBuilder("[");
        for (int i = 0; i < 1536; i++) {
            if (i > 0)
                embedding.append(",");
            // Use seed to create different but reproducible vectors
            double value = Math.sin(seed + i * 0.01);
            embedding.append(String.format("%.6f", value));
        }
        embedding.append("]");
        return embedding.toString();
    }

    @BeforeAll
    void setUpDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(2);
        config.setConnectionInitSql("SET search_path TO public,extensions,accounting");
        dataSource = new HikariDataSource(config);

        createSupabaseRoles();
        testCompanyId = createTestCompany();
    }

    @AfterAll
    void tearDownDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @BeforeEach
    void cleanUpVectorDocuments() {
        try (Connection connection = dataSource.getConnection();
                Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE accounting.vector_documents RESTART IDENTITY CASCADE");
        } catch (Exception e) {
            // Table might not exist yet, ignore
        }
    }

    @Test
    void shouldCreateVectorDocumentsTableWithCorrectSchema() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT column_name, data_type, is_nullable " +
                                "FROM information_schema.columns " +
                                "WHERE table_schema = 'accounting' " +
                                "AND table_name = 'vector_documents' " +
                                "ORDER BY ordinal_position")) {
            ResultSet rs = statement.executeQuery();

            List<String> columns = new ArrayList<>();
            while (rs.next()) {
                columns.add(rs.getString("column_name"));
            }

            assertThat(columns).containsExactly(
                    "id", "company_id", "document_id", "source_table", "source_id",
                    "fiscal_period", "content_tsv", "embedding", "metadata",
                    "created_at", "updated_at", "deleted_at");
        }
    }

    @Test
    void shouldCreateAllRequiredIndexes() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT indexname FROM pg_indexes " +
                                "WHERE schemaname = 'accounting' " +
                                "AND tablename = 'vector_documents'")) {
            ResultSet rs = statement.executeQuery();

            List<String> indexes = new ArrayList<>();
            while (rs.next()) {
                indexes.add(rs.getString("indexname"));
            }

            assertThat(indexes).contains(
                    "vector_documents_pkey",
                    "idx_vector_documents_company_deleted",
                    "idx_vector_documents_source",
                    "idx_vector_documents_fiscal_period",
                    "idx_vector_documents_metadata",
                    "idx_vector_documents_content_tsv",
                    "idx_vector_documents_embedding_hnsw");
        }
    }

    @Test
    void shouldCreateHNSWIndexWithCorrectParameters() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT am.amname AS index_type " +
                                "FROM pg_class c " +
                                "JOIN pg_index i ON c.oid = i.indexrelid " +
                                "JOIN pg_am am ON c.relam = am.oid " +
                                "WHERE c.relname = 'idx_vector_documents_embedding_hnsw'")) {
            ResultSet rs = statement.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("index_type")).isEqualTo("hnsw");
        }
    }

    @Test
    void shouldEnableRowLevelSecurityOnTable() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT relrowsecurity FROM pg_class c " +
                                "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                                "WHERE n.nspname = 'accounting' AND c.relname = 'vector_documents'")) {
            ResultSet rs = statement.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("relrowsecurity")).isTrue();
        }
    }

    @Test
    void shouldInsertAndRetrieveVectorDocument() throws Exception {
        runLiquibaseMigrations();

        UUID documentId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        String embedding = generateTestEmbedding(1);
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("module", "ar");
        metadata.put("document_type", "invoice");

        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO accounting.vector_documents " +
                                "(company_id, document_id, source_table, source_id, embedding, metadata) " +
                                "VALUES (?, ?, ?, ?, ?::vector, ?::jsonb) RETURNING id")) {
            insert.setObject(1, testCompanyId);
            insert.setObject(2, documentId);
            insert.setString(3, "invoices");
            insert.setObject(4, sourceId);
            insert.setString(5, embedding);
            insert.setString(6, metadata.toString());

            ResultSet rs = insert.executeQuery();
            assertThat(rs.next()).isTrue();
            UUID insertedId = (UUID) rs.getObject("id");
            assertThat(insertedId).isNotNull();
        }

        // Verify retrieval
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT * FROM accounting.vector_documents WHERE company_id = ?")) {
            select.setObject(1, testCompanyId);
            ResultSet rs = select.executeQuery();

            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject("company_id")).isEqualTo(testCompanyId);
            assertThat(rs.getObject("document_id")).isEqualTo(documentId);
            assertThat(rs.getString("source_table")).isEqualTo("invoices");
            assertThat(rs.getTimestamp("created_at")).isNotNull();
            assertThat(rs.getTimestamp("updated_at")).isNotNull();
            assertThat(rs.getTimestamp("deleted_at")).isNull();
        }
    }

    @Test
    void shouldPerformVectorSimilaritySearch() throws Exception {
        runLiquibaseMigrations();

        // Insert test vectors with different seeds for variety
        String[] embeddings = {
                generateTestEmbedding(1), // Very similar to query
                generateTestEmbedding(2), // Somewhat similar
                generateTestEmbedding(100) // Very different
        };

        for (String embedding : embeddings) {
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO accounting.vector_documents " +
                                    "(company_id, document_id, source_table, source_id, embedding) " +
                                    "VALUES (?, ?, 'test', ?, ?::vector)")) {
                insert.setObject(1, testCompanyId);
                insert.setObject(2, UUID.randomUUID());
                insert.setObject(3, UUID.randomUUID());
                insert.setString(4, embedding);
                insert.executeUpdate();
            }
        }

        // Perform similarity search using embedding with seed=1 (should match first
        // inserted vector)
        String queryEmbedding = generateTestEmbedding(1);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT embedding, embedding <-> ?::vector AS distance " +
                                "FROM accounting.vector_documents " +
                                "WHERE company_id = ? AND deleted_at IS NULL " +
                                "ORDER BY distance ASC LIMIT 2")) {
            select.setString(1, queryEmbedding);
            select.setObject(2, testCompanyId);

            ResultSet rs = select.executeQuery();

            // First result should be exact match (distance = 0)
            assertThat(rs.next()).isTrue();
            double firstDistance = rs.getDouble("distance");
            assertThat(firstDistance).isLessThan(0.01);

            // Second result should be similar but not identical
            assertThat(rs.next()).isTrue();
            double secondDistance = rs.getDouble("distance");
            assertThat(secondDistance).isGreaterThan(firstDistance);
        }
    }

    @Test
    void shouldEnforceTenantIsolationInQueries() throws Exception {
        runLiquibaseMigrations();

        // Create two separate companies
        UUID company1 = createCompany("Company 1");
        UUID company2 = createCompany("Company 2");
        String embedding = generateTestEmbedding(10);

        // Insert vectors for two companies
        for (UUID companyId : List.of(company1, company2)) {
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO accounting.vector_documents " +
                                    "(company_id, document_id, source_table, source_id, embedding) " +
                                    "VALUES (?, ?, 'test', ?, ?::vector)")) {
                insert.setObject(1, companyId);
                insert.setObject(2, UUID.randomUUID());
                insert.setObject(3, UUID.randomUUID());
                insert.setString(4, embedding);
                insert.executeUpdate();
            }
        }

        // Query for company1 should only return company1's vectors
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT company_id FROM accounting.vector_documents WHERE company_id = ?")) {
            select.setObject(1, company1);
            ResultSet rs = select.executeQuery();

            int count = 0;
            while (rs.next()) {
                assertThat(rs.getObject("company_id")).isEqualTo(company1);
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    void shouldSoftDeleteVectorDocument() throws Exception {
        runLiquibaseMigrations();

        UUID documentId = UUID.randomUUID();
        UUID id;

        // Insert document
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO accounting.vector_documents " +
                                "(company_id, document_id, source_table, source_id, embedding) " +
                                "VALUES (?, ?, 'test', ?, ?::vector) RETURNING id")) {
            insert.setObject(1, testCompanyId);
            insert.setObject(2, documentId);
            insert.setObject(3, UUID.randomUUID());
            insert.setString(4, generateTestEmbedding(20));
            ResultSet rs = insert.executeQuery();
            rs.next();
            id = (UUID) rs.getObject("id");
        }

        // Soft delete
        try (Connection connection = dataSource.getConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE accounting.vector_documents SET deleted_at = now() WHERE id = ?")) {
            update.setObject(1, id);
            int updated = update.executeUpdate();
            assertThat(updated).isEqualTo(1);
        }

        // Verify soft delete
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT deleted_at FROM accounting.vector_documents WHERE id = ?")) {
            select.setObject(1, id);
            ResultSet rs = select.executeQuery();
            assertThat(rs.next()).isTrue();
            assertThat(rs.getTimestamp("deleted_at")).isNotNull();
        }

        // Verify excluded from active queries
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT COUNT(*) FROM accounting.vector_documents " +
                                "WHERE company_id = ? AND deleted_at IS NULL")) {
            select.setObject(1, testCompanyId);
            ResultSet rs = select.executeQuery();
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
    }

    @Test
    void shouldFilterByMetadataUsingJSONB() throws Exception {
        runLiquibaseMigrations();

        ObjectNode metadata1 = objectMapper.createObjectNode();
        metadata1.put("module", "ar");
        metadata1.put("fiscal_period", "2024-10");

        ObjectNode metadata2 = objectMapper.createObjectNode();
        metadata2.put("module", "ap");
        metadata2.put("fiscal_period", "2024-09");

        // Insert documents with different metadata
        int seed = 30;
        for (ObjectNode metadata : List.of(metadata1, metadata2)) {
            try (Connection connection = dataSource.getConnection();
                    PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO accounting.vector_documents " +
                                    "(company_id, document_id, source_table, source_id, embedding, metadata) " +
                                    "VALUES (?, ?, 'test', ?, ?::vector, ?::jsonb)")) {
                insert.setObject(1, testCompanyId);
                insert.setObject(2, UUID.randomUUID());
                insert.setObject(3, UUID.randomUUID());
                insert.setString(4, generateTestEmbedding(seed++));
                insert.setString(5, metadata.toString());
                insert.executeUpdate();
            }
        }

        // Filter by module = "ar"
        try (Connection connection = dataSource.getConnection();
                PreparedStatement select = connection.prepareStatement(
                        "SELECT metadata FROM accounting.vector_documents " +
                                "WHERE company_id = ? AND metadata->>'module' = ?")) {
            select.setObject(1, testCompanyId);
            select.setString(2, "ar");

            ResultSet rs = select.executeQuery();
            int count = 0;
            while (rs.next()) {
                String metadataJson = rs.getString("metadata");
                // Parse JSON to verify module value (PostgreSQL formats JSON with spaces)
                ObjectNode parsedMetadata = (ObjectNode) objectMapper.readTree(metadataJson);
                assertThat(parsedMetadata.get("module").asText()).isEqualTo("ar");
                count++;
            }
            assertThat(count).isEqualTo(1);
        }
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
        return createCompany("Test Company");
    }

    private UUID createCompany(String name) {
        UUID companyId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO accounting.companies (id, name) VALUES (?, ?)")) {
            insert.setObject(1, companyId);
            insert.setString(2, name);
            insert.executeUpdate();
            return companyId;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create company: " + name, e);
        }
    }

    private void createSupabaseRoles() {
        String ensureRoles = "DO $$\n" +
                "BEGIN\n" +
                "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN\n" +
                "        CREATE ROLE authenticated;\n" +
                "    END IF;\n" +
                "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN\n" +
                "        CREATE ROLE service_role;\n" +
                "    END IF;\n" +
                "END$$;";

        String ensureCompaniesTable = "CREATE TABLE IF NOT EXISTS accounting.companies (" +
                "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
                "name TEXT NOT NULL, " +
                "created_at TIMESTAMPTZ DEFAULT now());";

        try (Connection connection = postgres.createConnection("")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
                statement.execute("CREATE SCHEMA IF NOT EXISTS accounting;");
                statement.execute(ensureRoles);
                statement.execute(ensureCompaniesTable);
                statement.execute("CREATE SCHEMA IF NOT EXISTS auth;");
                statement.execute("CREATE OR REPLACE FUNCTION auth.uid() RETURNS UUID LANGUAGE sql " +
                        "AS $$ SELECT '00000000-0000-0000-0000-000000000000'::UUID $$;");
                statement.execute("CREATE TABLE IF NOT EXISTS accounting.user_profiles (" +
                        "user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
                        "company_id UUID REFERENCES accounting.companies(id), " +
                        "role TEXT NOT NULL);");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bootstrap Supabase roles for tests", e);
        }
    }
}

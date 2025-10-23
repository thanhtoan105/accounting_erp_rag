package com.erp.rag.supabase.migration;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Liquibase migrations that manage the pgvector extension.
 *
 * Story 1.3 – AC1: Ensures CREATE EXTENSION vector executes via managed migrations
 * and exposes pgvector functions for downstream stories.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PgvectorExtensionMigrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("pgvector_test")
            .withUsername("testuser")
            .withPassword("testpass");

    private HikariDataSource dataSource;

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
    }

    @AfterAll
    void tearDownDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void shouldEnablePgvectorExtensionViaLiquibase() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT e.extname, n.nspname FROM pg_extension e " +
                             "JOIN pg_namespace n ON e.extnamespace = n.oid WHERE e.extname = 'vector'");
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("extname")).isEqualTo("vector");
            assertThat(resultSet.getString("nspname")).isEqualTo("extensions");
        }
    }

    @Test
    void shouldExposeVectorFunctionsAfterMigration() throws Exception {
        runLiquibaseMigrations();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT vector_dims('[0.1,0.2,0.3]'::vector)")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(3);
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

        try (Connection connection = postgres.createConnection("")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;");
                statement.execute(ensureRoles);
                statement.execute("CREATE SCHEMA IF NOT EXISTS accounting;");
                statement.execute("CREATE TABLE IF NOT EXISTS accounting.user_profiles (" +
                        "user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(), role TEXT NOT NULL);");
                statement.execute("CREATE SCHEMA IF NOT EXISTS auth;");
                statement.execute("CREATE OR REPLACE FUNCTION auth.uid() RETURNS UUID LANGUAGE sql " +
                        "AS $$ SELECT '00000000-0000-0000-0000-000000000000'::UUID $$;");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to bootstrap Supabase roles for tests", e);
        }
    }
}

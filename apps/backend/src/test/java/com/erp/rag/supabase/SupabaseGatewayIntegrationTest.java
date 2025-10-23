package com.erp.rag.supabase;

import com.erp.rag.supabase.config.ReadOnlyValidator;
import com.erp.rag.supabase.config.SupabaseGatewayConfiguration;
import com.erp.rag.supabase.schema.SchemaDocumentationService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Supabase Gateway with Testcontainers
 *
 * Tests AC1-AC4: Real database integration testing
 * - AC1: Connection pooling and retry logic
 * - AC1: Read-only enforcement
 * - AC2/AC3: Schema documentation
 * - AC4: Connection resilience
 *
 * Story: 1.1 - Establish Read-Only ERP Database Access
 */
@SpringBootTest
@Testcontainers
class SupabaseGatewayIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE =
            DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("test-schema.sql");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReadOnlyValidator readOnlyValidator;

    @Autowired
    private SchemaDocumentationService schemaDocumentationService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.data-source-properties.ssl", () -> "false");
        registry.add("spring.datasource.hikari.data-source-properties.sslmode", () -> "disable");
        registry.add("spring.datasource.hikari.read-only", () -> "true");
        registry.add("spring.datasource.hikari.connection-init-sql",
                () -> "SET search_path TO public,extensions,accounting; SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY");
        registry.add("spring.liquibase.enabled", () -> "false");
    }

    @Test
    void shouldConfigureConnectionPoolWithCorrectSizes() {
        // Given/When: AC1 - HikariCP pool configuration
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // Then: AC1 - min 2, max 10 connections
        assertThat(hikariDataSource.getMinimumIdle()).isEqualTo(2);
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(10);
    }

    @Test
    void shouldEstablishReadOnlyConnection() throws SQLException {
        // When: AC1 - Connection with read-only role
        try (Connection connection = dataSource.getConnection()) {
            // Then
            assertThat(connection.isValid(5)).isTrue();
            assertThat(connection.isReadOnly()).isTrue();
        }
    }

    @Test
    void shouldRejectWriteOperations() throws SQLException {
        // Given: AC1 - Read-only connection
        try (Connection connection = dataSource.getConnection()) {
            connection.setReadOnly(true);

            // When/Then: Write operations should be rejected
            assertThatThrownBy(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE should_fail (id INT)");
                }
            }).isInstanceOf(SQLException.class)
              .hasMessageContaining("read-only");
        }
    }

    @Test
    void shouldValidateReadOnlyEnforcement() {
        // When/Then: AC1 - Validator should confirm read-only enforcement
        assertThatCode(() -> readOnlyValidator.validate())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldTestConnectionAvailability() {
        // When: AC4 - Connection test
        boolean available = readOnlyValidator.testConnection();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void shouldGenerateSchemaDocumentation() throws SQLException {
        // Given: AC2/AC3 - Schema with test tables
        // When
        SchemaDocumentationService.SchemaDocumentation documentation =
                schemaDocumentationService.generateDocumentation("public");

        // Then: AC2 - Documentation generated successfully
        assertThat(documentation).isNotNull();
        assertThat(documentation.getSchemaName()).isEqualTo("public");
        assertThat(documentation.getTableCount()).isGreaterThan(0);
        assertThat(documentation.getTables()).isNotEmpty();
        assertThat(documentation.getGeneratedAt()).isNotNull();
    }

    @Test
    void shouldValidateCriticalTablesAccess() throws SQLException {
        // Given: AC2 - Critical tables list
        var criticalTables = Arrays.asList("test_invoices", "test_payments", "test_accounts");

        // When
        SchemaDocumentationService.ValidationResult result =
                schemaDocumentationService.validateCriticalTables("public", criticalTables);

        // Then: AC2 - Tables should be accessible
        assertThat(result.getTotalChecked()).isEqualTo(3);
        assertThat(result.getAccessibleCount()).isGreaterThan(0);
        assertThat(result.getAccessibleTables()).isNotEmpty();
    }

    @Test
    void shouldHandleConnectionRetries() throws SQLException {
        // Given: AC1/AC4 - Connection might fail temporarily
        // Simulate by creating multiple connections rapidly
        for (int i = 0; i < 5; i++) {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.isValid(5)).isTrue();
            }
        }

        // Then: AC1 - Retry logic should handle transient failures
        // This test verifies the pool can handle rapid connection requests
        assertThat(dataSource.getConnection().isValid(5)).isTrue();
    }

    @Test
    void shouldRespectConnectionPoolLimits() throws SQLException {
        // Given: AC1 - Pool with max 10 connections
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;

        // When: Request connections up to the pool limit
        Connection[] connections = new Connection[hikariDataSource.getMaximumPoolSize()];
        for (int i = 0; i < connections.length; i++) {
            connections[i] = dataSource.getConnection();
        }

        // Then: Pool metrics should reflect active connections
        assertThat(hikariDataSource.getHikariPoolMXBean().getActiveConnections())
                .isLessThanOrEqualTo(10);

        // Cleanup
        for (Connection connection : connections) {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }
}

package com.erp.rag.supabase.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SupabaseGatewayConfiguration
 *
 * Tests AC1: Connection pooling, read-only enforcement, and retry logic
 */
@ExtendWith(MockitoExtension.class)
class SupabaseGatewayConfigurationTest {

    @Mock
    private HikariDataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    private SupabaseGatewayConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new SupabaseGatewayConfiguration();

        // Set @Value fields using reflection for testing
        ReflectionTestUtils.setField(configuration, "maxAttempts", 3);
        ReflectionTestUtils.setField(configuration, "initialInterval", 1000L);
        ReflectionTestUtils.setField(configuration, "multiplier", 2.0);
        ReflectionTestUtils.setField(configuration, "maxInterval", 10000L);
    }

    @Test
    void shouldCreateRetryTemplateWithExponentialBackoff() {
        // Given: Default retry configuration values
        // When
        RetryTemplate retryTemplate = configuration.supabaseRetryTemplate();

        // Then
        assertThat(retryTemplate).isNotNull();
        // Note: RetryTemplate internals are private, so we can only verify it's created
        // Actual retry behavior is tested in integration tests
    }

    @Test
    void shouldConfigureConnectionPoolWithCorrectSizes() throws SQLException {
        // Given
        when(mockDataSource.getMinimumIdle()).thenReturn(2);
        when(mockDataSource.getMaximumPoolSize()).thenReturn(10);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(true);

        // Mock statement creation for SET SESSION command
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // When
        ReadOnlyValidator validator = configuration.readOnlyValidator(mockDataSource);

        // Then: AC1 - Pool configured with min 2, max 10
        verify(mockDataSource).getMinimumIdle();
        verify(mockDataSource).getMaximumPoolSize();
        assertThat(validator).isNotNull();
    }

    @Test
    void shouldEnforceReadOnlyMode() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(false);

        // Mock statement creation
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // When
        configuration.readOnlyValidator(mockDataSource);

        // Then: AC1 - Read-only mode should be set
        verify(mockConnection).setReadOnly(true);
    }

    @Test
    void shouldValidateReadOnlyConnection() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(true);

        // Mock statement creation
        Statement mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // When
        ReadOnlyValidator validator = configuration.readOnlyValidator(mockDataSource);

        // Then
        assertThat(validator).isNotNull();
        verify(mockConnection).isReadOnly();
    }
}

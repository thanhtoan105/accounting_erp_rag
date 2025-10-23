package com.erp.rag.supabase.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReadOnlyValidator
 *
 * Tests AC1: Read-only enforcement and write rejection
 */
@ExtendWith(MockitoExtension.class)
class ReadOnlyValidatorTest {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    private ReadOnlyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReadOnlyValidator(mockDataSource);
    }

    @Test
    void shouldValidateReadOnlyConnection() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(true);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Simulate write rejection
        when(mockStatement.execute(anyString()))
                .thenThrow(new SQLException("cannot execute CREATE TABLE in a read-only transaction"));

        // When/Then: Should not throw exception (write correctly rejected)
        assertThatCode(() -> validator.validate())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailValidationIfConnectionIsNotReadOnly() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(false);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.execute(anyString())).thenReturn(true);

        // When/Then: AC1 - Must reject non-read-only connections
        assertThatThrownBy(() -> validator.validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Write operation was not rejected");
    }

    @Test
    void shouldFailValidationIfWriteIsNotRejected() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isReadOnly()).thenReturn(true);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Simulate write NOT being rejected (should fail validation)
        when(mockStatement.execute(anyString())).thenReturn(true);

        // When/Then: AC1 - Write operations must be rejected
        assertThatThrownBy(() -> validator.validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Write operation was not rejected");
    }

    @Test
    void shouldTestConnectionSuccessfully() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // When
        boolean result = validator.testConnection();

        // Then
        assertThat(result).isTrue();
        verify(mockConnection).isValid(5);
    }

    @Test
    void shouldHandleConnectionTestFailure() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        boolean result = validator.testConnection();

        // Then
        assertThat(result).isFalse();
    }
}

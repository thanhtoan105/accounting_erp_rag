package com.erp.rag.supabase.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchemaDocumentationService
 *
 * Tests AC2/AC3: Schema documentation generation and table validation
 */
@ExtendWith(MockitoExtension.class)
class SchemaDocumentationServiceTest {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private DatabaseMetaData mockMetaData;

    @Mock
    private ResultSet mockTablesResultSet;

    @Mock
    private ResultSet mockColumnsResultSet;

    private SchemaDocumentationService service;

    @BeforeEach
    void setUp() {
        service = new SchemaDocumentationService(mockDataSource);
    }

    @Test
    void shouldGenerateSchemaDocumentation() throws SQLException {
        // Given: AC2 - Generate documentation for accessible tables
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getTables(isNull(), eq("accounting"), eq("%"), any()))
                .thenReturn(mockTablesResultSet);

        // Simulate 3 tables
        when(mockTablesResultSet.next()).thenReturn(true, true, true, false);
        when(mockTablesResultSet.getString("TABLE_NAME"))
                .thenReturn("invoices", "payments", "accounts");

        when(mockMetaData.getColumns(isNull(), eq("accounting"), anyString(), eq("%")))
                .thenReturn(mockColumnsResultSet);
        when(mockColumnsResultSet.next()).thenReturn(false);

        when(mockMetaData.getPrimaryKeys(isNull(), eq("accounting"), anyString()))
                .thenReturn(mock(ResultSet.class));
        when(mockMetaData.getImportedKeys(isNull(), eq("accounting"), anyString()))
                .thenReturn(mock(ResultSet.class));

        // When
        SchemaDocumentationService.SchemaDocumentation documentation =
                service.generateDocumentation("accounting");

        // Then
        assertThat(documentation.getSchemaName()).isEqualTo("accounting");
        assertThat(documentation.getTableCount()).isEqualTo(3);
        assertThat(documentation.getTables()).hasSize(3);
        assertThat(documentation.getGeneratedAt()).isNotNull();
    }

    @Test
    void shouldValidateCriticalTables() throws SQLException {
        // Given: AC2 - Validate access to critical tables
        List<String> criticalTables = Arrays.asList("invoices", "payments", "accounts");

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);

        // invoices and payments are accessible, accounts is not
        ResultSet invoicesResult = mock(ResultSet.class);
        when(invoicesResult.next()).thenReturn(true);

        ResultSet paymentsResult = mock(ResultSet.class);
        when(paymentsResult.next()).thenReturn(true);

        ResultSet accountsResult = mock(ResultSet.class);
        when(accountsResult.next()).thenReturn(false);

        when(mockMetaData.getTables(isNull(), eq("accounting"), eq("invoices"), any()))
                .thenReturn(invoicesResult);
        when(mockMetaData.getTables(isNull(), eq("accounting"), eq("payments"), any()))
                .thenReturn(paymentsResult);
        when(mockMetaData.getTables(isNull(), eq("accounting"), eq("accounts"), any()))
                .thenReturn(accountsResult);

        // When
        SchemaDocumentationService.ValidationResult result =
                service.validateCriticalTables("accounting", criticalTables);

        // Then: AC2 - Report accessible and inaccessible tables
        assertThat(result.getTotalChecked()).isEqualTo(3);
        assertThat(result.getAccessibleCount()).isEqualTo(2);
        assertThat(result.getInaccessibleCount()).isEqualTo(1);
        assertThat(result.getAccessibleTables()).containsExactlyInAnyOrder("invoices", "payments");
        assertThat(result.getInaccessibleTables()).containsExactly("accounts");
    }

    @Test
    void shouldExtractTableMetadata() throws SQLException {
        // Given
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);

        when(mockMetaData.getTables(isNull(), eq("accounting"), eq("%"), any()))
                .thenReturn(mockTablesResultSet);
        when(mockTablesResultSet.next()).thenReturn(true, false);
        when(mockTablesResultSet.getString("TABLE_NAME")).thenReturn("invoices");

        when(mockMetaData.getColumns(isNull(), eq("accounting"), eq("invoices"), eq("%")))
                .thenReturn(mockColumnsResultSet);
        when(mockColumnsResultSet.next()).thenReturn(true, true, false);
        when(mockColumnsResultSet.getString("COLUMN_NAME")).thenReturn("id", "amount");
        when(mockColumnsResultSet.getString("TYPE_NAME")).thenReturn("uuid", "numeric");
        when(mockColumnsResultSet.getInt("COLUMN_SIZE")).thenReturn(36, 10);
        when(mockColumnsResultSet.getInt("NULLABLE")).thenReturn(0, 1);

        ResultSet pkResultSet = mock(ResultSet.class);
        when(pkResultSet.next()).thenReturn(true, false);
        when(pkResultSet.getString("COLUMN_NAME")).thenReturn("id");
        when(mockMetaData.getPrimaryKeys(isNull(), eq("accounting"), eq("invoices")))
                .thenReturn(pkResultSet);

        when(mockMetaData.getImportedKeys(isNull(), eq("accounting"), eq("invoices")))
                .thenReturn(mock(ResultSet.class));

        // When
        SchemaDocumentationService.SchemaDocumentation documentation =
                service.generateDocumentation("accounting");

        // Then
        SchemaDocumentationService.TableMetadata table = documentation.getTables().get(0);
        assertThat(table.getTableName()).isEqualTo("invoices");
        assertThat(table.getColumnCount()).isEqualTo(2);
        assertThat(table.getColumns()).hasSize(2);
        assertThat(table.getPrimaryKeys()).containsExactly("id");
    }
}

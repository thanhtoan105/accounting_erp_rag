package com.erp.rag.supabase.schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Schema Documentation Generator
 *
 * AC2/AC3: Automatically generates documentation for ERP database tables.
 * - Enumerates accessible tables (target: ≥60)
 * - Captures table metadata (columns, types, constraints)
 * - Documents relationships
 *
 * Story: 1.1 - Establish Read-Only ERP Database Access
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaDocumentationService {

    private final DataSource dataSource;

    /**
     * Generates comprehensive schema documentation.
     * AC2: Enumerate accessible ERP tables and generate schema documentation.
     *
     * @param schemaName target schema (default: "accounting")
     * @return schema documentation with all tables and metadata
     */
    public SchemaDocumentation generateDocumentation(String schemaName) throws SQLException {
        log.info("Generating schema documentation for schema: {}", schemaName);

        SchemaDocumentation documentation = new SchemaDocumentation();
        documentation.setSchemaName(schemaName);
        documentation.setGeneratedAt(Instant.now());
        documentation.setTables(new ArrayList<>());

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            // Get all tables in the schema
            try (ResultSet tables = metaData.getTables(null, schemaName, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    TableMetadata tableMetadata = extractTableMetadata(metaData, schemaName, tableName);
                    documentation.getTables().add(tableMetadata);
                }
            }

            documentation.setTableCount(documentation.getTables().size());
            log.info("✓ Generated documentation for {} tables in schema '{}'",
                    documentation.getTableCount(), schemaName);
        }

        return documentation;
    }

    /**
     * Validates access to critical ERP tables.
     * AC2: Validate access to critical tables.
     */
    public ValidationResult validateCriticalTables(String schemaName, List<String> criticalTables) throws SQLException {
        log.info("Validating access to {} critical tables", criticalTables.size());

        ValidationResult result = new ValidationResult();
        result.setAccessibleTables(new ArrayList<>());
        result.setInaccessibleTables(new ArrayList<>());

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            for (String tableName : criticalTables) {
                try (ResultSet tables = metaData.getTables(null, schemaName, tableName, new String[]{"TABLE"})) {
                    if (tables.next()) {
                        result.getAccessibleTables().add(tableName);
                        log.debug("✓ Table accessible: {}.{}", schemaName, tableName);
                    } else {
                        result.getInaccessibleTables().add(tableName);
                        log.warn("✗ Table not accessible: {}.{}", schemaName, tableName);
                    }
                }
            }
        }

        result.setTotalChecked(criticalTables.size());
        result.setAccessibleCount(result.getAccessibleTables().size());
        result.setInaccessibleCount(result.getInaccessibleTables().size());

        log.info("Validation complete: {}/{} tables accessible",
                result.getAccessibleCount(), result.getTotalChecked());

        return result;
    }

    private TableMetadata extractTableMetadata(DatabaseMetaData metaData, String schemaName, String tableName)
            throws SQLException {
        TableMetadata table = new TableMetadata();
        table.setTableName(tableName);
        table.setSchemaName(schemaName);
        table.setColumns(new ArrayList<>());
        table.setPrimaryKeys(new ArrayList<>());
        table.setForeignKeys(new ArrayList<>());

        // Extract columns
        try (ResultSet columns = metaData.getColumns(null, schemaName, tableName, "%")) {
            while (columns.next()) {
                ColumnMetadata column = new ColumnMetadata();
                column.setColumnName(columns.getString("COLUMN_NAME"));
                column.setDataType(columns.getString("TYPE_NAME"));
                column.setColumnSize(columns.getInt("COLUMN_SIZE"));
                column.setNullable(columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(columns.getString("COLUMN_DEF"));
                table.getColumns().add(column);
            }
        }

        // Extract primary keys
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, schemaName, tableName)) {
            while (primaryKeys.next()) {
                table.getPrimaryKeys().add(primaryKeys.getString("COLUMN_NAME"));
            }
        }

        // Extract foreign keys
        try (ResultSet foreignKeys = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (foreignKeys.next()) {
                ForeignKeyMetadata fk = new ForeignKeyMetadata();
                fk.setColumnName(foreignKeys.getString("FKCOLUMN_NAME"));
                fk.setReferencedTable(foreignKeys.getString("PKTABLE_NAME"));
                fk.setReferencedColumn(foreignKeys.getString("PKCOLUMN_NAME"));
                table.getForeignKeys().add(fk);
            }
        }

        table.setColumnCount(table.getColumns().size());
        return table;
    }

    // Data classes for documentation structure

    @Data
    public static class SchemaDocumentation {
        private String schemaName;
        private Integer tableCount;
        private Instant generatedAt;
        private List<TableMetadata> tables;
    }

    @Data
    public static class TableMetadata {
        private String schemaName;
        private String tableName;
        private Integer columnCount;
        private List<ColumnMetadata> columns;
        private List<String> primaryKeys;
        private List<ForeignKeyMetadata> foreignKeys;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnMetadata {
        private String columnName;
        private String dataType;
        private Integer columnSize;
        private Boolean nullable;
        private String defaultValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignKeyMetadata {
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
    }

    @Data
    public static class ValidationResult {
        private Integer totalChecked;
        private Integer accessibleCount;
        private Integer inaccessibleCount;
        private List<String> accessibleTables;
        private List<String> inaccessibleTables;
    }
}

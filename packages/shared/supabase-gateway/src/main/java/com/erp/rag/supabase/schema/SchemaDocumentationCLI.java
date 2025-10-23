package com.erp.rag.supabase.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Schema Documentation CLI Tool
 *
 * AC2/AC3: Automated schema documentation generation.
 * Run with: --schema.documentation.enabled=true
 *
 * Story: 1.1 - Establish Read-Only ERP Database Access
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "schema.documentation.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SchemaDocumentationCLI implements CommandLineRunner {

    private final SchemaDocumentationService schemaDocumentationService;

    // AC2: Critical ERP tables to validate
    private static final List<String> CRITICAL_TABLES = Arrays.asList(
            "invoices", "payments", "journal_entries", "accounts",
            "customers", "vendors", "bank_transactions", "tax_declarations",
            "companies", "user_profiles", "audit_logs", "fiscal_periods",
            "invoice_lines", "invoice_payments", "account_balances",
            "bank_accounts", "cash_transactions"
    );

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Schema Documentation Generator ===");

        String schemaName = System.getProperty("schema.name", "accounting");
        String outputDir = System.getProperty("schema.output.dir", "docs/database");

        // Generate full schema documentation
        SchemaDocumentationService.SchemaDocumentation documentation =
                schemaDocumentationService.generateDocumentation(schemaName);

        // Validate critical tables
        SchemaDocumentationService.ValidationResult validation =
                schemaDocumentationService.validateCriticalTables(schemaName, CRITICAL_TABLES);

        // Save documentation to JSON
        saveDocumentation(documentation, outputDir, "schema-documentation.json");

        // Save validation report
        saveDocumentation(validation, outputDir, "table-validation.json");

        // Generate markdown report
        generateMarkdownReport(documentation, validation, outputDir);

        log.info("=== Documentation Generation Complete ===");
        log.info("Total tables documented: {}", documentation.getTableCount());
        log.info("Critical tables accessible: {}/{}", validation.getAccessibleCount(), validation.getTotalChecked());
        log.info("Output directory: {}", outputDir);

        // AC2: Verify we have ≥60 tables accessible
        if (documentation.getTableCount() < 60) {
            log.warn("WARNING: Only {} tables found (requirement: ≥60)", documentation.getTableCount());
        } else {
            log.info("✓ Requirement met: {} tables documented (≥60)", documentation.getTableCount());
        }
    }

    private void saveDocumentation(Object documentation, String outputDir, String filename) throws Exception {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File outputFile = outputPath.resolve(filename).toFile();
        mapper.writeValue(outputFile, documentation);

        log.info("✓ Saved: {}", outputFile.getAbsolutePath());
    }

    private void generateMarkdownReport(SchemaDocumentationService.SchemaDocumentation documentation,
                                         SchemaDocumentationService.ValidationResult validation,
                                         String outputDir) throws Exception {
        StringBuilder markdown = new StringBuilder();

        markdown.append("# ERP Database Schema Documentation\n\n");
        markdown.append("**Generated:** ").append(documentation.getGeneratedAt()).append("\n\n");
        markdown.append("**Schema:** ").append(documentation.getSchemaName()).append("\n\n");
        markdown.append("**Total Tables:** ").append(documentation.getTableCount()).append("\n\n");

        markdown.append("## Critical Tables Validation\n\n");
        markdown.append(String.format("- **Accessible:** %d/%d\n",
                validation.getAccessibleCount(), validation.getTotalChecked()));
        markdown.append(String.format("- **Inaccessible:** %d/%d\n\n",
                validation.getInaccessibleCount(), validation.getTotalChecked()));

        if (!validation.getInaccessibleTables().isEmpty()) {
            markdown.append("### Inaccessible Tables\n\n");
            validation.getInaccessibleTables().forEach(table ->
                    markdown.append("- ").append(table).append("\n"));
            markdown.append("\n");
        }

        markdown.append("## Tables\n\n");
        for (SchemaDocumentationService.TableMetadata table : documentation.getTables()) {
            markdown.append(String.format("### `%s.%s`\n\n",
                    table.getSchemaName(), table.getTableName()));
            markdown.append(String.format("**Columns:** %d\n\n", table.getColumnCount()));

            if (!table.getPrimaryKeys().isEmpty()) {
                markdown.append(String.format("**Primary Key:** %s\n\n",
                        String.join(", ", table.getPrimaryKeys())));
            }

            markdown.append("| Column | Type | Nullable |\n");
            markdown.append("|--------|------|----------|\n");
            for (SchemaDocumentationService.ColumnMetadata column : table.getColumns()) {
                markdown.append(String.format("| `%s` | %s | %s |\n",
                        column.getColumnName(),
                        column.getDataType(),
                        column.getNullable() ? "Yes" : "No"));
            }
            markdown.append("\n");
        }

        Path markdownPath = Paths.get(outputDir, "schema-documentation.md");
        Files.writeString(markdownPath, markdown.toString());

        log.info("✓ Saved markdown: {}", markdownPath.toAbsolutePath());
    }
}

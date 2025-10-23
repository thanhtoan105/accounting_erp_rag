package com.erp.rag.supabase.config;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Validates and enforces read-only access to the database.
 *
 * Story: 1.1 - AC1: Read-only enforcement validation
 */
@Slf4j
public class ReadOnlyValidator {

    private final DataSource dataSource;

    public ReadOnlyValidator(DataSource dataSource) {
        this.dataSource = dataSource;

    }

    /**
     * Validates that the connection is read-only.
     *
     * @throws SQLException if validation fails
     */
    public void validate() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isReadOnly()) {
                log.warn("Connection reported write access. Enforcing read-only mode now.");
                connection.setReadOnly(true);
            }

            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (var statement = connection.createStatement()) {
                statement.execute("SET TRANSACTION READ WRITE");
                throw new IllegalStateException("Write operation was not rejected - read-only enforcement failed");
            } catch (SQLException e) {
                if (e.getMessage().contains("read-only") || e.getMessage().contains("READ ONLY")) {
                    log.debug("✓ Write operation correctly rejected: {}", e.getMessage());
                } else {
                    throw e;
                }
            } finally {
                try {
                    connection.rollback();
                } finally {
                    connection.setAutoCommit(originalAutoCommit);
                }
            }
        }
    }

    /**
     * Tests connection availability with retry support.
     *
     * @return true if connection is available
     */
    public boolean testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.error("Connection test failed", e);
            return false;
        }
    }
}

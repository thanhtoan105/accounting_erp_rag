package com.erp.rag.piimasking;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for scanning text content to detect PII (Personally Identifiable Information) leakage.
 * Uses Vietnamese-specific regex patterns for tax IDs, names, phones, and emails.
 * 
 * Intended for automated daily scans of vector_documents and rag_queries tables
 * to ensure zero PII exposure in indexed content or LLM prompts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PiiScannerService {

    private final JdbcTemplate jdbcTemplate;
    
    // Vietnamese Tax ID pattern: 10 or 13 digits (MST format)
    private static final Pattern TAX_ID_PATTERN = Pattern.compile(
        "\\b[0-9]{10}(-[0-9]{3})?\\b"
    );
    
    // Email pattern (standard RFC 5322 simplified - matches emails anywhere in text)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[A-Za-z0-9._%+-]+@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\\.[A-Za-z]{2,}",
        Pattern.CASE_INSENSITIVE
    );
    
    // Vietnamese phone pattern: +84 or 0 prefix with 9-10 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\+84|0)(9[0-9]{8}|[2-8][0-9]{8,9})"
    );
    
    // Vietnamese name pattern (with Unicode diacritics and Unicode-aware boundaries)
    // Matches: "Nguyễn Văn A", "Trần Thị Bình", etc.
    private static final Pattern VIETNAMESE_NAME_PATTERN = Pattern.compile(
        "\\b[A-ZÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ]" +
        "[a-zàáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]+" +
        "( [A-ZÀÁẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ]" +
        "[a-zàáảãạăắằẳẵặâấầẩẫậđèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵ]+){1,3}\\b",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    /**
     * Scans vector_documents table for PII leakage.
     * 
     * @return Scan result with detected PII violations
     */
    public ScanResult scanVectorDocuments() {
        log.info("Starting PII scan for vector_documents table");
        ScanResult result = new ScanResult("vector_documents");
        
        try {
            // Check if table exists
            Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = 'vector_documents')",
                Boolean.class
            );
            
            if (Boolean.FALSE.equals(tableExists)) {
                log.warn("vector_documents table does not exist yet - skipping scan");
                result.setTableExists(false);
                return result;
            }
            
            // Scan content_tsv column for PII patterns
            List<DocumentContent> documents = jdbcTemplate.query(
                "SELECT id, content_tsv FROM vector_documents LIMIT 1000",
                (rs, rowNum) -> new DocumentContent(
                    rs.getString("id"),
                    rs.getString("content_tsv")
                )
            );
            
            for (DocumentContent doc : documents) {
                if (doc.getContent() != null) {
                    scanContentForPii(doc.getContent(), doc.getId(), result);
                }
            }
            
            result.setScannedCount(documents.size());
        } catch (Exception e) {
            log.error("Error scanning vector_documents: {}", e.getMessage());
            result.setError(e.getMessage());
        }
        
        log.info("Completed PII scan for vector_documents: {} violations found in {} records", 
            result.getViolations().size(), result.getScannedCount());
        return result;
    }

    /**
     * Scans rag_queries table for PII leakage in query text.
     * 
     * @return Scan result with detected PII violations
     */
    public ScanResult scanRagQueries() {
        log.info("Starting PII scan for rag_queries table");
        ScanResult result = new ScanResult("rag_queries");
        
        try {
            // Check if table exists
            Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = 'rag_queries')",
                Boolean.class
            );
            
            if (Boolean.FALSE.equals(tableExists)) {
                log.warn("rag_queries table does not exist yet - skipping scan");
                result.setTableExists(false);
                return result;
            }
            
            // Scan query_text column for PII patterns
            List<DocumentContent> queries = jdbcTemplate.query(
                "SELECT id, query_text FROM rag_queries LIMIT 1000",
                (rs, rowNum) -> new DocumentContent(
                    rs.getString("id"),
                    rs.getString("query_text")
                )
            );
            
            for (DocumentContent query : queries) {
                if (query.getContent() != null) {
                    scanContentForPii(query.getContent(), query.getId(), result);
                }
            }
            
            result.setScannedCount(queries.size());
        } catch (Exception e) {
            log.error("Error scanning rag_queries: {}", e.getMessage());
            result.setError(e.getMessage());
        }
        
        log.info("Completed PII scan for rag_queries: {} violations found in {} records", 
            result.getViolations().size(), result.getScannedCount());
        return result;
    }

    /**
     * Scans text content for all PII patterns and records violations.
     * 
     * @param content Text content to scan
     * @param recordId Record ID for violation tracking
     * @param result Scan result to append violations
     */
    private void scanContentForPii(String content, String recordId, ScanResult result) {
        // Check for Vietnamese Tax IDs
        if (matchesVietnameseTaxId(content)) {
            result.addViolation(new PiiViolation(
                recordId, 
                "TAX_ID", 
                "Detected Vietnamese tax ID pattern (10-13 digits)"
            ));
        }
        
        // Check for Email addresses
        if (matchesEmail(content)) {
            result.addViolation(new PiiViolation(
                recordId, 
                "EMAIL", 
                "Detected email address pattern"
            ));
        }
        
        // Check for Phone numbers
        if (matchesPhone(content)) {
            result.addViolation(new PiiViolation(
                recordId, 
                "PHONE", 
                "Detected Vietnamese phone number pattern"
            ));
        }
        
        // Check for Vietnamese names (high false positive rate - use with caution)
        if (matchesVietnameseName(content)) {
            result.addViolation(new PiiViolation(
                recordId, 
                "NAME", 
                "Detected potential Vietnamese name pattern"
            ));
        }
    }

    /**
     * Checks if content matches Vietnamese Tax ID format.
     * 
     * @param content Text content to check
     * @return true if tax ID pattern detected
     */
    public boolean matchesVietnameseTaxId(String content) {
        if (content == null) return false;
        Matcher matcher = TAX_ID_PATTERN.matcher(content);
        return matcher.find();
    }

    /**
     * Checks if content matches email address format.
     * 
     * @param content Text content to check
     * @return true if email pattern detected
     */
    public boolean matchesEmail(String content) {
        if (content == null) return false;
        Matcher matcher = EMAIL_PATTERN.matcher(content);
        return matcher.find();
    }

    /**
     * Checks if content matches Vietnamese phone number format.
     * 
     * @param content Text content to check
     * @return true if phone pattern detected
     */
    public boolean matchesPhone(String content) {
        if (content == null) return false;
        Matcher matcher = PHONE_PATTERN.matcher(content);
        return matcher.find();
    }

    /**
     * Checks if content matches Vietnamese name pattern.
     * Note: This has a high false positive rate due to Vietnamese language structure.
     * 
     * @param content Text content to check
     * @return true if Vietnamese name pattern detected
     */
    public boolean matchesVietnameseName(String content) {
        if (content == null) return false;
        Matcher matcher = VIETNAMESE_NAME_PATTERN.matcher(content);
        return matcher.find();
    }

    /**
     * Result of a PII scan operation.
     */
    @Data
    public static class ScanResult {
        private final String tableName;
        private int scannedCount;
        private final List<PiiViolation> violations = new ArrayList<>();
        private boolean tableExists = true;
        private String error;
        
        public void addViolation(PiiViolation violation) {
            violations.add(violation);
        }
        
        public boolean hasViolations() {
            return !violations.isEmpty();
        }
    }

    /**
     * Represents a single PII violation detected during scan.
     */
    @Data
    public static class PiiViolation {
        private final String recordId;
        private final String piiType; // TAX_ID, EMAIL, PHONE, NAME
        private final String description;
    }

    /**
     * Helper class for document content during scanning.
     */
    @Data
    private static class DocumentContent {
        private final String id;
        private final String content;
    }
}

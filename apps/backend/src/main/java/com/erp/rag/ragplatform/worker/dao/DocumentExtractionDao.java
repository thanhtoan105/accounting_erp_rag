package com.erp.rag.ragplatform.worker.dao;

import com.erp.rag.ragplatform.worker.domain.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DAO for extracting ERP documents from Supabase PostgreSQL.
 * <p>
 * Story 1.4 – AC1: Document extraction for all target document types via supabase-gateway.
 * Uses NamedParameterJdbcTemplate with retry logic for transient failures.
 * All queries filter by company_id for multi-tenant isolation and exclude soft-deleted records.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Repository
public class DocumentExtractionDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DocumentExtractionDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Extract invoices for a specific company, optionally filtering by updated_at for incremental sync.
     *
     * @param companyId   company UUID
     * @param updatedAfter optional timestamp for incremental extraction (null for full extraction)
     * @return list of invoice documents
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<InvoiceDocument> extractInvoices(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    i.id::text::uuid as id,
                    i.company_id::text::uuid as company_id,
                    i.customer_id::text::uuid as customer_id,
                    c.name as customer_name,
                    i.invoice_number,
                    i.invoice_date,
                    i.due_date,
                    i.total_amount,
                    i.paid_amount,
                    i.status,
                    i.notes,
                    fp.year || '-' || LPAD(fp.period::text, 2, '0') as fiscal_period,
                    i.deleted_at,
                    STRING_AGG(il.description, '; ') as description
                FROM accounting.invoices i
                JOIN accounting.customers c ON i.customer_id = c.id
                LEFT JOIN accounting.fiscal_periods fp ON i.fiscal_period_id = fp.id
                LEFT JOIN accounting.invoice_lines il ON i.id = il.invoice_id
                WHERE i.company_id = :companyId
                  AND i.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR i.updated_at > :updatedAfter)
                GROUP BY i.id, i.company_id, i.customer_id, c.name, i.invoice_number, 
                         i.invoice_date, i.due_date, i.total_amount, i.paid_amount, i.status, 
                         i.notes, fp.year, fp.period, i.deleted_at
                ORDER BY i.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapInvoice);
    }

    /**
     * Extract bills for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<BillDocument> extractBills(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    b.id::text::uuid as id,
                    b.company_id::text::uuid as company_id,
                    b.vendor_id::text::uuid as vendor_id,
                    v.name as vendor_name,
                    b.bill_number,
                    b.bill_date,
                    b.due_date,
                    b.total_amount,
                    b.paid_amount,
                    b.status,
                    b.notes,
                    fp.year || '-' || LPAD(fp.period::text, 2, '0') as fiscal_period,
                    b.deleted_at,
                    STRING_AGG(bl.description, '; ') as description
                FROM accounting.bills b
                JOIN accounting.vendors v ON b.vendor_id = v.id
                LEFT JOIN accounting.fiscal_periods fp ON b.fiscal_period_id = fp.id
                LEFT JOIN accounting.bill_lines bl ON b.id = bl.bill_id
                WHERE b.company_id = :companyId
                  AND b.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR b.updated_at > :updatedAfter)
                GROUP BY b.id, b.company_id, b.vendor_id, v.name, b.bill_number,
                         b.bill_date, b.due_date, b.total_amount, b.paid_amount, b.status,
                         b.notes, fp.year, fp.period, b.deleted_at
                ORDER BY b.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapBill);
    }

    /**
     * Extract journal entries for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<JournalEntryDocument> extractJournalEntries(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    je.id::text::uuid as id,
                    je.company_id::text::uuid as company_id,
                    je.entry_number,
                    je.entry_date,
                    je.entry_type,
                    je.description,
                    je.reference_no,
                    je.status,
                    fp.year || '-' || LPAD(fp.period::text, 2, '0') as fiscal_period,
                    je.deleted_at,
                    SUM(jel.debit) as total_debit,
                    SUM(jel.credit) as total_credit,
                    STRING_AGG(DISTINCT a.code, ', ') as account_codes
                FROM accounting.journal_entries je
                LEFT JOIN accounting.fiscal_periods fp ON je.fiscal_period_id = fp.id
                LEFT JOIN accounting.journal_entry_lines jel ON je.id = jel.journal_entry_id
                LEFT JOIN accounting.accounts a ON jel.account_id = a.id
                WHERE je.company_id = :companyId
                  AND je.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR je.updated_at > :updatedAfter)
                GROUP BY je.id, je.company_id, je.entry_number, je.entry_date, je.entry_type,
                         je.description, je.reference_no, je.status, fp.year, fp.period, je.deleted_at
                ORDER BY je.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapJournalEntry);
    }

    /**
     * Extract customers for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<CustomerDocument> extractCustomers(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    c.id::text::uuid as id,
                    c.company_id::text::uuid as company_id,
                    c.code,
                    c.name,
                    c.tax_code,
                    c.address,
                    c.phone,
                    c.email,
                    c.contact_person,
                    c.credit_limit,
                    c.payment_terms,
                    c.is_active,
                    c.deleted_at
                FROM accounting.customers c
                WHERE c.company_id = :companyId
                  AND c.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR c.updated_at > :updatedAfter)
                ORDER BY c.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapCustomer);
    }

    /**
     * Extract vendors for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<VendorDocument> extractVendors(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    v.id::text::uuid as id,
                    v.company_id::text::uuid as company_id,
                    v.code,
                    v.name,
                    v.name_en,
                    v.tax_code,
                    v.address,
                    v.phone,
                    v.email,
                    v.contact_person,
                    v.payment_terms,
                    v.is_active,
                    v.deleted_at
                FROM accounting.vendors v
                WHERE v.company_id = :companyId
                  AND v.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR v.updated_at > :updatedAfter)
                ORDER BY v.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapVendor);
    }

    /**
     * Extract payments for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<PaymentDocument> extractPayments(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    p.id::text::uuid as id,
                    p.company_id::text::uuid as company_id,
                    p.customer_id::text::uuid as customer_id,
                    c.name as customer_name,
                    p.payment_number,
                    p.payment_date,
                    p.amount,
                    p.payment_method,
                    p.reference_no,
                    p.notes,
                    fp.year || '-' || LPAD(fp.period::text, 2, '0') as fiscal_period,
                    p.deleted_at
                FROM accounting.payments p
                JOIN accounting.customers c ON p.customer_id = c.id
                LEFT JOIN accounting.fiscal_periods fp ON p.fiscal_period_id = fp.id
                WHERE p.company_id = :companyId
                  AND p.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR p.updated_at > :updatedAfter)
                ORDER BY p.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapPayment);
    }

    /**
     * Extract bank/cash transactions for a specific company.
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public List<BankTransactionDocument> extractBankTransactions(UUID companyId, OffsetDateTime updatedAfter) {
        String sql = """
                SELECT 
                    ct.id::text::uuid as id,
                    ct.company_id::text::uuid as company_id,
                    ct.bank_account_id::text::uuid as bank_account_id,
                    ba.account_name as bank_account_name,
                    ct.transaction_number,
                    ct.transaction_date,
                    ct.transaction_type,
                    ct.amount,
                    ct.description,
                    ct.reference_no,
                    fp.year || '-' || LPAD(fp.period::text, 2, '0') as fiscal_period,
                    ct.deleted_at
                FROM accounting.cash_transactions ct
                JOIN accounting.bank_accounts ba ON ct.bank_account_id = ba.id
                LEFT JOIN accounting.fiscal_periods fp ON ct.fiscal_period_id = fp.id
                WHERE ct.company_id = :companyId
                  AND ct.deleted_at IS NULL
                  AND (:updatedAfter IS NULL OR ct.updated_at > :updatedAfter)
                ORDER BY ct.updated_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId.toString())
                .addValue("updatedAfter", updatedAfter);

        return jdbcTemplate.query(sql, params, this::mapBankTransaction);
    }

    // Row mappers

    private InvoiceDocument mapInvoice(ResultSet rs, int rowNum) throws SQLException {
        InvoiceDocument doc = new InvoiceDocument();
        doc.setId(UUID.fromString(rs.getString("id")));
        doc.setCompanyId(UUID.fromString(rs.getString("company_id")));
        doc.setCustomerId(UUID.fromString(rs.getString("customer_id")));
        doc.setCustomerName(rs.getString("customer_name"));
        doc.setInvoiceNumber(rs.getString("invoice_number"));
        doc.setInvoiceDate(rs.getDate("invoice_date").toLocalDate());
        doc.setDueDate(rs.getDate("due_date").toLocalDate());
        doc.setTotalAmount(rs.getBigDecimal("total_amount"));
        doc.setPaidAmount(rs.getBigDecimal("paid_amount"));
        doc.setStatus(rs.getString("status"));
        doc.setNotes(rs.getString("notes"));
        doc.setFiscalPeriod(rs.getString("fiscal_period"));
        doc.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        doc.setDescription(rs.getString("description"));
        return doc;
    }

    private BillDocument mapBill(ResultSet rs, int rowNum) throws SQLException {
        BillDocument doc = new BillDocument();
        doc.setId(UUID.fromString(rs.getString("id")));
        doc.setCompanyId(UUID.fromString(rs.getString("company_id")));
        doc.setVendorId(UUID.fromString(rs.getString("vendor_id")));
        doc.setVendorName(rs.getString("vendor_name"));
        doc.setBillNumber(rs.getString("bill_number"));
        doc.setBillDate(rs.getDate("bill_date").toLocalDate());
        doc.setDueDate(rs.getDate("due_date").toLocalDate());
        doc.setTotalAmount(rs.getBigDecimal("total_amount"));
        doc.setPaidAmount(rs.getBigDecimal("paid_amount"));
        doc.setStatus(rs.getString("status"));
        doc.setNotes(rs.getString("notes"));
        doc.setFiscalPeriod(rs.getString("fiscal_period"));
        doc.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        doc.setDescription(rs.getString("description"));
        return doc;
    }

    private JournalEntryDocument mapJournalEntry(ResultSet rs, int rowNum) throws SQLException {
        JournalEntryDocument doc = new JournalEntryDocument();
        doc.setId(UUID.fromString(rs.getString("id")));
        doc.setCompanyId(UUID.fromString(rs.getString("company_id")));
        doc.setEntryNumber(rs.getString("entry_number"));
        doc.setEntryDate(rs.getDate("entry_date").toLocalDate());
        doc.setEntryType(rs.getString("entry_type"));
        doc.setDescription(rs.getString("description"));
        doc.setReferenceNo(rs.getString("reference_no"));
        doc.setStatus(rs.getString("status"));
        doc.setFiscalPeriod(rs.getString("fiscal_period"));
        doc.setDeletedAt(rs.getObject("deleted_at", OffsetDateTime.class));
        doc.setTotalDebit(rs.getBigDecimal("total_debit"));
        doc.setTotalCredit(rs.getBigDecimal("total_credit"));
        doc.setAccountCodes(rs.getString("account_codes"));
        return doc;
    }

    private CustomerDocument mapCustomer(ResultSet rs, int rowNum) throws SQLException {
        return new CustomerDocument(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("company_id")),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("tax_code"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("contact_person"),
                rs.getBigDecimal("credit_limit"),
                (Integer) rs.getObject("payment_terms"),
                rs.getBoolean("is_active"),
                rs.getObject("deleted_at", OffsetDateTime.class)
        );
    }

    private VendorDocument mapVendor(ResultSet rs, int rowNum) throws SQLException {
        return new VendorDocument(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("company_id")),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("name_en"),
                rs.getString("tax_code"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("contact_person"),
                (Integer) rs.getObject("payment_terms"),
                rs.getBoolean("is_active"),
                rs.getObject("deleted_at", OffsetDateTime.class)
        );
    }

    private PaymentDocument mapPayment(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentDocument(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("company_id")),
                UUID.fromString(rs.getString("customer_id")),
                rs.getString("customer_name"),
                rs.getString("payment_number"),
                rs.getDate("payment_date").toLocalDate(),
                rs.getBigDecimal("amount"),
                rs.getString("payment_method"),
                rs.getString("reference_no"),
                rs.getString("notes"),
                rs.getString("fiscal_period"),
                rs.getObject("deleted_at", OffsetDateTime.class)
        );
    }

    private BankTransactionDocument mapBankTransaction(ResultSet rs, int rowNum) throws SQLException {
        return new BankTransactionDocument(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("company_id")),
                UUID.fromString(rs.getString("bank_account_id")),
                rs.getString("bank_account_name"),
                rs.getString("transaction_number"),
                rs.getDate("transaction_date").toLocalDate(),
                rs.getString("transaction_type"),
                rs.getBigDecimal("amount"),
                rs.getString("description"),
                rs.getString("reference_no"),
                rs.getString("fiscal_period"),
                rs.getObject("deleted_at", OffsetDateTime.class)
        );
    }
}




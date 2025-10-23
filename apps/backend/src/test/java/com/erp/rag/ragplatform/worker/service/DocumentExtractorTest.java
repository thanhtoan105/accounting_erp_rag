package com.erp.rag.ragplatform.worker.service;

import com.erp.rag.ragplatform.worker.dao.DocumentExtractionDao;
import com.erp.rag.ragplatform.worker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentExtractor service.
 * <p>
 * Story 1.4 – AC1: Tests document extraction logic across all 7 document types.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentExtractorTest {

    @Mock
    private DocumentExtractionDao dao;

    private DocumentExtractor extractor;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        extractor = new DocumentExtractor(dao);
        companyId = UUID.randomUUID();
    }

    @Test
    void testExtractAll_ReturnsAllDocumentTypes() {
        // Arrange
        List<InvoiceDocument> invoices = List.of(createTestInvoice());
        List<BillDocument> bills = List.of(createTestBill());
        List<CustomerDocument> customers = List.of(createTestCustomer());
        List<VendorDocument> vendors = List.of(createTestVendor());
        List<PaymentDocument> payments = List.of(createTestPayment());
        List<JournalEntryDocument> journalEntries = List.of(createTestJournalEntry());
        List<BankTransactionDocument> bankTransactions = List.of(createTestBankTransaction());

        when(dao.extractInvoices(eq(companyId), any())).thenReturn(invoices);
        when(dao.extractBills(eq(companyId), any())).thenReturn(bills);
        when(dao.extractCustomers(eq(companyId), any())).thenReturn(customers);
        when(dao.extractVendors(eq(companyId), any())).thenReturn(vendors);
        when(dao.extractPayments(eq(companyId), any())).thenReturn(payments);
        when(dao.extractJournalEntries(eq(companyId), any())).thenReturn(journalEntries);
        when(dao.extractBankTransactions(eq(companyId), any())).thenReturn(bankTransactions);

        // Act
        List<ErpDocument> documents = extractor.extractAll(companyId);

        // Assert
        assertThat(documents).hasSize(7);
        assertThat(documents).containsAll(invoices);
        assertThat(documents).containsAll(bills);
        assertThat(documents).containsAll(customers);
    }

    @Test
    void testExtractFrom_SpecificTables() {
        // Arrange
        List<InvoiceDocument> invoices = List.of(createTestInvoice());
        when(dao.extractInvoices(eq(companyId), any())).thenReturn(invoices);

        // Act
        List<ErpDocument> documents = extractor.extractFrom(
                companyId, List.of("invoices"), null);

        // Assert
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).isInstanceOf(InvoiceDocument.class);
    }

    @Test
    void testInvoiceDocument_RawTextGeneration() {
        // Arrange
        InvoiceDocument invoice = createTestInvoice();

        // Act
        String rawText = invoice.getRawText();

        // Assert
        assertThat(rawText).isNotBlank();
        assertThat(rawText).contains("invoice");
        assertThat(rawText).contains("INV-001");
        assertThat(rawText).contains("10000.00");
        assertThat(rawText).contains("VND");
    }

    @Test
    void testCustomerDocument_Vietnamese_UTF8_Preserved() {
        // Arrange
        CustomerDocument customer = new CustomerDocument();
        customer.setId(UUID.randomUUID());
        customer.setCompanyId(companyId);
        customer.setCode("CUST-001");
        customer.setName("Công ty TNHH Thương mại Việt Nam"); // Vietnamese name with diacritics
        customer.setIsActive(true);

        // Act
        String rawText = customer.getRawText();

        // Assert
        assertThat(rawText).contains("Công ty TNHH Thương mại Việt Nam");
        assertThat(rawText).contains("ô"); // Check diacritics preserved
        assertThat(rawText).contains("ệ");
    }

    // Helper methods to create test documents

    private InvoiceDocument createTestInvoice() {
        return new InvoiceDocument(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                "Test Customer",
                "INV-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                "SENT",
                null,
                "2024-10",
                null);
    }

    private BillDocument createTestBill() {
        return new BillDocument(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                "Test Vendor",
                "BILL-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("5000.00"),
                BigDecimal.ZERO,
                "RECEIVED",
                null,
                "2024-10",
                null);
    }

    private CustomerDocument createTestCustomer() {
        return new CustomerDocument(
                UUID.randomUUID(),
                companyId,
                "CUST-001",
                "Test Customer",
                "0123456789",
                "123 Test St",
                "0901234567",
                "customer@test.com",
                "John Doe",
                new BigDecimal("50000.00"),
                30,
                true,
                null);
    }

    private VendorDocument createTestVendor() {
        return new VendorDocument(
                UUID.randomUUID(),
                companyId,
                "VEND-001",
                "Test Vendor",
                "Test Vendor Co",
                "0987654321",
                "456 Vendor Ave",
                "0909876543",
                "vendor@test.com",
                "Jane Smith",
                30,
                true,
                null);
    }

    private PaymentDocument createTestPayment() {
        return new PaymentDocument(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                "Test Customer",
                "PAY-001",
                LocalDate.now(),
                new BigDecimal("1000.00"),
                "BANK_TRANSFER",
                "REF-001",
                null,
                "2024-10",
                null);
    }

    private JournalEntryDocument createTestJournalEntry() {
        return new JournalEntryDocument(
                UUID.randomUUID(),
                companyId,
                "JE-001",
                LocalDate.now(),
                "GENERAL",
                "Test journal entry",
                "REF-001",
                "POSTED",
                "2024-10",
                null);
    }

    private BankTransactionDocument createTestBankTransaction() {
        return new BankTransactionDocument(
                UUID.randomUUID(),
                companyId,
                UUID.randomUUID(),
                "Main Bank Account",
                "BT-001",
                LocalDate.now(),
                "DEPOSIT",
                new BigDecimal("2000.00"),
                "Cash deposit",
                "REF-001",
                "2024-10",
                null);
    }
}

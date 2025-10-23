package com.erp.rag.ragplatform.worker.service;

import com.erp.rag.ragplatform.worker.domain.InvoiceDocument;
import com.erp.rag.ragplatform.worker.service.pii.PiiMaskingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TextTemplateRenderer.
 * <p>
 * Story 1.4 – AC2, AC5: Tests text preparation with Vietnamese UTF-8 support
 * and PII masking.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class TextTemplateRendererTest {

    @Mock
    private PiiMaskingService piiMaskingService;

    private TextTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new TextTemplateRenderer(piiMaskingService);
    }

    @Test
    void testRenderDocument_AppliesPiiMasking() throws Exception {
        // Arrange
        InvoiceDocument invoice = createTestInvoice("Nguyễn Văn A");

        String rawText = invoice.getRawText();
        String maskedText = rawText.replace("Nguyễn Văn A", "CUSTOMER_12345");

        when(piiMaskingService.maskText(eq(rawText), eq("invoice")))
                .thenReturn(maskedText);

        // Act
        String result = renderer.renderDocument(invoice);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("CUSTOMER_12345");
        assertThat(result).doesNotContain("Nguyễn Văn A");
    }

    @Test
    void testRenderDocument_Vietnamese_UTF8_Preserved() throws Exception {
        // Arrange
        String vietnameseText = "invoice INV-001: Hóa đơn bán hàng tiết kiệm | Amount: 1000000 VND | Date: 2024-10-21";

        when(piiMaskingService.maskText(any(), any())).thenReturn(vietnameseText);

        InvoiceDocument invoice = createTestInvoice("Test");

        // Act
        String result = renderer.renderDocument(invoice);

        // Assert
        assertThat(result).contains("ó");
        assertThat(result).contains("ơ");
        assertThat(result).contains("á");
        assertThat(result).contains("ế");
    }

    @Test
    void testRenderDocument_PiiMaskingFailure_ThrowsException() throws Exception {
        // Arrange
        InvoiceDocument invoice = createTestInvoice("Test Customer");

        when(piiMaskingService.maskText(any(), any()))
                .thenThrow(new PiiMaskingService.PiiMaskingException("Test failure"));

        // Act & Assert
        assertThatThrownBy(() -> renderer.renderDocument(invoice))
                .isInstanceOf(TextTemplateRenderer.TextRenderingException.class)
                .hasMessageContaining("PII masking failure");
    }

    @Test
    void testRenderDocument_SoftDeletedDocument_ReturnsNull() {
        // Arrange
        InvoiceDocument invoice = createTestInvoice("Test");
        invoice.setDeletedAt(java.time.OffsetDateTime.now());

        // Act
        String result = renderer.renderDocument(invoice);

        // Assert
        assertThat(result).isNull();
    }

    private InvoiceDocument createTestInvoice(String customerName) {
        InvoiceDocument invoice = new InvoiceDocument(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                customerName,
                "INV-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO,
                "SENT",
                null,
                "2024-10",
                null);
        invoice.setDescription("Test invoice description");
        return invoice;
    }
}

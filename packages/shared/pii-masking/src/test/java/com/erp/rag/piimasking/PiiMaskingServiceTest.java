package com.erp.rag.piimasking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PiiMaskingService Unit Tests")
class PiiMaskingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PiiMaskingService piiMaskingService;

    private static final UUID TEST_COMPANY_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID TEST_RECORD_ID = UUID.fromString("87654321-4321-4321-4321-210987654321");
    private static final String TEST_SALT = "test_salt_12345";

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService(jdbcTemplate);
        
        // Mock vault salt retrieval (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(jdbcTemplate.queryForObject(
            eq("SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?"),
            eq(String.class),
            anyString()
        )).thenReturn(TEST_SALT);
        
        // Mock mask mapping storage (lenient)
        lenient().when(jdbcTemplate.update(
            anyString(),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(1);
    }

    @Test
    @DisplayName("maskCustomerName - should return deterministic hash with Customer_ prefix")
    void testMaskCustomerName_Deterministic() {
        // Given
        String customerName = "Công ty TNHH ABC";
        
        // When
        String masked1 = piiMaskingService.maskCustomerName(customerName, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskCustomerName(customerName, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo(masked2); // Deterministic: same input = same output
        assertThat(masked1).startsWith("Customer_");
        assertThat(masked1).hasSize(14); // "Customer_" (9) + 5 hash chars
        
        // Verify mask mapping stored
        verify(jdbcTemplate, times(2)).update(
            contains("INSERT INTO pii_mask_map"),
            eq("customers"), eq(TEST_RECORD_ID), eq("name"), eq(masked1), anyString(), eq(1)
        );
    }

    @Test
    @DisplayName("maskCustomerName - should handle null and empty strings")
    void testMaskCustomerName_NullAndEmpty() {
        // When
        String maskedNull = piiMaskingService.maskCustomerName(null, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String maskedEmpty = piiMaskingService.maskCustomerName("", TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String maskedBlank = piiMaskingService.maskCustomerName("   ", TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(maskedNull).isEqualTo("Customer_00000");
        assertThat(maskedEmpty).isEqualTo("Customer_00000");
        assertThat(maskedBlank).isEqualTo("Customer_00000");
    }

    @Test
    @DisplayName("maskTaxId - should preserve last 4 digits with TAX_ prefix")
    void testMaskTaxId_PartialMasking() {
        // Given
        String taxId = "0123456789";
        
        // When
        String masked = piiMaskingService.maskTaxId(taxId, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked).isEqualTo("TAX_*****6789");
        assertThat(masked).endsWith("6789"); // Last 4 digits preserved
        
        verify(jdbcTemplate).update(
            contains("INSERT INTO pii_mask_map"),
            eq("customers"), eq(TEST_RECORD_ID), eq("tax_code"), eq(masked), anyString(), eq(1)
        );
    }

    @Test
    @DisplayName("maskTaxId - should handle short tax IDs")
    void testMaskTaxId_ShortInput() {
        // When
        String masked1 = piiMaskingService.maskTaxId("123", TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskTaxId(null, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo("TAX_****");
        assertThat(masked2).isEqualTo("TAX_****");
    }

    @Test
    @DisplayName("maskTaxId - should clean non-numeric characters")
    void testMaskTaxId_WithDashes() {
        // Given
        String taxIdWithDash = "0123456789-001";
        
        // When
        String masked = piiMaskingService.maskTaxId(taxIdWithDash, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked).isEqualTo("TAX_*****9001"); // Last 4 digits of cleaned "0123456789001"
    }

    @Test
    @DisplayName("maskEmail - should preserve domain and hash local part")
    void testMaskEmail_DomainPreservation() {
        // Given
        String email = "user@example.com";

        // When
        String masked = piiMaskingService.maskEmail(email, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);

        // Then
        assertThat(masked).endsWith("@example.com"); // Domain preserved
        assertThat(masked).isNotEqualTo(email); // Modified from original
    }

    @Test
    @DisplayName("maskEmail - should handle invalid emails")
    void testMaskEmail_InvalidFormat() {
        // When
        String masked1 = piiMaskingService.maskEmail(null, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskEmail("notanemail", TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked3 = piiMaskingService.maskEmail("multiple@at@signs.com", TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo("masked@unknown.com");
        assertThat(masked2).isEqualTo("masked@unknown.com");
        assertThat(masked3).isEqualTo("masked@unknown.com");
    }

    @Test
    @DisplayName("maskPhone - should return deterministic hash with Phone_ prefix")
    void testMaskPhone_Deterministic() {
        // Given
        String phone = "+84901234567";
        
        // When
        String masked1 = piiMaskingService.maskPhone(phone, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskPhone(phone, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo(masked2); // Deterministic
        assertThat(masked1).startsWith("Phone_");
        assertThat(masked1).hasSize(11); // "Phone_" (6) + 5 hash chars
        
        verify(jdbcTemplate, times(2)).update(
            contains("INSERT INTO pii_mask_map"),
            eq("customers"), eq(TEST_RECORD_ID), eq("phone"), eq(masked1), anyString(), eq(1)
        );
    }

    @Test
    @DisplayName("maskPhone - should clean non-numeric characters except +")
    void testMaskPhone_FormatCleaning() {
        // Given
        String phoneFormatted = "(028) 1234-567";
        String phoneClean = "0281234567";
        
        // When
        String masked1 = piiMaskingService.maskPhone(phoneFormatted, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskPhone(phoneClean, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo(masked2); // Same after cleaning
    }

    @Test
    @DisplayName("maskAddress - should extract Vietnamese city and return City_ format")
    void testMaskAddress_CityExtraction() {
        // Given
        String address1 = "123 Nguyễn Huệ, Quận 1, TPHCM";
        String address2 = "456 Lê Lợi, Q3, Hồ Chí Minh";
        String address3 = "789 Trần Phú, Hà Nội";
        
        // When
        String masked1 = piiMaskingService.maskAddress(address1, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskAddress(address2, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String masked3 = piiMaskingService.maskAddress(address3, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isEqualTo("City_TPHCM"); // Normalized
        assertThat(masked2).isEqualTo("City_TPHCM"); // Normalized from "Hồ Chí Minh"
        assertThat(masked3).isEqualTo("City_Hà Nội");
        
        verify(jdbcTemplate, times(3)).update(
            contains("INSERT INTO pii_mask_map"),
            eq("customers"), eq(TEST_RECORD_ID), eq("address"), anyString(), anyString(), eq(1)
        );
    }

    @Test
    @DisplayName("maskAddress - should return City_Unknown for unrecognized addresses")
    void testMaskAddress_UnknownCity() {
        // Given
        String address = "123 Unknown Street, Foreign Country";
        
        // When
        String masked = piiMaskingService.maskAddress(address, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked).isEqualTo("City_Unknown");
    }

    @Test
    @DisplayName("Masking with different companies should produce different hashes")
    void testMasking_DifferentCompanies() {
        // Given
        UUID company1 = UUID.randomUUID();
        UUID company2 = UUID.randomUUID();
        String name = "Test Customer";
        
        // Clear salt cache to force fresh lookups
        piiMaskingService.clearSaltCache();
        
        // Mock different salts for different companies
        lenient().when(jdbcTemplate.queryForObject(
            eq("SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?"),
            eq(String.class),
            eq("pii_masking_company_" + company1)
        )).thenReturn("salt_company_1");
        
        lenient().when(jdbcTemplate.queryForObject(
            eq("SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?"),
            eq(String.class),
            eq("pii_masking_company_" + company2)
        )).thenReturn("salt_company_2");
        
        // When
        String masked1 = piiMaskingService.maskCustomerName(name, company1, "customers", TEST_RECORD_ID);
        String masked2 = piiMaskingService.maskCustomerName(name, company2, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(masked1).isNotEqualTo(masked2); // Different companies = different hashes
    }

    @Test
    @DisplayName("clearSaltCache - should clear cached salts")
    void testClearSaltCache() {
        // Given
        String name = "Test Customer";
        piiMaskingService.maskCustomerName(name, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // When
        piiMaskingService.clearSaltCache();
        
        // Then - next call should query vault again
        piiMaskingService.maskCustomerName(name, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        verify(jdbcTemplate, atLeast(2)).queryForObject(
            eq("SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?"),
            eq(String.class),
            anyString()
        );
    }

    @Test
    @DisplayName("Masking should handle Vietnamese diacritics correctly")
    void testMasking_VietnameseDiacritics() {
        // Given
        String name = "Nguyễn Văn Minh";
        String address = "123 Đường Láng, Đống Đa, Hà Nội";
        
        // When
        String maskedName = piiMaskingService.maskCustomerName(name, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        String maskedAddress = piiMaskingService.maskAddress(address, TEST_COMPANY_ID, "customers", TEST_RECORD_ID);
        
        // Then
        assertThat(maskedName).startsWith("Customer_");
        assertThat(maskedAddress).isEqualTo("City_Hà Nội");
    }
}

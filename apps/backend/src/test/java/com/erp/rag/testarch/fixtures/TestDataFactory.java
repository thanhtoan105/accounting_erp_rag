package com.erp.rag.testarch.fixtures;

import com.github.javafaker.Faker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Test Data Factory for generating realistic Vietnamese accounting test data
 * 
 * Provides deterministic and randomized fixture builders for:
 * - Companies, Users, Customers, Vendors
 * - Invoices, Bills, Journal Entries
 * - Payments, Bank Transactions
 * 
 * Usage:
 * <pre>
 * {@code
 * UUID companyId = TestDataFactory.randomCompanyId();
 * String customerName = TestDataFactory.vietnameseCompanyName();
 * BigDecimal amount = TestDataFactory.amount(1000, 10000);
 * }
 * </pre>
 * 
 * @author BMAD Test Architect
 */
public class TestDataFactory {

    private static final Faker faker = new Faker(new Locale("vi"));
    private static final Faker fakerEn = new Faker(new Locale("en"));
    
    // ========================================================================
    // IDs and Basic Entities
    // ========================================================================
    
    public static UUID randomCompanyId() {
        return UUID.randomUUID();
    }
    
    public static UUID randomUserId() {
        return UUID.randomUUID();
    }
    
    public static UUID randomCustomerId() {
        return UUID.randomUUID();
    }
    
    public static UUID randomVendorId() {
        return UUID.randomUUID();
    }
    
    public static UUID randomInvoiceId() {
        return UUID.randomUUID();
    }
    
    // ========================================================================
    // Vietnamese Names and Addresses
    // ========================================================================
    
    public static String vietnameseCompanyName() {
        String[] prefixes = {
            "Công ty TNHH",
            "Công ty Cổ phần",
            "Doanh nghiệp tư nhân",
            "Công ty",
            "Chi nhánh"
        };
        String[] businesses = {
            "Thương mại",
            "Xuất nhập khẩu",
            "Sản xuất",
            "Dịch vụ",
            "Đầu tư",
            "Xây dựng",
            "Công nghệ"
        };
        String prefix = prefixes[faker.number().numberBetween(0, prefixes.length)];
        String business = businesses[faker.number().numberBetween(0, businesses.length)];
        String name = fakerEn.company().name();
        return prefix + " " + business + " " + name;
    }
    
    public static String vietnamesePersonName() {
        String[] firstNames = {
            "Nguyễn Văn", "Trần Thị", "Lê Văn", "Phạm Thị",
            "Hoàng Văn", "Huỳnh Thị", "Phan Văn", "Vũ Thị",
            "Võ Văn", "Đặng Thị", "Bùi Văn", "Đỗ Thị",
            "Ngô Văn", "Dương Thị", "Lý Văn"
        };
        String[] lastNames = {
            "An", "Bình", "Cường", "Dũng", "Hà", "Hòa", "Hưng",
            "Khang", "Linh", "Long", "Mai", "Minh", "Nam", "Phong",
            "Quân", "Sơn", "Tâm", "Thành", "Tú", "Tuấn", "Tùng"
        };
        String first = firstNames[faker.number().numberBetween(0, firstNames.length)];
        String last = lastNames[faker.number().numberBetween(0, lastNames.length)];
        return first + " " + last;
    }
    
    public static String vietnameseAddress() {
        String number = String.valueOf(faker.number().numberBetween(1, 999));
        String[] streets = {
            "Lê Lợi", "Nguyễn Huệ", "Hai Bà Trưng", "Trần Hưng Đạo",
            "Lý Thường Kiệt", "Phan Đình Phùng", "Hoàng Diệu",
            "Võ Thị Sáu", "Pasteur", "Nam Kỳ Khởi Nghĩa"
        };
        String[] wards = {
            "Phường Bến Nghé", "Phường Đa Kao", "Phường Bến Thành",
            "Phường 1", "Phường 2", "Phường 3", "Phường Tân Định"
        };
        String[] districts = {
            "Quận 1", "Quận 2", "Quận 3", "Quận 4", "Quận 5",
            "Quận Tân Bình", "Quận Phú Nhuận", "Quận Bình Thạnh"
        };
        String street = streets[faker.number().numberBetween(0, streets.length)];
        String ward = wards[faker.number().numberBetween(0, wards.length)];
        String district = districts[faker.number().numberBetween(0, districts.length)];
        return number + " " + street + ", " + ward + ", " + district + ", TP.HCM";
    }
    
    public static String vietnameseTaxId() {
        // Format: 10-13 digits
        long base = faker.number().numberBetween(1000000000L, 9999999999L);
        return String.valueOf(base);
    }
    
    public static String vietnamesePhone() {
        // Mobile: 09x, 08x, 07x, 03x + 8 digits
        String[] prefixes = {"090", "091", "093", "084", "085", "070", "076", "033", "034"};
        String prefix = prefixes[faker.number().numberBetween(0, prefixes.length)];
        long suffix = faker.number().numberBetween(10000000L, 99999999L);
        return prefix + suffix;
    }
    
    // ========================================================================
    // Financial Data
    // ========================================================================
    
    public static BigDecimal amount(double min, double max) {
        double value = faker.number().randomDouble(2, (long) min, (long) max);
        return BigDecimal.valueOf(value);
    }
    
    public static BigDecimal vndAmount() {
        // Common VND amounts: 100K - 100M
        return amount(100_000, 100_000_000);
    }
    
    public static BigDecimal usdAmount() {
        // Common USD amounts: $10 - $10,000
        return amount(10, 10_000);
    }
    
    public static String accountCode(String prefix) {
        // Circular 200 format: 111, 112, 131, 331, etc.
        int suffix = faker.number().numberBetween(0, 9);
        return prefix + suffix;
    }
    
    public static String invoiceNumber() {
        // Format: INV-2025-00001
        int year = LocalDate.now().getYear();
        int seq = faker.number().numberBetween(1, 99999);
        return String.format("INV-%d-%05d", year, seq);
    }
    
    public static String billNumber() {
        int year = LocalDate.now().getYear();
        int seq = faker.number().numberBetween(1, 99999);
        return String.format("BILL-%d-%05d", year, seq);
    }
    
    public static String journalEntryNumber() {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        int seq = faker.number().numberBetween(1, 9999);
        return String.format("JE-%d%02d-%04d", year, month, seq);
    }
    
    // ========================================================================
    // Dates
    // ========================================================================
    
    public static LocalDate recentDate() {
        return LocalDate.now().minusDays(faker.number().numberBetween(1, 90));
    }
    
    public static LocalDateTime recentDateTime() {
        return LocalDateTime.now().minusDays(faker.number().numberBetween(1, 90));
    }
    
    public static LocalDate futureDate() {
        return LocalDate.now().plusDays(faker.number().numberBetween(1, 90));
    }
    
    public static String fiscalPeriod() {
        int year = LocalDate.now().getYear();
        int month = faker.number().numberBetween(1, 12);
        return String.format("%d-%02d", year, month);
    }
    
    // ========================================================================
    // Descriptions (Vietnamese)
    // ========================================================================
    
    public static String invoiceDescription() {
        String[] templates = {
            "Bán hàng hóa theo hợp đồng số %s",
            "Cung cấp dịch vụ %s tháng %d/%d",
            "Xuất hóa đơn bán hàng cho %s",
            "Thanh toán tiền hàng đợt %d"
        };
        String template = templates[faker.number().numberBetween(0, templates.length)];
        if (template.contains("%s")) {
            return String.format(template, faker.number().digits(5));
        }
        return String.format(template, faker.number().numberBetween(1, 12),
            LocalDate.now().getYear(), faker.number().numberBetween(1, 5));
    }
    
    public static String paymentDescription() {
        String[] templates = {
            "Thanh toán tiền hàng theo hóa đơn %s",
            "Thu tiền khách hàng %s",
            "Chi tiền mặt cho %s",
            "Chuyển khoản thanh toán công nợ"
        };
        String template = templates[faker.number().numberBetween(0, templates.length)];
        return template.contains("%s") ? String.format(template, invoiceNumber()) : template;
    }
}

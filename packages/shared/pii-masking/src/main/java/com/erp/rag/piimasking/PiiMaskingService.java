package com.erp.rag.piimasking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for masking Personally Identifiable Information (PII) fields
 * using deterministic hashing and partial masking strategies.
 * 
 * Implements Vietnam Circular 200/2014/TT-BTC compliant PII masking
 * with Supabase Vault integration for cryptographic salt storage.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PiiMaskingService {

    private final JdbcTemplate jdbcTemplate;
    
    // Cache for salts to minimize vault queries
    private final Map<String, String> saltCache = new ConcurrentHashMap<>();
    
    // Current salt version (incremented on rotation)
    private static final int CURRENT_SALT_VERSION = 1;
    
    // Vietnamese city pattern for address masking
    private static final Pattern CITY_PATTERN = Pattern.compile(
        "(TP\\.?HCM|TPHCM|Hà Nội|Hồ Chí Minh|Đà Nẵng|Hải Phòng|Cần Thơ|" +
        "An Giang|Bà Rịa|Bắc Giang|Bắc Kạn|Bạc Liêu|Bắc Ninh|" +
        "Bến Tre|Bình Định|Bình Dương|Bình Phước|Bình Thuận|" +
        "Cà Mau|Cao Bằng|Đắk Lắk|Đắk Nông|Điện Biên|Đồng Nai|" +
        "Đồng Tháp|Gia Lai|Hà Giang|Hà Nam|Hà Tĩnh|Hải Dương|" +
        "Hậu Giang|Hòa Bình|Hưng Yên|Khánh Hòa|Kiên Giang|" +
        "Kon Tum|Lai Châu|Lâm Đồng|Lạng Sơn|Lào Cai|Long An|" +
        "Nam Định|Nghệ An|Ninh Bình|Ninh Thuận|Phú Thọ|Phú Yên|" +
        "Quảng Bình|Quảng Nam|Quảng Ngãi|Quảng Ninh|Quảng Trị|" +
        "Sóc Trăng|Sơn La|Tây Ninh|Thái Bình|Thái Nguyên|" +
        "Thanh Hóa|Thừa Thiên Huế|Tiền Giang|Trà Vinh|Tuyên Quang|" +
        "Vĩnh Long|Vĩnh Phúc|Yên Bái)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Masks a customer/company name using deterministic hashing.
     * Format: "Customer_xxxxx" where xxxxx is first 5 chars of hash.
     * 
     * @param name Customer/company name to mask
     * @param companyId Company ID for salt selection
     * @param sourceTable Source table name (for mapping storage)
     * @param sourceId Source record ID
     * @return Masked name in format "Customer_xxxxx"
     */
    public String maskCustomerName(String name, UUID companyId, String sourceTable, UUID sourceId) {
        if (name == null || name.trim().isEmpty()) {
            return "Customer_00000";
        }
        
        String salt = getSalt(companyId);
        String fullHash = DigestUtils.sha256Hex(name.trim() + salt);
        String shortHash = fullHash.substring(0, 5);
        String masked = "Customer_" + shortHash;
        
        // Store mapping in pii_mask_map
        storeMaskMapping(sourceTable, sourceId, "name", masked, fullHash, CURRENT_SALT_VERSION);
        
        log.debug("Masked customer name for company {} from table {}", companyId, sourceTable);
        return masked;
    }

    /**
     * Masks a tax ID using partial masking strategy.
     * Format: "TAX_*****1234" preserving last 4 digits.
     * 
     * @param taxId Tax ID to mask (10 or 13 digits for Vietnamese MST)
     * @param companyId Company ID for salt selection
     * @param sourceTable Source table name
     * @param sourceId Source record ID
     * @return Partially masked tax ID
     */
    public String maskTaxId(String taxId, UUID companyId, String sourceTable, UUID sourceId) {
        if (taxId == null || taxId.trim().isEmpty()) {
            return "TAX_****";
        }
        
        String cleanTaxId = taxId.trim().replaceAll("[^0-9]", "");
        
        if (cleanTaxId.length() < 4) {
            return "TAX_****";
        }
        
        String lastFour = cleanTaxId.substring(cleanTaxId.length() - 4);
        String masked = "TAX_*****" + lastFour;
        
        // Store full hash for reversibility
        String salt = getSalt(companyId);
        String fullHash = DigestUtils.sha256Hex(cleanTaxId + salt);
        storeMaskMapping(sourceTable, sourceId, "tax_code", masked, fullHash, CURRENT_SALT_VERSION);
        
        log.debug("Masked tax ID for company {} from table {}", companyId, sourceTable);
        return masked;
    }

    /**
     * Masks an email address preserving domain.
     * Format: "user_xxxxx@example.com" where xxxxx is hash of local part.
     * 
     * @param email Email address to mask
     * @param companyId Company ID for salt selection
     * @param sourceTable Source table name
     * @param sourceId Source record ID
     * @return Domain-preserved masked email
     */
    public String maskEmail(String email, UUID companyId, String sourceTable, UUID sourceId) {
        if (email == null || email.trim().isEmpty()) {
            return "masked@unknown.com";
        }

        String e = email.trim();
        int at = e.indexOf('@');

        // Validate @ position
        if (at <= 0 || at == e.length() - 1) {
            return "masked@unknown.com";
        }

        String localPart = e.substring(0, at).trim();
        String domain = e.substring(at + 1).trim().toLowerCase(); // Normalize domain to lowercase

        // Validate domain doesn't contain another @ (invalid email)
        if (domain.contains("@")) {
            return "masked@unknown.com";
        }

        String salt = getSalt(companyId);

        // Hash only the local part for short hash (as per method contract)
        String localHash = DigestUtils.sha256Hex(localPart.toLowerCase() + salt);
        String shortHash = localHash.substring(0, 5);

        // Preserve first 4 chars of local part if available
        String prefix = localPart.length() >= 4
            ? localPart.substring(0, 4)
            : localPart;

        String masked = prefix + "_" + shortHash + "@" + domain;

        // Store full email hash for reversibility
        String fullHash = DigestUtils.sha256Hex(e.toLowerCase() + salt);
        storeMaskMapping(sourceTable, sourceId, "email", masked, fullHash, CURRENT_SALT_VERSION);

        log.debug("Masked email for company {} from table {}", companyId, sourceTable);
        return masked;
    }

    /**
     * Masks a phone number using deterministic hashing.
     * Format: "Phone_xxxxx".
     * 
     * @param phone Phone number to mask
     * @param companyId Company ID for salt selection
     * @param sourceTable Source table name
     * @param sourceId Source record ID
     * @return Masked phone number
     */
    public String maskPhone(String phone, UUID companyId, String sourceTable, UUID sourceId) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone_00000";
        }
        
        String cleanPhone = phone.trim().replaceAll("[^0-9+]", "");
        
        String salt = getSalt(companyId);
        String fullHash = DigestUtils.sha256Hex(cleanPhone + salt);
        String shortHash = fullHash.substring(0, 5);
        String masked = "Phone_" + shortHash;
        
        storeMaskMapping(sourceTable, sourceId, "phone", masked, fullHash, CURRENT_SALT_VERSION);
        
        log.debug("Masked phone for company {} from table {}", companyId, sourceTable);
        return masked;
    }

    /**
     * Masks an address keeping only city/province level.
     * Format: "City_TPHCM" or "City_HàNội".
     * 
     * @param address Full address to mask
     * @param companyId Company ID for salt selection
     * @param sourceTable Source table name
     * @param sourceId Source record ID
     * @return City-only masked address
     */
    public String maskAddress(String address, UUID companyId, String sourceTable, UUID sourceId) {
        if (address == null || address.trim().isEmpty()) {
            return "City_Unknown";
        }
        
        String city = extractVietnameseCity(address);
        String masked = "City_" + city;
        
        // Store full hash for reversibility
        String salt = getSalt(companyId);
        String fullHash = DigestUtils.sha256Hex(address.trim() + salt);
        storeMaskMapping(sourceTable, sourceId, "address", masked, fullHash, CURRENT_SALT_VERSION);
        
        log.debug("Masked address for company {} from table {}", companyId, sourceTable);
        return masked;
    }

    /**
     * Extracts city/province name from Vietnamese address using regex pattern.
     * 
     * @param address Full address string
     * @return City/province name or "Unknown" if not found
     */
    private String extractVietnameseCity(String address) {
        Matcher matcher = CITY_PATTERN.matcher(address);
        if (matcher.find()) {
            String city = matcher.group(1);
            // Normalize common variations
            if (city.matches("(?i)(TP\\.?HCM|TPHCM|Hồ Chí Minh)")) {
                return "TPHCM";
            }
            return city.trim();
        }
        return "Unknown";
    }

    /**
     * Retrieves cryptographic salt from Supabase Vault.
     * Uses company-specific salt if available, falls back to global salt.
     * 
     * @param companyId Company ID for salt selection
     * @return Salt string from vault
     */
    private String getSalt(UUID companyId) {
        String saltKey = companyId != null 
            ? "pii_masking_company_" + companyId 
            : "pii_masking_global_salt";
        
        // Check cache first
        if (saltCache.containsKey(saltKey)) {
            return saltCache.get(saltKey);
        }
        
        try {
            // Query Supabase Vault (requires service_role privileges)
            // NOTE: Supabase Vault extension must be enabled on database
            String salt = jdbcTemplate.queryForObject(
                "SELECT decrypted_secret FROM vault.decrypted_secrets WHERE name = ?",
                String.class,
                saltKey
            );
            
            if (salt != null && !salt.isEmpty()) {
                saltCache.put(saltKey, salt);
                log.info("Retrieved salt from vault: {}", saltKey);
                return salt;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve salt from vault for key {}, falling back to global salt: {}", 
                saltKey, e.getMessage());
        }
        
        // Fallback to global salt if company-specific not found
        if (companyId != null) {
            return getSalt(null);
        }
        
        // Final fallback: use hardcoded default salt (for testing only)
        log.warn("Using default fallback salt - this should not happen in production!");
        return "default_salt_for_testing_only_change_in_production";
    }

    /**
     * Stores PII mask mapping in pii_mask_map table for reversibility and audit trail.
     * 
     * @param sourceTable Source table name (e.g., "customers", "companies")
     * @param sourceId Source record ID
     * @param field Field name (e.g., "name", "tax_code")
     * @param maskedValue Masked value (e.g., "Customer_a7f5d")
     * @param hash Full SHA-256 hash for reversibility
     * @param saltVersion Salt version used for this hash
     */
    private void storeMaskMapping(String sourceTable, UUID sourceId, String field, 
                                    String maskedValue, String hash, int saltVersion) {
        try {
            jdbcTemplate.update(
                "INSERT INTO pii_mask_map (source_table, source_id, field, masked_value, hash, salt_version) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (source_table, source_id, field) DO UPDATE " +
                "SET masked_value = EXCLUDED.masked_value, hash = EXCLUDED.hash, " +
                "salt_version = EXCLUDED.salt_version, created_at = now()",
                sourceTable, sourceId, field, maskedValue, hash, saltVersion
            );
            log.debug("Stored mask mapping for {}.{} field {}", sourceTable, sourceId, field);
        } catch (Exception e) {
            // Log error but don't fail masking operation (fire-and-forget for performance)
            log.error("Failed to store mask mapping for {}.{} field {}: {}", 
                sourceTable, sourceId, field, e.getMessage());
        }
    }

    /**
     * Clears the salt cache. Useful for testing or after salt rotation.
     */
    public void clearSaltCache() {
        saltCache.clear();
        log.info("Salt cache cleared");
    }
}

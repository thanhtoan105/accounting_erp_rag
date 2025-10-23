package com.erp.rag.ragplatform.worker.service.pii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Implementation of PII masking service with Vietnamese-aware patterns.
 * <p>
 * Story 1.4 – AC5: PII masking implementation for embedding worker.
 * Masks customer names, vendor names, tax IDs, phone numbers, emails, and
 * addresses.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class PiiMaskingServiceImpl implements PiiMaskingService {

    private static final Logger logger = LoggerFactory.getLogger(PiiMaskingServiceImpl.class);

    // Vietnamese phone pattern: +84, 0, followed by 9-10 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+84|0)\\d{9,10}");

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Vietnamese tax code: 10 or 13 digits
    private static final Pattern TAX_CODE_PATTERN = Pattern.compile(
            "\\b\\d{10}(?:-\\d{3})?\\b");

    // Vietnamese address keywords (simplified for MVP)
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(?:Số|số)\\s+\\d+[^,]*,\\s*(?:phường|Phường|quận|Quận|huyện|Huyện|thành phố|Thành phố|tỉnh|Tỉnh)[^|]*",
            Pattern.CASE_INSENSITIVE);

    private int customerCounter = 10000;
    private int vendorCounter = 20000;
    private int phoneCounter = 40000;
    private int emailCounter = 50000;
    private int addressCounter = 60000;

    @Override
    public String maskText(String rawText, String documentType) throws PiiMaskingException {
        if (rawText == null || rawText.isBlank()) {
            return rawText;
        }

        long startTime = System.currentTimeMillis();

        try {
            String maskedText = rawText;

            // Apply masking rules based on document type
            switch (documentType.toLowerCase()) {
                case "invoice":
                case "payment":
                    maskedText = maskCustomerNames(maskedText);
                    maskedText = maskEmails(maskedText);
                    maskedText = maskPhones(maskedText);
                    maskedText = maskAddresses(maskedText);
                    break;

                case "bill":
                case "vendor":
                    maskedText = maskVendorNames(maskedText);
                    maskedText = maskEmails(maskedText);
                    maskedText = maskPhones(maskedText);
                    maskedText = maskAddresses(maskedText);
                    break;

                case "customer":
                    maskedText = maskCustomerNames(maskedText);
                    maskedText = maskTaxCodes(maskedText);
                    maskedText = maskEmails(maskedText);
                    maskedText = maskPhones(maskedText);
                    maskedText = maskAddresses(maskedText);
                    break;

                case "journal_entry":
                    maskedText = maskEmails(maskedText);
                    maskedText = maskPhones(maskedText);
                    break;

                case "bank_transaction":
                    maskedText = maskEmails(maskedText);
                    maskedText = maskPhones(maskedText);
                    maskedText = maskAddresses(maskedText);
                    break;

                default:
                    logger.warn("Unknown document type for PII masking: {}", documentType);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > 100) {
                logger.warn("PII masking exceeded 100ms SLA: {}ms for document type {}",
                        elapsedTime, documentType);
            }

            return maskedText;

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.error("PII masking failed after {}ms: {}", elapsedTime, e.getMessage(), e);
            throw new PiiMaskingException("Failed to mask PII in text", e);
        }
    }

    private String maskCustomerNames(String text) {
        // For MVP, use simple pattern matching for "Customer: <name>" format
        Pattern customerPattern = Pattern.compile("Customer:\\s*([^|]+)");
        Matcher matcher = customerPattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = "Customer: CUSTOMER_" + (customerCounter++);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String maskVendorNames(String text) {
        // For MVP, use simple pattern matching for "Vendor: <name>" format
        Pattern vendorPattern = Pattern.compile("Vendor:\\s*([^|]+)");
        Matcher matcher = vendorPattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = "Vendor: VENDOR_" + (vendorCounter++);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String maskTaxCodes(String text) {
        Matcher matcher = TAX_CODE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String taxCode = matcher.group();
            String lastFour = taxCode.length() >= 4 ? taxCode.substring(taxCode.length() - 4) : taxCode;
            String replacement = "TAX_*****" + lastFour;
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String maskPhones(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = "PHONE_" + (phoneCounter++);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String maskEmails(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = "EMAIL_" + (emailCounter++);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String maskAddresses(String text) {
        Matcher matcher = ADDRESS_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String replacement = "ADDRESS_" + (addressCounter++);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }
}

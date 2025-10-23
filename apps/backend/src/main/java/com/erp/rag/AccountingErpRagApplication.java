package com.erp.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main application entry point for Accounting ERP RAG platform.
 *
 * This Spring Boot application provides:
 * - Read-only access to ERP database via Supabase
 * - RAG pipeline for AI-powered accounting insights
 * - Compliance with Vietnam Circular 200/2014/TT-BTC
 */
@SpringBootApplication
@EnableRetry
public class AccountingErpRagApplication {

    public static void main(String[] args) {
        
        SpringApplication.run(AccountingErpRagApplication.class, args);
    }
}

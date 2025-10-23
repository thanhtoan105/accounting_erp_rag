package com.erp.rag.ragplatform.rag.dto;

import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * DTO for RAG query API requests.
 * <p>
 * Story 1.5 – AC1: Request payload for /api/v1/rag/query endpoint with
 * validation constraints.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class QueryRequest {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Query text is required")
    @Size(min = 3, max = 5000, message = "Query must be between 3 and 5000 characters")
    private String query;

    @NotBlank(message = "Language is required")
    @Pattern(regexp = "^(vi|en)$", message = "Language must be 'vi' or 'en'")
    private String language;

    private Map<String, Object> filters;

    // Constructors
    public QueryRequest() {
    }

    public QueryRequest(Long companyId, String query, String language) {
        this.companyId = companyId;
        this.query = query;
        this.language = language;
    }

    // Getters and Setters
    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "companyId=" + companyId +
                ", query='" + query + '\'' +
                ", language='" + language + '\'' +
                ", filters=" + filters +
                '}';
    }
}

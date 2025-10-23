package com.erp.rag.ragplatform.rag.dto;

import java.util.Map;

/**
 * DTO for retrieved document in RAG query response.
 * <p>
 * Story 1.5 – AC10: Retrieved document with id, type, relevance, excerpt, and
 * metadata.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class RetrievedDocumentDTO {

    private Long id;
    private String documentType;
    private String module;
    private Double relevanceScore;
    private String excerpt;
    private Map<String, Object> metadata;

    public RetrievedDocumentDTO() {
    }

    public RetrievedDocumentDTO(Long id, String documentType, String module, Double relevanceScore,
            String excerpt, Map<String, Object> metadata) {
        this.id = id;
        this.documentType = documentType;
        this.module = module;
        this.relevanceScore = relevanceScore;
        this.excerpt = excerpt;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "RetrievedDocumentDTO{" +
                "id=" + id +
                ", documentType='" + documentType + '\'' +
                ", module='" + module + '\'' +
                ", relevanceScore=" + relevanceScore +
                '}';
    }
}

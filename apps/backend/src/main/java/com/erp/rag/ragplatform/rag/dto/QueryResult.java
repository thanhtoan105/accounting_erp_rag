package com.erp.rag.ragplatform.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing a single query result document with metadata.
 * <p>
 * Story 1.5 – AC3, AC4: Document with relevance score, metadata, and
 * content for context window management.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class QueryResult {

    private final UUID documentId;
    private final String sourceTable;
    private final String sourceId;
    private final String fiscalPeriod;
    private final String title;
    private final String content;
    private final Double relevanceScore;
    private final Object metadata;

    public QueryResult(UUID documentId, String sourceTable, String sourceId,
                     String fiscalPeriod, String title, String content,
                     Double relevanceScore, Object metadata) {
        this.documentId = documentId;
        this.sourceTable = sourceTable;
        this.sourceId = sourceId;
        this.fiscalPeriod = fiscalPeriod;
        this.title = title;
        this.content = content;
        this.relevanceScore = relevanceScore;
        this.metadata = metadata;
    }

    // Getters
    public UUID getDocumentId() {
        return documentId;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getFiscalPeriod() {
        return fiscalPeriod;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public Object getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "documentId=" + documentId +
                ", sourceTable='" + sourceTable + '\'' +
                ", sourceId=" + sourceId +
                ", fiscalPeriod='" + fiscalPeriod + '\'' +
                ", title='" + title + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", relevanceScore=" + relevanceScore +
                ", metadata=" + metadata +
                '}';
    }
}
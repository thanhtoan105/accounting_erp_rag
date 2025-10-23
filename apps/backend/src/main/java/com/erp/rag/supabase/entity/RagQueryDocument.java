package com.erp.rag.supabase.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing the junction table between RAG queries and retrieved
 * vector documents.
 * <p>
 * Story 1.5 – AC4, AC7, AC10: Junction table persisting rank, relevance_score,
 * excerpt, and tokens_used for each document retrieved in a query. Enables
 * citation tracking and recall analysis for compliance audit trail.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Entity
@Table(name = "rag_query_documents", schema = "accounting",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_rag_query_documents_query_doc",
                           columnNames = {"query_id", "document_vector_id"})
       },
       indexes = {
           @Index(name = "idx_rag_query_documents_query",
                  columnList = "query_id"),
           @Index(name = "idx_rag_query_documents_document",
                  columnList = "document_vector_id")
       })
public class RagQueryDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "query_id", nullable = false)
    private UUID queryId;

    @Column(name = "document_vector_id", nullable = false)
    private UUID documentVectorId;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "relevance_score", nullable = false)
    private Double relevanceScore;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "excerpt", columnDefinition = "text")
    private String excerpt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public RagQueryDocument() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQueryId() {
        return queryId;
    }

    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    public UUID getDocumentVectorId() {
        return documentVectorId;
    }

    public void setDocumentVectorId(UUID documentVectorId) {
        this.documentVectorId = documentVectorId;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RagQueryDocument{" +
                "id=" + id +
                ", queryId=" + queryId +
                ", documentVectorId=" + documentVectorId +
                ", rank=" + rank +
                ", relevanceScore=" + relevanceScore +
                ", tokensUsed=" + tokensUsed +
                ", createdAt=" + createdAt +
                '}';
    }
}

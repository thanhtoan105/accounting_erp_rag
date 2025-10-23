package com.erp.rag.supabase.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a logged RAG query for compliance audit trail.
 * <p>
 * Story 1.5 – AC7: Query logging with immutable audit trail capturing user_id,
 * company_id, query_text, query_embedding (for reuse), language, status,
 * timestamps, and latency metrics per Circular 200 audit requirements.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Entity
@Table(name = "rag_queries", schema = "accounting",
       indexes = {
           @Index(name = "idx_rag_queries_company_user",
                  columnList = "company_id, user_id, created_at"),
           @Index(name = "idx_rag_queries_created_at",
                  columnList = "created_at")
       })
public class RagQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "user_id", nullable = true)
    private UUID userId;

    @Column(name = "query_text", columnDefinition = "text", nullable = false)
    private String queryText;

    @Column(name = "query_embedding", columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String queryEmbedding;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "llm_provider", length = 50)
    private String llmProvider;

    @Column(name = "retrieval_latency_ms")
    private Integer retrievalLatencyMs;

    @Column(name = "generation_latency_ms")
    private Integer generationLatencyMs;

    @Column(name = "total_latency_ms")
    private Integer totalLatencyMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public RagQuery() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getQueryEmbedding() {
        return queryEmbedding;
    }

    public void setQueryEmbedding(String queryEmbedding) {
        this.queryEmbedding = queryEmbedding;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public Integer getRetrievalLatencyMs() {
        return retrievalLatencyMs;
    }

    public void setRetrievalLatencyMs(Integer retrievalLatencyMs) {
        this.retrievalLatencyMs = retrievalLatencyMs;
    }

    public Integer getGenerationLatencyMs() {
        return generationLatencyMs;
    }

    public void setGenerationLatencyMs(Integer generationLatencyMs) {
        this.generationLatencyMs = generationLatencyMs;
    }

    public Integer getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Integer totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "RagQuery{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", userId=" + userId +
                ", queryText='" + queryText + '\'' +
                ", language='" + language + '\'' +
                ", status='" + status + '\'' +
                ", retrievalLatencyMs=" + retrievalLatencyMs +
                ", totalLatencyMs=" + totalLatencyMs +
                ", createdAt=" + createdAt +
                '}';
    }
}
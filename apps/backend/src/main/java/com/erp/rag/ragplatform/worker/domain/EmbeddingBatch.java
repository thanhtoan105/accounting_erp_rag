package com.erp.rag.ragplatform.worker.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import com.erp.rag.supabase.vector.JsonNodeConverter;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing an embedding generation batch run.
 * <p>
 * Story 1.4 – AC9: Tracks pipeline runs with status state machine
 * (queued→running→complete/failed),
 * document counts, error tracking, and metadata for observability.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Entity
@Table(name = "embedding_batches", schema = "accounting")
public class EmbeddingBatch {

    public enum BatchType {
        FULL, INCREMENTAL, MANUAL
    }

    public enum Status {
        QUEUED, RUNNING, FAILED, COMPLETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "batch_type", nullable = false, length = 20)
    private BatchType batchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "total_documents")
    private Integer totalDocuments;

    @Column(name = "processed_documents", nullable = false)
    private Integer processedDocuments = 0;

    @Column(name = "failed_documents", nullable = false)
    private Integer failedDocuments = 0;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "triggered_by", length = 255)
    private String triggeredBy;

    @Column(name = "batch_hash", length = 64)
    private String batchHash;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public EmbeddingBatch() {
        // Default constructor for JPA
    }

    public EmbeddingBatch(UUID companyId, BatchType batchType, String triggeredBy) {
        this.companyId = companyId;
        this.batchType = batchType;
        this.status = Status.QUEUED;
        this.triggeredBy = triggeredBy;
        this.processedDocuments = 0;
        this.failedDocuments = 0;
    }

    // State machine transitions

    public void start(int totalDocuments) {
        if (this.status != Status.QUEUED) {
            throw new IllegalStateException("Can only start a queued batch");
        }
        this.status = Status.RUNNING;
        this.totalDocuments = totalDocuments;
        this.startedAt = OffsetDateTime.now();
    }

    public void incrementProcessed() {
        this.processedDocuments++;
    }

    public void incrementFailed() {
        this.failedDocuments++;
    }

    public void complete() {
        if (this.status != Status.RUNNING) {
            throw new IllegalStateException("Can only complete a running batch");
        }
        this.status = Status.COMPLETE;
        this.completedAt = OffsetDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public BatchType getBatchType() {
        return batchType;
    }

    public void setBatchType(BatchType batchType) {
        this.batchType = batchType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(Integer totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public Integer getProcessedDocuments() {
        return processedDocuments;
    }

    public void setProcessedDocuments(Integer processedDocuments) {
        this.processedDocuments = processedDocuments;
    }

    public Integer getFailedDocuments() {
        return failedDocuments;
    }

    public void setFailedDocuments(Integer failedDocuments) {
        this.failedDocuments = failedDocuments;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getBatchHash() {
        return batchHash;
    }

    public void setBatchHash(String batchHash) {
        this.batchHash = batchHash;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

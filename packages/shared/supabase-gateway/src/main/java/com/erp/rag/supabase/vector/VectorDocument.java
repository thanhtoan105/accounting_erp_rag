package com.erp.rag.supabase.vector;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import com.erp.rag.supabase.vector.JsonNodeConverter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a vector document stored in the vector database.
 * <p>
 * Story 1.3 – AC2: Vector storage with multi-tenant isolation, metadata
 * filtering, and soft deletes.
 * Embeddings are stored as VECTOR(1536) for OpenAI text-embedding-3-small
 * compatibility.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Entity
@Table(name = "vector_documents", schema = "accounting")
public class VectorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "source_table", nullable = false)
    private String sourceTable;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "content_text", nullable = false, columnDefinition = "text")
    private String contentText;

    @Column(name = "content_tsv", columnDefinition = "tsvector")
    private String contentTsv; // TSVECTOR stored as text for JDBC compatibility

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding; // VECTOR stored as text representation: "[0.1,0.2,...]"

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public VectorDocument() {
        // Default constructor required by Spring Data JDBC
    }

    public VectorDocument(Long companyId, String sourceTable, Long sourceId,
            String contentType, String contentText, String embedding, JsonNode metadata) {
        this.companyId = Objects.requireNonNull(companyId, "companyId must not be null");
        this.sourceTable = Objects.requireNonNull(sourceTable, "sourceTable must not be null");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId must not be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        this.contentText = Objects.requireNonNull(contentText, "contentText must not be null");
        this.embedding = Objects.requireNonNull(embedding, "embedding must not be null");
        this.metadata = metadata != null ? metadata : null;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }



    public String getContentTsv() {
        return contentTsv;
    }

    public void setContentTsv(String contentTsv) {
        this.contentTsv = contentTsv;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Checks if this document is soft-deleted.
     *
     * @return true if deleted_at is not null
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft-deletes this document by setting deleted_at to current time.
     */
    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VectorDocument that = (VectorDocument) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VectorDocument{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", sourceTable='" + sourceTable + '\'' +
                ", sourceId=" + sourceId +
                ", contentType='" + contentType + '\'' +
                ", hasEmbedding=" + (embedding != null) +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}

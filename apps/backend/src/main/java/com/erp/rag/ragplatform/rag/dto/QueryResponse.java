package com.erp.rag.ragplatform.rag.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for RAG query API response.
 * <p>
 * Story 1.5 – AC10: Response payload with queryId, retrievedDocuments,
 * groundedContext, and latency metrics.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class QueryResponse {

    private UUID queryId;
    private List<RetrievedDocumentDTO> retrievedDocuments;
    private String groundedContext;
    private LatencyMetrics latencyMs;

    public QueryResponse() {
    }

    public QueryResponse(UUID queryId, List<RetrievedDocumentDTO> retrievedDocuments,
            String groundedContext, LatencyMetrics latencyMs) {
        this.queryId = queryId;
        this.retrievedDocuments = retrievedDocuments;
        this.groundedContext = groundedContext;
        this.latencyMs = latencyMs;
    }

    // Getters and Setters
    public UUID getQueryId() {
        return queryId;
    }

    public void setQueryId(UUID queryId) {
        this.queryId = queryId;
    }

    public List<RetrievedDocumentDTO> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public void setRetrievedDocuments(List<RetrievedDocumentDTO> retrievedDocuments) {
        this.retrievedDocuments = retrievedDocuments;
    }

    public String getGroundedContext() {
        return groundedContext;
    }

    public void setGroundedContext(String groundedContext) {
        this.groundedContext = groundedContext;
    }

    public LatencyMetrics getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(LatencyMetrics latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Override
    public String toString() {
        return "QueryResponse{" +
                "queryId=" + queryId +
                ", retrievedDocuments=" + retrievedDocuments.size() + " documents" +
                ", latencyMs=" + latencyMs +
                '}';
    }
}

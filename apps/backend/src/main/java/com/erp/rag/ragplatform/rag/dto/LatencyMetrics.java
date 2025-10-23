package com.erp.rag.ragplatform.rag.dto;

/**
 * DTO for latency metrics in RAG query response.
 * <p>
 * Story 1.5 – AC10: Latency breakdown (embedding, search, contextPrep, total).
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
public class LatencyMetrics {

    private Integer embedding;
    private Integer search;
    private Integer contextPrep;
    private Integer total;

    public LatencyMetrics() {
    }

    public LatencyMetrics(Integer embedding, Integer search, Integer contextPrep, Integer total) {
        this.embedding = embedding;
        this.search = search;
        this.contextPrep = contextPrep;
        this.total = total;
    }

    // Getters and Setters
    public Integer getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Integer embedding) {
        this.embedding = embedding;
    }

    public Integer getSearch() {
        return search;
    }

    public void setSearch(Integer search) {
        this.search = search;
    }

    public Integer getContextPrep() {
        return contextPrep;
    }

    public void setContextPrep(Integer contextPrep) {
        this.contextPrep = contextPrep;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "LatencyMetrics{" +
                "embedding=" + embedding +
                ", search=" + search +
                ", contextPrep=" + contextPrep +
                ", total=" + total +
                '}';
    }
}

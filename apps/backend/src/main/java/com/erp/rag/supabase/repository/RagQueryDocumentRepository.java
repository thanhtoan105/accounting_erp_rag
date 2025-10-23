package com.erp.rag.supabase.repository;

import com.erp.rag.supabase.entity.RagQueryDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link RagQueryDocument} junction table operations.
 * <p>
 * Story 1.5 – AC7: Persist retrieved documents for each query with rank,
 * relevance score, excerpt for citation tracking and recall analysis.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Repository
public interface RagQueryDocumentRepository extends JpaRepository<RagQueryDocument, UUID> {

    /**
     * Find all documents retrieved for a specific query, ordered by rank.
     *
     * @param queryId the query UUID
     * @return list of query documents ordered by rank (top-ranked first)
     */
    @Query(value = "SELECT * FROM accounting.rag_query_documents " +
            "WHERE query_id = :queryId " +
            "ORDER BY rank ASC", nativeQuery = true)
    List<RagQueryDocument> findByQueryIdOrderByRank(@Param("queryId") UUID queryId);

    /**
     * Find all queries that retrieved a specific document.
     *
     * @param documentVectorId the document vector UUID
     * @return list of query documents
     */
    @Query(value = "SELECT * FROM accounting.rag_query_documents " +
            "WHERE document_vector_id = :documentVectorId " +
            "ORDER BY created_at DESC", nativeQuery = true)
    List<RagQueryDocument> findByDocumentVectorId(@Param("documentVectorId") UUID documentVectorId);

    /**
     * Count documents retrieved for a query.
     *
     * @param queryId the query UUID
     * @return count of documents
     */
    @Query(value = "SELECT COUNT(*) FROM accounting.rag_query_documents " +
            "WHERE query_id = :queryId", nativeQuery = true)
    long countByQueryId(@Param("queryId") UUID queryId);
}

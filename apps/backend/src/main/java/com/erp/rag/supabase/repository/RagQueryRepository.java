package com.erp.rag.supabase.repository;

import com.erp.rag.supabase.entity.RagQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link RagQuery} entities for compliance audit trail.
 * <p>
 * Story 1.5 – AC7: Query logging for compliance with Vietnam Circular 200
 * requirements using rag_queries table with immutable timestamps.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Repository
public interface RagQueryRepository extends JpaRepository<RagQuery, Long> {

    /**
     * Find queries by company ID ordered by created_at (most recent first).
     *
     * @param companyId company Long ID
     * @return list of queries for the company
     */
    List<RagQuery> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    /**
     * Find queries by company and user, ordered by created_at.
     *
     * @param companyId company Long ID
     * @param userId    user UUID
     * @return list of queries for the user
     */
    List<RagQuery> findByCompanyIdAndUserIdOrderByCreatedAtDesc(Long companyId, UUID userId);

    /**
     * Find queries by company within a date range.
     *
     * @param companyId company Long ID
     * @param startDate start of date range
     * @param endDate   end of date range
     * @return list of queries in date range
     */
    List<RagQuery> findByCompanyIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long companyId,
            java.time.OffsetDateTime startDate,
            java.time.OffsetDateTime endDate);

    /**
     * Count queries by company for analytics.
     *
     * @param companyId company Long ID
     * @return count of queries for the company
     */
    long countByCompanyId(Long companyId);

    /**
     * Find queries by status for monitoring.
     *
     * @param status query status (pending, streaming, complete, error)
     * @return list of queries with given status
     */
    List<RagQuery> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Update query_embedding using native SQL with proper vector casting.
     *
     * @param queryId query ID
     * @param queryEmbedding embedding string in format "[0.1,0.2,...]"
     * @return number of rows updated
     */
    @Modifying
    @Query(value = "UPDATE accounting.rag_queries SET query_embedding = CAST(:queryEmbedding AS vector) WHERE id = :queryId", nativeQuery = true)
    int updateQueryEmbedding(@Param("queryId") Long queryId, @Param("queryEmbedding") String queryEmbedding);
}
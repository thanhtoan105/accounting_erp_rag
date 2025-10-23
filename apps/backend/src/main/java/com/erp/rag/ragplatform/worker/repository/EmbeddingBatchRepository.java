package com.erp.rag.ragplatform.worker.repository;

import com.erp.rag.ragplatform.worker.domain.EmbeddingBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link EmbeddingBatch} entity.
 * <p>
 * Story 1.4 – AC9: Data access for embedding batch tracking and status queries.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Repository
public interface EmbeddingBatchRepository extends JpaRepository<EmbeddingBatch, UUID> {

    /**
     * Find all batches for a specific company, ordered by creation date.
     *
     * @param companyId company UUID
     * @return list of embedding batches
     */
    @Query("SELECT b FROM EmbeddingBatch b WHERE b.companyId = :companyId ORDER BY b.createdAt DESC")
    List<EmbeddingBatch> findByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Find batches by status for a specific company.
     *
     * @param companyId company UUID
     * @param status    batch status
     * @return list of embedding batches
     */
    @Query("SELECT b FROM EmbeddingBatch b WHERE b.companyId = :companyId AND b.status = :status ORDER BY b.createdAt DESC")
    List<EmbeddingBatch> findByCompanyIdAndStatus(@Param("companyId") UUID companyId,
            @Param("status") EmbeddingBatch.Status status);

    /**
     * Find batch by hash to prevent duplicate processing.
     *
     * @param batchHash hash of batch parameters
     * @return optional embedding batch
     */
    @Query("SELECT b FROM EmbeddingBatch b WHERE b.batchHash = :batchHash")
    Optional<EmbeddingBatch> findByBatchHash(@Param("batchHash") String batchHash);

    /**
     * Find the most recent batch for a company.
     *
     * @param companyId company UUID
     * @return optional embedding batch
     */
    @Query("SELECT b FROM EmbeddingBatch b WHERE b.companyId = :companyId ORDER BY b.createdAt DESC LIMIT 1")
    Optional<EmbeddingBatch> findMostRecentByCompanyId(@Param("companyId") UUID companyId);
}

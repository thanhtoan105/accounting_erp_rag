package com.erp.rag.supabase.vector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link VectorDocument} with tenant-scoped queries and vector
 * similarity search.
 * <p>
 * Story 1.3 – AC2: Vector access with multi-tenant isolation via company_id
 * filtering.
 * All queries automatically filter by company_id and exclude soft-deleted
 * records (deleted_at IS NULL).
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Repository
public interface VectorDocumentRepository extends JpaRepository<VectorDocument, UUID> {

    /**
     * Find all non-deleted vector documents for a specific company.
     *
     * @param companyId the company UUID
     * @return list of active vector documents
     */
    @Query(value = "SELECT * FROM accounting.vector_documents " +
            "WHERE company_id = :companyId AND deleted_at IS NULL " +
            "ORDER BY created_at DESC", nativeQuery = true)
    List<VectorDocument> findByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Find a non-deleted vector document by ID and company.
     *
     * @param id        the document UUID
     * @param companyId the company UUID
     * @return optional vector document
     */
    @Query(value = "SELECT * FROM accounting.vector_documents " +
            "WHERE id = :id AND company_id = :companyId AND deleted_at IS NULL", nativeQuery = true)
    Optional<VectorDocument> findByIdAndCompanyId(@Param("id") UUID id, @Param("companyId") UUID companyId);

    /**
     * Find vector documents by source table and source ID for a specific company.
     *
     * @param companyId   the company UUID
     * @param sourceTable the source table name
     * @param sourceId    the source record UUID
     * @return list of vector documents
     */
    @Query(value = "SELECT * FROM accounting.vector_documents " +
            "WHERE company_id = :companyId " +
            "AND source_table = :sourceTable " +
            "AND source_id = :sourceId " +
            "AND deleted_at IS NULL", nativeQuery = true)
    List<VectorDocument> findBySource(@Param("companyId") UUID companyId,
            @Param("sourceTable") String sourceTable,
            @Param("sourceId") UUID sourceId);

    /**
     * Find vector documents by fiscal period for a specific company.
     *
     * @param companyId    the company UUID
     * @param fiscalPeriod the fiscal period (format: YYYY-MM)
     * @return list of vector documents
     */
    @Query(value = "SELECT * FROM accounting.vector_documents " +
            "WHERE company_id = :companyId " +
            "AND fiscal_period = :fiscalPeriod " +
            "AND deleted_at IS NULL " +
            "ORDER BY created_at DESC", nativeQuery = true)
    List<VectorDocument> findByFiscalPeriod(@Param("companyId") UUID companyId,
            @Param("fiscalPeriod") String fiscalPeriod);

    /**
     * Perform vector similarity search using cosine distance.
     * Returns top K nearest neighbors for a given embedding within the company
     * scope.
     *
     * @param companyId      the company UUID
     * @param queryEmbedding the query embedding as text (format: "[0.1,0.2,...]")
     * @param limit          maximum number of results
     * @return list of vector documents ordered by similarity (most similar first)
     */
    @Query(value = "SELECT *, " +
            "embedding <-> CAST(:queryEmbedding AS vector) AS distance " +
            "FROM accounting.vector_documents " +
            "WHERE company_id = :companyId AND deleted_at IS NULL " +
            "ORDER BY distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<VectorDocument> findSimilarVectors(@Param("companyId") UUID companyId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit);

    /**
     * Perform vector similarity search with metadata filtering.
     *
     * @param companyId      the company UUID
     * @param queryEmbedding the query embedding as text
     * @param metadataFilter JSONB path expression for filtering (e.g., '$.module ==
     *                       "ar"')
     * @param limit          maximum number of results
     * @return list of vector documents ordered by similarity
     */
    @Query(value = "SELECT *, " +
            "embedding <-> CAST(:queryEmbedding AS vector) AS distance " +
            "FROM accounting.vector_documents " +
            "WHERE company_id = :companyId " +
            "AND deleted_at IS NULL " +
            "AND jsonb_path_exists(metadata, CAST(:metadataFilter AS jsonpath)) " +
            "ORDER BY distance ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<VectorDocument> findSimilarVectorsWithMetadata(@Param("companyId") UUID companyId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("metadataFilter") String metadataFilter,
            @Param("limit") int limit);

    /**
     * Count non-deleted vector documents for a company.
     *
     * @param companyId the company UUID
     * @return count of active documents
     */
    @Query(value = "SELECT COUNT(*) FROM accounting.vector_documents " +
            "WHERE company_id = :companyId AND deleted_at IS NULL", nativeQuery = true)
    long countByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Soft-delete a vector document.
     *
     * @param id        the document UUID
     * @param companyId the company UUID (for tenant isolation)
     * @return number of rows updated (should be 1 if successful)
     */
    @Modifying
    @Query(value = "UPDATE accounting.vector_documents " +
            "SET deleted_at = now(), updated_at = now() " +
            "WHERE id = :id AND company_id = :companyId AND deleted_at IS NULL", nativeQuery = true)
    int softDelete(@Param("id") UUID id, @Param("companyId") UUID companyId);

    /**
     * Delete all vector documents for a specific source (hard delete for cleanup).
     *
     * @param companyId   the company UUID
     * @param sourceTable the source table name
     * @param sourceId    the source record UUID
     */
    @Modifying
    @Query(value = "DELETE FROM accounting.vector_documents " +
            "WHERE company_id = :companyId " +
            "AND source_table = :sourceTable " +
            "AND source_id = :sourceId", nativeQuery = true)
    void deleteBySource(@Param("companyId") UUID companyId,
            @Param("sourceTable") String sourceTable,
            @Param("sourceId") UUID sourceId);
}

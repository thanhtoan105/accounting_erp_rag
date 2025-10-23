package com.erp.rag.ragplatform.rag.controller;

import com.erp.rag.ragplatform.rag.dto.QueryRequest;
import com.erp.rag.ragplatform.rag.dto.QueryResponse;
import com.erp.rag.ragplatform.rag.service.RagQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for RAG query API.
 * <p>
 * Story 1.5 – AC1: REST endpoint /api/v1/rag/query accepting POST requests
 * with natural language query text, language, company_id, and optional filters.
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/rag")
public class RagQueryController {

    private static final Logger logger = LoggerFactory.getLogger(RagQueryController.class);

    private final RagQueryService ragQueryService;

    public RagQueryController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    /**
     * Process a RAG query request.
     * <p>
     * Story 1.5 – AC1: Accept natural language queries, validate RBAC via JWT
     * (future), execute query pipeline, return structured response.
     * </p>
     *
     * @param request query request with companyId, query text, language, filters
     * @return query response with queryId, retrieved documents, grounded context
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> processQuery(@Valid @RequestBody QueryRequest request) {
        logger.info("Received RAG query request: companyId={}, language={}, query={}",
                request.getCompanyId(), request.getLanguage(), request.getQuery());

        try {
            // TODO: Extract userId from JWT token (AC1 - RBAC validation)
            UUID userId = null; // Placeholder for JWT extraction

            QueryResponse response = ragQueryService.processQuery(request, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to process query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

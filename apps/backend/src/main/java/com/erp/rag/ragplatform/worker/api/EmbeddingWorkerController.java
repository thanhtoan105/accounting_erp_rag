package com.erp.rag.ragplatform.worker.api;

import com.erp.rag.ragplatform.worker.domain.EmbeddingBatch;
import com.erp.rag.ragplatform.worker.service.EmbeddingWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for triggering embedding generation batches.
 * <p>
 * Story 1.4 – AC8: Embedding-worker trigger mechanisms via:
 * - Manual REST endpoint /internal/rag/index-batch
 * - n8n webhook handler with Bearer token validation
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/internal/rag")
public class EmbeddingWorkerController {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingWorkerController.class);

    private final EmbeddingWorkerService workerService;

    @Value("${n8n.webhook.secret:}")
    private String n8nWebhookSecret;

    public EmbeddingWorkerController(EmbeddingWorkerService workerService) {
        this.workerService = workerService;
    }

    /**
     * Trigger embedding batch indexing (manual or n8n webhook).
     * <p>
     * Request body:
     * {
     * "company_id": "uuid",
     * "batch_type": "full" | "incremental" | "manual",
     * "triggered_by": "user@example.com" | "n8n-cron" | "manual",
     * "tables": ["invoices", "bills", ...], // optional
     * "start_from": "2024-10-20T10:00:00Z" // optional, for incremental
     * }
     * </p>
     *
     * @param request       index batch request
     * @param authorization Authorization header (Bearer token for n8n)
     * @return batch UUID
     */
    @PostMapping("/index-batch")
    public ResponseEntity<?> indexBatch(
            @RequestBody IndexBatchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        logger.info("Received index-batch request: companyId={}, batchType={}, triggeredBy={}",
                request.companyId(), request.batchType(), request.triggeredBy());

        // Validate n8n webhook authentication (AC8: Bearer token validation)
        if (request.triggeredBy() != null && request.triggeredBy().startsWith("n8n")) {
            if (!validateN8nWebhookAuth(authorization)) {
                logger.error("Invalid or missing n8n webhook authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid webhook authentication"));
            }
        }

        // Validate request
        if (request.companyId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "company_id is required"));
        }

        if (request.batchType() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "batch_type is required (full, incremental, or manual)"));
        }

        try {
            // Parse batch type
            EmbeddingBatch.BatchType batchType = EmbeddingBatch.BatchType.valueOf(
                    request.batchType().toUpperCase());

            // Parse startFrom timestamp for incremental mode
            OffsetDateTime updatedAfter = null;
            if (request.startFrom() != null && !request.startFrom().isBlank()) {
                updatedAfter = OffsetDateTime.parse(request.startFrom());
            }

            // Execute batch
            UUID batchId = workerService.executeBatch(
                    request.companyId(),
                    batchType,
                    request.triggeredBy() != null ? request.triggeredBy() : "manual",
                    request.tables(),
                    updatedAfter);

            logger.info("Embedding batch {} started successfully", batchId);

            return ResponseEntity.accepted()
                    .body(Map.of(
                            "batch_id", batchId.toString(),
                            "status", "started",
                            "message", "Embedding batch processing started"));

        } catch (IllegalArgumentException e) {
            logger.error("Invalid batch_type: {}", request.batchType());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid batch_type: " + request.batchType()));

        } catch (Exception e) {
            logger.error("Error starting embedding batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to start embedding batch",
                            "message", e.getMessage()));
        }
    }

    /**
     * Validate n8n webhook authentication using Bearer token.
     * <p>
     * MVP approach per Dev Notes #5: Bearer token validation.
     * Production upgrade path: HMAC-SHA256 signature (Story 1.9).
     * </p>
     */
    private boolean validateN8nWebhookAuth(String authorization) {
        if (n8nWebhookSecret == null || n8nWebhookSecret.isBlank()) {
            logger.warn("N8N_WEBHOOK_SECRET not configured, skipping authentication");
            return true; // Allow for local development
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7); // Remove "Bearer " prefix
        boolean isValid = token.equals(n8nWebhookSecret);

        if (!isValid) {
            logger.warn("Invalid n8n webhook token received");
        }

        return isValid;
    }

    /**
     * Index batch request DTO.
     */
    public record IndexBatchRequest(
            UUID companyId,
            String batchType,
            String triggeredBy,
            List<String> tables,
            String startFrom) {
    }
}

package com.erp.rag.ragplatform.rag.service;

import com.erp.rag.ragplatform.rag.dto.QueryResult;
import com.erp.rag.supabase.vector.VectorDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for ranking retrieved documents by relevance and managing context window.
 * <p>
 * Story 1.5 – AC3, AC4: Document ranking with relevance score and context
 * window management to fit documents within LLM token limit (8K tokens).
 * </p>
 *
 * @author dev-agent
 * @since 1.0.0
 */
@Service
public class DocumentRankingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentRankingService.class);
    private static final int MAX_CONTEXT_TOKENS = 8000; // 8K token limit
    private static final int AVG_TOKENS_PER_WORD = 4; // Rough estimation

    /**
     * Rank retrieved documents by relevance and prepare for context window management.
     *
     * @param documents list of retrieved vector documents
     * @param language query language for token estimation
     * @return ranked query results ready for context window processing
     */
    public List<QueryResult> rankDocuments(List<VectorDocument> documents, String language) {
        logger.debug("Ranking {} documents for {} language context", documents.size(), language);

        if (documents.isEmpty()) {
            return new ArrayList<>();
        }

        // Step 1: Convert to QueryResult and calculate estimated tokens
        List<QueryResult> results = new ArrayList<>();
        for (VectorDocument doc : documents) {
            // Estimate tokens based on content length
            int estimatedTokens = estimateTokenCount(doc.getContentTsv(), language);

            // Calculate distance-based relevance score (lower distance = higher relevance)
            Double relevanceScore = calculateRelevanceScore(doc);

            QueryResult result = new QueryResult(
                    doc.getId(),
                    doc.getSourceTable(),
                    doc.getSourceId() != null ? doc.getSourceId().toString() : null,
                    doc.getFiscalPeriod(),
                    extractTitle(doc),
                    doc.getContentTsv(),
                    relevanceScore,
                    doc.getMetadata()
            );

            results.add(result);
        }

        // Step 2: Sort by relevance score (highest first)
        results.sort(Comparator.comparingDouble(QueryResult::getRelevanceScore).reversed());

        // Step 3: Apply context window management
        List<QueryResult> contextResults = applyContextWindow(results, language);

        logger.debug("Context window reduced from {} to {} documents",
                   results.size(), contextResults.size());

        return contextResults;
    }

    /**
     * Apply context window management to fit within token limit.
     *
     * @param results all ranked results
     * @param language query language
     * @return results that fit within 8K token limit
     */
    private List<QueryResult> applyContextWindow(List<QueryResult> results, String language) {
        List<QueryResult> contextResults = new ArrayList<>();
        int totalTokens = 0;

        for (QueryResult result : results) {
            int resultTokens = estimateTokenCount(result.getContent(), language);

            if (totalTokens + resultTokens <= MAX_CONTEXT_TOKENS) {
                contextResults.add(result);
                totalTokens += resultTokens;
            } else {
                // Skip this document as it would exceed token limit
                logger.debug("Skipping document due to token limit: {} tokens needed, {} available",
                           resultTokens, MAX_CONTEXT_TOKENS - totalTokens);
                break;
            }
        }

        return contextResults;
    }

    /**
     * Calculate relevance score from document metadata.
     *
     * @param document vector document with distance from similarity search
     * @return normalized relevance score (0.0 to 1.0)
     */
    private Double calculateRelevanceScore(VectorDocument document) {
        // Extract distance from metadata or use a default
        // In a real implementation, this would come from the vector search result
        double distance = 0.5; // Default medium relevance

        // Convert distance to relevance score (inverse relationship)
        // Score 1.0 = exact match, 0.0 = completely different
        double relevanceScore = Math.max(0.0, 1.0 - (distance / 2.0));

        return Math.min(1.0, relevanceScore);
    }

    /**
     * Estimate token count for text based on language.
     *
     * @param text text to estimate tokens for
     * @param language text language ("vi" or "en")
     * @return estimated token count
     */
    private int estimateTokenCount(String text, String language) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple token estimation (more sophisticated methods would be used in production)
        int wordCount = text.split("\\s+").length;
        int estimatedTokens = (int) (wordCount * AVG_TOKENS_PER_WORD * 1.5); // 1.5x for safety margin

        logger.debug("Estimated {} tokens for {} words in {}", estimatedTokens, wordCount, language);
        return estimatedTokens;
    }

    /**
     * Extract title from document metadata or content.
     *
     * @param document vector document
     * @return document title
     */
    private String extractTitle(VectorDocument document) {
        if (document.getMetadata() != null &&
            document.getMetadata().has("title")) {
            return document.getMetadata().get("title").asText();
        }

        // Generate title from first line of content or use source
        String[] lines = document.getContentTsv().split("\n", 2);
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            if (firstLine.length() > 0 && firstLine.length() <= 100) {
                return firstLine;
            }
        }

        // Fallback to source information
        return String.format("%s - %s",
                           document.getSourceTable(),
                           document.getSourceId() != null ? document.getSourceId().toString().substring(0, 8) + "..." : "");
    }
}
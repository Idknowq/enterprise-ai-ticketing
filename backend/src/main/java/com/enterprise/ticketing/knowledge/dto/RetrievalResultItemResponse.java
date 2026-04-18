package com.enterprise.ticketing.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standardized retrieval result item")
public record RetrievalResultItemResponse(
        Long docId,
        String title,
        String chunkId,
        String contentSnippet,
        double score,
        DocumentMetadataResponse metadata,
        String whyMatched,
        Long citationId
) {
}

package com.enterprise.ticketing.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Standardized retrieval result item")
public record RetrievalResultItemResponse(
        Long docId,
        String title,
        String chunkId,
        String contentSnippet,
        double score,
        Double retrievalScore,
        Double rerankScore,
        String sourceRef,
        DocumentMetadataResponse metadata,
        Map<String, Object> metadataMap,
        String whyMatched,
        Long citationId
) {
}

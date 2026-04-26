package com.enterprise.ticketing.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Structured citation returned by retrieval")
public record AiCitation(
        String sourceType,
        Long documentId,
        String chunkId,
        String title,
        String snippet,
        Double score,
        Double retrievalScore,
        Double rerankScore,
        String sourceRef,
        Map<String, Object> metadata
) {
}

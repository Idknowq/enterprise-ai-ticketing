package com.enterprise.ticketing.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Diagnostics emitted by the retrieval node")
public record AiRetrievalDiagnostics(
        String retrievalMode,
        int candidateCount,
        int returnedCount,
        Map<String, Object> filterSummary,
        String message
) {
}

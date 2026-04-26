package com.enterprise.ticketing.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Retrieval diagnostics for audit and debugging")
public record RetrievalDiagnosticsResponse(
        String retrievalMode,
        int candidateCount,
        int returnedCount,
        Map<String, Object> filterSummary
) {
}

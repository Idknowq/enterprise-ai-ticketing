package com.enterprise.ticketing.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Retrieval response tailored for AI and ticket evidence consumption")
public record RetrievalSearchResponse(
        String query,
        Long ticketId,
        List<RetrievalResultItemResponse> results
) {
}

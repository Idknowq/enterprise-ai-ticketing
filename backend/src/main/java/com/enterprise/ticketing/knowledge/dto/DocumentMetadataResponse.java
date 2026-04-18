package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Knowledge document metadata")
public record DocumentMetadataResponse(
        Long docId,
        String title,
        String category,
        String department,
        KnowledgeAccessLevel accessLevel,
        String version,
        Instant updatedAt
) {
}

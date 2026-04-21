package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.DocumentIndexStatus;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Knowledge document response")
public record DocumentResponse(
        Long id,
        String title,
        String sourceFilename,
        KnowledgeDocumentType documentType,
        DocumentIndexStatus indexStatus,
        int chunkCount,
        String embeddingModel,
        DocumentMetadataResponse metadata,
        @Schema(nullable = true)
        Instant lastIndexedAt,
        Instant createdAt
) {
}

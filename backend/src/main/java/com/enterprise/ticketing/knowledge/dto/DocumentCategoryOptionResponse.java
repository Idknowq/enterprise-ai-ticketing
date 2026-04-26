package com.enterprise.ticketing.knowledge.dto;

import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Knowledge document category option")
public record DocumentCategoryOptionResponse(
        String code,
        String displayName,
        String description
) {
    public static DocumentCategoryOptionResponse from(KnowledgeDocumentCategory category) {
        return new DocumentCategoryOptionResponse(category.code(), category.displayName(), category.description());
    }
}

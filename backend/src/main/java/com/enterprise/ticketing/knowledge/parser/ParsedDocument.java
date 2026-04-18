package com.enterprise.ticketing.knowledge.parser;

import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;

public record ParsedDocument(
        KnowledgeDocumentType documentType,
        String content,
        String contentType
) {
}

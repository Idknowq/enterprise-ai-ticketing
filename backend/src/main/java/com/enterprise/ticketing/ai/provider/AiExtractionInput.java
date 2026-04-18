package com.enterprise.ticketing.ai.provider;

public record AiExtractionInput(
        String title,
        String description,
        String category
) {
}

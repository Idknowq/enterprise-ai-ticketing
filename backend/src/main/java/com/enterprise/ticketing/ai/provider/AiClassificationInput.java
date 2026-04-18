package com.enterprise.ticketing.ai.provider;

public record AiClassificationInput(
        String title,
        String description,
        String existingCategory
) {
}

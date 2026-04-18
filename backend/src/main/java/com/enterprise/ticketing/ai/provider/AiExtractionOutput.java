package com.enterprise.ticketing.ai.provider;

import java.util.Map;

public record AiExtractionOutput(
        Map<String, String> extractedFields
) {
}

package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ai.dto.AiCitation;
import java.util.List;
import java.util.Map;

public record AiResolutionInput(
        String title,
        String description,
        AiClassificationOutput classification,
        Map<String, String> extractedFields,
        List<AiCitation> citations
) {
}

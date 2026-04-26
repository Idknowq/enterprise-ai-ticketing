package com.enterprise.ticketing.ai.provider;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StructuredLlmOutputValidator {

    public void validateClassification(AiClassificationOutput output) {
        if (output == null) {
            throw new IllegalStateException("classification output is missing");
        }
        if (output.priority() == null) {
            throw new IllegalStateException("classification priority is required");
        }
        if (Double.isNaN(output.confidence()) || output.confidence() < 0d || output.confidence() > 1d) {
            throw new IllegalStateException("classification confidence must be between 0 and 1");
        }
    }

    public void validateExtraction(AiExtractionOutput output) {
        if (output == null) {
            throw new IllegalStateException("extraction output is missing");
        }
        Map<String, String> extractedFields = output.extractedFields();
        if (extractedFields == null) {
            throw new IllegalStateException("extractedFields is required");
        }
        boolean hasInvalidEntry = extractedFields.entrySet().stream()
                .anyMatch(entry -> !StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue()));
        if (hasInvalidEntry) {
            throw new IllegalStateException("extractedFields must be a flat non-empty string map");
        }
    }

    public void validateResolution(AiResolutionOutput output) {
        if (output == null) {
            throw new IllegalStateException("resolution output is missing");
        }
        if (!StringUtils.hasText(output.draftReply())) {
            throw new IllegalStateException("draftReply is required");
        }
        List<String> suggestedActions = output.suggestedActions();
        if (suggestedActions == null || suggestedActions.isEmpty()) {
            throw new IllegalStateException("suggestedActions must not be empty");
        }
        boolean hasBlankAction = suggestedActions.stream().anyMatch(action -> !StringUtils.hasText(action));
        if (hasBlankAction) {
            throw new IllegalStateException("suggestedActions must not contain blank items");
        }
    }
}

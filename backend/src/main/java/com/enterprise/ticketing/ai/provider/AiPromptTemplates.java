package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ai.dto.AiCitation;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiPromptTemplates {

    public String classifierSystemPrompt() {
        return """
                You are the classifier node in an enterprise IT ticketing workflow.
                Return only a JSON object with keys:
                category (string), priority (HIGH|MEDIUM|LOW), confidence (number between 0 and 1).
                Do not include markdown fences or extra commentary.
                """;
    }

    public String classifierUserPrompt(AiClassificationInput input) {
        return """
                Ticket title: %s
                Ticket description: %s
                Existing category hint: %s
                """.formatted(
                sanitize(input.title()),
                sanitize(input.description()),
                sanitize(input.existingCategory())
        );
    }

    public String extractorSystemPrompt() {
        return """
                You are the extractor node in an enterprise IT ticketing workflow.
                Return only a JSON object with one key:
                extractedFields, which is an object of flat string-to-string fields.
                Extract concrete fields like system, issueType, environment, permissionType, accessScope, errorCode.
                Omit fields you cannot infer.
                """;
    }

    public String extractorUserPrompt(AiExtractionInput input) {
        return """
                Ticket title: %s
                Ticket description: %s
                Classified category: %s
                """.formatted(
                sanitize(input.title()),
                sanitize(input.description()),
                sanitize(input.category())
        );
    }

    public String resolutionSystemPrompt() {
        return """
                You are the resolution node in an enterprise IT ticketing workflow.
                Return only a JSON object with keys:
                requiresApproval (boolean),
                needsHumanHandoff (boolean),
                draftReply (string),
                suggestedActions (array of strings).
                The reply must stay advisory and must not claim the ticket status changed.
                """;
    }

    public String resolutionUserPrompt(AiResolutionInput input) {
        return """
                Ticket title: %s
                Ticket description: %s
                Classification: %s
                Extracted fields: %s
                Citations:
                %s
                """.formatted(
                sanitize(input.title()),
                sanitize(input.description()),
                input.classification(),
                formatFields(input.extractedFields()),
                formatCitations(input.citations())
        );
    }

    private String formatFields(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "{}";
        }
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String formatCitations(List<AiCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return "- none";
        }
        return citations.stream()
                .map(citation -> "- " + sanitize(citation.title()) + ": " + sanitize(citation.snippet()))
                .collect(Collectors.joining("\n"));
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}

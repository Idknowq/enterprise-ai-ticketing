package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.config.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.ai.provider.local", name = "enabled", havingValue = "true")
public class LocalStructuredLlmProvider implements StructuredLlmProvider {

    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper;
    private final AiPromptTemplates promptTemplates;
    private final RestClient restClient;

    public LocalStructuredLlmProvider(
            ApplicationProperties applicationProperties,
            ObjectMapper objectMapper,
            AiPromptTemplates promptTemplates
    ) {
        this.applicationProperties = applicationProperties;
        this.objectMapper = objectMapper;
        this.promptTemplates = promptTemplates;

        String baseUrl = applicationProperties.getAi().getProvider().getLocal().getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("app.ai.provider.local.base-url is required when local provider is enabled");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = applicationProperties.getAi().getProvider().getLocal().getTimeout();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerType() {
        return "local-llm";
    }

    @Override
    public String defaultModelName() {
        return applicationProperties.getAi().getProvider().getLocal().getModel();
    }

    @Override
    public StructuredLlmResponse<AiClassificationOutput> classify(AiClassificationInput input) {
        return invoke(promptTemplates.classifierSystemPrompt(), promptTemplates.classifierUserPrompt(input), AiClassificationOutput.class);
    }

    @Override
    public StructuredLlmResponse<AiExtractionOutput> extract(AiExtractionInput input) {
        return invoke(promptTemplates.extractorSystemPrompt(), promptTemplates.extractorUserPrompt(input), AiExtractionOutput.class);
    }

    @Override
    public StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input) {
        return invoke(promptTemplates.resolutionSystemPrompt(), promptTemplates.resolutionUserPrompt(input), AiResolutionOutput.class);
    }

    private <T> StructuredLlmResponse<T> invoke(String systemPrompt, String userPrompt, Class<T> outputType) {
        Map<String, Object> request = Map.of(
                "model", defaultModelName(),
                "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        String responseBody = restClient.post()
                .uri(applicationProperties.getAi().getProvider().getLocal().getChatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    String apiKey = applicationProperties.getAi().getProvider().getLocal().getApiKey();
                    if (StringUtils.hasText(apiKey)) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .body(request)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = extractContentText(contentNode);
            T output = objectMapper.readValue(stripJsonFence(content), outputType);
            JsonNode usageNode = root.path("usage");
            return new StructuredLlmResponse<>(
                    output,
                    providerType(),
                    defaultModelName(),
                    usageNode.path("prompt_tokens").asInt(0),
                    usageNode.path("completion_tokens").asInt(0),
                    false,
                    null
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse local structured LLM response", exception);
        }
    }

    private String extractContentText(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray() && !contentNode.isEmpty()) {
            return contentNode.path(0).path("text").asText();
        }
        return new String(contentNode.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String stripJsonFence(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}

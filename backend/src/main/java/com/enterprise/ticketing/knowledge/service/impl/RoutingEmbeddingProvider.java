package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingEmbeddingProvider implements EmbeddingProvider {

    private final ApplicationProperties applicationProperties;
    private final LocalOllamaEmbeddingProvider localProvider;
    private final OpenAiEmbeddingProvider commercialProvider;

    public RoutingEmbeddingProvider(
            ApplicationProperties applicationProperties,
            LocalOllamaEmbeddingProvider localProvider,
            OpenAiEmbeddingProvider commercialProvider
    ) {
        this.applicationProperties = applicationProperties;
        this.localProvider = localProvider;
        this.commercialProvider = commercialProvider;
        validateSelectedProvider();
    }

    @Override
    public String modelName() {
        return activeProvider().modelName();
    }

    @Override
    public int dimension() {
        return activeProvider().dimension();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return activeProvider().embed(texts);
    }

    private EmbeddingProvider activeProvider() {
        return switch (routingMode()) {
            case "local" -> localProvider;
            case "commercial" -> commercialProvider;
            default -> throw new IllegalStateException(
                    "Unsupported app.knowledge.embedding.routing.mode: " + applicationProperties.getKnowledge().getEmbedding().getRouting().getMode()
            );
        };
    }

    private void validateSelectedProvider() {
        switch (routingMode()) {
            case "local" -> localProvider.validateConfiguration();
            case "commercial" -> commercialProvider.validateConfiguration();
            default -> throw new IllegalStateException(
                    "Unsupported app.knowledge.embedding.routing.mode: " + applicationProperties.getKnowledge().getEmbedding().getRouting().getMode()
            );
        }
    }

    private String routingMode() {
        return applicationProperties.getKnowledge().getEmbedding().getRouting().getMode().trim().toLowerCase(Locale.ROOT);
    }
}

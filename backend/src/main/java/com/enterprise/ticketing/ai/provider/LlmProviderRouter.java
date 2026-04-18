package com.enterprise.ticketing.ai.provider;

import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LlmProviderRouter implements StructuredLlmProvider {

    private final RuleBasedStructuredLlmProvider fallbackProvider;
    private final StructuredLlmProvider primaryProvider;

    public LlmProviderRouter(
            RuleBasedStructuredLlmProvider fallbackProvider,
            ObjectProvider<DeepSeekStructuredLlmProvider> deepSeekProvider
    ) {
        this.fallbackProvider = fallbackProvider;
        DeepSeekStructuredLlmProvider deepSeekStructuredLlmProvider = deepSeekProvider.getIfAvailable();
        this.primaryProvider = deepSeekStructuredLlmProvider == null
                ? fallbackProvider
                : deepSeekStructuredLlmProvider;
    }

    @Override
    public String providerType() {
        return primaryProvider.providerType();
    }

    @Override
    public String defaultModelName() {
        return primaryProvider.defaultModelName();
    }

    @Override
    public StructuredLlmResponse<AiClassificationOutput> classify(AiClassificationInput input) {
        return withFallback(() -> primaryProvider.classify(input), () -> fallbackProvider.classify(input));
    }

    @Override
    public StructuredLlmResponse<AiExtractionOutput> extract(AiExtractionInput input) {
        return withFallback(() -> primaryProvider.extract(input), () -> fallbackProvider.extract(input));
    }

    @Override
    public StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input) {
        return withFallback(() -> primaryProvider.resolve(input), () -> fallbackProvider.resolve(input));
    }

    private <T> StructuredLlmResponse<T> withFallback(
            Supplier<StructuredLlmResponse<T>> primaryCall,
            Supplier<StructuredLlmResponse<T>> fallbackCall
    ) {
        try {
            return primaryCall.get();
        } catch (RuntimeException exception) {
            if (primaryProvider == fallbackProvider) {
                throw exception;
            }
            return fallbackCall.get();
        }
    }
}

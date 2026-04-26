package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.config.ApplicationProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LlmProviderRouter implements StructuredLlmProvider {

    private static final int MAX_PRIMARY_ATTEMPTS = 2;

    private final RuleBasedStructuredLlmProvider fallbackProvider;
    private final StructuredLlmProvider primaryProvider;
    private final StructuredLlmProvider localProvider;
    private final StructuredLlmOutputValidator outputValidator;

    public LlmProviderRouter(
            ApplicationProperties applicationProperties,
            RuleBasedStructuredLlmProvider fallbackProvider,
            ObjectProvider<DeepSeekStructuredLlmProvider> deepSeekProvider,
            ObjectProvider<LocalStructuredLlmProvider> localProvider,
            StructuredLlmOutputValidator outputValidator
    ) {
        this.fallbackProvider = fallbackProvider;
        this.outputValidator = outputValidator;
        DeepSeekStructuredLlmProvider deepSeekStructuredLlmProvider = deepSeekProvider.getIfAvailable();
        this.localProvider = localProvider.getIfAvailable();
        if ("rule-based".equalsIgnoreCase(applicationProperties.getAi().getProvider().getType())) {
            this.primaryProvider = fallbackProvider;
        } else if (deepSeekStructuredLlmProvider != null) {
            this.primaryProvider = deepSeekStructuredLlmProvider;
        } else if (this.localProvider != null) {
            this.primaryProvider = this.localProvider;
        } else {
            throw new IllegalStateException("LLM provider is configured but unavailable: " + applicationProperties.getAi().getProvider().getType());
        }
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
        return withFallback(
                new ProviderInvocation<>(
                        primaryProvider,
                        () -> primaryProvider.classify(input),
                        outputValidator::validateClassification
                ),
                localProvider == null || localProvider == primaryProvider ? null : new ProviderInvocation<>(
                        localProvider,
                        () -> localProvider.classify(input),
                        outputValidator::validateClassification
                ),
                new ProviderInvocation<>(
                        fallbackProvider,
                        () -> fallbackProvider.classify(input),
                        outputValidator::validateClassification
                )
        );
    }

    @Override
    public StructuredLlmResponse<AiExtractionOutput> extract(AiExtractionInput input) {
        return withFallback(
                new ProviderInvocation<>(
                        primaryProvider,
                        () -> primaryProvider.extract(input),
                        outputValidator::validateExtraction
                ),
                localProvider == null || localProvider == primaryProvider ? null : new ProviderInvocation<>(
                        localProvider,
                        () -> localProvider.extract(input),
                        outputValidator::validateExtraction
                ),
                new ProviderInvocation<>(
                        fallbackProvider,
                        () -> fallbackProvider.extract(input),
                        outputValidator::validateExtraction
                )
        );
    }

    @Override
    public StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input) {
        return withFallback(
                new ProviderInvocation<>(
                        primaryProvider,
                        () -> primaryProvider.resolve(input),
                        outputValidator::validateResolution
                ),
                localProvider == null || localProvider == primaryProvider ? null : new ProviderInvocation<>(
                        localProvider,
                        () -> localProvider.resolve(input),
                        outputValidator::validateResolution
                ),
                new ProviderInvocation<>(
                        fallbackProvider,
                        () -> fallbackProvider.resolve(input),
                        outputValidator::validateResolution
                )
        );
    }

    private <T> StructuredLlmResponse<T> withFallback(
            ProviderInvocation<T> primaryInvocation,
            ProviderInvocation<T> localInvocation,
            ProviderInvocation<T> fallbackInvocation
    ) {
        List<String> failureReasons = new ArrayList<>();
        StructuredLlmResponse<T> response = invokeWithRetries(primaryInvocation, failureReasons);
        if (response != null) {
            return response;
        }
        if (localInvocation != null && localInvocation.provider() != null && localInvocation.provider() != fallbackProvider) {
            response = invokeWithRetries(localInvocation, failureReasons);
            if (response != null) {
                return response.withFallback(String.join(" | ", failureReasons));
            }
        }
        response = invokeWithRetries(fallbackInvocation, failureReasons);
        if (response == null) {
            throw new IllegalStateException(String.join(" | ", failureReasons));
        }
        return response.withFallback(String.join(" | ", failureReasons));
    }

    private <T> StructuredLlmResponse<T> invokeWithRetries(
            ProviderInvocation<T> invocation,
            List<String> failureReasons
    ) {
        RuntimeException lastFailure = null;
        int attempts = invocation.provider() == fallbackProvider ? 1 : MAX_PRIMARY_ATTEMPTS;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                StructuredLlmResponse<T> response = invocation.call().get();
                invocation.validator().accept(response.output());
                return response;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt >= attempts) {
                    failureReasons.add(buildFallbackReason(invocation.provider(), exception));
                }
            }
        }
        if (invocation.provider() == fallbackProvider && lastFailure != null) {
            throw lastFailure;
        }
        return null;
    }

    private String buildFallbackReason(StructuredLlmProvider provider, RuntimeException exception) {
        String reason = exception == null ? "primary provider failed" : exception.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = exception == null ? "primary provider failed" : exception.getClass().getSimpleName();
        }
        return "Provider " + provider.providerType() + " failed: " + reason;
    }

    private record ProviderInvocation<T>(
            StructuredLlmProvider provider,
            java.util.function.Supplier<StructuredLlmResponse<T>> call,
            java.util.function.Consumer<T> validator
    ) {
    }
}

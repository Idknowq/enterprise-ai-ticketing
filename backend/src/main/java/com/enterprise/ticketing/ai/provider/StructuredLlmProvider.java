package com.enterprise.ticketing.ai.provider;

public interface StructuredLlmProvider {

    String providerType();

    String defaultModelName();

    StructuredLlmResponse<AiClassificationOutput> classify(AiClassificationInput input);

    StructuredLlmResponse<AiExtractionOutput> extract(AiExtractionInput input);

    StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input);
}

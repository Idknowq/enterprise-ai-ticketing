package com.enterprise.ticketing.ai.provider;

public record StructuredLlmResponse<T>(
        T output,
        String providerType,
        String modelName,
        int tokenInput,
        int tokenOutput,
        boolean fallbackUsed,
        String fallbackReason
) {

    public StructuredLlmResponse<T> withFallback(String fallbackReason) {
        return new StructuredLlmResponse<>(
                output,
                providerType,
                modelName,
                tokenInput,
                tokenOutput,
                true,
                fallbackReason
        );
    }
}

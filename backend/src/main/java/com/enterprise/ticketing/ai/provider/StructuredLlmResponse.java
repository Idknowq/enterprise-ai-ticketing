package com.enterprise.ticketing.ai.provider;

public record StructuredLlmResponse<T>(
        T output,
        String modelName,
        int tokenInput,
        int tokenOutput
) {
}

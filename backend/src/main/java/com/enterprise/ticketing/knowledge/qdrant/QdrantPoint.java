package com.enterprise.ticketing.knowledge.qdrant;

import java.util.Map;

public record QdrantPoint(
        String id,
        float[] vector,
        Map<String, Object> payload
) {
}

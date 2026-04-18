package com.enterprise.ticketing.knowledge.qdrant;

import java.util.Map;

public record QdrantSearchHit(
        double score,
        Map<String, Object> payload
) {
}

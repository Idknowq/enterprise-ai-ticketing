package com.enterprise.ticketing.knowledge.qdrant;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class QdrantClient {

    private final ApplicationProperties applicationProperties;
    private final EmbeddingProvider embeddingProvider;
    private final RestClient restClient;
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);
    private final String baseUrl;

    public QdrantClient(
            ApplicationProperties applicationProperties,
            EmbeddingProvider embeddingProvider,
            RestClient.Builder restClientBuilder
    ) {
        this.applicationProperties = applicationProperties;
        this.embeddingProvider = embeddingProvider;
        this.baseUrl = "http://" + applicationProperties.getQdrant().getHost() + ":" + applicationProperties.getQdrant().getHttpPort();
        RestClient.Builder builder = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (applicationProperties.getQdrant().getApiKey() != null && !applicationProperties.getQdrant().getApiKey().isBlank()) {
            builder.defaultHeader("api-key", applicationProperties.getQdrant().getApiKey());
        }
        this.restClient = builder.build();
    }

    public void upsert(List<QdrantPoint> points) {
        ensureCollection();
        List<Map<String, Object>> serializedPoints = new ArrayList<>(points.size());
        for (QdrantPoint point : points) {
            serializedPoints.add(Map.of(
                    "id", point.id(),
                    "vector", toVectorList(point.vector()),
                    "payload", point.payload()
            ));
        }

        invoke(() -> restClient.put()
                .uri("/collections/{collection}/points?wait=true", collectionName())
                .body(Map.of("points", serializedPoints))
                .retrieve()
                .toBodilessEntity(), "Failed to upsert document vectors into Qdrant");
    }

    @SuppressWarnings("unchecked")
    public List<QdrantSearchHit> search(float[] vector, int limit, Map<String, Object> filter) {
        ensureCollection();
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("vector", toVectorList(vector));
        requestBody.put("limit", limit);
        requestBody.put("with_payload", true);
        requestBody.put("with_vector", false);
        if (!CollectionUtils.isEmpty(filter)) {
            requestBody.put("filter", filter);
        }

        Map<String, Object> response = invoke(() -> restClient.post()
                .uri("/collections/{collection}/points/search", collectionName())
                .body(requestBody)
                .retrieve()
                .body(Map.class), "Failed to search Qdrant knowledge vectors");

        Object result = response == null ? null : response.get("result");
        if (!(result instanceof List<?> rows)) {
            return List.of();
        }

        List<QdrantSearchHit> hits = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> item)) {
                continue;
            }
            Object payload = item.get("payload");
            Object score = item.get("score");
            if (payload instanceof Map<?, ?> payloadMap && score instanceof Number scoreNumber) {
                hits.add(new QdrantSearchHit(scoreNumber.doubleValue(), (Map<String, Object>) payloadMap));
            }
        }
        return hits;
    }

    public Map<String, Object> matchFilter(String key, Object value) {
        return Map.of("key", key, "match", Map.of("value", value));
    }

    public Map<String, Object> anyFilter(String key, Set<?> values) {
        return Map.of("key", key, "match", Map.of("any", values));
    }

    private void ensureCollection() {
        if (collectionReady.get()) {
            return;
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri("/collections/{collection}", collectionName())
                    .retrieve()
                    .body(Map.class);
            assertCollectionVectorSize(response);
            collectionReady.set(true);
            return;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw unavailable("Failed to inspect Qdrant collection at " + baseUrl);
            }
        } catch (Exception exception) {
            throw unavailable("Failed to inspect Qdrant collection at " + baseUrl);
        }

        invoke(() -> restClient.put()
                .uri("/collections/{collection}", collectionName())
                .body(Map.of(
                        "vectors", Map.of(
                                "size", embeddingProvider.dimension(),
                                "distance", "Cosine"
                        )
                ))
                .retrieve()
                .toBodilessEntity(), "Failed to create Qdrant collection");
        collectionReady.set(true);
    }

    private <T> T invoke(QdrantCall<T> call, String message) {
        try {
            return call.execute();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                collectionReady.set(false);
            }
            String responseBody = exception.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) {
                throw unavailable(message + ": " + exception.getStatusText() + " - " + responseBody);
            }
            throw unavailable(message + ": " + exception.getStatusText());
        } catch (Exception exception) {
            throw unavailable(message);
        }
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(ErrorCode.KNOWLEDGE_VECTOR_STORE_UNAVAILABLE, message);
    }

    @SuppressWarnings("unchecked")
    private void assertCollectionVectorSize(Map<?, ?> response) {
        int expectedDimension = embeddingProvider.dimension();
        Object result = response == null ? null : response.get("result");
        if (!(result instanceof Map<?, ?> resultMap)) {
            return;
        }
        Object config = resultMap.get("config");
        if (!(config instanceof Map<?, ?> configMap)) {
            return;
        }
        Object params = configMap.get("params");
        if (!(params instanceof Map<?, ?> paramsMap)) {
            return;
        }
        Object vectors = paramsMap.get("vectors");
        Integer actualDimension = resolveVectorSize(vectors);
        if (actualDimension == null) {
            return;
        }
        if (actualDimension != expectedDimension) {
            throw unavailable(
                    "Qdrant collection '" + collectionName()
                            + "' vector size mismatch: expected " + expectedDimension
                            + " from embedding model " + embeddingProvider.modelName()
                            + " but existing collection uses " + actualDimension
                            + ". Recreate the collection before switching embedding models."
            );
        }
    }

    private Integer resolveVectorSize(Object vectors) {
        if (vectors instanceof Map<?, ?> vectorMap) {
            Object size = vectorMap.get("size");
            if (size instanceof Number number) {
                return number.intValue();
            }
            Object defaultVector = vectorMap.get("");
            if (defaultVector instanceof Map<?, ?> defaultVectorMap) {
                Object defaultSize = defaultVectorMap.get("size");
                if (defaultSize instanceof Number number) {
                    return number.intValue();
                }
            }
        }
        return null;
    }

    private String collectionName() {
        return applicationProperties.getKnowledge().getCollectionName();
    }

    private List<Float> toVectorList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float item : vector) {
            values.add(item);
        }
        return values;
    }

    @FunctionalInterface
    private interface QdrantCall<T> {
        T execute();
    }
}

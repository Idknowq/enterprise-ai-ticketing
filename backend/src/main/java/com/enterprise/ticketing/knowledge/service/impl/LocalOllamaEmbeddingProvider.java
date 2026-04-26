package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LocalOllamaEmbeddingProvider implements EmbeddingProvider {

    private final ApplicationProperties applicationProperties;
    private final RestClient.Builder restClientBuilder;
    private final RestClient restClient;

    @Autowired
    public LocalOllamaEmbeddingProvider(
            ApplicationProperties applicationProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.applicationProperties = applicationProperties;
        this.restClientBuilder = restClientBuilder;
        this.restClient = null;
    }

    LocalOllamaEmbeddingProvider(
            ApplicationProperties applicationProperties,
            RestClient restClient
    ) {
        this.applicationProperties = applicationProperties;
        this.restClientBuilder = null;
        this.restClient = restClient;
    }

    @Override
    public String modelName() {
        return applicationProperties.getKnowledge().getEmbedding().getLocal().getModel();
    }

    @Override
    public int dimension() {
        return applicationProperties.getKnowledge().getEmbedding().getLocal().getDimension();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embed(List<String> texts) {
        validateConfiguration();

        List<float[]> vectors = new ArrayList<>(texts.size());
        RestClient restClient = restClient();
        for (String text : texts) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri(applicationProperties.getKnowledge().getEmbedding().getLocal().getEmbeddingPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "model", modelName(),
                                "prompt", text == null ? "" : text
                        ))
                        .retrieve()
                        .body(Map.class);

                Object embedding = response == null ? null : response.get("embedding");
                if (!(embedding instanceof List<?> values)) {
                    throw new BusinessException(
                            ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                            "Local embedding provider returned an invalid response for model " + modelName()
                    );
                }
                vectors.add(toVector(values, "local"));
            } catch (BusinessException exception) {
                throw exception;
            } catch (RestClientException exception) {
                throw new BusinessException(
                        ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                        "Failed to call local embedding provider ollama at "
                                + applicationProperties.getKnowledge().getEmbedding().getLocal().getBaseUrl()
                                + " for model " + modelName()
                );
            }
        }
        return vectors;
    }

    void validateConfiguration() {
        ApplicationProperties.Knowledge.Embedding.Local local = applicationProperties.getKnowledge().getEmbedding().getLocal();
        if (!local.isEnabled()) {
            throw new IllegalStateException("app.knowledge.embedding.local.enabled must be true when routing mode is local");
        }
        if (!StringUtils.hasText(local.getBaseUrl())) {
            throw new IllegalStateException("app.knowledge.embedding.local.base-url is required when routing mode is local");
        }
        if (!StringUtils.hasText(local.getEmbeddingPath())) {
            throw new IllegalStateException("app.knowledge.embedding.local.embedding-path is required when routing mode is local");
        }
        if (!StringUtils.hasText(local.getModel())) {
            throw new IllegalStateException("app.knowledge.embedding.local.model is required when routing mode is local");
        }
        if (local.getDimension() <= 0) {
            throw new IllegalStateException("app.knowledge.embedding.local.dimension must be greater than 0");
        }
    }

    private RestClient restClient() {
        if (restClient != null) {
            return restClient;
        }
        ApplicationProperties.Knowledge.Embedding.Local local = applicationProperties.getKnowledge().getEmbedding().getLocal();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = local.getTimeout();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder
                .baseUrl(local.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private float[] toVector(List<?> values, String providerLabel) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!(value instanceof Number number)) {
                throw new BusinessException(
                        ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                        "Embedding vector from " + providerLabel + " provider contains a non-numeric value"
                );
            }
            vector[i] = number.floatValue();
        }
        if (vector.length != dimension()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "Embedding dimension mismatch for local model " + modelName()
                            + ": expected " + dimension() + " but received " + vector.length
            );
        }
        return vector;
    }
}

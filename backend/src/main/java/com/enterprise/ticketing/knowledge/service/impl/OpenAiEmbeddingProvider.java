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
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final ApplicationProperties applicationProperties;
    private final RestClient.Builder restClientBuilder;
    private final RestClient restClient;

    @Autowired
    public OpenAiEmbeddingProvider(
            ApplicationProperties applicationProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.applicationProperties = applicationProperties;
        this.restClientBuilder = restClientBuilder;
        this.restClient = null;
    }

    OpenAiEmbeddingProvider(
            ApplicationProperties applicationProperties,
            RestClient restClient
    ) {
        this.applicationProperties = applicationProperties;
        this.restClientBuilder = null;
        this.restClient = restClient;
    }

    @Override
    public String modelName() {
        return applicationProperties.getKnowledge().getEmbedding().getCommercial().getModel();
    }

    @Override
    public int dimension() {
        return applicationProperties.getKnowledge().getEmbedding().getCommercial().getDimension();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embed(List<String> texts) {
        validateConfiguration();
        try {
            Map<String, Object> response = restClient().post()
                    .uri(applicationProperties.getKnowledge().getEmbedding().getCommercial().getEmbeddingPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(
                            applicationProperties.getKnowledge().getEmbedding().getCommercial().getApiKey()
                    ))
                    .body(Map.of(
                            "model", modelName(),
                            "input", texts.stream().map(text -> text == null ? "" : text).toList()
                    ))
                    .retrieve()
                    .body(Map.class);

            Object data = response == null ? null : response.get("data");
            if (!(data instanceof List<?> rows) || rows.size() != texts.size()) {
                throw new BusinessException(
                        ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                        "Commercial embedding provider returned an invalid response for model " + modelName()
                );
            }

            List<float[]> vectors = new ArrayList<>(rows.size());
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> item)) {
                    throw new BusinessException(
                            ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                            "Commercial embedding provider returned a malformed response row"
                    );
                }
                Object embedding = item.get("embedding");
                if (!(embedding instanceof List<?> values)) {
                    throw new BusinessException(
                            ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                            "Commercial embedding provider response row is missing embedding data"
                    );
                }
                vectors.add(toVector(values));
            }
            return vectors;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                    "Failed to call commercial embedding provider openai at "
                            + applicationProperties.getKnowledge().getEmbedding().getCommercial().getBaseUrl()
                            + " for model " + modelName()
            );
        }
    }

    void validateConfiguration() {
        ApplicationProperties.Knowledge.Embedding.Commercial commercial = applicationProperties.getKnowledge().getEmbedding().getCommercial();
        if (!commercial.isEnabled()) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.enabled must be true when routing mode is commercial");
        }
        if (!StringUtils.hasText(commercial.getBaseUrl())) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.base-url is required when routing mode is commercial");
        }
        if (!StringUtils.hasText(commercial.getEmbeddingPath())) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.embedding-path is required when routing mode is commercial");
        }
        if (!StringUtils.hasText(commercial.getModel())) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.model is required when routing mode is commercial");
        }
        if (!StringUtils.hasText(commercial.getApiKey())) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.api-key is required when routing mode is commercial");
        }
        if (commercial.getDimension() <= 0) {
            throw new IllegalStateException("app.knowledge.embedding.commercial.dimension must be greater than 0");
        }
    }

    private RestClient restClient() {
        if (restClient != null) {
            return restClient;
        }
        ApplicationProperties.Knowledge.Embedding.Commercial commercial = applicationProperties.getKnowledge().getEmbedding().getCommercial();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = commercial.getTimeout();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder
                .baseUrl(commercial.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private float[] toVector(List<?> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (!(value instanceof Number number)) {
                throw new BusinessException(
                        ErrorCode.COMMON_SERVICE_UNAVAILABLE,
                        "Embedding vector from commercial provider contains a non-numeric value"
                );
            }
            vector[i] = number.floatValue();
        }
        if (vector.length != dimension()) {
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "Embedding dimension mismatch for commercial model " + modelName()
                            + ": expected " + dimension() + " but received " + vector.length
            );
        }
        return vector;
    }
}

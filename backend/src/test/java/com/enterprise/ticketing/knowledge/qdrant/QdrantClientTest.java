package com.enterprise.ticketing.knowledge.qdrant;

import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QdrantClientTest {

    @Test
    void throwsWhenExistingCollectionDimensionDoesNotMatchActiveEmbeddingProvider() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getQdrant().setHost("127.0.0.1");
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        when(embeddingProvider.dimension()).thenReturn(768);
        when(embeddingProvider.modelName()).thenReturn("nomic-embed-text:latest");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:6333/collections/knowledge_chunks"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"result\":{\"config\":{\"params\":{\"vectors\":{\"size\":1024,\"distance\":\"Cosine\"}}}}}",
                        MediaType.APPLICATION_JSON
                ));

        QdrantClient client = new QdrantClient(properties, embeddingProvider, builder);

        assertThatThrownBy(() -> client.search(new float[]{0.1F, 0.2F}, 5, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vector size mismatch")
                .hasMessageContaining("expected 768")
                .hasMessageContaining("uses 1024");

        server.verify();
    }
}

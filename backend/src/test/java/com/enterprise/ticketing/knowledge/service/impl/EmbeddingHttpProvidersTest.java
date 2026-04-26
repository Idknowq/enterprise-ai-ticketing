package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingHttpProvidersTest {

    @Test
    void parsesLocalOllamaEmbeddingResponse() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().getEmbedding().getLocal().setDimension(2);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/api/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"embedding\":[0.1,0.2]}", MediaType.APPLICATION_JSON));

        LocalOllamaEmbeddingProvider provider = new LocalOllamaEmbeddingProvider(properties, builder.build());
        List<float[]> vectors = provider.embed(List.of("vpn"));

        assertThat(vectors).hasSize(1);
        assertThat(vectors.get(0)).containsExactly(0.1F, 0.2F);
        server.verify();
    }

    @Test
    void parsesCommercialOpenAiEmbeddingResponse() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().getEmbedding().getCommercial().setEnabled(true);
        properties.getKnowledge().getEmbedding().getCommercial().setApiKey("test-key");
        properties.getKnowledge().getEmbedding().getCommercial().setDimension(2);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[0.3,0.4]}]}", MediaType.APPLICATION_JSON));

        OpenAiEmbeddingProvider provider = new OpenAiEmbeddingProvider(properties, builder.build());
        List<float[]> vectors = provider.embed(List.of("vpn"));

        assertThat(vectors).hasSize(1);
        assertThat(vectors.get(0)).containsExactly(0.3F, 0.4F);
        server.verify();
    }
}

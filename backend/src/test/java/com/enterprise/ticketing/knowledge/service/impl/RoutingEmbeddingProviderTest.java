package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingEmbeddingProviderTest {

    @Test
    void routesToLocalProviderWhenModeIsLocal() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().getEmbedding().getRouting().setMode("local");

        LocalOllamaEmbeddingProvider localProvider = mock(LocalOllamaEmbeddingProvider.class);
        OpenAiEmbeddingProvider commercialProvider = mock(OpenAiEmbeddingProvider.class);
        when(localProvider.modelName()).thenReturn("nomic-embed-text:latest");
        when(localProvider.dimension()).thenReturn(768);
        when(localProvider.embed(List.of("vpn"))).thenReturn(List.of(new float[]{0.1F, 0.2F}));

        RoutingEmbeddingProvider router = new RoutingEmbeddingProvider(properties, localProvider, commercialProvider);

        assertThat(router.modelName()).isEqualTo("nomic-embed-text:latest");
        assertThat(router.dimension()).isEqualTo(768);
        assertThat(router.embed(List.of("vpn"))).hasSize(1);
        verify(localProvider).validateConfiguration();
        verify(localProvider).embed(List.of("vpn"));
    }

    @Test
    void routesToCommercialProviderWhenModeIsCommercial() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().getEmbedding().getRouting().setMode("commercial");

        LocalOllamaEmbeddingProvider localProvider = mock(LocalOllamaEmbeddingProvider.class);
        OpenAiEmbeddingProvider commercialProvider = mock(OpenAiEmbeddingProvider.class);
        when(commercialProvider.modelName()).thenReturn("text-embedding-3-large");
        when(commercialProvider.dimension()).thenReturn(3072);
        when(commercialProvider.embed(List.of("vpn"))).thenReturn(List.of(new float[]{0.1F, 0.2F}));

        RoutingEmbeddingProvider router = new RoutingEmbeddingProvider(properties, localProvider, commercialProvider);

        assertThat(router.modelName()).isEqualTo("text-embedding-3-large");
        assertThat(router.dimension()).isEqualTo(3072);
        assertThat(router.embed(List.of("vpn"))).hasSize(1);
        verify(commercialProvider).validateConfiguration();
        verify(commercialProvider).embed(List.of("vpn"));
    }
}

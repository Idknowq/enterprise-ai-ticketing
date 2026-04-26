package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.qdrant.QdrantClient;
import com.enterprise.ticketing.knowledge.service.CitationService;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetrievalServiceImplTest {

    @Test
    void searchUsesStandardCategoryCodeInQdrantFilter() {
        ApplicationProperties properties = new ApplicationProperties();
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        DocumentAccessPolicy accessPolicy = mock(DocumentAccessPolicy.class);
        CitationService citationService = mock(CitationService.class);
        TicketQueryService ticketQueryService = mock(TicketQueryService.class);

        when(embeddingProvider.embed(any())).thenReturn(java.util.List.of(new float[]{0.1F, 0.2F}));
        when(accessPolicy.allowedAccessLevels()).thenReturn(Set.of(KnowledgeAccessLevel.PUBLIC, KnowledgeAccessLevel.INTERNAL));
        when(accessPolicy.allowedDepartments(null)).thenReturn(Set.of("GLOBAL", "IT"));
        when(qdrantClient.matchFilter("category", "REMOTE_ACCESS")).thenReturn(Map.of("category", "REMOTE_ACCESS"));
        when(qdrantClient.anyFilter(eq("department"), any())).thenReturn(Map.of("department", "any"));
        when(qdrantClient.anyFilter(eq("accessLevel"), any())).thenReturn(Map.of("accessLevel", "any"));
        when(qdrantClient.search(any(), eq(5), any())).thenReturn(java.util.List.of());

        RetrievalServiceImpl service = new RetrievalServiceImpl(
                properties,
                embeddingProvider,
                qdrantClient,
                accessPolicy,
                citationService,
                ticketQueryService
        );
        RetrievalSearchRequest request = new RetrievalSearchRequest();
        request.setQuery("vpn certificate expired");
        request.setCategory(KnowledgeDocumentCategory.REMOTE_ACCESS);
        request.setLimit(5);

        service.search(request);

        ArgumentCaptor<Map<String, Object>> filterCaptor = forClass(Map.class);
        verify(qdrantClient).search(any(), eq(5), filterCaptor.capture());
        assertThat(filterCaptor.getValue().toString()).contains("REMOTE_ACCESS");
        verify(qdrantClient).matchFilter("category", "REMOTE_ACCESS");
    }
}

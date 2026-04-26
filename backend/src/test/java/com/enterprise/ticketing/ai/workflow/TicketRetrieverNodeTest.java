package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiRetrievalStatus;
import com.enterprise.ticketing.ai.provider.AiClassificationOutput;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.dto.RetrievalDiagnosticsResponse;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchRequest;
import com.enterprise.ticketing.knowledge.dto.RetrievalSearchResponse;
import com.enterprise.ticketing.knowledge.service.RetrievalService;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.dto.TicketUserSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketRetrieverNodeTest {

    @Test
    void returnsEmptyCitationsInsteadOfHeuristicsWhenRetrievalMisses() {
        RetrievalService retrievalService = mock(RetrievalService.class);
        when(retrievalService.search(any())).thenReturn(new RetrievalSearchResponse(
                "VPN 连接失败 证书失效",
                4L,
                new RetrievalDiagnosticsResponse(
                        "HYBRID_CANDIDATES_WITH_RERANK",
                        12,
                        0,
                        Map.of("category", "VPN")
                ),
                List.of()
        ));
        TicketRetrieverNode node = new TicketRetrieverNode(
                providerOf(retrievalService),
                applicationProperties(),
                mock(AiRunLogService.class)
        );

        AiWorkflowState state = workflowState();
        node.execute(state);

        assertThat(state.getCitations()).isEmpty();
        assertThat(state.getRetrievalStatus()).isEqualTo(AiRetrievalStatus.EMPTY);
        assertThat(state.getRetrievalDiagnostics().retrievalMode()).isEqualTo("HYBRID_CANDIDATES_WITH_RERANK");
        assertThat(state.getRetrievalDiagnostics().filterSummary()).containsEntry("category", "VPN");
    }

    @Test
    void recordsErrorStatusWhenRetrievalThrows() {
        RetrievalService retrievalService = mock(RetrievalService.class);
        when(retrievalService.search(any())).thenThrow(new IllegalStateException("qdrant unavailable"));
        TicketRetrieverNode node = new TicketRetrieverNode(
                providerOf(retrievalService),
                applicationProperties(),
                mock(AiRunLogService.class)
        );

        AiWorkflowState state = workflowState();
        node.execute(state);

        assertThat(state.getCitations()).isEmpty();
        assertThat(state.getRetrievalStatus()).isEqualTo(AiRetrievalStatus.ERROR);
        assertThat(state.getRetrievalDiagnostics().message()).contains("qdrant unavailable");
    }

    @Test
    void doesNotInferRetrievalCategoryFromAiCategoryWhenRealFieldsAreMissing() {
        RetrievalService retrievalService = mock(RetrievalService.class);
        when(retrievalService.search(any())).thenReturn(new RetrievalSearchResponse(
                "VPN 连接失败 证书失效",
                4L,
                new RetrievalDiagnosticsResponse("HYBRID_CANDIDATES_WITH_RERANK", 0, 0, Map.of()),
                List.of()
        ));
        TicketRetrieverNode node = new TicketRetrieverNode(
                providerOf(retrievalService),
                applicationProperties(),
                mock(AiRunLogService.class)
        );

        AiWorkflowState state = workflowState();
        state.setExtractedFields(Map.of());
        node.execute(state);

        var requestCaptor = forClass(RetrievalSearchRequest.class);
        verify(retrievalService).search(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCategory()).isNull();
    }

    private AiWorkflowState workflowState() {
        AiWorkflowState state = new AiWorkflowState(
                "ai-test",
                new TicketResponse(
                        4L,
                        "VPN 连接失败",
                        "客户端提示证书失效",
                        "IT",
                        TicketPriority.MEDIUM,
                        TicketStatus.OPEN,
                        new TicketUserSummaryResponse(1L, "support01", "Support", "IT"),
                        null,
                        Instant.parse("2026-04-24T10:00:00Z"),
                        Instant.parse("2026-04-24T10:00:00Z")
                )
        );
        state.setClassification(new AiClassificationOutput("VPN_ISSUE", TicketPriority.MEDIUM, 0.94d));
        state.setExtractedFields(Map.of("system", "VPN", "issueType", "CERTIFICATE_EXPIRED"));
        return state;
    }

    private ApplicationProperties applicationProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getAi().setRetrievalTopK(4);
        return properties;
    }

    private ObjectProvider<RetrievalService> providerOf(RetrievalService retrievalService) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("retrievalService", retrievalService);
        return beanFactory.getBeanProvider(RetrievalService.class);
    }
}

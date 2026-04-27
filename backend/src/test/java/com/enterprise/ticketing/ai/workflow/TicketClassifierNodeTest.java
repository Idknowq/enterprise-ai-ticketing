package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.provider.AiClassificationInput;
import com.enterprise.ticketing.ai.provider.AiClassificationOutput;
import com.enterprise.ticketing.ai.provider.LlmProviderRouter;
import com.enterprise.ticketing.ai.provider.StructuredLlmResponse;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.dto.TicketUserSummaryResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketClassifierNodeTest {

    @Test
    void overwritesProviderCategoryWithCanonicalTicketCategory() {
        LlmProviderRouter router = mock(LlmProviderRouter.class);
        when(router.classify(any(AiClassificationInput.class))).thenReturn(new StructuredLlmResponse<>(
                new AiClassificationOutput("VPN_ISSUE", TicketPriority.HIGH, 0.77d),
                "deepseek",
                "deepseek-chat",
                10,
                5,
                false,
                null
        ));
        TicketClassifierNode node = new TicketClassifierNode(router, mock(AiRunLogService.class));
        AiWorkflowState state = workflowState("REMOTE_ACCESS", TicketPriority.LOW);

        node.execute(state);

        assertThat(state.getClassification().category()).isEqualTo("REMOTE_ACCESS");
        assertThat(state.getClassification().priority()).isEqualTo(TicketPriority.LOW);
        assertThat(state.getClassification().confidence()).isEqualTo(0.77d);
    }

    @Test
    void rejectsLegacyTicketCategoryBeforeCallingProvider() {
        LlmProviderRouter router = mock(LlmProviderRouter.class);
        when(router.providerType()).thenReturn("deepseek");
        when(router.defaultModelName()).thenReturn("deepseek-chat");
        TicketClassifierNode node = new TicketClassifierNode(router, mock(AiRunLogService.class));

        assertThatThrownBy(() -> node.execute(workflowState("VPN_ISSUE", TicketPriority.MEDIUM)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported knowledge document category");
    }

    private AiWorkflowState workflowState(String category, TicketPriority priority) {
        return new AiWorkflowState(
                "ai-test",
                new TicketResponse(
                        4L,
                        "VPN 连接失败",
                        "客户端提示证书失效",
                        category,
                        priority,
                        TicketStatus.OPEN,
                        new TicketUserSummaryResponse(1L, "support01", "Support", "IT"),
                        null,
                        Instant.parse("2026-04-24T10:00:00Z"),
                        Instant.parse("2026-04-24T10:00:00Z")
                )
        );
    }
}

package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.provider.AiClassificationInput;
import com.enterprise.ticketing.ai.provider.AiClassificationOutput;
import com.enterprise.ticketing.ai.provider.LlmProviderRouter;
import com.enterprise.ticketing.ai.provider.StructuredLlmResponse;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class TicketClassifierNode {

    private final LlmProviderRouter llmProviderRouter;
    private final AiRunLogService aiRunLogService;

    public TicketClassifierNode(LlmProviderRouter llmProviderRouter, AiRunLogService aiRunLogService) {
        this.llmProviderRouter = llmProviderRouter;
        this.aiRunLogService = aiRunLogService;
    }

    public void execute(AiWorkflowState state) {
        long startedAt = System.nanoTime();
        try {
            StructuredLlmResponse<AiClassificationOutput> response = llmProviderRouter.classify(
                    new AiClassificationInput(
                            state.getTicket().title(),
                            state.getTicket().description(),
                            state.getTicket().category()
                    )
            );
            state.setClassification(response.output());
            state.putNodeExecutionDetails(
                    AiNodeName.CLASSIFIER,
                    new AiNodeExecutionDetails(
                            AiNodeName.CLASSIFIER,
                            response.providerType(),
                            response.modelName(),
                            response.fallbackUsed(),
                            response.fallbackReason()
                    )
            );
            int latencyMs = toLatencyMs(startedAt);
            aiRunLogService.recordSuccess(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.CLASSIFIER,
                    response.providerType(),
                    response.modelName(),
                    latencyMs,
                    response.tokenInput(),
                    response.tokenOutput(),
                    response.fallbackUsed(),
                    response.fallbackReason(),
                    null,
                    "Category=" + response.output().category() + ", priority=" + response.output().priority(),
                    response.output()
            );
        } catch (RuntimeException exception) {
            aiRunLogService.recordFailure(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.CLASSIFIER,
                    llmProviderRouter.providerType(),
                    llmProviderRouter.defaultModelName(),
                    toLatencyMs(startedAt),
                    false,
                    null,
                    null,
                    exception.getMessage(),
                    null
            );
            throw exception;
        }
    }

    private int toLatencyMs(long startedAt) {
        return (int) Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}

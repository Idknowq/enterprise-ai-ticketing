package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.provider.AiResolutionInput;
import com.enterprise.ticketing.ai.provider.AiResolutionOutput;
import com.enterprise.ticketing.ai.provider.LlmProviderRouter;
import com.enterprise.ticketing.ai.provider.StructuredLlmResponse;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class TicketResolutionNode {

    private final LlmProviderRouter llmProviderRouter;
    private final AiRunLogService aiRunLogService;

    public TicketResolutionNode(LlmProviderRouter llmProviderRouter, AiRunLogService aiRunLogService) {
        this.llmProviderRouter = llmProviderRouter;
        this.aiRunLogService = aiRunLogService;
    }

    public void execute(AiWorkflowState state) {
        long startedAt = System.nanoTime();
        try {
            StructuredLlmResponse<AiResolutionOutput> response = llmProviderRouter.resolve(
                    new AiResolutionInput(
                            state.getTicket().title(),
                            state.getTicket().description(),
                            state.getClassification(),
                            state.getExtractedFields(),
                            state.getCitations()
                    )
            );
            state.setResolution(response.output());
            state.putNodeExecutionDetails(
                    AiNodeName.RESOLUTION,
                    new AiNodeExecutionDetails(
                            AiNodeName.RESOLUTION,
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
                    AiNodeName.RESOLUTION,
                    response.providerType(),
                    response.modelName(),
                    latencyMs,
                    response.tokenInput(),
                    response.tokenOutput(),
                    response.fallbackUsed(),
                    response.fallbackReason(),
                    state.getRetrievalStatus().name(),
                    "requiresApproval=" + response.output().requiresApproval()
                            + ", needsHumanHandoff=" + response.output().needsHumanHandoff(),
                    response.output()
            );
        } catch (RuntimeException exception) {
            aiRunLogService.recordFailure(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.RESOLUTION,
                    llmProviderRouter.providerType(),
                    llmProviderRouter.defaultModelName(),
                    toLatencyMs(startedAt),
                    false,
                    null,
                    state.getRetrievalStatus().name(),
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

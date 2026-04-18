package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.provider.AiExtractionInput;
import com.enterprise.ticketing.ai.provider.AiExtractionOutput;
import com.enterprise.ticketing.ai.provider.LlmProviderRouter;
import com.enterprise.ticketing.ai.provider.StructuredLlmResponse;
import com.enterprise.ticketing.ai.service.impl.AiRunLogService;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class TicketExtractorNode {

    private final LlmProviderRouter llmProviderRouter;
    private final AiRunLogService aiRunLogService;

    public TicketExtractorNode(LlmProviderRouter llmProviderRouter, AiRunLogService aiRunLogService) {
        this.llmProviderRouter = llmProviderRouter;
        this.aiRunLogService = aiRunLogService;
    }

    public void execute(AiWorkflowState state) {
        long startedAt = System.nanoTime();
        try {
            StructuredLlmResponse<AiExtractionOutput> response = llmProviderRouter.extract(
                    new AiExtractionInput(
                            state.getTicket().title(),
                            state.getTicket().description(),
                            state.getClassification().category()
                    )
            );
            state.setExtractedFields(response.output().extractedFields());
            int latencyMs = toLatencyMs(startedAt);
            aiRunLogService.recordSuccess(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.EXTRACTOR,
                    response.modelName(),
                    latencyMs,
                    response.tokenInput(),
                    response.tokenOutput(),
                    "Extracted " + state.getExtractedFields().size() + " fields",
                    response.output()
            );
        } catch (RuntimeException exception) {
            aiRunLogService.recordFailure(
                    state.getTicket().id(),
                    state.getWorkflowId(),
                    AiNodeName.EXTRACTOR,
                    llmProviderRouter.defaultModelName(),
                    toLatencyMs(startedAt),
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

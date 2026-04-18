package com.enterprise.ticketing.ai.service.impl;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import com.enterprise.ticketing.ai.dto.AiNodeRunResponse;
import com.enterprise.ticketing.ai.entity.AiRunEntity;
import com.enterprise.ticketing.ai.repository.AiRunRepository;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class AiRunLogService {

    private final AiRunRepository aiRunRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public AiRunLogService(
            AiRunRepository aiRunRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.aiRunRepository = aiRunRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public AiRunEntity recordSuccess(
            Long ticketId,
            String workflowId,
            AiNodeName nodeName,
            String modelName,
            int latencyMs,
            int tokenInput,
            int tokenOutput,
            String resultSummary,
            Object resultPayload
    ) {
        return persist(
                ticketId,
                workflowId,
                nodeName,
                AiRunStatus.SUCCESS,
                modelName,
                latencyMs,
                tokenInput,
                tokenOutput,
                resultSummary,
                resultPayload,
                null
        );
    }

    public AiRunEntity recordFailure(
            Long ticketId,
            String workflowId,
            AiNodeName nodeName,
            String modelName,
            int latencyMs,
            String errorMessage,
            Object resultPayload
    ) {
        return persist(
                ticketId,
                workflowId,
                nodeName,
                AiRunStatus.FAILED,
                modelName,
                latencyMs,
                0,
                0,
                null,
                resultPayload,
                errorMessage
        );
    }

    public AiNodeRunResponse toResponse(AiRunEntity entity) {
        return new AiNodeRunResponse(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getNodeName(),
                entity.getStatus(),
                entity.getModelName(),
                entity.getLatencyMs(),
                entity.getTokenInput(),
                entity.getTokenOutput(),
                entity.getResultSummary(),
                entity.getResultPayload(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    private AiRunEntity persist(
            Long ticketId,
            String workflowId,
            AiNodeName nodeName,
            AiRunStatus status,
            String modelName,
            int latencyMs,
            int tokenInput,
            int tokenOutput,
            String resultSummary,
            Object resultPayload,
            String errorMessage
    ) {
        AiRunEntity entity = new AiRunEntity();
        entity.setTicket(entityManager.getReference(TicketEntity.class, ticketId));
        entity.setWorkflowId(workflowId);
        entity.setNodeName(nodeName);
        entity.setStatus(status);
        entity.setModelName(modelName == null ? "unknown" : modelName);
        entity.setLatencyMs(latencyMs);
        entity.setTokenInput(tokenInput);
        entity.setTokenOutput(tokenOutput);
        entity.setResultSummary(resultSummary);
        entity.setResultPayload(toJson(resultPayload));
        entity.setErrorMessage(errorMessage);

        AiRunEntity saved = aiRunRepository.save(entity);
        meterRegistry.counter(
                "ticketing.ai.node.runs",
                "node", nodeName.name(),
                "status", status.name(),
                "model", saved.getModelName()
        ).increment();
        Timer.builder("ticketing.ai.node.latency")
                .tag("node", nodeName.name())
                .tag("status", status.name())
                .tag("model", saved.getModelName())
                .register(meterRegistry)
                .record(Duration.ofMillis(latencyMs));
        return saved;
    }

    private JsonNode toJson(Object resultPayload) {
        if (resultPayload == null) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.valueToTree(resultPayload);
    }
}

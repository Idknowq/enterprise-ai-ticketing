package com.enterprise.ticketing.ai.service.impl;

import com.enterprise.ticketing.ai.domain.AiNodeName;
import com.enterprise.ticketing.ai.domain.AiRunStatus;
import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.ai.dto.AiNodeRunResponse;
import com.enterprise.ticketing.ai.dto.AiWorkflowRunResponse;
import com.enterprise.ticketing.ai.entity.AiRunEntity;
import com.enterprise.ticketing.ai.repository.AiRunRepository;
import com.enterprise.ticketing.ai.service.AiOrchestrationService;
import com.enterprise.ticketing.ai.workflow.AiWorkflowState;
import com.enterprise.ticketing.ai.workflow.TicketClassifierNode;
import com.enterprise.ticketing.ai.workflow.TicketExtractorNode;
import com.enterprise.ticketing.ai.workflow.TicketResolutionNode;
import com.enterprise.ticketing.ai.workflow.TicketRetrieverNode;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAiOrchestrationService implements AiOrchestrationService {

    private final ApplicationProperties applicationProperties;
    private final TicketQueryService ticketQueryService;
    private final TicketClassifierNode ticketClassifierNode;
    private final TicketExtractorNode ticketExtractorNode;
    private final TicketRetrieverNode ticketRetrieverNode;
    private final TicketResolutionNode ticketResolutionNode;
    private final AiRunLogService aiRunLogService;
    private final AiRunRepository aiRunRepository;
    private final ObjectMapper objectMapper;

    public DefaultAiOrchestrationService(
            ApplicationProperties applicationProperties,
            TicketQueryService ticketQueryService,
            TicketClassifierNode ticketClassifierNode,
            TicketExtractorNode ticketExtractorNode,
            TicketRetrieverNode ticketRetrieverNode,
            TicketResolutionNode ticketResolutionNode,
            AiRunLogService aiRunLogService,
            AiRunRepository aiRunRepository,
            ObjectMapper objectMapper
    ) {
        this.applicationProperties = applicationProperties;
        this.ticketQueryService = ticketQueryService;
        this.ticketClassifierNode = ticketClassifierNode;
        this.ticketExtractorNode = ticketExtractorNode;
        this.ticketRetrieverNode = ticketRetrieverNode;
        this.ticketResolutionNode = ticketResolutionNode;
        this.aiRunLogService = aiRunLogService;
        this.aiRunRepository = aiRunRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiDecisionResult runForTicket(Long ticketId) {
        ensureEnabled();
        TicketDetailResponse ticketDetail = ticketQueryService.getTicketDetail(ticketId);
        String workflowId = workflowId();
        AiWorkflowState state = new AiWorkflowState(workflowId, ticketDetail.ticket());

        try {
            ticketClassifierNode.execute(state);
            ticketExtractorNode.execute(state);
            ticketRetrieverNode.execute(state);
            ticketResolutionNode.execute(state);

            AiDecisionResult decisionResult = buildDecisionResult(state);
            aiRunLogService.recordSuccess(
                    ticketId,
                    workflowId,
                    AiNodeName.ORCHESTRATION,
                    "ai-orchestration-service",
                    0,
                    0,
                    0,
                    "AI orchestration completed",
                    decisionResult
            );
            return decisionResult;
        } catch (RuntimeException exception) {
            aiRunLogService.recordFailure(
                    ticketId,
                    workflowId,
                    AiNodeName.ORCHESTRATION,
                    "ai-orchestration-service",
                    0,
                    exception.getMessage(),
                    Map.of("ticketId", ticketId)
            );
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(
                    ErrorCode.COMMON_INTERNAL_ERROR,
                    "AI orchestration failed for ticket " + ticketId + ": " + exception.getMessage()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiWorkflowRunResponse> listRuns(Long ticketId) {
        ensureEnabled();
        ticketQueryService.getTicketDetail(ticketId);

        List<AiRunEntity> runEntities = aiRunRepository.findByTicketIdOrderByCreatedAtDescIdDesc(ticketId);
        Map<String, List<AiRunEntity>> grouped = new LinkedHashMap<>();
        for (AiRunEntity entity : runEntities) {
            grouped.computeIfAbsent(entity.getWorkflowId(), ignored -> new ArrayList<>()).add(entity);
        }

        return grouped.values().stream()
                .map(this::toWorkflowRunResponse)
                .toList();
    }

    private AiWorkflowRunResponse toWorkflowRunResponse(List<AiRunEntity> entities) {
        List<AiRunEntity> orderedEntities = entities.stream()
                .sorted(Comparator.comparing(AiRunEntity::getCreatedAt).thenComparing(AiRunEntity::getId))
                .toList();
        List<AiNodeRunResponse> nodes = orderedEntities.stream()
                .map(aiRunLogService::toResponse)
                .toList();
        AiRunEntity orchestrationNode = orderedEntities.stream()
                .filter(entity -> entity.getNodeName() == AiNodeName.ORCHESTRATION)
                .reduce((first, second) -> second)
                .orElse(null);

        AiRunStatus status = orchestrationNode != null
                ? orchestrationNode.getStatus()
                : orderedEntities.stream().anyMatch(entity -> entity.getStatus() == AiRunStatus.FAILED)
                ? AiRunStatus.FAILED
                : AiRunStatus.SUCCESS;

        AiDecisionResult finalDecision = orchestrationNode == null || orchestrationNode.getStatus() != AiRunStatus.SUCCESS
                ? null
                : toDecisionResult(orchestrationNode.getResultPayload());

        return new AiWorkflowRunResponse(
                orderedEntities.get(0).getWorkflowId(),
                status,
                orderedEntities.get(0).getCreatedAt(),
                finalDecision,
                nodes
        );
    }

    private AiDecisionResult buildDecisionResult(AiWorkflowState state) {
        return new AiDecisionResult(
                state.getWorkflowId(),
                state.getTicket().id(),
                state.getClassification().category(),
                state.getClassification().priority(),
                state.getClassification().confidence(),
                state.getResolution().requiresApproval(),
                state.getResolution().needsHumanHandoff(),
                state.getResolution().draftReply(),
                state.getResolution().suggestedActions(),
                state.getExtractedFields(),
                state.getCitations(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS)
        );
    }

    private AiDecisionResult toDecisionResult(JsonNode payload) {
        if (payload == null || payload.isMissingNode() || payload.isNull() || payload.isEmpty()) {
            return null;
        }
        return objectMapper.convertValue(payload, AiDecisionResult.class);
    }

    private void ensureEnabled() {
        if (!applicationProperties.getAi().isEnabled()) {
            throw new BusinessException(ErrorCode.COMMON_SERVICE_UNAVAILABLE, "AI orchestration module is disabled");
        }
    }

    private String workflowId() {
        return "ai-" + UUID.randomUUID().toString().replace("-", "");
    }
}

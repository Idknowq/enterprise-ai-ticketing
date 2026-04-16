package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.auth.repository.UserRepository;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.ticket.domain.TicketEventType;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.CreateTicketRequest;
import com.enterprise.ticketing.ticket.dto.TicketCommentResponse;
import com.enterprise.ticketing.ticket.dto.TicketEventResponse;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.entity.TicketCommentEntity;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.enterprise.ticketing.ticket.entity.TicketEventEntity;
import com.enterprise.ticketing.ticket.repository.TicketCommentRepository;
import com.enterprise.ticketing.ticket.repository.TicketEventRepository;
import com.enterprise.ticketing.ticket.repository.TicketRepository;
import com.enterprise.ticketing.ticket.service.TicketAssignmentService;
import com.enterprise.ticketing.ticket.service.TicketCommentService;
import com.enterprise.ticketing.ticket.service.TicketEventService;
import com.enterprise.ticketing.ticket.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketCoreServiceImpl
        implements TicketService, TicketCommentService, TicketAssignmentService, TicketEventService {

    private final TicketRepository ticketRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final UserRepository userRepository;
    private final TicketAccessPolicy ticketAccessPolicy;
    private final TicketDtoMapper ticketDtoMapper;
    private final ObjectMapper objectMapper;

    public TicketCoreServiceImpl(
            TicketRepository ticketRepository,
            TicketEventRepository ticketEventRepository,
            TicketCommentRepository ticketCommentRepository,
            UserRepository userRepository,
            TicketAccessPolicy ticketAccessPolicy,
            TicketDtoMapper ticketDtoMapper,
            ObjectMapper objectMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketEventRepository = ticketEventRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.userRepository = userRepository;
        this.ticketAccessPolicy = ticketAccessPolicy;
        this.ticketDtoMapper = ticketDtoMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        if (!StringUtils.hasText(request.title()) || !StringUtils.hasText(request.description())) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_ERROR, "Ticket title and description are required");
        }
        UserEntity requester = requireUser(ticketAccessPolicy.currentUserId());

        TicketEntity ticket = new TicketEntity();
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(normalize(request.category()));
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : request.priority());
        ticket.setRequester(requester);

        TicketEntity savedTicket = ticketRepository.saveAndFlush(ticket);
        recordEventInternal(
                savedTicket,
                TicketEventType.CREATED,
                "Ticket created",
                payloadOf(
                        "title", savedTicket.getTitle(),
                        "priority", savedTicket.getPriority(),
                        "category", savedTicket.getCategory(),
                        "status", savedTicket.getStatus()
                ),
                requester
        );
        return ticketDtoMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatus targetStatus, String reason) {
        TicketEntity ticket = requireTicket(ticketId);
        ticketAccessPolicy.checkManualStatusUpdateAccess(ticket, targetStatus);
        UserEntity operator = requireUser(ticketAccessPolicy.currentUserId());

        TicketStatus fromStatus = ticket.getStatus();
        ticket.transitionTo(targetStatus);
        TicketEntity savedTicket = ticketRepository.saveAndFlush(ticket);

        String summary = StringUtils.hasText(reason)
                ? "Status changed to " + targetStatus
                : "Status changed from " + fromStatus + " to " + targetStatus;
        recordEventInternal(
                savedTicket,
                TicketEventType.STATUS_CHANGED,
                summary,
                payloadOf(
                        "fromStatus", fromStatus,
                        "toStatus", targetStatus,
                        "reason", normalize(reason),
                        "source", "MANUAL"
                ),
                operator
        );
        return ticketDtoMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponse markAiProcessing(Long ticketId, String summary) {
        return updateStatusAsSystem(ticketId, TicketStatus.AI_PROCESSING, summary, "AI");
    }

    @Override
    @Transactional
    public TicketResponse markWaitingApproval(Long ticketId, String summary) {
        return updateStatusAsSystem(ticketId, TicketStatus.WAITING_APPROVAL, summary, "WORKFLOW");
    }

    @Override
    @Transactional
    public TicketResponse markResolved(Long ticketId, String summary) {
        return updateStatusAsSystem(ticketId, TicketStatus.RESOLVED, summary, "WORKFLOW");
    }

    @Override
    @Transactional
    public TicketResponse markRejected(Long ticketId, String summary) {
        return updateStatusAsSystem(ticketId, TicketStatus.REJECTED, summary, "WORKFLOW");
    }

    @Override
    @Transactional
    public TicketCommentResponse appendComment(Long ticketId, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.COMMON_VALIDATION_ERROR, "Comment content is required");
        }
        TicketEntity ticket = requireTicket(ticketId);
        ticketAccessPolicy.checkCommentAccess(ticket);
        UserEntity author = requireUser(ticketAccessPolicy.currentUserId());

        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content.trim());

        TicketCommentEntity savedComment = ticketCommentRepository.save(comment);
        recordEventInternal(
                ticket,
                TicketEventType.COMMENT_ADDED,
                "Comment added",
                payloadOf(
                        "commentId", savedComment.getId(),
                        "excerpt", excerpt(savedComment.getContent())
                ),
                author
        );
        return ticketDtoMapper.toTicketCommentResponse(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(Long ticketId) {
        TicketEntity ticket = requireTicket(ticketId);
        ticketAccessPolicy.checkReadAccess(ticket);
        return ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(ticketDtoMapper::toTicketCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long ticketId, Long assigneeId, String note) {
        ticketAccessPolicy.checkAssignmentAccess();
        TicketEntity ticket = requireTicket(ticketId);
        UserEntity operator = requireUser(ticketAccessPolicy.currentUserId());
        UserEntity assignee = requireUser(assigneeId);

        if (ticket.getStatus().isTerminal()) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "Cannot assign a ticket in terminal status: " + ticket.getStatus()
            );
        }

        boolean assignableRole = assignee.getRoles().stream()
                .map(role -> role.getCode())
                .anyMatch(role -> role == SystemRole.SUPPORT_AGENT || role == SystemRole.ADMIN);
        if (!assignableRole) {
            throw new BusinessException(
                    ErrorCode.COMMON_CONFLICT,
                    "Assignee must have SUPPORT_AGENT or ADMIN role"
            );
        }

        Long previousAssigneeId = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
        if (Objects.equals(previousAssigneeId, assignee.getId())) {
            throw new BusinessException(ErrorCode.COMMON_CONFLICT, "Ticket is already assigned to this user");
        }

        ticket.assignTo(assignee);
        TicketEntity savedTicket = ticketRepository.saveAndFlush(ticket);
        recordEventInternal(
                savedTicket,
                TicketEventType.ASSIGNED,
                "Assigned to " + assignee.getDisplayName(),
                payloadOf(
                        "previousAssigneeId", previousAssigneeId,
                        "assigneeId", assignee.getId(),
                        "assigneeUsername", assignee.getUsername(),
                        "note", normalize(note)
                ),
                operator
        );
        return ticketDtoMapper.toTicketResponse(savedTicket);
    }

    @Override
    @Transactional
    public TicketEventResponse recordEvent(
            Long ticketId,
            TicketEventType eventType,
            String summary,
            Map<String, Object> payload,
            Long operatorId
    ) {
        TicketEntity ticket = requireTicket(ticketId);
        UserEntity operator = operatorId == null ? null : requireUser(operatorId);
        TicketEventEntity event = recordEventInternal(ticket, eventType, summary, payload, operator);
        return ticketDtoMapper.toTicketEventResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketEventResponse> listEvents(Long ticketId) {
        TicketEntity ticket = requireTicket(ticketId);
        ticketAccessPolicy.checkReadAccess(ticket);
        return ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(ticketDtoMapper::toTicketEventResponse)
                .toList();
    }

    private TicketResponse updateStatusAsSystem(Long ticketId, TicketStatus targetStatus, String summary, String source) {
        TicketEntity ticket = requireTicket(ticketId);
        TicketStatus fromStatus = ticket.getStatus();
        ticket.transitionTo(targetStatus);
        TicketEntity savedTicket = ticketRepository.saveAndFlush(ticket);
        String eventSummary = StringUtils.hasText(summary)
                ? summary.trim()
                : "Status changed from " + fromStatus + " to " + targetStatus;
        recordEventInternal(
                savedTicket,
                TicketEventType.STATUS_CHANGED,
                eventSummary,
                payloadOf(
                        "fromStatus", fromStatus,
                        "toStatus", targetStatus,
                        "source", source
                ),
                null
        );
        return ticketDtoMapper.toTicketResponse(savedTicket);
    }

    private TicketEventEntity recordEventInternal(
            TicketEntity ticket,
            TicketEventType eventType,
            String summary,
            Map<String, ?> payload,
            UserEntity operator
    ) {
        TicketEventEntity event = new TicketEventEntity();
        event.setTicket(ticket);
        event.setEventType(eventType);
        event.setEventSummary(summary);
        ObjectNode payloadNode = objectMapper.createObjectNode();
        if (payload != null) {
            payload.forEach((key, value) -> payloadNode.set(key, objectMapper.valueToTree(value)));
        }
        event.setEventPayload(payloadNode);
        event.setOperator(operator);
        return ticketEventRepository.save(event);
    }

    private TicketEntity requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Ticket not found: " + ticketId));
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "User not found: " + userId));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String excerpt(String content) {
        String normalized = content == null ? "" : content.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

    private Map<String, Object> payloadOf(Object... values) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            payload.put((String) values[index], values[index + 1]);
        }
        return payload;
    }
}

package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.auth.entity.UserEntity;
import com.enterprise.ticketing.ticket.dto.TicketCommentResponse;
import com.enterprise.ticketing.ticket.dto.TicketEventResponse;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import com.enterprise.ticketing.ticket.dto.TicketSummaryResponse;
import com.enterprise.ticketing.ticket.dto.TicketUserSummaryResponse;
import com.enterprise.ticketing.ticket.entity.TicketCommentEntity;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.enterprise.ticketing.ticket.entity.TicketEventEntity;
import org.springframework.stereotype.Component;

@Component
public class TicketDtoMapper {

    public TicketResponse toTicketResponse(TicketEntity ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                toUserSummary(ticket.getRequester()),
                toUserSummary(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketSummaryResponse toTicketSummaryResponse(TicketEntity ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getStatus(),
                toUserSummary(ticket.getRequester()),
                toUserSummary(ticket.getAssignee()),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketCommentResponse toTicketCommentResponse(TicketCommentEntity comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getContent(),
                toUserSummary(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public TicketEventResponse toTicketEventResponse(TicketEventEntity event) {
        return new TicketEventResponse(
                event.getId(),
                event.getEventType(),
                event.getEventSummary(),
                event.getEventPayload(),
                toUserSummary(event.getOperator()),
                event.getCreatedAt()
        );
    }

    private TicketUserSummaryResponse toUserSummary(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new TicketUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartment()
        );
    }
}

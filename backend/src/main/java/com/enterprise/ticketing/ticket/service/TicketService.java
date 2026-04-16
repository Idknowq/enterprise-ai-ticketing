package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.CreateTicketRequest;
import com.enterprise.ticketing.ticket.dto.TicketResponse;

public interface TicketService {

    TicketResponse createTicket(CreateTicketRequest request);

    TicketResponse updateStatus(Long ticketId, TicketStatus targetStatus, String reason);

    TicketResponse markAiProcessing(Long ticketId, String summary);

    TicketResponse markWaitingApproval(Long ticketId, String summary);

    TicketResponse markResolved(Long ticketId, String summary);

    TicketResponse markRejected(Long ticketId, String summary);
}

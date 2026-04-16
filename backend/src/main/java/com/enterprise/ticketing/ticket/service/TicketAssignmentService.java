package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ticket.dto.TicketResponse;

public interface TicketAssignmentService {

    TicketResponse assignTicket(Long ticketId, Long assigneeId, String note);
}

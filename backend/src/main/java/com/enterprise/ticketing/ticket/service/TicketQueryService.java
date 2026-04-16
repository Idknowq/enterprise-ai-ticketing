package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.dto.TicketListQuery;
import com.enterprise.ticketing.ticket.dto.TicketListResponse;

public interface TicketQueryService {

    TicketListResponse listTickets(TicketListQuery query);

    TicketDetailResponse getTicketDetail(Long ticketId);
}

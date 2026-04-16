package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ticket.domain.TicketEventType;
import com.enterprise.ticketing.ticket.dto.TicketEventResponse;
import java.util.List;
import java.util.Map;

public interface TicketEventService {

    TicketEventResponse recordEvent(Long ticketId, TicketEventType eventType, String summary, Map<String, Object> payload, Long operatorId);

    List<TicketEventResponse> listEvents(Long ticketId);
}

package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ticket.dto.TicketCommentResponse;
import java.util.List;

public interface TicketCommentService {

    TicketCommentResponse appendComment(Long ticketId, String content);

    List<TicketCommentResponse> listComments(Long ticketId);
}

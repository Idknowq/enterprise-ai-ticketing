package com.enterprise.ticketing.ticket.service;

import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.ticket.dto.TicketAiDecisionAssessment;

public interface TicketAiDecisionService {

    TicketAiDecisionAssessment assessDecision(AiDecisionResult decisionResult);
}

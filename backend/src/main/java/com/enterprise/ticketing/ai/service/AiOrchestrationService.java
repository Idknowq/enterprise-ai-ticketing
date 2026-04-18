package com.enterprise.ticketing.ai.service;

import com.enterprise.ticketing.ai.dto.AiDecisionResult;
import com.enterprise.ticketing.ai.dto.AiWorkflowRunResponse;
import java.util.List;

public interface AiOrchestrationService {

    AiDecisionResult runForTicket(Long ticketId);

    List<AiWorkflowRunResponse> listRuns(Long ticketId);
}

package com.enterprise.ticketing.approval.service;

import com.enterprise.ticketing.approval.dto.ApprovalStageResponse;
import com.enterprise.ticketing.approval.dto.PendingApprovalResponse;
import java.util.List;

public interface ApprovalQueryService {

    List<PendingApprovalResponse> listPendingApprovals();

    List<ApprovalStageResponse> listTicketApprovals(Long ticketId);
}

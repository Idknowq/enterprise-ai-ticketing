package com.enterprise.ticketing.approval.service;

import com.enterprise.ticketing.approval.dto.ApprovalCommandRequest;
import com.enterprise.ticketing.approval.dto.ApprovalDecisionResponse;

public interface ApprovalCommandService {

    ApprovalDecisionResponse approve(Long approvalId, ApprovalCommandRequest request);

    ApprovalDecisionResponse reject(Long approvalId, ApprovalCommandRequest request);
}

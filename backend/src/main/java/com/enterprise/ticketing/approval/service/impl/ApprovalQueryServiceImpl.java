package com.enterprise.ticketing.approval.service.impl;

import com.enterprise.ticketing.approval.dto.ApprovalStageResponse;
import com.enterprise.ticketing.approval.dto.PendingApprovalResponse;
import com.enterprise.ticketing.approval.entity.ApprovalEntity;
import com.enterprise.ticketing.approval.repository.ApprovalRepository;
import com.enterprise.ticketing.approval.service.ApprovalQueryService;
import com.enterprise.ticketing.auth.context.UserContext;
import com.enterprise.ticketing.auth.domain.SystemRole;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalQueryServiceImpl implements ApprovalQueryService {

    private final ApprovalRepository approvalRepository;
    private final UserContext userContext;
    private final TicketQueryService ticketQueryService;

    public ApprovalQueryServiceImpl(
            ApprovalRepository approvalRepository,
            UserContext userContext,
            TicketQueryService ticketQueryService
    ) {
        this.approvalRepository = approvalRepository;
        this.userContext = userContext;
        this.ticketQueryService = ticketQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingApprovalResponse> listPendingApprovals() {
        boolean admin = userContext.hasRole(SystemRole.ADMIN);
        boolean approver = userContext.hasRole(SystemRole.APPROVER);
        if (!admin && !approver) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN, "Current user cannot view pending approvals");
        }

        Long approverId = admin ? null : userContext.requireCurrentUserId();
        return approvalRepository.findPendingApprovals(com.enterprise.ticketing.approval.domain.ApprovalStatus.PENDING, approverId)
                .stream()
                .map(this::toPendingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalStageResponse> listTicketApprovals(Long ticketId) {
        ticketQueryService.getTicketDetail(ticketId);
        return approvalRepository.findByTicketIdOrderByStageOrderAsc(ticketId)
                .stream()
                .map(this::toStageResponse)
                .toList();
    }

    private PendingApprovalResponse toPendingResponse(ApprovalEntity entity) {
        return new PendingApprovalResponse(
                entity.getId(),
                entity.getTicket().getId(),
                entity.getTicket().getTitle(),
                entity.getTicket().getStatus(),
                entity.getWorkflowId(),
                entity.getAiWorkflowId(),
                entity.getStageOrder(),
                entity.getStageKey().name(),
                entity.getStageKey().getDisplayName(),
                entity.getStatus(),
                entity.getApprover().getId(),
                entity.getApprover().getDisplayName(),
                entity.getRequestedAt()
        );
    }

    private ApprovalStageResponse toStageResponse(ApprovalEntity entity) {
        return new ApprovalStageResponse(
                entity.getId(),
                entity.getWorkflowId(),
                entity.getAiWorkflowId(),
                entity.getStageOrder(),
                entity.getStageKey().name(),
                entity.getStageKey().getDisplayName(),
                entity.getStatus(),
                entity.getApprover().getId(),
                entity.getApprover().getDisplayName(),
                entity.getApprover().getUsername(),
                entity.getComment(),
                entity.getRequestedAt(),
                entity.getDecidedAt()
        );
    }
}

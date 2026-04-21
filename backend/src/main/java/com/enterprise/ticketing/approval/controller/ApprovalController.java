package com.enterprise.ticketing.approval.controller;

import com.enterprise.ticketing.approval.dto.ApprovalCommandRequest;
import com.enterprise.ticketing.approval.dto.ApprovalDecisionResponse;
import com.enterprise.ticketing.approval.dto.ApprovalStageResponse;
import com.enterprise.ticketing.approval.dto.PendingApprovalResponse;
import com.enterprise.ticketing.approval.service.ApprovalCommandService;
import com.enterprise.ticketing.approval.service.ApprovalQueryService;
import com.enterprise.ticketing.common.api.OpenApiResultSchemas;
import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Approval", description = "Approval workflow APIs")
@RestController
@RequestMapping("${app.api-base-path:/api}/approvals")
public class ApprovalController {

    private final ApprovalQueryService approvalQueryService;
    private final ApprovalCommandService approvalCommandService;

    public ApprovalController(
            ApprovalQueryService approvalQueryService,
            ApprovalCommandService approvalCommandService
    ) {
        this.approvalQueryService = approvalQueryService;
        this.approvalCommandService = approvalCommandService;
    }

    @Operation(summary = "List pending approvals", description = "Return pending approvals for the current approver. Admin can view all.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = OpenApiResultSchemas.PendingApprovalListResult.class)))
    @GetMapping("/pending")
    public Result<List<PendingApprovalResponse>> pending() {
        return Result.success(approvalQueryService.listPendingApprovals(), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "List ticket approval stages", description = "Return approval history for a ticket.")
    @GetMapping("/tickets/{ticketId}")
    public Result<List<ApprovalStageResponse>> listTicketApprovals(@PathVariable Long ticketId) {
        return Result.success(approvalQueryService.listTicketApprovals(ticketId), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Approve an approval item", description = "Approve a pending approval and resume the workflow.")
    @PostMapping("/{id}/approve")
    public Result<ApprovalDecisionResponse> approve(
            @PathVariable("id") Long approvalId,
            @Valid @RequestBody(required = false) ApprovalCommandRequest request
    ) {
        ApprovalCommandRequest payload = request == null ? new ApprovalCommandRequest(null, null) : request;
        return Result.success(approvalCommandService.approve(approvalId, payload), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Reject an approval item", description = "Reject a pending approval and terminate the workflow.")
    @PostMapping("/{id}/reject")
    public Result<ApprovalDecisionResponse> reject(
            @PathVariable("id") Long approvalId,
            @Valid @RequestBody(required = false) ApprovalCommandRequest request
    ) {
        ApprovalCommandRequest payload = request == null ? new ApprovalCommandRequest(null, null) : request;
        return Result.success(approvalCommandService.reject(approvalId, payload), TraceIdUtils.currentTraceId());
    }
}

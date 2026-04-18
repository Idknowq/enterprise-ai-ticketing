import type {
  ApprovalCommandRequest,
  ApprovalDecisionResponse,
  ApprovalStageResponse,
  PendingApprovalResponse,
} from "@/types/api";
import { request } from "@/lib/http";

export function listPendingApprovals() {
  return request<PendingApprovalResponse[]>("/approvals/pending");
}

export function listTicketApprovals(ticketId: number) {
  return request<ApprovalStageResponse[]>(`/approvals/tickets/${ticketId}`);
}

export function approveApproval(approvalId: number, payload?: ApprovalCommandRequest) {
  return request<ApprovalDecisionResponse>(`/approvals/${approvalId}/approve`, {
    method: "POST",
    body: payload || {},
  });
}

export function rejectApproval(approvalId: number, payload?: ApprovalCommandRequest) {
  return request<ApprovalDecisionResponse>(`/approvals/${approvalId}/reject`, {
    method: "POST",
    body: payload || {},
  });
}

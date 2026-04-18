import { request } from "@/lib/http";
import type { AiDecisionResult, AiWorkflowRunResponse } from "@/types/api";

export function runTicketAi(ticketId: number) {
  return request<AiDecisionResult>(`/ai/tickets/${ticketId}/run`, {
    method: "POST",
  });
}

export function listTicketAiRuns(ticketId: number) {
  return request<AiWorkflowRunResponse[]>(`/ai/tickets/${ticketId}/runs`);
}


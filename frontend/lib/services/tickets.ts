import { buildQuery, request } from "@/lib/http";
import type {
  AppendTicketCommentRequest,
  AssignTicketRequest,
  CreateTicketRequest,
  TicketDetailResponse,
  TicketListQuery,
  TicketListResponse,
  TicketResponse,
  UpdateTicketStatusRequest,
} from "@/types/api";

export function listTickets(query: TicketListQuery) {
  return request<TicketListResponse>(`/tickets${buildQuery(query)}`);
}

export function createTicket(payload: CreateTicketRequest) {
  return request<TicketResponse>("/tickets", {
    method: "POST",
    body: payload,
  });
}

export function getTicketDetail(ticketId: number) {
  return request<TicketDetailResponse>(`/tickets/${ticketId}`);
}

export function appendTicketComment(ticketId: number, payload: AppendTicketCommentRequest) {
  return request(`/tickets/${ticketId}/comments`, {
    method: "POST",
    body: payload,
  });
}

export function assignTicket(ticketId: number, payload: AssignTicketRequest) {
  return request<TicketResponse>(`/tickets/${ticketId}/assign`, {
    method: "POST",
    body: payload,
  });
}

export function updateTicketStatus(ticketId: number, payload: UpdateTicketStatusRequest) {
  return request<TicketResponse>(`/tickets/${ticketId}/status`, {
    method: "POST",
    body: payload,
  });
}


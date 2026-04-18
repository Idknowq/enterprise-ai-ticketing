import { request } from "@/lib/http";
import type { DashboardMetricsResponse, MonitoringOverview, TicketStatus } from "@/types/api";

const statusOrder: TicketStatus[] = [
  "OPEN",
  "AI_PROCESSING",
  "WAITING_APPROVAL",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
  "REJECTED",
];

export async function getMonitoringOverview() {
  const metrics = await request<DashboardMetricsResponse>("/observability/dashboard");
  const total = metrics.totalTickets;

  const statusDistribution = statusOrder
    .map((status) => ({
      status,
      count: metrics.ticketStatusDistribution[status] || 0,
      ratio: total ? ((metrics.ticketStatusDistribution[status] || 0) / total) * 100 : 0,
    }))
    .filter((item) => item.count > 0);

  const overview: MonitoringOverview = {
    ticketTotal: total,
    aiSuccessRate: metrics.aiSuggestionSuccessRate,
    averageModelLatencyMs: metrics.averageAiLatencyMs,
    averageRetrievalLatencyMs: metrics.averageRetrievalLatencyMs,
    averageApprovalWaitMs: metrics.averageApprovalWaitMs,
    pendingApprovals: metrics.pendingApprovals,
    workflowFailureCount: metrics.workflowFailureCount,
    workflowRetryCount: metrics.workflowRetryCount,
    statusDistribution,
    derivedFrom: "后端 /api/observability/dashboard 聚合指标",
  };
  return { overview };
}

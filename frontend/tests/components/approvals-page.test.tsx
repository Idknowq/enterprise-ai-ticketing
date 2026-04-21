import { App as AntApp } from "antd";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ApprovalsPage from "@/app/(console)/approvals/page";
import {
  approveApproval,
  listPendingApprovals,
  rejectApproval,
} from "@/lib/services/approvals";

vi.mock("@/components/app-provider", () => ({
  useAuth: () => ({
    user: {
      id: 3,
      username: "approver01",
      displayName: "Approver",
      department: "IT",
      roles: ["APPROVER"],
    },
  }),
}));

vi.mock("@/lib/services/approvals", () => ({
  listPendingApprovals: vi.fn(),
  approveApproval: vi.fn(),
  rejectApproval: vi.fn(),
}));

describe("ApprovalsPage", () => {
  beforeEach(() => {
    vi.mocked(listPendingApprovals).mockReset();
    vi.mocked(approveApproval).mockReset();
    vi.mocked(rejectApproval).mockReset();
  });

  it("loads pending approvals and submits an approve decision", async () => {
    vi.mocked(listPendingApprovals).mockResolvedValue([pendingApproval()]);
    vi.mocked(approveApproval).mockResolvedValue(decisionResponse("APPROVED"));

    render(
      <AntApp>
        <ApprovalsPage />
      </AntApp>,
    );

    expect(await screen.findByText("生产只读权限申请")).toBeDefined();
    fireEvent.click(screen.getByTestId("approve-ticket-301"));
    fireEvent.change(screen.getByTestId("approval-comment-input"), {
      target: { value: "同意上线验证" },
    });
    fireEvent.click(screen.getByTestId("approval-submit-button"));

    await waitFor(() => {
      expect(approveApproval).toHaveBeenCalledWith(
        701,
        expect.objectContaining({
          comment: "同意上线验证",
          requestId: expect.stringMatching(/^approve-701-/),
        }),
      );
    });
  });

  it("submits a reject decision from the modal", async () => {
    vi.mocked(listPendingApprovals).mockResolvedValue([pendingApproval()]);
    vi.mocked(rejectApproval).mockResolvedValue(decisionResponse("REJECTED"));

    render(
      <AntApp>
        <ApprovalsPage />
      </AntApp>,
    );

    expect(await screen.findByText("生产只读权限申请")).toBeDefined();
    fireEvent.click(screen.getByTestId("reject-ticket-301"));
    fireEvent.change(screen.getByTestId("approval-comment-input"), {
      target: { value: "权限范围过大" },
    });
    fireEvent.click(screen.getByTestId("approval-submit-button"));

    await waitFor(() => {
      expect(rejectApproval).toHaveBeenCalledWith(
        701,
        expect.objectContaining({
          comment: "权限范围过大",
          requestId: expect.stringMatching(/^reject-701-/),
        }),
      );
    });
  });
});

function pendingApproval() {
  return {
    approvalId: 701,
    ticketId: 301,
    ticketTitle: "生产只读权限申请",
    ticketStatus: "WAITING_APPROVAL" as const,
    workflowId: "approval-ticket-301",
    aiWorkflowId: "ai-301",
    stageOrder: 1,
    stageKey: "manager",
    stageDisplayName: "直属主管审批",
    approvalStatus: "PENDING" as const,
    approverId: 3,
    approverName: "Approver",
    requestedAt: "2026-04-19T12:00:00Z",
  };
}

function decisionResponse(status: "APPROVED" | "REJECTED") {
  return {
    id: 701,
    ticketId: 301,
    workflowId: "approval-ticket-301",
    stageOrder: 1,
    stageKey: "manager",
    status,
    comment: status === "APPROVED" ? "同意上线验证" : "权限范围过大",
    idempotent: false,
    requestedAt: "2026-04-19T12:00:00Z",
    decidedAt: "2026-04-19T12:05:00Z",
  };
}

import { App as AntApp } from "antd";
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TicketDetailPage from "@/app/(console)/tickets/[id]/page";
import { listTicketApprovals } from "@/lib/services/approvals";
import { listTicketAiRuns, runTicketAi } from "@/lib/services/ai";
import { listDocumentCategories, searchKnowledge } from "@/lib/services/documents";
import {
  appendTicketComment,
  assignTicket,
  getTicketDetail,
  updateTicketStatus,
} from "@/lib/services/tickets";

vi.mock("next/navigation", () => ({
  useParams: () => ({ id: "101" }),
}));

vi.mock("@/components/app-provider", () => ({
  useAuth: () => ({
    user: {
      id: 1,
      username: "employee01",
      displayName: "Employee One",
      department: "IT",
      roles: ["EMPLOYEE"],
    },
  }),
}));

vi.mock("@/lib/services/approvals", () => ({
  listTicketApprovals: vi.fn(),
}));

vi.mock("@/lib/services/ai", () => ({
  listTicketAiRuns: vi.fn(),
  runTicketAi: vi.fn(),
}));

vi.mock("@/lib/services/documents", () => ({
  listDocumentCategories: vi.fn(),
  searchKnowledge: vi.fn(),
}));

vi.mock("@/lib/services/tickets", () => ({
  appendTicketComment: vi.fn(),
  assignTicket: vi.fn(),
  getTicketDetail: vi.fn(),
  updateTicketStatus: vi.fn(),
}));

describe("TicketDetailPage", () => {
  beforeEach(() => {
    vi.mocked(getTicketDetail).mockReset();
    vi.mocked(appendTicketComment).mockReset();
    vi.mocked(assignTicket).mockReset();
    vi.mocked(updateTicketStatus).mockReset();
    vi.mocked(listTicketApprovals).mockReset();
    vi.mocked(listTicketAiRuns).mockReset();
    vi.mocked(runTicketAi).mockReset();
    vi.mocked(listDocumentCategories).mockReset();
    vi.mocked(listDocumentCategories).mockResolvedValue([
      {
        code: "REMOTE_ACCESS",
        displayName: "远程访问 / VPN",
        description: "VPN 证书失效、远程办公连接失败、客户端配置",
      },
    ]);
    vi.mocked(searchKnowledge).mockReset();
  });

  it("does not create disconnected management forms for employee users", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
    vi.mocked(getTicketDetail).mockResolvedValue(ticketDetail());
    vi.mocked(listTicketAiRuns).mockResolvedValue([]);
    vi.mocked(listTicketApprovals).mockResolvedValue([]);

    render(
      <AntApp>
        <TicketDetailPage />
      </AntApp>,
    );

    expect(await screen.findByRole("heading", { name: "VPN certificate expired" })).toBeDefined();
    expect(screen.getByText("当前账号为只读/协作视角，状态更新与指派动作仅对支持人员或管理员开放。")).toBeDefined();

    await waitFor(() => {
      expect(
        consoleError.mock.calls.some((call) =>
          call.some((value) => String(value).includes("Instance created by `useForm` is not connected")),
        ),
      ).toBe(false);
    });

    consoleError.mockRestore();
  });
});

function ticketDetail() {
  return {
    ticket: {
      id: 101,
      title: "VPN certificate expired",
      description: "VPN client reports certificate expired.",
      category: "REMOTE_ACCESS" as const,
      priority: "HIGH" as const,
      status: "OPEN" as const,
      requester: user(1, "employee01"),
      assignee: null,
      createdAt: "2026-04-19T12:00:00Z",
      updatedAt: "2026-04-19T12:30:00Z",
    },
    comments: [],
    timeline: [
      {
        id: 1,
        eventType: "CREATED" as const,
        summary: "工单已创建",
        payload: null,
        operator: user(1, "employee01"),
        createdAt: "2026-04-19T12:00:00Z",
      },
    ],
  };
}

function user(id: number, username: string) {
  return {
    id,
    username,
    displayName: username.toUpperCase(),
    department: "IT",
  };
}

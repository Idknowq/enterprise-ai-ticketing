import { App as AntApp } from "antd";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import TicketsPage from "@/app/(console)/tickets/page";
import { listDocumentCategories } from "@/lib/services/documents";
import { createTicket, listTickets } from "@/lib/services/tickets";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("@/lib/services/tickets", () => ({
  listTickets: vi.fn(),
  createTicket: vi.fn(),
}));

vi.mock("@/lib/services/documents", () => ({
  listDocumentCategories: vi.fn(),
}));

describe("TicketsPage", () => {
  beforeEach(() => {
    push.mockReset();
    vi.mocked(listDocumentCategories).mockReset();
    vi.mocked(listDocumentCategories).mockResolvedValue([
      {
        code: "REMOTE_ACCESS",
        displayName: "远程访问 / VPN",
        description: "VPN 证书失效、远程办公连接失败、客户端配置",
      },
    ]);
    vi.mocked(listTickets).mockReset();
    vi.mocked(createTicket).mockReset();
  });

  it("loads the ticket list and opens an existing ticket link", async () => {
    vi.mocked(listTickets).mockResolvedValue({
      items: [ticketSummary()],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });

    render(
      <AntApp>
        <TicketsPage />
      </AntApp>,
    );

    expect(await screen.findByText("VPN certificate expired")).toBeDefined();
    expect(screen.getByText("#101")).toBeDefined();
    expect(screen.getByRole("link", { name: "VPN certificate expired" }).getAttribute("href")).toBe("/tickets/101");
  });

  it("creates a ticket and navigates to its detail page", async () => {
    vi.mocked(listTickets).mockResolvedValue({
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });
    vi.mocked(createTicket).mockResolvedValue(ticketResponse(202));

    render(
      <AntApp>
        <TicketsPage />
      </AntApp>,
    );

    await screen.findByText("工单列表");
    fireEvent.click(screen.getByRole("button", { name: /新建工单/ }));
    fireEvent.change(screen.getByPlaceholderText("例如：VPN 证书失效，无法远程办公"), {
      target: { value: "VPN certificate expired" },
    });
    fireEvent.change(screen.getByPlaceholderText("请填写问题现象、影响范围、复现时间、已尝试动作等"), {
      target: { value: "VPN client reports certificate expired." },
    });
    fireEvent.click(screen.getByRole("button", { name: "提交工单" }));

    await waitFor(() => {
      expect(createTicket).toHaveBeenCalledWith({
        title: "VPN certificate expired",
        description: "VPN client reports certificate expired.",
        priority: "MEDIUM",
      });
      expect(push).toHaveBeenCalledWith("/tickets/202");
    });
  });
});

function ticketSummary() {
  return {
    id: 101,
    title: "VPN certificate expired",
    category: "REMOTE_ACCESS" as const,
    priority: "HIGH" as const,
    status: "OPEN" as const,
    requester: user(1, "alice"),
    assignee: null,
    createdAt: "2026-04-19T12:00:00Z",
    updatedAt: "2026-04-19T12:30:00Z",
  };
}

function ticketResponse(id: number) {
  return {
    id,
    title: "VPN certificate expired",
    description: "VPN client reports certificate expired.",
    category: "REMOTE_ACCESS" as const,
    priority: "MEDIUM" as const,
    status: "OPEN" as const,
    requester: user(1, "alice"),
    assignee: null,
    createdAt: "2026-04-19T12:00:00Z",
    updatedAt: "2026-04-19T12:30:00Z",
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

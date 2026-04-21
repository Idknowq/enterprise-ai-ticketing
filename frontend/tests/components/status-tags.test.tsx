import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  ApprovalStatusTag,
  PriorityTag,
  RoleTags,
  TicketStatusTag,
} from "@/components/status-tags";

describe("status-tags", () => {
  it("renders localized ticket status", () => {
    render(<TicketStatusTag value="WAITING_APPROVAL" />);

    expect(screen.getByText("待审批")).toBeDefined();
  });

  it("renders fallback text for missing priority", () => {
    render(<PriorityTag value={null} />);

    expect(screen.getByText("未设置")).toBeDefined();
  });

  it("renders approval and role labels", () => {
    render(
      <div>
        <ApprovalStatusTag value="APPROVED" />
        <RoleTags roles={["APPROVER", "ADMIN"]} />
      </div>,
    );

    expect(screen.getByText("已通过")).toBeDefined();
    expect(screen.getByText("审批人")).toBeDefined();
    expect(screen.getByText("管理员")).toBeDefined();
  });
});

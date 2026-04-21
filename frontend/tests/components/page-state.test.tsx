import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { InlineEmpty, PageError, PageLoading } from "@/components/page-state";

describe("page-state components", () => {
  it("renders loading tip", () => {
    render(<PageLoading tip="正在同步工单..." />);

    expect(screen.getByText("正在同步工单...")).toBeDefined();
  });

  it("renders error message and triggers retry callback", () => {
    const onRetry = vi.fn();

    render(<PageError title="加载失败" message="接口超时" onRetry={onRetry} />);

    fireEvent.click(screen.getByRole("button", { name: "重新加载" }));

    expect(screen.getByText("加载失败")).toBeDefined();
    expect(screen.getByText("接口超时")).toBeDefined();
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("renders inline empty action", () => {
    render(<InlineEmpty description="没有待审批记录" action={<button type="button">去创建</button>} />);

    expect(screen.getByText("没有待审批记录")).toBeDefined();
    expect(screen.getByRole("button", { name: "去创建" })).toBeDefined();
  });
});

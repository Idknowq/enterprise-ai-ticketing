import { App as AntApp } from "antd";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import LoginPage from "@/app/(auth)/login/page";

const replace = vi.fn();
const login = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/components/app-provider", () => ({
  useAuth: () => ({
    login,
    user: null,
    authLoading: false,
  }),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    replace.mockReset();
    login.mockReset();
  });

  it("logs in with the demo account and redirects to tickets", async () => {
    login.mockResolvedValue({
      id: 1,
      username: "admin01",
      displayName: "Admin",
      department: "IT",
      roles: ["ADMIN"],
    });

    render(
      <AntApp>
        <LoginPage />
      </AntApp>,
    );

    fireEvent.click(screen.getByRole("button", { name: "登录控制台" }));

    await waitFor(() => {
      expect(login).toHaveBeenCalledWith({
        username: "admin01",
        password: "ChangeMe123!",
      });
      expect(replace).toHaveBeenCalledWith("/tickets");
    });
  });
});

import { ApiError, buildQuery, request } from "@/lib/http";
import { emitAuthExpired, getStoredSession } from "@/lib/auth-storage";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/auth-storage", () => ({
  emitAuthExpired: vi.fn(),
  getStoredSession: vi.fn(),
}));

describe("http helpers", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.mocked(getStoredSession).mockReset();
    vi.mocked(emitAuthExpired).mockReset();
  });

  it("builds query strings without empty values", () => {
    expect(
      buildQuery({
        page: 0,
        size: 10,
        keyword: "",
        status: null,
        category: "VPN",
      }),
    ).toBe("?page=0&size=10&category=VPN");
  });

  it("sends bearer token and returns envelope data", async () => {
    vi.mocked(getStoredSession).mockReturnValue({
      accessToken: "demo-token",
      expiresAt: "2026-04-19T12:00:00Z",
    });

    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          code: "COMMON_SUCCESS",
          message: "Success",
          data: { items: [] },
          timestamp: "2026-04-19T12:00:00Z",
          traceId: "trace-1",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const payload = await request<{ items: unknown[] }>("/tickets");

    expect(payload).toEqual({ items: [] });
    expect(fetchSpy).toHaveBeenCalledWith(
      "/backend-api/tickets",
      expect.objectContaining({
        method: "GET",
        cache: "no-store",
        headers: expect.any(Headers),
      }),
    );

    const headers = fetchSpy.mock.calls[0]?.[1]?.headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer demo-token");
  });

  it("throws network error when fetch fails", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));

    await expect(request("/tickets")).rejects.toMatchObject({
      name: "ApiError",
      code: "NETWORK_ERROR",
      status: 0,
    });
  });

  it("emits auth-expired event for unauthorized responses", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: false,
          code: "COMMON_UNAUTHORIZED",
          message: "Unauthorized",
          data: null,
          timestamp: "2026-04-19T12:00:00Z",
          traceId: "trace-401",
        }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      ),
    );

    await expect(request("/tickets")).rejects.toBeInstanceOf(ApiError);
    expect(emitAuthExpired).toHaveBeenCalledTimes(1);
  });
});

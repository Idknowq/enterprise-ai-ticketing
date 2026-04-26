import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { login } from "@/lib/services/auth";
import { approveApproval } from "@/lib/services/approvals";
import { listTickets } from "@/lib/services/tickets";

const requests: Array<{
  path: string;
  body?: unknown;
  searchParams?: URLSearchParams;
}> = [];

const server = setupServer(
  http.post("/backend-api/auth/login", async ({ request }) => {
    requests.push({
      path: new URL(request.url).pathname,
      body: await request.json(),
    });

    return HttpResponse.json(successEnvelope({
      accessToken: "jwt-token",
      tokenType: "Bearer",
      expiresInSeconds: 3600,
      expiresAt: "2026-04-19T13:00:00Z",
      user: {
        id: 1,
        username: "admin01",
        displayName: "Admin",
        department: "IT",
        roles: ["ADMIN"],
      },
    }));
  }),
  http.get("/backend-api/tickets", ({ request }) => {
    const url = new URL(request.url);
    requests.push({
      path: url.pathname,
      searchParams: url.searchParams,
    });

    return HttpResponse.json(successEnvelope({
      items: [],
      page: Number(url.searchParams.get("page")),
      size: Number(url.searchParams.get("size")),
      totalElements: 0,
      totalPages: 0,
    }));
  }),
  http.post("/backend-api/approvals/:approvalId/approve", async ({ params, request }) => {
    requests.push({
      path: `/backend-api/approvals/${params.approvalId}/approve`,
      body: await request.json(),
    });

    return HttpResponse.json(successEnvelope({
      id: Number(params.approvalId),
      ticketId: 301,
      workflowId: "approval-ticket-301",
      stageOrder: 1,
      stageKey: "manager",
      status: "APPROVED",
      comment: "同意",
      idempotent: false,
      requestedAt: "2026-04-19T12:00:00Z",
      decidedAt: "2026-04-19T12:05:00Z",
    }));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  requests.length = 0;
  server.resetHandlers();
});
afterAll(() => server.close());

describe("service layer with MSW", () => {
  it("posts login credentials to the backend auth endpoint", async () => {
    const response = await login({
      username: "admin01",
      password: "ChangeMe123!",
    });

    expect(response.accessToken).toBe("jwt-token");
    expect(requests[0]).toMatchObject({
      path: "/backend-api/auth/login",
      body: {
        username: "admin01",
        password: "ChangeMe123!",
      },
    });
  });

  it("serializes ticket list filters into query params", async () => {
    await listTickets({
      page: 1,
      size: 10,
      keyword: "vpn",
      status: "OPEN",
      category: "REMOTE_ACCESS",
    });

    expect(requests[0]?.path).toBe("/backend-api/tickets");
    expect(requests[0]?.searchParams?.get("page")).toBe("1");
    expect(requests[0]?.searchParams?.get("size")).toBe("10");
    expect(requests[0]?.searchParams?.get("keyword")).toBe("vpn");
    expect(requests[0]?.searchParams?.get("status")).toBe("OPEN");
    expect(requests[0]?.searchParams?.get("category")).toBe("REMOTE_ACCESS");
  });

  it("posts approval decisions with idempotency request id", async () => {
    await approveApproval(701, {
      comment: "同意",
      requestId: "approve-701-test",
    });

    expect(requests[0]).toMatchObject({
      path: "/backend-api/approvals/701/approve",
      body: {
        comment: "同意",
        requestId: "approve-701-test",
      },
    });
  });
});

function successEnvelope<T>(data: T) {
  return {
    success: true,
    code: "COMMON_SUCCESS",
    message: "Success",
    data,
    timestamp: "2026-04-19T12:00:00Z",
  };
}

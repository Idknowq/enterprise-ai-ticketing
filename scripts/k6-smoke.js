import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: Number(__ENV.K6_VUS || "2"),
  duration: __ENV.K6_DURATION || "20s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1000"],
  },
};

const baseUrl = __ENV.BASE_URL || "http://127.0.0.1:8080";

export function setup() {
  const response = http.post(
    `${baseUrl}/api/auth/login`,
    JSON.stringify({
      username: __ENV.K6_USERNAME || "admin01",
      password: __ENV.K6_PASSWORD || "ChangeMe123!",
    }),
    { headers: { "Content-Type": "application/json" } },
  );

  check(response, {
    "login status is 200": (res) => res.status === 200,
    "login returns token": (res) => Boolean(res.json("data.accessToken")),
  });

  return {
    token: response.json("data.accessToken"),
  };
}

export default function (data) {
  const authHeaders = {
    headers: {
      Authorization: `Bearer ${data.token}`,
    },
  };

  const platform = http.get(`${baseUrl}/api/platform/info`);
  check(platform, {
    "platform info is 200": (res) => res.status === 200,
    "platform info succeeds": (res) => res.json("success") === true,
  });

  const tickets = http.get(`${baseUrl}/api/tickets?page=0&size=10`, authHeaders);
  check(tickets, {
    "ticket list is 200": (res) => res.status === 200,
    "ticket list succeeds": (res) => res.json("success") === true,
  });

  const approvals = http.get(`${baseUrl}/api/approvals/pending`, authHeaders);
  check(approvals, {
    "pending approvals is 200": (res) => res.status === 200,
    "pending approvals succeeds": (res) => res.json("success") === true,
  });

  const dashboard = http.get(`${baseUrl}/api/observability/dashboard`, authHeaders);
  check(dashboard, {
    "dashboard is 200": (res) => res.status === 200,
    "dashboard succeeds": (res) => res.json("success") === true,
  });

  sleep(1);
}

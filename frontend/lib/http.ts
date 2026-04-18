import { emitAuthExpired, getStoredSession } from "@/lib/auth-storage";
import type { ResultEnvelope } from "@/types/api";

export class ApiError extends Error {
  code: string;
  status: number;
  traceId?: string;

  constructor(message: string, code: string, status: number, traceId?: string) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
    this.traceId = traceId;
  }
}

export function getApiBaseUrl() {
  return process.env.NEXT_PUBLIC_API_BASE_URL || "/backend-api";
}

export function buildQuery(params: object) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    search.set(key, String(value));
  });

  const query = search.toString();
  return query ? `?${query}` : "";
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: BodyInit | object;
  token?: string | null;
  isFormData?: boolean;
}

export async function request<T>(path: string, options: RequestOptions = {}) {
  const token = options.token ?? getStoredSession()?.accessToken ?? null;
  const url = `${getApiBaseUrl()}${path}`;
  const headers = new Headers();

  if (!options.isFormData) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  let response: Response;
  try {
    response = await fetch(url, {
      method: options.method || "GET",
      headers,
      body:
        options.body === undefined
          ? undefined
          : options.isFormData
            ? (options.body as BodyInit)
            : JSON.stringify(options.body),
      cache: "no-store",
    });
  } catch {
    throw new ApiError("无法连接后端服务，请先启动 backend 并确认 8080 端口可访问。", "NETWORK_ERROR", 0);
  }

  let payload: ResultEnvelope<T> | null = null;

  try {
    payload = (await response.json()) as ResultEnvelope<T>;
  } catch {
    payload = null;
  }

  if (!response.ok || !payload?.success) {
    const message = payload?.message || `Request failed with status ${response.status}`;
    const code = payload?.code || "HTTP_ERROR";
    const traceId = payload?.traceId;

    if (response.status === 401) {
      emitAuthExpired();
    }

    throw new ApiError(message, code, response.status, traceId);
  }

  return payload.data;
}

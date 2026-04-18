import { request } from "@/lib/http";
import type { CurrentUserResponse, LoginRequest, LoginResponse } from "@/types/api";

export function login(payload: LoginRequest) {
  return request<LoginResponse>("/auth/login", {
    method: "POST",
    body: payload,
    token: null,
  });
}

export function getCurrentUser(token?: string | null) {
  return request<CurrentUserResponse>("/auth/me", { token });
}


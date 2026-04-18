import { request } from "@/lib/http";
import type { PlatformInfoResponse } from "@/types/api";

export function getPlatformInfo() {
  return request<PlatformInfoResponse>("/platform/info", {
    token: null,
  });
}


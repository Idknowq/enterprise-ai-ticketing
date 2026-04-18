import { buildQuery, request } from "@/lib/http";
import type {
  DocumentListQuery,
  DocumentListResponse,
  DocumentResponse,
  DocumentUploadPayload,
  RetrievalSearchRequest,
  RetrievalSearchResponse,
} from "@/types/api";

export function listDocuments(query: DocumentListQuery) {
  return request<DocumentListResponse>(`/documents${buildQuery(query)}`);
}

export function uploadDocument(payload: DocumentUploadPayload) {
  const formData = new FormData();
  formData.set("file", payload.file);
  formData.set("category", payload.category);
  formData.set("accessLevel", payload.accessLevel);
  formData.set("version", payload.version);
  formData.set("updatedAt", payload.updatedAt);

  if (payload.title) {
    formData.set("title", payload.title);
  }

  if (payload.department) {
    formData.set("department", payload.department);
  }

  return request<DocumentResponse>("/documents/upload", {
    method: "POST",
    body: formData,
    isFormData: true,
  });
}

export function searchKnowledge(payload: RetrievalSearchRequest) {
  return request<RetrievalSearchResponse>("/retrieval/search", {
    method: "POST",
    body: payload,
  });
}

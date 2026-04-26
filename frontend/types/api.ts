export type Role = "EMPLOYEE" | "SUPPORT_AGENT" | "APPROVER" | "ADMIN";

export type TicketStatus =
  | "OPEN"
  | "AI_PROCESSING"
  | "WAITING_APPROVAL"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "REJECTED";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export type TicketEventType =
  | "CREATED"
  | "STATUS_CHANGED"
  | "COMMENT_ADDED"
  | "ASSIGNED"
  | "WORKFLOW_STARTED"
  | "WORKFLOW_RESUMED"
  | "WORKFLOW_COMPLETED"
  | "AI_REVIEW_REQUIRED"
  | "APPROVAL_REQUESTED"
  | "APPROVAL_APPROVED"
  | "APPROVAL_REJECTED";

export type KnowledgeAccessLevel = "PUBLIC" | "INTERNAL" | "RESTRICTED" | "CONFIDENTIAL";
export type KnowledgeDocumentCategory =
  | "REMOTE_ACCESS"
  | "IDENTITY_ACCOUNT"
  | "PASSWORD_MFA"
  | "ACCESS_REQUEST"
  | "EMAIL_COLLABORATION"
  | "DEVICE_HARDWARE"
  | "OPERATING_SYSTEM"
  | "SOFTWARE_APPLICATION"
  | "NETWORK_CONNECTIVITY"
  | "SECURITY_INCIDENT"
  | "DATA_BACKUP_RECOVERY"
  | "CLOUD_INFRASTRUCTURE"
  | "DATABASE_DATA_PLATFORM"
  | "DEV_ENGINEERING"
  | "ITSM_PROCESS"
  | "ASSET_PROCUREMENT"
  | "CHANGE_RELEASE"
  | "POLICY_COMPLIANCE"
  | "GENERAL_FAQ"
  | "OTHER";
export type DocumentIndexStatus = "PENDING" | "INDEXED" | "FAILED";
export type KnowledgeDocumentType = "MARKDOWN" | "PDF" | "TXT";
export type AiRunStatus = "SUCCESS" | "FAILED";
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface ResultEnvelope<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
  traceId?: string;
}

export interface CurrentUserResponse {
  id: number;
  username: string;
  displayName: string;
  department: string;
  roles: Role[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  expiresAt: string;
  user: CurrentUserResponse;
}

export interface TicketUserSummaryResponse {
  id: number;
  username: string;
  displayName: string;
  department: string;
}

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  category: KnowledgeDocumentCategory | null;
  priority: TicketPriority | null;
  status: TicketStatus;
  requester: TicketUserSummaryResponse;
  assignee: TicketUserSummaryResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketCommentResponse {
  id: number;
  content: string;
  author: TicketUserSummaryResponse;
  createdAt: string;
  updatedAt: string;
}

export interface TicketEventResponse {
  id: number;
  eventType: TicketEventType;
  summary: string;
  payload: Record<string, unknown> | null;
  operator: TicketUserSummaryResponse | null;
  createdAt: string;
}

export interface TicketDetailResponse {
  ticket: TicketResponse;
  comments: TicketCommentResponse[];
  timeline: TicketEventResponse[];
}

export interface TicketSummaryResponse {
  id: number;
  title: string;
  category: KnowledgeDocumentCategory | null;
  priority: TicketPriority | null;
  status: TicketStatus;
  requester: TicketUserSummaryResponse;
  assignee: TicketUserSummaryResponse | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketListResponse {
  items: TicketSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TicketListQuery {
  keyword?: string;
  status?: TicketStatus;
  priority?: TicketPriority;
  category?: KnowledgeDocumentCategory;
  requesterId?: number;
  assigneeId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "ASC" | "DESC";
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  category?: KnowledgeDocumentCategory;
  priority?: TicketPriority;
}

export interface AppendTicketCommentRequest {
  content: string;
}

export interface AssignTicketRequest {
  assigneeId: number;
  note?: string;
}

export interface UpdateTicketStatusRequest {
  status: TicketStatus;
  reason?: string;
}

export interface DocumentMetadataResponse {
  docId: number;
  title: string;
  category: KnowledgeDocumentCategory;
  department: string;
  accessLevel: KnowledgeAccessLevel;
  version: string;
  updatedAt: string;
}

export interface DocumentResponse {
  id: number;
  title: string;
  sourceFilename: string;
  documentType: KnowledgeDocumentType;
  indexStatus: DocumentIndexStatus;
  chunkCount: number;
  embeddingModel: string;
  metadata: DocumentMetadataResponse;
  lastIndexedAt: string | null;
  createdAt: string;
}

export interface DocumentListResponse {
  items: DocumentResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DocumentListQuery {
  keyword?: string;
  category?: KnowledgeDocumentCategory;
  department?: string;
  accessLevel?: KnowledgeAccessLevel;
  indexStatus?: DocumentIndexStatus;
  page?: number;
  size?: number;
}

export interface DocumentUploadPayload {
  file: File;
  title?: string;
  category: KnowledgeDocumentCategory;
  department?: string;
  accessLevel: KnowledgeAccessLevel;
  version: string;
  updatedAt: string;
}

export interface RetrievalSearchRequest {
  query?: string;
  ticketId?: number;
  ticketContext?: string;
  category?: KnowledgeDocumentCategory;
  department?: string;
  accessLevel?: KnowledgeAccessLevel;
  limit?: number;
  saveCitations?: boolean;
  aiRunId?: string;
}

export interface RetrievalResultItemResponse {
  docId: number;
  title: string;
  chunkId: string;
  contentSnippet: string;
  score: number;
  retrievalScore?: number | null;
  rerankScore?: number | null;
  sourceRef?: string | null;
  metadata: DocumentMetadataResponse;
  metadataMap?: Record<string, unknown> | null;
  whyMatched: string;
  citationId: number | null;
}

export interface RetrievalSearchResponse {
  query: string;
  ticketId: number | null;
  diagnostics?: AiRetrievalDiagnostics | null;
  results: RetrievalResultItemResponse[];
}

export interface DocumentCategoryOptionResponse {
  code: KnowledgeDocumentCategory;
  displayName: string;
  description: string;
}

export interface AiCitation {
  sourceType: string;
  documentId: number | null;
  chunkId: string | null;
  title: string;
  snippet: string;
  score: number | null;
  retrievalScore?: number | null;
  rerankScore?: number | null;
  sourceRef: string | null;
  metadata?: Record<string, unknown> | null;
}

export interface AiRetrievalDiagnostics {
  retrievalMode?: string | null;
  candidateCount?: number | null;
  returnedCount?: number | null;
  filterSummary?: Record<string, unknown> | null;
  message?: string | null;
}

export interface AiDecisionResult {
  schemaVersion?: string | null;
  workflowId: string;
  ticketId: number;
  category: KnowledgeDocumentCategory;
  priority: TicketPriority | null;
  confidence: number;
  providerType?: string | null;
  modelName?: string | null;
  analysisMode?: string | null;
  fallbackUsed?: boolean;
  fallbackReason?: string | null;
  requiresApproval: boolean;
  needsHumanHandoff: boolean;
  draftReply: string;
  suggestedActions: string[];
  extractedFields: Record<string, string>;
  citations: AiCitation[];
  retrievalStatus?: string | null;
  retrievalDiagnostics?: AiRetrievalDiagnostics | null;
  generatedAt: string;
}

export interface AiNodeRunResponse {
  id: number;
  workflowId: string;
  nodeName: string;
  status: AiRunStatus;
  providerType?: string | null;
  modelName: string;
  latencyMs: number;
  tokenInput: number;
  tokenOutput: number;
  fallbackUsed?: boolean;
  fallbackReason?: string | null;
  retrievalStatus?: string | null;
  resultSummary: string;
  resultPayload: Record<string, unknown> | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface AiWorkflowRunResponse {
  workflowId: string;
  status: AiRunStatus;
  startedAt: string;
  finalDecision: AiDecisionResult | null;
  nodes: AiNodeRunResponse[];
}

export interface PlatformInfoResponse {
  application: string;
  version: string;
  activeProfiles: string[];
  apiBasePath: string;
  modules: Record<string, boolean>;
}

export interface PendingApprovalResponse {
  approvalId: number;
  ticketId: number;
  ticketTitle: string;
  ticketStatus: TicketStatus;
  workflowId: string;
  aiWorkflowId: string | null;
  stageOrder: number;
  stageKey: string;
  stageDisplayName: string;
  approvalStatus: ApprovalStatus;
  approverId: number | null;
  approverName: string | null;
  requestedAt: string;
}

export interface ApprovalDecisionResponse {
  id: number;
  ticketId: number;
  workflowId: string;
  stageOrder: number;
  stageKey: string;
  status: ApprovalStatus;
  comment: string;
  idempotent: boolean;
  requestedAt: string;
  decidedAt: string;
}

export interface ApprovalCommandRequest {
  comment?: string;
  requestId?: string;
}

export interface ApprovalStageResponse {
  id: number;
  workflowId: string;
  aiWorkflowId: string | null;
  stageOrder: number;
  stageKey: string;
  stageDisplayName: string;
  status: ApprovalStatus;
  approverId: number;
  approverName: string;
  approverUsername: string;
  comment: string | null;
  requestedAt: string;
  decidedAt: string | null;
}

export interface DashboardMetricsResponse {
  totalTickets: number;
  ticketStatusDistribution: Record<string, number>;
  aiSuggestionSuccessRate: number;
  averageAiLatencyMs: number;
  averageRetrievalLatencyMs: number;
  averageApprovalWaitMs: number;
  pendingApprovals: number;
  workflowFailureCount: number;
  workflowRetryCount: number;
}

export interface MonitoringOverview {
  ticketTotal: number;
  aiSuccessRate: number | null;
  averageModelLatencyMs: number | null;
  averageRetrievalLatencyMs: number | null;
  averageApprovalWaitMs: number | null;
  pendingApprovals: number;
  workflowFailureCount: number;
  workflowRetryCount: number;
  statusDistribution: Array<{
    status: TicketStatus;
    count: number;
    ratio: number;
  }>;
  derivedFrom: string;
}

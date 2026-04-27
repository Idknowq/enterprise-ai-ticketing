# 接口契约文档

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件维护接口和跨模块契约口径；HTTP 细节以运行时 `/api-docs` 生成的 OpenAPI 为最终事实源。  
Related Docs: [Architecture](ARCHITECTURE.md), [Data Model](DATA_MODEL.md), [Security](SECURITY.md), [Testing](TESTING.md)

## 适用范围

- 定义 HTTP API、统一响应、错误码、鉴权规则和跨模块 service/workflow 契约。
- 指导前端、后端、测试和后续 thread 对齐接口语义。
- 作为 OpenAPI smoke 和接口评审的人工可读补充。

## 非目标

- 不逐字段复制所有 DTO；具体 schema 以 `/api-docs` 为准。
- 不维护表结构；见 [Data Model](DATA_MODEL.md)。
- 不写页面交互细节；见前端模块说明。

## 统一响应结构

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `code` | String | 是 | `COMMON_SUCCESS` | 业务错误码或成功码 | 影响前端错误处理 |
| `message` | String | 是 | `Success` | 人类可读消息 | 影响 UI 展示 |
| `data` | Generic | 否 | `null` | 成功响应数据 | 影响 OpenAPI schema |
| `traceId` | String | 否 | 当前请求 trace | 排障关联 | 影响观测 |

## 鉴权规则

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Login | Auth Policy | 是 | Public | `POST /api/auth/login` 不需要 JWT | 影响登录页 |
| Current User | Auth Policy | 是 | JWT | `GET /api/auth/me` 需要有效 JWT | 影响前端会话 |
| Business APIs | Auth Policy | 是 | JWT | 除登录和平台基础信息外默认需要 JWT | 影响安全测试 |
| RBAC | Auth Policy | 是 | Role based | 控制器和 service 层按角色校验 | 影响 `docs/SECURITY.md` |

## 错误码

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `COMMON_BAD_REQUEST` | ErrorCode | 是 | HTTP 400 | 请求格式或参数错误 | 影响前端表单 |
| `COMMON_VALIDATION_ERROR` | ErrorCode | 是 | HTTP 400 | Bean Validation 失败 | 影响 WebMvc 测试 |
| `COMMON_UNAUTHORIZED` | ErrorCode | 是 | HTTP 401 | 未认证 | 影响安全测试 |
| `COMMON_FORBIDDEN` | ErrorCode | 是 | HTTP 403 | 无权限 | 影响权限测试 |
| `COMMON_NOT_FOUND` | ErrorCode | 是 | HTTP 404 | 资源不存在 | 影响详情页 |
| `COMMON_CONFLICT` | ErrorCode | 是 | HTTP 409 | 状态冲突或幂等冲突 | 影响 workflow |
| `COMMON_SERVICE_UNAVAILABLE` | ErrorCode | 是 | HTTP 503 | 外部依赖不可用 | 影响运维 |
| `AUTH_INVALID_CREDENTIALS` | ErrorCode | 是 | HTTP 401 | 登录失败 | 影响登录页 |
| `AUTH_USER_DISABLED` | ErrorCode | 是 | HTTP 403 | 用户禁用 | 影响认证 |
| `AUTH_INVALID_TOKEN` | ErrorCode | 是 | HTTP 401 | JWT 非法 | 影响安全 |
| `AUTH_TOKEN_EXPIRED` | ErrorCode | 是 | HTTP 401 | JWT 过期 | 影响前端续期策略 |
| `KNOWLEDGE_UNSUPPORTED_FILE` | ErrorCode | 是 | HTTP 400 | 不支持的知识文件 | 影响上传 |
| `KNOWLEDGE_DOCUMENT_PROCESSING_FAILED` | ErrorCode | 是 | HTTP 400 | 文档处理失败 | 影响知识模块 |
| `KNOWLEDGE_RETRIEVAL_FAILED` | ErrorCode | 是 | HTTP 503 | 检索失败 | 影响 AI fallback |
| `KNOWLEDGE_VECTOR_STORE_UNAVAILABLE` | ErrorCode | 是 | HTTP 503 | Qdrant 不可用 | 影响运维 |

## HTTP API

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `GET /api/platform/info` | HTTP | 是 | Public | 平台基础信息和模块开关 | 影响前端初始化 |
| `POST /api/auth/login` | HTTP | 是 | Public | 用户名密码登录，返回 JWT | 影响认证测试 |
| `GET /api/auth/me` | HTTP | 是 | JWT | 当前用户信息 | 影响权限展示 |
| `POST /api/tickets` | HTTP | 是 | JWT | 创建工单 | 影响工单、AI 和 E2E |
| `GET /api/tickets` | HTTP | 是 | JWT | 工单列表，按权限过滤 | 影响列表和安全 |
| `GET /api/tickets/{id}` | HTTP | 是 | JWT | 工单详情、事件、评论、引用等 | 影响详情页 |
| `POST /api/tickets/{id}/comments` | HTTP | 是 | JWT | 添加工单评论 | 影响时间线 |
| `POST /api/tickets/{id}/assign` | HTTP | 是 | SUPPORT/ADMIN | 分配工单 | 影响权限 |
| `POST /api/tickets/{id}/status` | HTTP | 是 | JWT + policy | 手动状态流转 | 影响状态机 |
| `POST /api/documents/upload` | HTTP | 是 | ADMIN | 上传知识文档 | 影响知识索引 |
| `GET /api/documents` | HTTP | 是 | ADMIN/SUPPORT | 文档列表和筛选 | 影响前端文档页 |
| `GET /api/documents/categories` | HTTP | 是 | JWT | 标准 IT 服务类别 | 影响工单和文档表单 |
| `POST /api/retrieval/search` | HTTP | 是 | JWT | 手动检索知识证据 | 影响 AI 和调试 |
| `POST /api/ai/tickets/{id}/run` | HTTP | 是 | SUPPORT/ADMIN | 触发 AI 主链路 | 影响审批和 E2E |
| `GET /api/ai/tickets/{id}/runs` | HTTP | 是 | JWT + ticket access | 查询 AI run 记录 | 影响详情页 |
| `GET /api/approvals/pending` | HTTP | 是 | APPROVER/ADMIN | 待审批列表 | 影响审批页 |
| `GET /api/approvals/tickets/{ticketId}` | HTTP | 是 | JWT + policy | 工单审批历史 | 影响详情页 |
| `POST /api/approvals/{id}/approve` | HTTP | 是 | Approver/Admin | 审批通过 | 影响 workflow |
| `POST /api/approvals/{id}/reject` | HTTP | 是 | Approver/Admin | 审批驳回 | 影响 workflow |
| `GET /api/observability/dashboard` | HTTP | 是 | ADMIN/SUPPORT | 聚合指标 | 影响监控页 |

## Service 契约

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Ticket access policy | Service | 是 | `TicketAccessPolicy` | 所有工单读取和状态变更必须经过权限策略 | 影响安全 |
| Ticket status update | Service | 是 | `TicketService` | 只有 Ticket 模块落地工单状态 | 影响 workflow 和 AI |
| Document retrieval | Service | 是 | `RetrievalService` | AI 和调试接口通过统一检索服务拿证据 | 影响 RAG |
| AI orchestration | Service | 是 | `AiOrchestrationService` | 编排节点输出结构化结果并记录 AI run | 影响审计 |
| Approval command | Service | 是 | `ApprovalCommandService` | 审批通过/驳回需要幂等和权限校验 | 影响 workflow |
| Observability dashboard | Service | 是 | `ObservabilityDashboardService` | 聚合指标，不直接暴露存储细节 | 影响前端 |

## Workflow 契约

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 调用方 | Module | 是 | AI | AI 判断满足审批条件后启动 workflow | 影响 AI schema |
| 提供方 | Module | 是 | workflow/approval | Temporal workflow 创建审批并等待 signal | 影响审批 |
| 输入 | Schema | 是 | `ApprovalWorkflowInput` | ticket id、requester、category、summary 等 | 影响 DTO |
| 输出 | Schema | 是 | Ticket status | 审批通过进入处理或解决，驳回进入 `REJECTED` | 影响状态机 |
| 错误 | ErrorCode | 是 | `COMMON_CONFLICT` / `COMMON_SERVICE_UNAVAILABLE` | 冲突或依赖不可用 | 影响重试 |
| 幂等性 | Policy | 是 | Required | workflow 启动、审批创建、审批 signal 和 activity replay 必须幂等 | 影响测试 |
| 权限要求 | Policy | 是 | APPROVER/ADMIN | 只有指定审批人或管理员可处理审批 | 影响安全 |

## OpenAPI 维护规则

- 运行时 `/api-docs` 是 HTTP schema 的最终事实源。
- 文档与 OpenAPI 冲突时，优先修代码注解或 DTO，使 `/api-docs` 正确，再更新本文档。
- 新增接口必须补充 WebMvc 或 E2E 覆盖，低副作用接口应纳入 `scripts/openapi-smoke.sh`。
- 任何响应 wrapper 变更必须检查 Schemathesis smoke。

## 维护规则

- 新增、删除或改变 HTTP API 时，必须更新本文件和相关测试。
- 跨模块契约字段变化时，必须同步更新调用方和提供方模块说明。
- 不在本文档复制完整 OpenAPI JSON。

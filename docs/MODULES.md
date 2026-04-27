# 模块边界文档

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是模块职责、非职责、依赖和跨模块协作边界的事实源。  
Related Docs: [Architecture](ARCHITECTURE.md), [API Contracts](API_CONTRACTS.md), [Data Model](DATA_MODEL.md), [Testing](TESTING.md)

## 适用范围

- 替代旧按 thread 编号的分散模块说明。
- 供后续 thread 修改模块时判断职责边界和契约影响。
- 覆盖后端模块、前端控制台和基础设施相关职责。

## 非目标

- 不记录个人 thread 工作日志。
- 不维护完整 API 字段；见 [API Contracts](API_CONTRACTS.md)。
- 不维护完整运行配置；见 [Operations](OPERATIONS.md)。

## 模块总览

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `common` | Backend Module | 是 | Enabled | 统一响应、错误、异常、工具和平台信息 | 影响所有模块 |
| `auth` | Backend Module | 是 | Enabled | 登录、JWT、用户上下文、RBAC、seed 用户 | 影响安全和所有 API |
| `ticket` | Backend Module | 是 | Enabled | 工单主业务、状态机、事件、评论、分配 | 影响 AI、审批、前端 |
| `knowledge` | Backend Module | 是 | Enabled | 文档上传、解析、切分、embedding、检索、引用 | 影响 AI 和文档页 |
| `ai` | Backend Module | 是 | Enabled | 分类、抽取、检索适配、建议、AI run 审计 | 影响审批和详情页 |
| `approval` | Backend Module | 是 | Enabled | 审批查询、命令、权限、审批记录 | 影响 workflow |
| `workflow` | Backend Module | 是 | Enabled | Temporal workflow/activity，审批挂起和恢复 | 影响审批和状态 |
| `observability` | Backend Module | 是 | Enabled | dashboard 聚合、指标、trace 辅助 | 影响运维 |
| `frontend` | Frontend Module | 是 | Enabled | Next.js 控制台 | 影响用户体验和 E2E |

## `common`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | `Result<T>`、错误码、异常映射、平台信息 | 影响所有 API |
| 非职责 | Scope | 是 | 无 | 不承载业务状态和模块逻辑 | 防止公共模块膨胀 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/common` | 公共 API 和错误处理 | 影响包结构 |
| 提供契约 | Contract | 是 | Result/error envelope | 给所有 controller 使用 | 影响 OpenAPI |
| 测试覆盖 | Test | 是 | WebMvc | `PlatformControllerWebMvcTest` 等 | 影响质量门禁 |

## `auth`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 登录、JWT、用户主体、角色、访问控制 helper | 影响所有受保护接口 |
| 非职责 | Scope | 是 | 无 | 不决定业务资源状态，不做工单领域判断 | 防止权限和业务耦合 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/auth` | auth controller/service/security | 影响包结构 |
| 依赖模块 | Dependency | 是 | `common` | 使用统一错误和异常 | 影响错误语义 |
| 提供契约 | Contract | 是 | `UserContext`、`AccessControlService`、`RoleChecker` | 供业务模块校验用户和角色 | 影响安全 |
| 消费契约 | Contract | 是 | `roles`、`users`、`user_roles` | 使用 auth 数据表 | 影响数据模型 |
| 测试覆盖 | Test | 是 | Unit + WebMvc | `RoleCheckerTest`、`AuthControllerWebMvcTest` | 影响认证门禁 |
| 扩展点 | Extension | 否 | 无 | 外部 IdP、用户管理、token 刷新 | 需安全设计 |

## `ticket`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 工单创建、查询、评论、分配、状态机、事件 | 影响核心业务 |
| 非职责 | Scope | 是 | 无 | 不直接做 AI 推理、文档检索或审批 signal | 保持模块边界 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/ticket` | ticket controller/service/domain | 影响包结构 |
| 依赖模块 | Dependency | 是 | `auth`、`common`、`knowledge` category enum | 使用权限和标准类别 | 影响分类 |
| 提供契约 | Contract | 是 | `TicketService`、`TicketQueryService`、HTTP `/api/tickets` | 供 AI、workflow、frontend 使用 | 影响接口 |
| 消费契约 | Contract | 是 | `UserContext` | 权限过滤 | 影响安全 |
| 测试覆盖 | Test | 是 | Unit + WebMvc | `TicketStatusTest`、`TicketAccessPolicyTest`、`TicketControllerWebMvcTest` | 影响状态机 |
| 扩展点 | Extension | 否 | 无 | SLA、标签、附件、升级规则 | 需更新 PRD/API/DATA |

## `knowledge`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 文档上传、解析、切分、embedding、Qdrant 写入和检索 | 影响 RAG |
| 非职责 | Scope | 是 | 无 | 不生成最终 AI 处理建议，不改变工单状态 | 防止越界 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/knowledge` | document/retrieval/qdrant/parser | 影响包结构 |
| 依赖模块 | Dependency | 是 | `auth`、`common` | 文档访问过滤和错误处理 | 影响安全 |
| 提供契约 | Contract | 是 | `/api/documents`、`/api/retrieval/search`、`RetrievalService` | 供前端和 AI 使用 | 影响接口 |
| 消费契约 | Contract | 是 | Qdrant、embedding provider | 向量化和检索 | 影响运维 |
| 测试覆盖 | Test | 是 | Unit + WebMvc | `TextChunkerTest`、`DocumentServiceImplTest`、`RetrievalServiceImplTest`、`QdrantClientTest` | 影响检索质量 |
| 扩展点 | Extension | 否 | 无 | 重排、文档版本、权限标签、批量导入 | 需更新数据模型 |

## `ai`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | AI 主链路、节点执行、provider 路由、AI run 审计 | 影响智能处理 |
| 非职责 | Scope | 是 | 无 | 不绕过 TicketService 修改状态，不直接审批 | 保持审计和状态一致 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/ai` | ai controller/provider/workflow/service | 影响包结构 |
| 依赖模块 | Dependency | 是 | `ticket`、`knowledge`、`workflow`、`common` | 读取工单、检索证据、触发审批 | 影响契约 |
| 提供契约 | Contract | 是 | `/api/ai/tickets/{id}/run`、AI run records | 供前端和审计使用 | 影响详情页 |
| 消费契约 | Contract | 是 | LLM provider、RetrievalService、TicketService | AI 节点输入输出结构化 | 影响测试 |
| 测试覆盖 | Test | 是 | Unit | `TicketClassifierNodeTest`、`TicketRetrieverNodeTest`、provider tests | 影响 AI 质量 |
| 扩展点 | Extension | 否 | 无 | 多模型路由、评测、提示词版本、人工复核策略 | 需更新安全和测试 |

## `approval` 与 `workflow`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 审批项创建、查询、通过/驳回、workflow 挂起恢复 | 影响审批闭环 |
| 非职责 | Scope | 是 | 无 | 不做 AI 决策，不直接暴露 Temporal 内部细节给前端 | 防止耦合 |
| 入口代码 | Path | 是 | `approval`、`workflow` packages | approval controller/service 和 Temporal workflow/activity | 影响包结构 |
| 依赖模块 | Dependency | 是 | `auth`、`ticket`、`common` | 审批权限和状态更新 | 影响状态机 |
| 提供契约 | Contract | 是 | `/api/approvals`、workflow signal/activity | 供前端和 AI 使用 | 影响接口 |
| 消费契约 | Contract | 是 | `TicketService`、Temporal service | 状态落地和长流程 | 影响运维 |
| 测试覆盖 | Test | 是 | Unit | `ApprovalCommandServiceImplTest`、`ApprovalWorkflowActivitiesImplTest` | 影响幂等 |
| 扩展点 | Extension | 否 | 无 | 审批模板、并行审批、委托、超时 | 需更新 PRD/DATA/API |

## `observability`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 聚合 dashboard、指标、trace 关联、运行态信息 | 影响运维 |
| 非职责 | Scope | 是 | 无 | 不承载业务决策 | 防止监控反向影响业务 |
| 入口代码 | Path | 是 | `backend/src/main/java/com/enterprise/ticketing/observability` | controller/service/dto | 影响包结构 |
| 依赖模块 | Dependency | 是 | ticket/ai/approval repositories 或 service | 汇总指标 | 影响 dashboard |
| 提供契约 | Contract | 是 | `/api/observability/dashboard` | 前端监控页消费 | 影响前端 |
| 测试覆盖 | Test | 是 | Smoke/WebMvc | 平台和 dashboard smoke | 影响运维信心 |

## `frontend`

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 职责 | Scope | 是 | 无 | 登录、工单、详情、审批、文档、监控页面 | 影响用户流程 |
| 非职责 | Scope | 是 | 无 | 不保存业务事实，不绕过后端权限 | 影响安全 |
| 入口代码 | Path | 是 | `frontend/app`、`frontend/components`、`frontend/lib` | 页面、组件、服务层 | 影响前端结构 |
| 依赖模块 | Dependency | 是 | 后端 HTTP API | 通过 `frontend/lib/services` 调用 API | 影响接口 |
| 提供契约 | Contract | 是 | UI workflows | 面向用户的控制台 | 影响 E2E |
| 消费契约 | Contract | 是 | `Result<T>`、JWT、OpenAPI 语义 | 依赖后端统一响应 | 影响错误处理 |
| 测试覆盖 | Test | 是 | Vitest + Playwright | 组件、服务、E2E smoke | 影响发布 |

## 维护规则

- 新增模块或模块依赖时，必须更新本文件和 [Architecture](ARCHITECTURE.md)。
- 模块之间只能通过明确 API、service、event 或 workflow 契约交互。
- 模块文档不再写入按 thread 编号的模块 README。

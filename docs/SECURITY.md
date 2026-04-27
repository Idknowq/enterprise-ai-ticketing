# 安全文档

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是认证、授权、数据访问、敏感信息、AI 安全和审计要求的事实源。  
Related Docs: [API Contracts](API_CONTRACTS.md), [Data Model](DATA_MODEL.md), [Operations](OPERATIONS.md), [Testing](TESTING.md)

## 适用范围

- 定义 MVP 当前安全边界和后续变更要求。
- 覆盖 JWT、RBAC、资源访问、知识库权限、审批权限、敏感配置和 AI 输出安全。
- 为测试和评审提供安全检查清单。

## 非目标

- 不宣称满足生产级合规认证。
- 不维护完整威胁建模报告。
- 不替代依赖扫描和渗透测试。

## 认证

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 登录方式 | Auth | 是 | 用户名/密码 | `POST /api/auth/login` 返回 JWT | 影响前端和测试 |
| Token 类型 | Auth | 是 | Bearer JWT | 后续接口使用 `Authorization: Bearer <token>` | 影响 API |
| Issuer | Config | 是 | `APP_AUTH_JWT_ISSUER` | 默认 `enterprise-ai-ticketing` | 影响 token 校验 |
| Secret | Secret | 是 | `APP_AUTH_JWT_SECRET` | dev 默认值仅限本地 | 生产必须替换 |
| TTL | Config | 是 | `8h` | `APP_AUTH_JWT_ACCESS_TOKEN_TTL` | 影响会话策略 |

## 授权

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `EMPLOYEE` | Role | 是 | seed | 只能访问本人相关工单和允许级别知识 | 影响权限测试 |
| `SUPPORT_AGENT` | Role | 是 | seed | 处理工单，访问支持范围文档 | 影响工单流程 |
| `APPROVER` | Role | 是 | seed | 查看和处理自己待审批项 | 影响审批 |
| `ADMIN` | Role | 是 | seed | 管理文档、跨资源查看和代审 | 高风险权限 |
| `RoleChecker` | Component | 是 | Enabled | controller 方法级角色校验 | 影响 API |
| `AccessControlService` | Component | 是 | Enabled | 资源归属和部门权限 helper | 影响 service |

## 数据访问规则

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 工单读取 | Policy | 是 | RBAC + ownership | 员工仅本人相关；支持和管理员具备更高访问 | 影响 `TicketAccessPolicyTest` |
| 工单状态变更 | Policy | 是 | 状态机 + role | 手动变更必须经过状态机和权限策略 | 影响数据一致性 |
| 审批待办 | Policy | 是 | approver/admin | 审批人只处理自己待办，管理员可代审 | 影响 workflow |
| 文档列表 | Policy | 是 | admin/support | 文档管理接口限制角色 | 影响知识库 |
| 检索结果 | Policy | 是 | access level + department | 不返回越权文档片段 | 影响 AI 安全 |
| AI run 查询 | Policy | 是 | ticket access | 能访问工单才能看 AI run | 影响详情页 |

## 知识库安全

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `PUBLIC` | AccessLevel | 是 | 无 | 可广泛检索 | 影响员工结果 |
| `INTERNAL` | AccessLevel | 是 | 无 | 内部可见 | 影响员工结果 |
| `RESTRICTED` | AccessLevel | 是 | 无 | 支持、审批或更高权限可见 | 影响检索过滤 |
| `CONFIDENTIAL` | AccessLevel | 是 | 无 | 仅最高权限或未来专门策略 | 当前需谨慎使用 |
| Department filter | Policy | 是 | `GLOBAL` | 跨部门文档使用 `GLOBAL`，其他按部门过滤 | 影响 RAG |

## 敏感信息

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 数据库密码 | Secret | 是 | dev 默认 | 本地可用，生产必须替换 | 影响部署 |
| JWT secret | Secret | 是 | dev 默认 | 生产必须至少 32 字符且安全存储 | 影响认证 |
| LLM API key | Secret | 否 | 空 | 不得提交到仓库 | 影响 AI provider |
| Embedding API key | Secret | 否 | 空 | 不得提交到仓库 | 影响知识库 |
| 用户密码 | Secret | 是 | seed hash | 不得在日志输出明文 | 影响审计 |

## AI 输出安全

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Schema 化输出 | Policy | 是 | Required | AI 结果必须结构化，不能让自然语言驱动状态变更 | 影响 AI 和 workflow |
| 检索引用 | Policy | 是 | Required | 建议必须保留 citations，便于追踪 | 影响审计 |
| 人工复核 | Policy | 是 | Required | fallback、检索错误、低置信度或高风险场景需要人工复核 | 影响审批触发 |
| 权限过滤前置 | Policy | 是 | Required | RAG 检索必须先应用文档访问过滤 | 影响数据泄露 |
| 不自动执行生产操作 | Policy | 是 | Required | AI 只生成建议或触发审批，不直接改外部生产系统 | 影响产品边界 |

## 审计要求

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `ticket_events` | Audit | 是 | Required | 记录工单状态和关键业务事件 | 影响合规 |
| `ai_runs` | Audit | 是 | Required | 记录 provider、模型、fallback、节点输出摘要 | 影响 AI 可追踪 |
| `citations` | Audit | 是 | Required | 记录知识引用来源 | 影响 RAG 可信度 |
| `approvals` | Audit | 是 | Required | 记录审批阶段、审批人、状态和意见 | 影响流程追踪 |
| Trace id | Audit | 否 | Enabled | 用于跨日志和请求排障 | 影响观测 |

## 安全测试要求

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 401/403 测试 | Test | 是 | WebMvc/E2E | 未登录和越权必须被拒绝 | 影响 API |
| 资源归属测试 | Test | 是 | Unit | 工单和审批访问边界 | 影响数据泄露 |
| 文档检索权限测试 | Test | 是 | Unit | 不返回越权知识片段 | 影响 RAG |
| 依赖扫描 | Test | P2 | 待完善 | Trivy 或等效工具 | 影响发布 |
| ZAP baseline | Test | P2 | 待完善 | 被动扫描高危项 | 影响上线 |

## 维护规则

- 新增角色、权限、资源访问策略或敏感配置时，必须更新本文件。
- 任何绕过 `TicketService` 或权限策略直接修改核心状态的设计都需要架构评审。
- 安全相关变更必须补负向测试。

# 测试策略文档

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是测试分层、必跑命令、MVP 阻断测试和质量门禁的事实源。  
Related Docs: [PRD](PRD.md), [API Contracts](API_CONTRACTS.md), [Data Model](DATA_MODEL.md), [Operations](OPERATIONS.md)

## 适用范围

- 定义当前 MVP 的测试策略和交付前质量门禁。
- 指导后续 thread 为新增能力补测试。
- 汇总当前已落地测试基线，不记录流水式推进日志。

## 非目标

- 不记录每次测试执行历史。
- 不替代 CI 配置。
- 不要求 MVP 阶段一次性补齐所有深度安全、容量和 Testcontainers 测试。

## 必跑命令

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `mvn test` | Command | 是 | `backend` | 后端单元和切片测试 | 后端变更必须跑 |
| `npm run lint` | Command | 是 | `frontend` | 前端 ESLint 门禁 | 前端变更必须跑 |
| `npm run typecheck` | Command | 是 | `frontend` | Next typegen + TypeScript | 前端类型变更必须跑 |
| `npm test` | Command | 是 | `frontend` | Vitest 单测和服务层测试 | 前端逻辑变更必须跑 |
| `npm run build` | Command | 是 | `frontend` | Next.js 构建 | 前端发布前必须跑 |
| `npm run test:e2e` | Command | 是 | `frontend` | Playwright smoke | 端到端流程变更必须跑 |
| `./scripts/openapi-smoke.sh` | Command | 是 | repo root | Schemathesis OpenAPI smoke | API 变更必须跑 |

## 测试分层

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 后端纯逻辑单测 | Layer | 是 | JUnit | 状态机、权限 helper、chunker、provider 路由 | 影响快速反馈 |
| 后端 WebMvc 切片 | Layer | 是 | Spring Test | controller URL、参数绑定、统一响应、validation | 影响 API 契约 |
| 后端服务测试 | Layer | 是 | Mockito | 审批命令、workflow activity、知识服务 | 影响业务正确性 |
| 前端组件测试 | Layer | 是 | Vitest + Testing Library | 登录、工单、审批、状态组件 | 影响 UI 行为 |
| 前端服务层测试 | Layer | 是 | MSW | HTTP path、query、body 和错误处理 | 影响 API 对接 |
| E2E smoke | Layer | 是 | Playwright | 登录、建单、AI、审批、文档、监控 | 影响发布信心 |
| OpenAPI smoke | Layer | 是 | Schemathesis | 低副作用接口契约 fuzz | 影响接口稳定性 |
| 安全 smoke | Layer | P1 | 待完善 | 依赖扫描、被动扫描、权限负向 | 影响上线风险 |
| 性能 smoke | Layer | P1 | 待完善 | 小流量核心接口检查 | 影响回归风险 |

## 当前测试覆盖

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `RoleCheckerTest` | Backend Test | 是 | Unit | 角色判断 | 影响 auth |
| `TicketStatusTest` | Backend Test | 是 | Unit | 工单状态机 | 影响 data model |
| `TicketAccessPolicyTest` | Backend Test | 是 | Unit | 工单访问控制 | 影响 security |
| `AuthControllerWebMvcTest` | Backend Test | 是 | WebMvc | 登录和当前用户接口 | 影响 API |
| `TicketControllerWebMvcTest` | Backend Test | 是 | WebMvc | 工单 API mapping 和 validation | 影响 API |
| `DocumentControllerWebMvcTest` | Backend Test | 是 | WebMvc | 文档列表和上传 | 影响 knowledge |
| `ApprovalCommandServiceImplTest` | Backend Test | 是 | Unit | 审批权限、幂等、终态冲突 | 影响 workflow |
| `ApprovalWorkflowActivitiesImplTest` | Backend Test | 是 | Unit | workflow activity 行为 | 影响审批 |
| `LlmProviderRouterTest` | Backend Test | 是 | Unit | AI provider 路由 | 影响 AI |
| `RetrievalServiceImplTest` | Backend Test | 是 | Unit | 检索过滤和结果 | 影响 RAG |
| `frontend/tests/components` | Frontend Test | 是 | Vitest | 登录、列表、详情、审批、状态组件 | 影响 UI |
| `frontend/tests/services/msw-services.test.ts` | Frontend Test | 是 | MSW | 服务层 API 对接 | 影响前后端契约 |
| `frontend/tests/e2e/smoke.spec.ts` | E2E Test | 是 | Playwright | MVP 主链路 smoke | 影响发布 |
| `scripts/openapi-smoke.sh` | Contract Test | 是 | Schemathesis | OpenAPI 低副作用接口 smoke | 影响接口 |

## MVP 阻断测试

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 登录失败或 JWT 失效 | Blocker | 是 | 无 | 不能发布 | 影响所有用户 |
| 工单创建/查询/详情失败 | Blocker | 是 | 无 | 不能发布 | 影响核心业务 |
| 工单状态机非法流转未拦截 | Blocker | 是 | 无 | 不能发布 | 影响数据一致性 |
| 审批通过/驳回不幂等 | Blocker | 是 | 无 | 不能发布 | 影响 workflow |
| 文档上传或检索越权 | Blocker | 是 | 无 | 不能发布 | 影响安全 |
| AI run 不可追踪 | Blocker | 是 | 无 | 不能发布 | 影响审计 |
| OpenAPI schema 与响应不一致 | Blocker | 是 | 无 | 不能发布 | 影响前端和测试 |

## 待补测试

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| OpenAPI 错误响应 schema | Gap | P1 | 未完整 | 覆盖 400/401/403/404/409/500 envelope | 影响契约 |
| Schemathesis 负向 smoke | Gap | P1 | 未完整 | 限制低副作用接口 | 影响健壮性 |
| Flyway migration smoke | Gap | P1 | 未完整 | 轻量启动校验或 Testcontainers | 影响数据安全 |
| 文档上传页组件测试 | Gap | P2 | 未完整 | 前端上传表单 | 影响 UI |
| k6 小流量 smoke | Gap | P2 | 未完整 | 核心接口性能退化 | 影响运维 |
| Trivy / ZAP baseline | Gap | P2 | 未完整 | 依赖和被动安全扫描 | 影响安全 |

## 维护规则

- 新增 P0/P1 功能必须补对应测试或在 PR 中说明暂缓原因。
- API 变更必须跑 OpenAPI smoke，并更新 [API Contracts](API_CONTRACTS.md)。
- 状态机和数据模型变更必须更新后端单测。
- 前端页面或服务层变更必须至少跑 lint、typecheck、Vitest；主链路变更必须跑 E2E。

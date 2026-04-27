# 协作规范

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是分支、提交、PR、代码所有权、文档更新和多 thread 协作的事实源。  
Related Docs: [Doc Writing Guide](DOC_WRITING_GUIDE.md), [Modules](MODULES.md), [Testing](TESTING.md), [Security](SECURITY.md)

## 适用范围

- 适用于本项目后续所有代码、文档、测试和配置变更。
- 适用于单人开发、多 thread 并行开发和 PR 评审。
- 定义哪些变更必须同步更新哪些文档。

## 非目标

- 不规定每个模块内部的详细实现方案。
- 不替代测试策略；测试要求见 [Testing](TESTING.md)。
- 不替代安全策略；安全要求见 [Security](SECURITY.md)。

## 代码所有权

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `backend/src/main/java/.../auth` | Ownership | 是 | Auth owner | 登录、JWT、RBAC、用户上下文 | 影响安全 |
| `backend/src/main/java/.../ticket` | Ownership | 是 | Ticket owner | 工单核心、状态机、事件 | 影响核心业务 |
| `backend/src/main/java/.../knowledge` | Ownership | 是 | Knowledge owner | 文档、embedding、检索、引用 | 影响 AI |
| `backend/src/main/java/.../ai` | Ownership | 是 | AI owner | AI 编排、provider、AI run | 影响审批 |
| `backend/src/main/java/.../approval` | Ownership | 是 | Approval owner | 审批命令和查询 | 影响 workflow |
| `backend/src/main/java/.../workflow` | Ownership | 是 | Workflow owner | Temporal workflow/activity | 影响审批 |
| `frontend` | Ownership | 是 | Frontend owner | 控制台页面、组件、服务层 | 影响 E2E |
| `docs` | Ownership | 是 | Project Lead | 正式项目文档 | 影响所有 thread |

## 多 thread 协作规则

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 先读事实源 | Rule | 是 | Required | 修改前先读 `README.md` 和相关 `docs/` 文件 | 减少冲突 |
| 禁止新增 thread README | Rule | 是 | Required | 不再新增按 thread 编号的模块 README | 文档集中维护 |
| 修改模块边界先改文档 | Rule | 是 | Required | 模块职责或依赖变化必须更新 `MODULES.md` | 影响架构 |
| 跨模块契约先对齐 | Rule | 是 | Required | API/service/workflow 变更必须先明确调用方和提供方 | 影响并行开发 |
| 不覆盖他人改动 | Rule | 是 | Required | 发现无关 dirty changes 时保留 | 保护工作区 |
| 小步提交 | Rule | 是 | Recommended | 每次 PR 聚焦一个明确主题 | 降低评审成本 |

## 分支与提交

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Branch prefix | Rule | 是 | `codex/` | Codex 创建分支默认使用此前缀 | 影响仓库管理 |
| Commit message | Rule | 是 | 英文或中文均可 | 需说明行为变更，不写空泛信息 | 影响追溯 |
| Commit scope | Rule | 是 | 单一主题 | 避免一个提交混合功能、格式和文档大改 | 影响 review |
| Generated files | Rule | 是 | 显式说明 | 代码生成或截图产物必须说明来源 | 影响维护 |

## PR Checklist

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 需求是否清楚 | Checklist | 是 | Required | PR 描述说明目标和非目标 | 影响评审 |
| 测试是否执行 | Checklist | 是 | Required | 列出实际执行命令和结果 | 影响质量 |
| API 是否变更 | Checklist | 是 | Conditional | 变更则更新 `API_CONTRACTS.md` 和 OpenAPI smoke | 影响前端 |
| 数据是否变更 | Checklist | 是 | Conditional | 变更则新增 migration 并更新 `DATA_MODEL.md` | 影响部署 |
| 权限是否变更 | Checklist | 是 | Conditional | 变更则更新 `SECURITY.md` 并补负向测试 | 影响安全 |
| AI schema 是否变更 | Checklist | 是 | Conditional | 变更则更新 API、AI 测试和前端展示 | 影响审批 |
| 环境变量是否变更 | Checklist | 是 | Conditional | 变更则更新 `OPERATIONS.md` | 影响运行 |
| 文档是否同步 | Checklist | 是 | Required | 遵守 `DOC_WRITING_GUIDE.md` | 影响后续 thread |

## 命名规范

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Java package | Rule | 是 | `com.enterprise.ticketing.<module>` | 按模块分包 | 影响模块边界 |
| DTO | Rule | 是 | `*Request` / `*Response` | API 入参和出参命名清楚 | 影响 OpenAPI |
| Migration | Rule | 是 | `V{N}__description.sql` | 只增不改旧 migration | 影响数据 |
| Frontend service | Rule | 是 | `frontend/lib/services/<domain>.ts` | API 调用集中维护 | 影响测试 |
| Test file | Rule | 是 | `*Test` / `*.test.tsx` | 与被测对象对应 | 影响可维护性 |
| Docs | Rule | 是 | 大写主题名 | 根入口 `README.md`，专题文档在 `docs/` | 影响链接 |

## 文档更新触发条件

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 修改用户场景或验收 | Trigger | 是 | `PRD.md` | 产品范围变化 | 影响测试 |
| 修改模块依赖或链路 | Trigger | 是 | `ARCHITECTURE.md` / `MODULES.md` | 架构变化 | 影响协作 |
| 修改 HTTP API | Trigger | 是 | `API_CONTRACTS.md` | 接口契约变化 | 影响前端 |
| 修改表、枚举或状态 | Trigger | 是 | `DATA_MODEL.md` | 数据事实变化 | 影响 migration |
| 修改启动或配置 | Trigger | 是 | `OPERATIONS.md` | 运行方式变化 | 影响联调 |
| 修改认证授权 | Trigger | 是 | `SECURITY.md` | 安全策略变化 | 影响负向测试 |
| 修改测试策略 | Trigger | 是 | `TESTING.md` | 质量门禁变化 | 影响 CI |

## 维护规则

- 任何 PR 如果改变公共行为但没有对应测试或文档说明，默认不应合入。
- 评审时优先检查行为风险、权限风险、数据一致性和测试缺口。
- 文档事实冲突时，以对应 source-of-truth 文档为准，并修正其他引用。

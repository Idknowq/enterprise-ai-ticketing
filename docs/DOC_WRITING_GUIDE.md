# 文档编写规范

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是所有正式文档的编写规范事实源。  
Related Docs: [README](../README.md), [CONTRIBUTING](CONTRIBUTING.md), [Modules](MODULES.md), [API Contracts](API_CONTRACTS.md), [Data Model](DATA_MODEL.md)

## 适用范围

- 适用于根目录 `README.md` 和 `docs/` 下所有正式项目文档。
- 适用于后续多个 thread 并行维护同一份文档时的结构、术语、事实源和验收规则。
- 适用于 PR、代码评审、模块交接、迭代升级中的文档更新。

## 非目标

- 不规定代码风格；代码风格见 [CONTRIBUTING](CONTRIBUTING.md)。
- 不替代每篇专题文档的具体内容。
- 不要求归档文档 `docs/archive/` retroactively 补齐元信息头。

## 文件头规范

每份正式文档必须以下列格式开头：

```markdown
# 文档标题

Status: Active
Owner: Project Lead
Last Verified: 2026-04-27
Source of Truth: 本文件维护某类事实；其他文档只能引用。
Related Docs: [README](../README.md)
```

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `Status` | Enum | 是 | `Draft` | 只能是 `Draft`、`Active`、`Deprecated` | 影响文档可信度 |
| `Owner` | String | 是 | `Project Lead` | 负责最终口径的人或模块 | 影响评审责任 |
| `Last Verified` | Date | 是 | 当前日期 | 使用 `YYYY-MM-DD` | 代码事实变更时必须更新 |
| `Source of Truth` | Text | 是 | 无 | 说明本文件负责维护哪类事实 | 防止重复维护 |
| `Related Docs` | Links | 是 | 无 | 指向相关正式文档 | 帮助 reader 跳转 |

## 必备章节

每份正式文档至少包含：

- `适用范围`
- `非目标`
- 主体章节
- `维护规则` 或 `变更规则`

允许按文档类型增加章节，但不得删除适用范围和非目标。

## Source of Truth 规则

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 产品需求 | Fact | 是 | `docs/PRD.md` | 用户、场景、目标、验收 | 功能改动必须更新 |
| 系统架构 | Fact | 是 | `docs/ARCHITECTURE.md` | 模块关系、链路、技术选择 | 架构改动必须更新 |
| HTTP API | Fact | 是 | OpenAPI + `docs/API_CONTRACTS.md` | 代码生成 OpenAPI 优先 | 接口改动必须更新 |
| 数据表和状态机 | Fact | 是 | `docs/DATA_MODEL.md` | migration、枚举、状态流转 | 数据改动必须更新 |
| 模块职责 | Fact | 是 | `docs/MODULES.md` | 职责、非职责、依赖 | 模块边界改动必须更新 |
| 运行配置 | Fact | 是 | `docs/OPERATIONS.md` | 环境变量、启动、监控 | 部署改动必须更新 |
| 测试策略 | Fact | 是 | `docs/TESTING.md` | 分层、命令、门禁 | 测试改动必须更新 |
| 安全策略 | Fact | 是 | `docs/SECURITY.md` | 鉴权、授权、敏感数据、审计 | 安全改动必须更新 |

## 表格规范

接口、状态、错误码、数据表、环境变量必须使用表格，至少包含：

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `名称` | String | 是 | 无 | 字段、接口、状态、变量或表名 | 影响引用 |
| `类型` | String | 是 | 无 | 数据类型或分类 | 影响实现 |
| `是否必填` | Boolean/Text | 是 | 无 | 是、否或条件必填 | 影响校验 |
| `默认值` | String | 是 | 无 | 没有默认值写 `无` | 影响运行 |
| `说明` | Text | 是 | 无 | 业务语义 | 影响维护 |
| `变更影响` | Text | 是 | 无 | 修改时必须同步检查的文档或模块 | 影响评审 |

## 跨模块契约规范

所有跨模块契约必须写清：

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 调用方 | Module | 是 | 无 | 谁消费契约 | 影响模块依赖 |
| 提供方 | Module | 是 | 无 | 谁负责实现 | 影响代码所有权 |
| 输入 | Schema | 是 | 无 | DTO、参数或事件字段 | 影响校验 |
| 输出 | Schema | 是 | 无 | 返回结构或事件结果 | 影响调用方 |
| 错误 | ErrorCode | 是 | 无 | 失败语义 | 影响错误处理 |
| 幂等性 | Policy | 是 | 无 | 是否支持重复调用 | 影响 workflow 和重试 |
| 权限要求 | Policy | 是 | 无 | 角色或资源范围 | 影响安全 |

## 术语表

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Ticket | Domain Term | 是 | 无 | 工单，核心业务对象 | 影响 API 和数据模型 |
| AI Run | Domain Term | 是 | 无 | 一次 AI 编排执行记录 | 影响审计和可观测性 |
| Citation | Domain Term | 是 | 无 | 知识引用，连接工单、AI run 和文档 | 影响知识检索 |
| Approval | Domain Term | 是 | 无 | 审批项，绑定 ticket 和 workflow | 影响状态流转 |
| Workflow | Domain Term | 是 | Temporal | 长流程编排 | 影响幂等和排障 |
| Source of Truth | Documentation Term | 是 | 无 | 某类事实唯一维护位置 | 影响所有文档 |

## 图表规范

- 架构、状态机、流程统一使用 Mermaid。
- Mermaid 节点名使用英文 id，展示文本可以中文。
- 禁止把截图作为架构唯一事实源。
- 图下方必须有简短文字解释维护边界。

## 更新检查清单

文档 PR 合入前必须检查：

- 是否更新了 `Last Verified`。
- 是否只在唯一事实源维护核心事实。
- 是否没有新增按 thread 编号的模块 README。
- 是否没有在 `README.md` 复制完整接口或表结构。
- 所有命令是否标注工作目录并能复制执行。
- 所有链接是否指向存在的文件。
- 涉及 API、状态机、数据表、权限、环境变量、AI schema 时，是否同步更新相关文档。

## 维护规则

- 其他 thread 修改文档时，必须先读本文件。
- 无法确认事实时，优先读代码、migration、配置和测试，不要凭记忆写文档。
- 归档文档只作为历史参考，不再作为当前事实源。

# 企业级 AI 工单编排系统（MVP）开发文档

## 1. 文档目的

本文档用于指导“企业级 AI 工单编排系统”MVP 版本的设计与开发，覆盖以下内容：

- 产品目标与边界
- MVP 需求与功能清单
- 技术栈设计
- 系统模块划分
- 数据模型与接口边界
- 多 thread（多 agent）并行开发拆分方案
- 里程碑与协作规范

本文档目标不是定义最终完整产品，而是确保在 **2~3 个月** 内完成一个可演示、可扩展、可写入简历的第一版系统。

---

## 2. 项目背景

传统企业内部支持系统通常存在以下问题：

1. 工单分类依赖人工，响应速度慢。
2. 知识散落在 FAQ、SOP、历史工单和文档中，难以统一检索。
3. 简单 FAQ 可自动处理，但复杂问题仍需审批、人工介入、跨部门协同。
4. 工单处理过程缺乏统一的审计、可追踪性和质量评估能力。
5. 现有“AI 问答”方案无法真正纳入企业工作流。

因此，本项目的目标不是实现一个聊天机器人，而是实现一个 **AI 驱动的企业级工单编排系统**，使 AI 能够参与工单流转、知识检索、流程判断、审批挂起和自动回复生成。

---

## 3. MVP 目标

### 3.1 核心目标

构建一个面向企业内部 **IT 服务台场景** 的 AI 工单系统，支持：

- 用户提交工单
- 系统自动分类与字段抽取
- 检索知识库与历史工单
- 自动生成处理建议或回复草稿
- 对需要审批的问题发起审批流
- 全程记录状态、事件、引用和审计信息
- 提供基础可观测性能力

### 3.2 非目标（MVP 阶段不做）

以下内容不纳入 MVP：

- 多租户架构
- 多业务域（HR、财务、法务等）同时覆盖
- 多渠道接入（邮件、Slack、飞书等）
- 大规模自动执行内部系统操作
- 复杂自定义工作流设计器
- 完整 BI 报表系统

---

## 4. 实际应用场景（MVP 范围）

MVP 聚焦 **企业 IT 服务台**，典型工单包括：

- VPN 无法连接
- 账号权限申请
- 密码重置
- 开发环境异常
- 办公软件授权问题
- 设备申请或故障报修

### 示例场景 1：VPN 故障

用户提交问题：

> 我今天在家连接公司 VPN 失败，客户端提示证书失效。

系统执行：

1. 自动识别为 IT/VPN 类工单。
2. 抽取关键信息：VPN、证书、远程办公。
3. 检索知识库中关于 VPN 证书失效的 SOP。
4. 检索历史工单，看是否为近期高频故障。
5. 自动生成回复草稿或建议执行步骤。
6. 若需要重新开通证书权限，则进入审批流程。

### 示例场景 2：权限申请

用户提交问题：

> 我需要开通生产环境日志只读权限，用于排查线上问题。

系统执行：

1. 识别为权限申请类工单。
2. 抽取关键信息：生产环境、日志、只读权限。
3. 检索权限申请 SOP。
4. 判断该类请求需要直属主管或系统管理员审批。
5. 自动生成申请说明和审批摘要。
6. 等待审批结果后更新工单状态。

---

## 5. 产品范围

### 5.1 用户角色

MVP 定义以下角色：

1. **普通员工**
   - 提交工单
   - 查看本人相关工单
   - 评论和补充信息

2. **一线支持人员**
   - 查看和处理分配给自己的工单
   - 查看 AI 建议
   - 接管工单
   - 手动关闭或升级工单

3. **审批人**
   - 查看待审批请求
   - 审批/驳回
   - 填写审批意见

4. **管理员**
   - 管理知识文档
   - 管理用户和角色
   - 配置工单分类、优先级、SLA 等基础参数

---

## 6. MVP 功能需求

## 6.1 工单管理

### 必须实现

- 创建工单
- 编辑工单描述（限制在特定状态下）
- 查看工单详情
- 工单列表检索与筛选
- 工单状态流转
- 评论/补充说明
- 工单优先级
- 工单分配给处理人

### 状态建议

- `OPEN`
- `AI_PROCESSING`
- `WAITING_APPROVAL`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`
- `REJECTED`

### 验收标准

- 任意工单应有明确状态。
- 所有状态变化必须记录事件日志。
- 工单详情页可查看完整时间线。

---

## 6.2 认证与权限

### 必须实现

- 登录
- JWT 鉴权
- 基于角色的权限控制（RBAC）
- 用户只能查看自己有权限访问的工单和审批项

### 验收标准

- 普通员工不能访问他人工单。
- 审批人只能查看自己待处理审批。
- 管理员可访问管理页。

---

## 6.3 知识库与检索

### 必须实现

- 上传知识文档（Markdown / PDF / TXT）
- 文档解析与切分
- 向量化并写入向量库
- 基于工单内容执行检索
- 返回引用来源
- 基于文档标签/部门做过滤检索

### 元数据建议

- 文档标题
- 文档类别
- 部门
- 访问级别
- 更新时间
- 版本号

### 验收标准

- 提交工单后可返回 3~5 条相关引用片段。
- 工单详情页可展示引用列表。
- 不符合权限范围的文档不得出现在检索结果中。

---

## 6.4 AI 编排能力

### MVP 必须实现的 Agent 节点

1. **分类节点（Classifier）**
   - 输出工单类别、优先级、置信度

2. **字段抽取节点（Extractor）**
   - 输出结构化字段，例如设备、错误码、系统、权限类型

3. **检索节点（Retriever）**
   - 基于工单内容和抽取字段执行知识检索

4. **处理建议节点（Resolution）**
   - 输出：自动回复草稿 / 建议处理步骤 / 是否需要审批 / 是否需要人工接管

### AI 输出要求

- 输出必须结构化（JSON 或固定 schema）
- 不允许直接返回裸文本给后端核心逻辑
- 每次 AI 运行必须记录模型、耗时、token、结果摘要

### 验收标准

- 同一工单可完成完整 AI 链路：分类 → 抽取 → 检索 → 回复建议。
- 回复草稿必须能展示引用来源。
- 若 AI 判断需审批，则系统进入审批流程。

---

## 6.5 审批流

### MVP 只做单模板审批流

例如：

- 权限申请类工单 → 直属主管审批 → 系统管理员审批

### 必须实现

- 发起审批
- 审批通过/驳回
- 审批意见记录
- 审批结果驱动工单状态变化
- 支持 workflow 挂起与恢复

### 验收标准

- 工单进入 `WAITING_APPROVAL` 后不可直接关闭。
- 审批结果回传后，工单自动进入下一状态。
- 重复审批回调不得导致状态错乱。

---

## 6.6 审计与事件日志

### 必须实现

- 工单创建日志
- 工单状态变化日志
- AI 节点执行日志
- 审批日志
- 操作人记录

### 验收标准

- 每个工单可回放关键事件。
- 关键事件必须具备时间戳、操作者、事件类型和摘要信息。

---

## 6.7 可观测性

### MVP 必须实现

- 请求级 trace
- AI 调用耗时统计
- 检索耗时统计
- 工单处理成功/失败计数
- 审批等待时长指标

### Dashboard 最低要求

- 工单处理总数
- AI 自动建议生成成功率
- 平均检索耗时
- 平均模型响应耗时
- 工单状态分布

### 验收标准

- 任一工单请求应可追踪到完整链路。
- AI 节点失败时能在日志和 trace 中定位。

---

## 7. 非功能需求

## 7.1 性能

- 工单创建 API：P95 < 300ms（不含异步 AI 链路）
- 工单列表查询：P95 < 500ms
- AI 主链路目标耗时：< 8s（可接受异步返回）
- 检索单次延迟：目标 < 1.5s

## 7.2 并发与解耦

- 工单创建后，AI 流程通过消息队列或 workflow 异步执行
- 前端不阻塞等待完整 AI 结果
- 重任务不占用主 API 请求线程

## 7.3 可维护性

- 模块解耦，服务接口清晰
- AI 编排逻辑与工单核心状态机分离
- 所有关键节点有统一日志和错误处理

## 7.4 安全性

- 所有接口需鉴权
- 知识文档按部门或级别过滤
- Prompt 中禁止直接注入敏感系统指令
- 模型输出进入系统动作前需经过业务规则校验

---

## 8. 推荐技术栈

## 8.1 后端

- **Java 17**
- **Spring Boot 3.x**
- Spring Web
- Spring Security
- Spring Data JPA / MyBatis（二选一，推荐 JPA 先提高开发效率）

### 选择理由

- 企业后台风格明确
- 分层架构成熟
- 权限、事务、接口、审计更容易标准化

---

## 8.2 数据层

- **PostgreSQL**：结构化业务数据
- **Redis**：缓存、幂等 key、热点数据
- **Qdrant**：知识向量检索

---

## 8.3 AI / 编排层

- **LangGraph**：Agent 有状态编排
- LLM Provider：OpenAI / Anthropic / 本地兼容 OpenAI API 的模型服务
- Embedding Provider：与主模型解耦配置

---

## 8.4 工作流与异步

- **Temporal**：长流程、审批挂起/恢复、失败重试
- **RabbitMQ** 或 **Kafka**：事件异步化

### MVP 推荐

若优先开发效率，建议：

- 先上 RabbitMQ
- 核心审批主链路接 Temporal

---

## 8.5 前端

- **Next.js / React**
- TypeScript
- Ant Design

### MVP 页面

- 登录页
- 工单列表页
- 工单详情页
- 审批页
- 知识管理页
- 基础监控页

---

## 8.6 可观测性

- **OpenTelemetry SDK**
- **OpenTelemetry Collector**
- **Jaeger**：trace
- **Prometheus + Grafana**：metric

---

## 8.7 工程与部署

- Docker / Docker Compose
- GitHub Actions（可选）
- OpenAPI / Swagger
- `.env` + config profile 管理配置

---

## 9. 系统模块划分

建议拆为以下逻辑模块：

1. **Auth & User Module**
2. **Ticket Core Module**
3. **Knowledge & Retrieval Module**
4. **AI Orchestration Module**
5. **Workflow & Approval Module**
6. **Observability & Audit Module**
7. **Frontend Console Module**

说明：

- Ticket Core 是系统主干。
- AI Orchestration 只输出“建议”与“判断”，不直接篡改核心状态。
- Workflow 负责可靠执行与审批恢复。
- Observability 统一记录跨模块 trace / log / metric。

---

## 10. 核心数据模型

## 10.1 tickets

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| title | varchar | 工单标题 |
| description | text | 工单描述 |
| category | varchar | 工单类别 |
| priority | varchar | 优先级 |
| status | varchar | 当前状态 |
| requester_id | bigint | 提交人 |
| assignee_id | bigint | 处理人 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

## 10.2 ticket_events

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| ticket_id | bigint | 工单 ID |
| event_type | varchar | 事件类型 |
| event_payload | jsonb | 事件内容 |
| operator_id | bigint | 操作人 |
| created_at | timestamp | 时间 |

## 10.3 approvals

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| ticket_id | bigint | 工单 ID |
| approver_id | bigint | 审批人 |
| status | varchar | PENDING/APPROVED/REJECTED |
| comment | text | 审批意见 |
| approved_at | timestamp | 审批时间 |

## 10.4 documents

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| title | varchar | 文档标题 |
| category | varchar | 文档类别 |
| department | varchar | 所属部门 |
| access_level | varchar | 访问级别 |
| version | varchar | 版本 |
| source_type | varchar | 文件来源 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

## 10.5 ai_runs

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| ticket_id | bigint | 工单 ID |
| workflow_id | varchar | workflow 标识 |
| node_name | varchar | AI 节点名称 |
| model_name | varchar | 模型名称 |
| latency_ms | int | 耗时 |
| token_input | int | 输入 token |
| token_output | int | 输出 token |
| result_summary | text | 结果摘要 |
| created_at | timestamp | 记录时间 |

## 10.6 retrieval_citations

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| ticket_id | bigint | 工单 ID |
| doc_id | bigint | 文档 ID |
| chunk_id | varchar | 文档分块标识 |
| score | float | 检索分数 |
| filter_scope | varchar | 检索过滤范围 |
| created_at | timestamp | 记录时间 |

---

## 11. 核心接口边界（MVP）

## 11.1 工单接口

- `POST /api/tickets` 创建工单
- `GET /api/tickets` 查询工单列表
- `GET /api/tickets/{id}` 查询工单详情
- `POST /api/tickets/{id}/comments` 添加评论
- `POST /api/tickets/{id}/assign` 分配工单
- `POST /api/tickets/{id}/status` 更新状态

## 11.2 审批接口

- `GET /api/approvals/pending` 查询待审批项
- `POST /api/approvals/{id}/approve` 审批通过
- `POST /api/approvals/{id}/reject` 审批驳回

## 11.3 知识库接口

- `POST /api/documents/upload` 上传文档
- `GET /api/documents` 查询文档
- `POST /api/retrieval/search` 检索测试接口

## 11.4 AI 相关接口

- `POST /api/ai/tickets/{id}/run` 手动触发 AI 流程（调试用）
- `GET /api/ai/tickets/{id}/runs` 查询 AI 节点执行记录

---

## 12. 多 thread 开发拆分方案

目标不是机械分层，而是：

- 降低线程间代码冲突
n- 提高可并行度
- 让每个 thread 都有清晰输入/输出边界
- 避免多个 thread 同时修改同一核心文件

建议拆分为 **6 个 thread**。

---

## Thread 1：平台基础设施与项目骨架

### 负责范围

- 单仓项目初始化
- Docker Compose
- PostgreSQL / Redis / Qdrant / RabbitMQ / Temporal / OTel 本地开发环境
- 配置管理
- 公共依赖
- 日志基础配置
- OpenAPI/Swagger 基础接入
- 通用异常处理
- 公共 DTO / Result 包装约定

### 输入

- 总体技术栈方案

### 输出

- 所有其他 thread 可直接基于其骨架开发
- 本地一键启动环境
- README 中的环境启动部分

### 边界原则

- 不实现业务逻辑
- 不负责具体工单/AI/审批功能

### 交付物

- `docker-compose.yml`
- `application.yml` / profiles
- 公共 starter 代码
- base package 结构

---

## Thread 2：认证、用户与权限模块

### 负责范围

- 用户模型
- 登录接口
- JWT 鉴权
- Spring Security 配置
- RBAC 中间件
- 当前登录人上下文
- 管理员/普通用户/审批人角色实现

### 输入

- Thread 1 提供的工程骨架

### 输出

- 稳定可复用的鉴权体系
- 供 Ticket / Approval / Document 模块复用的权限判断能力

### 边界原则

- 不处理工单核心状态机
- 不处理知识检索逻辑
- 只提供认证与权限能力

### 对外契约

- `AuthService`
- `UserContext`
- 角色校验注解/拦截器

---

## Thread 3：工单核心模块（主业务骨架）

### 负责范围

- 工单数据模型
- 工单 CRUD
- 工单状态机
- 工单评论
- 工单分配
- 工单时间线/事件记录
- 工单详情页后端聚合接口

### 输入

- Thread 1 提供基础设施
- Thread 2 提供鉴权能力

### 输出

- 整个系统的核心主线模块
- 稳定的工单领域模型

### 边界原则

- 不实现 AI 推理内部逻辑
- 不实现审批工作流引擎本身
- 只负责工单主领域状态与领域事件

### 对外契约

- `TicketService`
- `TicketEventService`
- 状态流转接口
- 工单聚合查询接口

### 说明

这是最核心 thread，应尽量保持领域模型稳定，其他 thread 不应直接改其数据库核心表结构，除非通过明确接口协作。

---

## Thread 4：知识库与检索模块

### 负责范围

- 文档上传
- 文档解析
- 文档切分
- embedding 调用
- Qdrant 写入与检索
- 文档元数据过滤
- 检索结果格式化
- citation 存储

### 输入

- Thread 1 基础设施
- Thread 2 权限上下文
- Thread 3 提供 ticket 基础信息（仅读）

### 输出

- 检索服务
- 文档管理接口
- 引用结果标准格式

### 边界原则

- 不决定工单状态
- 不决定是否审批
- 只负责“给定输入 -> 返回可解释检索结果”

### 对外契约

- `DocumentService`
- `RetrievalService`
- `EmbeddingProvider`
- `CitationService`

### 为什么单独拆成一个 thread

因为该模块与 AI 模块耦合看似很高，但实际上最容易独立迭代，并且会涉及单独的数据存储、文档处理和外部服务接入。

---

## Thread 5：AI 编排模块

### 负责范围

- LangGraph workflow
- 分类节点
- 字段抽取节点
- 检索节点接入
- 回复建议节点
- AI 输出 schema 定义
- AI 执行记录写入
- Prompt 模板管理（MVP 可本地配置化）

### 输入

- Thread 3 的工单基础数据读取接口
- Thread 4 的检索服务

### 输出

- 一个完整 AI 主链路
- 对 Ticket/Workflow 模块输出结构化结论

### 边界原则

- AI 模块不能直接写死工单状态
- 只能返回：分类、抽取字段、建议、是否审批、是否人工接管
- 最终状态变化由 Ticket / Workflow 模块完成

### 对外契约

- `AiOrchestrationService`
- `runForTicket(ticketId)`
- 输出 `AiDecisionResult`

### 为什么单独拆

这个模块会频繁迭代 prompt、模型和 schema，单独隔离后不会反复污染主业务代码。

---

## Thread 6：审批工作流与可观测性模块

### 负责范围

- Temporal workflow 定义
- 审批挂起/恢复
- 审批回调幂等处理
- SLA 超时逻辑（MVP 可先做基础版）
- OpenTelemetry 接入
- trace / metric / log 打点
- AI 流程与工单流程链路关联

### 输入

- Thread 3 的工单领域接口
- Thread 5 的 AI 决策结果

### 输出

- 可恢复的审批流程
- 可追踪的全链路执行信息

### 边界原则

- 不负责前端页面
- 不负责文档解析或 AI 内部节点实现
- 主要负责“流程可靠性 + 系统可观测性”

### 对外契约

- `ApprovalWorkflowService`
- `ApprovalCommandService`
- `TelemetryService`

---

## 是否要单独拆前端 thread？

如果 Codex thread 数量充足，建议再拆一个：

## Thread 7（可选）：前端控制台

### 负责范围

- 登录页
- 工单列表页
- 工单详情页
- 审批页
- 文档管理页
- 基础监控页

### 输入

- 后端 OpenAPI
- UI 设计约定

### 输出

- 可演示控制台

### 说明

如果 thread 数有限，可在 Thread 1 完成骨架后，最后由一个专门 thread 接手前端。

---

## 13. 为什么这样拆 thread 效果会更好

相比按“前后端”或“数据库/接口/页面”粗暴拆分，这种方式优势更明显：

### 13.1 冲突更少

- Thread 3 稳定核心领域模型
- Thread 4 和 5 通过 service 接口读写，不直接反复修改 ticket 核心代码
- Thread 6 负责 workflow 和观测，不侵入 AI 节点内部逻辑

### 13.2 并行度更高

可并行推进：

- Thread 1 先搭环境
- Thread 2、3 同时开发
- Thread 4、5 在 Ticket 基础接口 ready 后并行
- Thread 6 后接入 workflow 和 trace
- 前端 thread 后期快速接入接口联调

### 13.3 适合 agent 协作

每个 thread 有清晰的目标、边界、输入、输出和接口契约，适合 Codex 这类多 agent 协作模式。

---

## 14. 多 thread 协作规则

为了避免多个 thread 协作失控，建议遵守以下规则：

### 14.1 统一先由 Thread 1 建立规范

包括：

- 包结构
- 命名规范
- DTO / Response 规范
- 错误码规范
- 数据库 migration 方式
- 配置方式

### 14.2 Thread 3 的领域模型优先冻结

Ticket 核心表、状态机、领域服务接口先定下来，其他 thread 尽量不要随意改动。

### 14.3 所有跨模块通信优先通过 service 接口或事件

不要跨 thread 直接写对方内部实现。

### 14.4 AI 输出必须 schema 化

例如：

```json
{
  "category": "VPN_ISSUE",
  "priority": "MEDIUM",
  "requiresApproval": false,
  "needsHumanHandoff": false,
  "draftReply": "...",
  "suggestedActions": ["..."],
  "extractedFields": {
    "system": "VPN",
    "issueType": "CERTIFICATE_EXPIRED"
  }
}
```

### 14.5 所有状态更新只能通过 TicketService/WorkflowService

不要让 AI 线程直接改数据库状态。

### 14.6 统一用 migration 管理表结构

推荐 Flyway 或 Liquibase。

---

## 15. 推荐开发顺序

### Phase 1

- Thread 1：基础设施
- Thread 2：认证权限
- Thread 3：工单核心

### Phase 2

- Thread 4：知识库与检索
- Thread 5：AI 编排

### Phase 3

- Thread 6：审批工作流与可观测性
- Thread 7（可选）：前端控制台

### Phase 4

- 联调
- 压测
- Bugfix
- Demo 数据与文档完善

---

## 16. 里程碑

## Milestone 1：业务骨架可运行

完成：

- 登录
- 工单 CRUD
- 状态机
- 审计日志

## Milestone 2：知识检索接入

完成：

- 文档上传
- Qdrant 检索
- 工单详情页展示引用

## Milestone 3：AI 主链路闭环

完成：

- 分类
- 抽取
- 检索
- 自动回复建议

## Milestone 4：审批流闭环

完成：

- 审批挂起
- 审批回调
- 状态恢复

## Milestone 5：可观测性与演示准备

完成：

- Trace
- Metrics
- Dashboard
- README
- 架构图
- 演示脚本

---

## 17. 后续迭代方向

MVP 完成后，可按以下方向演进：

1. 多渠道接入（邮件/IM）
2. 多租户
3. 自动执行内部操作
4. 知识缺口分析
5. 智能升级/转派
6. 工单质量评测
7. Prompt / Agent 版本管理
8. 成本控制与模型路由

---

## 18. 附录：建议仓库目录（后端）

```text
backend/
  src/main/java/com/example/ticketing/
    common/
    auth/
    user/
    ticket/
    approval/
    knowledge/
    ai/
    workflow/
    observability/
    config/
  src/main/resources/
    db/migration/
    application.yml
frontend/
infra/
  docker/
  otel/
  temporal/
README.md
```

---

## 19. 附录：建议 thread 对应目录映射

- Thread 1：`infra/`、`common/`、`config/`
- Thread 2：`auth/`、`user/`
- Thread 3：`ticket/`
- Thread 4：`knowledge/`
- Thread 5：`ai/`
- Thread 6：`approval/`、`workflow/`、`observability/`
- Thread 7（可选）：`frontend/`

这样拆分后，多数 thread 可以在独立目录和清晰契约上工作，减少反复合并冲突。

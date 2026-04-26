# Ticket Core 模块说明

本文档用于说明当前仓库中 Ticket Core 模块的设计边界、核心接口，以及其他 thread 如何通过稳定 service 契约使用工单核心能力。

## 1. 模块目标

本模块负责提供企业级 AI 工单编排系统的主业务骨架，覆盖：

- 工单主表模型
- 工单事件表模型
- 工单评论表模型
- 工单 CRUD 基础能力
- 工单列表查询与筛选
- 工单详情聚合查询
- 工单评论/补充说明
- 工单分配
- 工单状态机
- 工单时间线/事件日志记录

本模块不实现 AI 内部推理、知识库检索、Temporal 审批流程或前端页面，只负责稳定的 Ticket 核心领域模型和领域服务入口。

## 2. 关键边界

- Ticket Core 是工单领域模型拥有者
- 其他模块不能绕过 Ticket Core 直接修改 `tickets.status`
- 所有关键状态变化都必须写入 `ticket_events`
- 所有跨模块写操作优先通过 service 接口完成
- 设计目标优先是稳定和清晰，而不是过度抽象
- `category` 已与全项目标准类别对齐，所有新入参、新输出和新写入值都必须使用标准 code

## 3. 当前状态机

支持以下状态：

- `OPEN`
- `AI_PROCESSING`
- `WAITING_APPROVAL`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`
- `REJECTED`

当前允许的状态流转如下：

- `OPEN -> AI_PROCESSING | WAITING_APPROVAL | IN_PROGRESS | RESOLVED | REJECTED | CLOSED`
- `AI_PROCESSING -> OPEN | WAITING_APPROVAL | IN_PROGRESS | RESOLVED | REJECTED`
- `WAITING_APPROVAL -> IN_PROGRESS | REJECTED`
- `IN_PROGRESS -> WAITING_APPROVAL | RESOLVED | REJECTED | CLOSED`
- `RESOLVED -> IN_PROGRESS | CLOSED`
- `REJECTED -> CLOSED`
- `CLOSED` 为终态，不允许继续流转

额外规则：

- `WAITING_APPROVAL` 不允许直接关闭
- 普通请求人默认只能关闭自己已 `RESOLVED` 的工单
- `SUPPORT_AGENT` 和 `ADMIN` 可以执行人工分配和常规状态更新
- `APPROVER` 默认可查看 `WAITING_APPROVAL` 工单，但不直接拥有 Ticket Core 的任意状态修改权限

## 4. 数据模型

### 4.1 tickets

核心字段：

- `id`
- `title`
- `description`
- `category`
- `priority`
- `status`
- `requester_id`
- `assignee_id`
- `created_at`
- `updated_at`

说明：

- `status` 始终必填，确保任意工单都有明确状态
- `requester_id` 指向提交人
- `assignee_id` 指向当前处理人，可为空
- `category` 使用全项目统一标准 code，例如 `REMOTE_ACCESS`、`ACCESS_REQUEST`

### 4.2 ticket_events

核心字段：

- `id`
- `ticket_id`
- `event_type`
- `event_summary`
- `event_payload`
- `operator_id`
- `created_at`

说明：

- 用于记录工单创建、状态变化、评论、分配等关键事件
- `event_payload` 为 `jsonb`，便于后续 AI、审批、工作流扩展
- 事件日志按时间升序可回放完整时间线

### 4.3 ticket_comments

核心字段：

- `id`
- `ticket_id`
- `author_id`
- `content`
- `created_at`
- `updated_at`

说明：

- 用于承载用户补充说明和处理过程评论
- 评论本身也会同步写入 `ticket_events`

## 5. 关键代码位置

### 数据库 migration

- `backend/src/main/resources/db/migration/V3__init_ticket_core.sql`

### 领域模型

- `backend/src/main/java/com/enterprise/ticketing/ticket/domain/TicketStatus.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/domain/TicketPriority.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/domain/TicketEventType.java`

### 实体与仓库

- `backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketEventEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketCommentEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/repository/TicketRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/repository/TicketEventRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/repository/TicketCommentRepository.java`

### Service 契约

- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketQueryService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketEventService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketCommentService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketAssignmentService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketAiDecisionService.java`

### Service 实现

- `backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketCoreServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketQueryServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketAccessPolicy.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/DefaultTicketAiDecisionService.java`

### API 控制器

- `backend/src/main/java/com/enterprise/ticketing/ticket/controller/TicketController.java`

## 6. 当前提供的 HTTP 接口

已实现以下接口：

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/assign`
- `POST /api/tickets/{id}/status`

所有接口统一返回 `Result<T>`，并要求携带 JWT。

## 6.1 AI 决策消费规则

Ticket Core 当前不再只根据 `draftReply` 或空 `citations` 判断是否继续自动流转，而是通过 `TicketAiDecisionService` 显式消费 Thread 5 升级后的 `AiDecisionResult` 字段。

当前重点消费：

- `needsHumanHandoff`
- `fallbackUsed`
- `fallbackReason`
- `retrievalStatus`
- `retrievalDiagnostics`

当前规则：

- 若 `needsHumanHandoff=true`，则要求人工复核
- 若 `fallbackUsed=true`，则要求人工复核
- 若 `retrievalStatus=ERROR` 或 `UNAVAILABLE`，则要求人工复核
- 若以上风险均不存在，才允许继续自动流转
- `EMPTY` 不再被误判为“AI 未运行”，是否人工介入由 `needsHumanHandoff` 和上述风险字段共同决定
- Ticket Core 的 `category` 创建入参和列表筛选也只接受标准 code，不再接受 `VPN`、`IT` 等自由文本

当前事件日志约定：

- 当 AI 结果被判定为需要人工复核时，写入 `ticket_events`
- 事件类型为 `AI_REVIEW_REQUIRED`
- `event_payload` 中会保留 `fallbackUsed / fallbackReason / retrievalStatus / retrievalDiagnostics / reasons`

## 7. 其他 thread 如何使用 Ticket Core

### 7.1 Thread 4：知识库与检索

建议方式：

- 通过 `TicketQueryService` 读取工单标题、描述、状态、分类等基础信息
- 不直接更新工单状态
- 若需要在详情页展示 citation，由 Thread 4 自己维护 citation 数据，再在后续聚合层接入

不要做的事：

- 不直接写 `tickets`
- 不直接写 `ticket_events`

### 7.2 Thread 5：AI 编排

建议方式：

- 使用 `TicketQueryService` 读取工单内容
- AI 只输出结构化决策，不直接改数据库
- Ticket Core 与 AI 之间传递的 `category` 应直接使用标准 code，例如 `REMOTE_ACCESS`
- 若要判断 Ticket Core 是否允许继续自动流转，优先调用：
  - `TicketAiDecisionService.assessDecision(decisionResult)`
- 当 AI 进入处理中，可调用：
  - `TicketService.markAiProcessing(ticketId, summary)`
- 若 AI 判断需要审批，不要只凭 `draftReply` 或空 `citations` 决定下一步，应先结合：
  - `fallbackUsed`
  - `fallbackReason`
  - `retrievalStatus`
  - `retrievalDiagnostics`
  - `needsHumanHandoff`
- 如需记录 AI 侧补充事件，可通过：
  - `TicketEventService.recordEvent(...)`

不要做的事：

- 不要直接改 `tickets.status`
- 不要跳过状态机
- 不要把空 `citations` 直接当成 AI 未执行或一定失败
- 不要再向 Ticket Core 传 `VPN`、`VPN_ISSUE`、`IT` 这类自由文本 category

### 7.3 Thread 6：审批与工作流

建议方式：

- 启动审批前，先调用：
  - `TicketAiDecisionService.assessDecision(decisionResult)`
- 只有当 `assessment.approvalFlowAllowed=true` 时，才继续自动发起审批
- 若 `assessment.manualReviewRequired=true`，应停止自动流转，并保留人工复核入口
- 工作流挂起时调用 `TicketService.markWaitingApproval(...)`
- 审批通过后，由 Workflow 层决定下一步是：
  - `updateStatus(ticketId, IN_PROGRESS, reason)`
  - 或 `markResolved(ticketId, summary)`
- 审批拒绝时调用：
  - `markRejected(ticketId, summary)`
- 需要补充流程事件时，通过：
  - `TicketEventService.recordEvent(...)`

重点：

- Workflow 是可靠执行者，但 Ticket Core 仍然是状态拥有者
- 最终状态变化必须通过 Ticket Core service 完成

### 7.4 Thread 7：前端

当前可直接依赖以下接口：

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/assign`
- `POST /api/tickets/{id}/status`

其中 `GET /api/tickets/{id}` 已是聚合结果，适合直接用于详情页。

## 8. 对外稳定契约建议

其他模块当前可以稳定依赖以下入口：

- `TicketService`
- `TicketQueryService`
- `TicketEventService`
- `TicketCommentService`
- `TicketAssignmentService`
- `TicketAiDecisionService`

建议优先使用的方法：

- `createTicket`
- `assignTicket`
- `updateStatus`
- `appendComment`
- `markAiProcessing`
- `markWaitingApproval`
- `markResolved`
- `markRejected`
- `assessDecision`

## 9. 当前限制与后续扩展点

当前实现刻意保持简单，以下内容留给后续 thread 扩展：

- 更细粒度的字段编辑规则
- SLA、升级、撤销、重开等更复杂状态语义
- 更丰富的 AI 专属事件类型和自动流转策略
- AI 运行记录与 citation 聚合展示
- 更复杂的权限策略和部门级隔离

当前原则是先冻结核心模型和 service 入口，减少后续 thread 冲突。

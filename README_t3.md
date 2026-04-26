# Ticket Core 模块说明

本文档用于说明当前仓库中 Ticket Core 模块的设计边界、使用方式、验证方法，以及其他 thread 如何通过稳定 service 契约使用工单核心能力。

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

当前事件日志约定：

- 当 AI 结果被判定为需要人工复核时，写入 `ticket_events`
- 事件类型为 `AI_REVIEW_REQUIRED`
- `event_payload` 中会保留 `fallbackUsed / fallbackReason / retrievalStatus / retrievalDiagnostics / reasons`

## 7. 如何使用

### 7.1 启动前准备

确保以下基础设施可用：

- PostgreSQL
- 认证模块已初始化用户和角色

如果本地使用仓库默认方式，可在项目根目录执行：

```bash
docker compose up -d
```

然后进入后端目录启动应用：

```bash
cd backend
mvn spring-boot:run
```

### 7.2 可直接复制的 curl 命令

建议先准备几个环境变量：

```bash
BASE_URL='http://localhost:8080'
USERNAME='employee01'
PASSWORD='ChangeMe123!'
```

#### 7.2.1 登录并获取 token

```bash
curl -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$USERNAME\",
    \"password\": \"$PASSWORD\"
  }"
```

如果本机安装了 `jq`，可以直接提取 token：

```bash
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$USERNAME\",
    \"password\": \"$PASSWORD\"
  }" | jq -r '.data.accessToken')
```

如果没有 `jq`，也可以先手工复制返回里的 `accessToken`：

```bash
TOKEN='把这里替换成登录返回的accessToken'
```

建议顺手准备一个工单 ID 变量：

```bash
TICKET_ID=1
```

#### 7.2.2 创建工单

```bash
curl -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 证书失效",
    "description": "今天在家连接公司 VPN 失败，客户端提示证书失效。",
    "category": "VPN",
    "priority": "HIGH"
  }'
```

如果安装了 `jq`，可以直接把创建出的工单 ID 记下来：

```bash
TICKET_ID=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 证书失效",
    "description": "今天在家连接公司 VPN 失败，客户端提示证书失效。",
    "category": "VPN",
    "priority": "HIGH"
  }' | jq -r '.data.id')
```

#### 7.2.3 查询工单列表

查询全部可见工单：

```bash
curl "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $TOKEN"
```

按状态和优先级筛选：

```bash
curl "$BASE_URL/api/tickets?page=0&size=20&status=OPEN&priority=HIGH" \
  -H "Authorization: Bearer $TOKEN"
```

按关键字筛选：

```bash
curl "$BASE_URL/api/tickets?keyword=VPN&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

#### 7.2.4 查询工单详情

```bash
curl "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $TOKEN"
```

#### 7.2.5 添加评论

```bash
curl -X POST "$BASE_URL/api/tickets/$TICKET_ID/comments" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "content": "我切换了网络后仍然报同样的错误。"
  }'
```

#### 7.2.6 分配工单

这个接口一般要用 `support01` 或 `admin01` 的 token。

注意：

- `assigneeId` 校验的是“被分配人”的角色，不是当前操作者的角色
- 即使当前登录的是 `admin01`，如果 `assigneeId` 对应的是普通员工，也会返回 `Assignee must have SUPPORT_AGENT or ADMIN role`
- 当前演示账号的数据库 ID 不是稳定契约，不要假设 `2` 一定是 `support01`

先查看当前 token 对应用户：

```bash
curl "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN"
```

如果你用 `admin01` 登录并想先分配给自己，可以把返回里的 `data.id` 记下来：

```bash
ASSIGNEE_ID=4
```

```bash
curl -X POST "$BASE_URL/api/tickets/$TICKET_ID/assign" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"assigneeId\": $ASSIGNEE_ID,
    \"note\": \"转交给 VPN 支持同学处理\"
  }"
```

#### 7.2.7 更新工单状态

更新为 `IN_PROGRESS`：

```bash
curl -X POST "$BASE_URL/api/tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "IN_PROGRESS",
    "reason": "已由支持人员接单处理"
  }'
```

更新为 `RESOLVED`：

```bash
curl -X POST "$BASE_URL/api/tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "RESOLVED",
    "reason": "已指导用户刷新证书并恢复连接"
  }'
```

请求人关闭已解决工单：

```bash
curl -X POST "$BASE_URL/api/tickets/$TICKET_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "CLOSED",
    "reason": "问题已确认解决"
  }'
```

#### 7.2.8 验证当前登录用户

这个不是 Ticket Core 接口，但联调时很常用：

```bash
curl "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN"
```

#### 7.2.9 一组最小联调命令

按顺序执行：

```bash
BASE_URL='http://localhost:8080'
USERNAME='employee01'
PASSWORD='ChangeMe123!'
TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$USERNAME\",
    \"password\": \"$PASSWORD\"
  }" | jq -r '.data.accessToken')
TICKET_ID=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 证书失效",
    "description": "今天在家连接公司 VPN 失败，客户端提示证书失效。",
    "category": "VPN",
    "priority": "HIGH"
  }' | jq -r '.data.id')
curl "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### 7.3 登录获取 JWT

使用 Thread 2 提供的登录接口：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "employee01",
  "password": "ChangeMe123!"
}
```

后续调用 Ticket Core 接口时，需带上：

```http
Authorization: Bearer <accessToken>
```

### 7.4 创建工单

请求：

```http
POST /api/tickets
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "VPN 证书失效",
  "description": "今天在家连接公司 VPN 失败，客户端提示证书失效。",
  "category": "VPN",
  "priority": "HIGH"
}
```

预期：

- 工单状态默认为 `OPEN`
- `requester_id` 为当前登录用户
- 自动写入一条 `CREATED` 事件

### 7.5 查询工单列表

支持分页和基础筛选：

```http
GET /api/tickets?page=0&size=20&status=OPEN&priority=HIGH
Authorization: Bearer <token>
```

支持字段：

- `keyword`
- `status`
- `priority`
- `category`
- `requesterId`
- `assigneeId`
- `page`
- `size`
- `sortBy`
- `sortDirection`

权限规则：

- 普通员工默认只能看到自己提交的工单，或分配给自己的工单
- `SUPPORT_AGENT` / `ADMIN` 可查看全部工单
- `APPROVER` 可查看处于 `WAITING_APPROVAL` 的工单

### 7.6 查询工单详情

请求：

```http
GET /api/tickets/{id}
Authorization: Bearer <token>
```

返回聚合结构包括：

- 基础信息
- 评论列表
- 时间线事件列表

这就是当前详情页后端聚合接口。

### 7.7 添加评论

请求：

```http
POST /api/tickets/{id}/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "我切换了网络后仍然报同样的错误。"
}
```

预期：

- 新增一条评论
- 自动写入一条 `COMMENT_ADDED` 事件

### 7.8 分配工单

请求：

```http
POST /api/tickets/{id}/assign
Authorization: Bearer <token>
Content-Type: application/json

{
  "assigneeId": 4,
  "note": "转交给 VPN 支持同学处理"
}
```

限制：

- 仅 `SUPPORT_AGENT` / `ADMIN` 可执行
- 被分配人必须拥有 `SUPPORT_AGENT` 或 `ADMIN` 角色
- `CLOSED` / `REJECTED` 这类终态工单不允许再分配
- 示例中的 `4` 只是演示，实际请以 `/api/auth/me` 或数据库查询到的真实 ID 为准
- 自动写入一条 `ASSIGNED` 事件

### 7.9 更新状态

请求：

```http
POST /api/tickets/{id}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "reason": "已由支持人员接单处理"
}
```

预期：

- 状态变化经过状态机校验
- 自动写入一条 `STATUS_CHANGED` 事件

## 8. 如何验证

### 8.1 编译验证

在项目根目录执行：

```bash
cd backend
mvn -q -DskipTests compile
```

当前实现已通过该编译检查。

### 8.2 数据库验证

应用启动后，确认 Flyway 已执行 `V3__init_ticket_core.sql`，数据库中应存在：

- `tickets`
- `ticket_events`
- `ticket_comments`

### 8.3 创建工单验证

1. 使用 `employee01` 登录
2. 调用 `POST /api/tickets`
3. 检查返回体中状态是否为 `OPEN`
4. 检查 `ticket_events` 中是否写入 `CREATED`

### 8.4 状态机验证

建议验证以下场景：

1. 创建工单后从 `OPEN -> IN_PROGRESS`
2. 从 `IN_PROGRESS -> RESOLVED`
3. 由请求人将 `RESOLVED -> CLOSED`
4. 尝试 `WAITING_APPROVAL -> CLOSED`，应失败
5. 尝试 `CLOSED -> OPEN`，应失败

### 8.5 评论与时间线验证

1. 对工单调用 `POST /api/tickets/{id}/comments`
2. 查询 `GET /api/tickets/{id}`
3. 确认返回中包含：
   - 新评论
   - 对应 `COMMENT_ADDED` 时间线事件

### 8.6 分配验证

1. 使用 `support01` 或 `admin01` 登录
2. 调用 `POST /api/tickets/{id}/assign`
3. 确认工单返回中 `assignee` 已更新
4. 确认 `ticket_events` 中存在 `ASSIGNED`

### 8.7 权限验证

建议验证以下场景：

1. 普通员工访问他人工单，应返回 `403`
2. 普通员工尝试分配工单，应返回 `403`
3. 普通员工尝试任意更新未解决工单状态，应返回 `403`
4. 审批人访问 `WAITING_APPROVAL` 工单详情，应成功

## 9. 其他 thread 如何使用 Ticket Core

### 9.1 Thread 4：知识库与检索

建议方式：

- 通过 `TicketQueryService` 读取工单标题、描述、状态、分类等基础信息
- 不直接更新工单状态
- 若需要在详情页展示 citation，由 Thread 4 自己维护 citation 数据，再在后续聚合层接入

不要做的事：

- 不直接写 `tickets`
- 不直接写 `ticket_events`

### 9.2 Thread 5：AI 编排

建议方式：

- 使用 `TicketQueryService` 读取工单内容
- AI 只输出结构化决策，不直接改数据库
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

### 9.3 Thread 6：审批与工作流

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

### 9.4 Thread 7：前端

当前可直接依赖以下接口：

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/assign`
- `POST /api/tickets/{id}/status`

其中 `GET /api/tickets/{id}` 已是聚合结果，适合直接用于详情页。

## 10. 对外稳定契约建议

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

## 11. 当前限制与后续扩展点

当前实现刻意保持简单，以下内容留给后续 thread 扩展：

- 更细粒度的字段编辑规则
- SLA、升级、撤销、重开等更复杂状态语义
- 更丰富的 AI 专属事件类型和自动流转策略
- AI 运行记录与 citation 聚合展示
- 更复杂的权限策略和部门级隔离

当前原则是先冻结核心模型和 service 入口，减少后续 thread 冲突。

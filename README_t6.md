# Workflow / Approval / Observability 模块说明

本文档说明 Thread 6 交付的审批工作流与可观测性模块，包括 Temporal workflow、审批挂起/恢复、幂等处理，以及给其他 thread 使用的稳定契约。

## 1. 模块目标

本模块负责：

- AI 结果触发审批 workflow
- 审批链路可靠执行
- 挂起等待人工审批并恢复
- 审批回调幂等保护
- OpenTelemetry trace / metric / log 基础接入
- 为监控页提供后端聚合指标与 Prometheus 指标

本模块不负责：

- 工单 CRUD
- AI 内部节点逻辑
- 文档解析
- 前端页面

## 2. 当前审批模板

MVP 只支持单模板两级审批：

1. `LINE_MANAGER` 阶段
   - 当前通过 `APPROVER` 角色解析审批人
2. `SYSTEM_ADMIN` 阶段
   - 当前通过 `ADMIN` 角色解析审批人

说明：

- 当前用户模型没有“直属主管”关系，因此 MVP 先以 `APPROVER` 角色代替直属主管位点
- 后续只需替换 `ApprovalStageKey` 对应的审批人解析逻辑，不需要改 Temporal workflow 主体

## 3. 关键流程

### 3.1 AI 触发审批

当前 `POST /api/ai/tickets/{id}/run` 已接入审批 workflow：

1. Thread 5 的 `AiOrchestrationService` 产出 `AiDecisionResult`
2. `TicketAiDecisionService` 会先评估 AI 结果是否允许自动流转
3. 若出现以下任一条件，则不会自动启动审批 workflow，而是写入 `AI_REVIEW_REQUIRED` 事件，转为人工复核：
   - `needsHumanHandoff=true`
   - `fallbackUsed=true`
   - `retrievalStatus=ERROR`
   - `retrievalStatus=UNAVAILABLE`
4. 只有当：
   - `requiresApproval=true`
   - 且未命中上述人工复核条件
   时，`ApprovalWorkflowService.handleAiDecision(...)` 才会启动 Temporal workflow
5. workflow 第一个 activity 会：
   - 创建首个审批记录
   - 将工单推进到 `WAITING_APPROVAL`
   - 写入 `WORKFLOW_STARTED` / `APPROVAL_REQUESTED` 事件

### 3.1.1 当前消费的 AI 契约字段

Workflow / Approval / Observability 当前显式消费以下 AI 字段：

- `requiresApproval`
- `needsHumanHandoff`
- `fallbackUsed`
- `fallbackReason`
- `retrievalStatus`
- `retrievalDiagnostics`

其中：

- `requiresApproval` 决定是否具备进入审批流的候选资格
- `needsHumanHandoff`、`fallbackUsed`、`retrievalStatus=ERROR|UNAVAILABLE` 会阻断自动流转
- `fallbackReason` 和 `retrievalDiagnostics` 会进入事件日志与观测信息，方便排障和人工复核

### 3.2 挂起 / 恢复

Temporal workflow 定义在：

- `backend/src/main/java/com/enterprise/ticketing/workflow/ApprovalWorkflow.java`
- `backend/src/main/java/com/enterprise/ticketing/workflow/impl/ApprovalWorkflowImpl.java`

执行模型：

1. `openApprovalStage(...)` 创建当前审批节点
2. workflow 使用 `Workflow.await(...)` 挂起等待 `submitDecision(...)` signal
3. 审批接口发送 signal 后，workflow 恢复执行
4. 审批通过：
   - 若还有下一阶段，则继续创建下一审批节点
   - 若已是最终阶段，则工单推进到 `IN_PROGRESS`
5. 审批驳回：
   - 工单推进到 `REJECTED`
   - workflow 结束

## 4. 幂等策略

### 4.1 workflow 启动幂等

- 同一张工单若已存在 `PENDING` 审批，则不会重复启动新 workflow
- 同一 `workflowId` 重复启动时，Temporal `WorkflowExecutionAlreadyStarted` 会被吞并为幂等返回

### 4.2 审批回调幂等

- 审批记录以 `status=PENDING/APPROVED/REJECTED` 为单向状态机
- 已终态的审批再次收到相同动作：
  - 返回当前结果
  - 计入 retry metric
- 已终态的审批再次收到相反动作：
  - 返回冲突错误，防止状态错乱

### 4.3 activity 重试幂等

- `approvals(workflow_id, stage_order)` 唯一索引保证同一阶段只落一条审批记录
- `openApprovalStage(...)` 会先查已有记录，避免 Temporal activity 重试重复建单

## 5. 对外契约

### 5.1 Thread 5 / AI 模块调用

优先调用：

- `ApprovalWorkflowService.handleAiDecision(AiDecisionResult decisionResult)`

输入要求：

- `decisionResult.ticketId()` 必填
- `decisionResult.workflowId()` 必填
- `decisionResult.requiresApproval()` 为 `true` 仅代表“候选需要审批”
- 若 `needsHumanHandoff=true`、`fallbackUsed=true` 或 `retrievalStatus=ERROR|UNAVAILABLE`，则不会自动启动 workflow

### 5.2 审批接口

- `GET /api/approvals/pending`
- `GET /api/approvals/tickets/{ticketId}`
- `POST /api/approvals/{id}/approve`
- `POST /api/approvals/{id}/reject`

### 5.3 监控接口

- `GET /api/observability/dashboard`

该接口适合 Thread 7 直接拉取监控页概览数据。

## 6. 关键文件

### 数据库

- `backend/src/main/resources/db/migration/V6__init_approval_workflow.sql`

### Approval

- `backend/src/main/java/com/enterprise/ticketing/approval/entity/ApprovalEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/approval/repository/ApprovalRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/approval/service/ApprovalWorkflowService.java`
- `backend/src/main/java/com/enterprise/ticketing/approval/service/ApprovalCommandService.java`
- `backend/src/main/java/com/enterprise/ticketing/approval/controller/ApprovalController.java`
- `backend/src/main/java/com/enterprise/ticketing/approval/service/impl/ApprovalWorkflowServiceImpl.java`

### Workflow / Temporal

- `backend/src/main/java/com/enterprise/ticketing/config/TemporalWorkflowConfig.java`
- `backend/src/main/java/com/enterprise/ticketing/workflow/ApprovalWorkflow.java`
- `backend/src/main/java/com/enterprise/ticketing/workflow/impl/ApprovalWorkflowImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/workflow/impl/ApprovalWorkflowActivitiesImpl.java`

### AI 决策评估桥接

- `backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketAiDecisionService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/DefaultTicketAiDecisionService.java`
- `backend/src/main/java/com/enterprise/ticketing/ticket/dto/TicketAiDecisionAssessment.java`

### Observability

- `backend/src/main/java/com/enterprise/ticketing/observability/service/TelemetryService.java`
- `backend/src/main/java/com/enterprise/ticketing/observability/service/impl/DefaultTelemetryService.java`
- `backend/src/main/java/com/enterprise/ticketing/observability/controller/ObservabilityController.java`
- `infra/grafana/dashboards/enterprise-ai-ticketing-workflow.json`

## 7. 当前指标

Prometheus 指标覆盖：

- `ticketing.ticket.total`
- `ticketing.ticket.status`
- `ticketing.ai.orchestration.runs`
- `ticketing.ai.orchestration.latency`
- `ticketing.ai.node.runs`
- `ticketing.ai.node.latency`
- `ticketing.workflow.ai.decision.handled`
- `ticketing.workflow.ai.decision.manual_review_required`
- `ticketing.workflow.approval.workflows.started`
- `ticketing.workflow.approval.workflows.completed`
- `ticketing.workflow.approval.stage.opened`
- `ticketing.workflow.approval.wait.duration`
- `ticketing.workflow.approval.commands`
- `ticketing.workflow.approval.failures`
- `ticketing.workflow.approval.retries`

## 8. 如何使用

### 8.1 启动依赖

在仓库根目录执行：

```bash
docker compose up -d
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

### 8.2 准备环境变量

建议先准备这些变量：

```bash
BASE_URL='http://localhost:8080'
EMPLOYEE_USERNAME='employee01'
APPROVER_USERNAME='approver01'
ADMIN_USERNAME='admin01'
PASSWORD='ChangeMe123!'
```

如果本机安装了 `jq`，可直接提取 token：

```bash
EMPLOYEE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$EMPLOYEE_USERNAME\",
    \"password\": \"$PASSWORD\"
  }" | jq -r '.data.accessToken')

APPROVER_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$APPROVER_USERNAME\",
    \"password\": \"$PASSWORD\"
  }" | jq -r '.data.accessToken')

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"password\": \"$PASSWORD\"
  }" | jq -r '.data.accessToken')
```

如果没有 `jq`，可以先单独执行登录命令，手工复制返回中的 `accessToken`：

```bash
curl -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "employee01",
    "password": "ChangeMe123!"
  }'
```

### 8.3 创建一个会触发审批的工单

当前规则型 AI 对权限申请类工单会倾向给出 `requiresApproval=true`。用员工账号创建工单：

```bash
TICKET_ID=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "申请生产环境日志只读权限",
    "description": "我需要开通生产环境日志只读权限，用于排查线上问题，请协助发起审批。",
    "category": "ACCESS",
    "priority": "HIGH"
  }' | jq -r '.data.id')

echo "$TICKET_ID"
```

### 8.4 触发 AI + 启动审批 workflow

```bash
curl -X POST "$BASE_URL/api/ai/tickets/$TICKET_ID/run" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

预期：

- 返回的 AI 结果中 `requiresApproval` 为 `true`
- 若同时满足 `needsHumanHandoff=false`、`fallbackUsed=false`、`retrievalStatus` 不为 `ERROR/UNAVAILABLE`：
  - 工单状态会进入 `WAITING_APPROVAL`
  - Temporal 中会创建一个 `approval-ticket-<ticketId>-<aiWorkflowId>` workflow
- 若命中人工复核条件：
  - 不会自动进入 `WAITING_APPROVAL`
  - 不会自动创建审批 workflow
  - 工单时间线中会出现 `AI_REVIEW_REQUIRED`

### 8.5 查看待审批列表

直属主管阶段由 `approver01` 处理：

```bash
curl "$BASE_URL/api/approvals/pending" \
  -H "Authorization: Bearer $APPROVER_TOKEN"
```

如果安装了 `jq`，可以直接提取当前审批 ID：

```bash
APPROVAL_ID=$(curl -s "$BASE_URL/api/approvals/pending" \
  -H "Authorization: Bearer $APPROVER_TOKEN" | jq -r '.data[0].approvalId')

echo "$APPROVAL_ID"
```

### 8.6 直属主管审批通过

```bash
curl -X POST "$BASE_URL/api/approvals/$APPROVAL_ID/approve" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "comment": "业务理由已确认，同意进入管理员审批。",
    "requestId": "approve-stage-1-demo"
  }'
```

执行后，再看管理员待审批：

```bash
curl "$BASE_URL/api/approvals/pending" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

提取管理员阶段审批 ID：

```bash
ADMIN_APPROVAL_ID=$(curl -s "$BASE_URL/api/approvals/pending" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data[0].approvalId')

echo "$ADMIN_APPROVAL_ID"
```

### 8.7 系统管理员审批通过

```bash
curl -X POST "$BASE_URL/api/approvals/$ADMIN_APPROVAL_ID/approve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "comment": "管理员审批通过。",
    "requestId": "approve-stage-2-demo"
  }'
```

审批全部通过后，查询工单详情：

```bash
curl "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

预期：

- 工单状态从 `WAITING_APPROVAL` 进入 `IN_PROGRESS`
- 时间线中能看到 `WORKFLOW_STARTED`、`APPROVAL_REQUESTED`、`APPROVAL_APPROVED`、`WORKFLOW_COMPLETED`

### 8.8 驳回路径验证

如果你想验证驳回链路，重新创建一个权限申请工单并再次触发 AI，然后在任一审批阶段执行：

```bash
curl -X POST "$BASE_URL/api/approvals/$APPROVAL_ID/reject" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "comment": "当前缺少必要的业务审批依据，驳回。",
    "requestId": "reject-stage-1-demo"
  }'
```

预期：

- 工单状态进入 `REJECTED`
- workflow 结束
- 时间线中出现 `APPROVAL_REJECTED` 和 `WORKFLOW_COMPLETED`

## 9. 如何验证

### 9.1 验证工单进入 `WAITING_APPROVAL`

```bash
curl "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

检查返回中的：

- `data.ticket.status == "WAITING_APPROVAL"`，在第一阶段审批前成立

前提：

- `requiresApproval=true`
- `needsHumanHandoff=false`
- `fallbackUsed=false`
- `retrievalStatus` 不为 `ERROR` 或 `UNAVAILABLE`

### 9.2 验证审批挂起与恢复

先查询待审批项：

```bash
curl "$BASE_URL/api/approvals/pending" \
  -H "Authorization: Bearer $APPROVER_TOKEN"
```

再执行审批通过：

```bash
curl -X POST "$BASE_URL/api/approvals/$APPROVAL_ID/approve" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"comment":"approve for workflow resume","requestId":"resume-check-1"}'
```

如果此时还有下一阶段审批，说明 workflow 已恢复并继续推进；如果最终阶段已完成，再查工单状态应为 `IN_PROGRESS`。

### 9.3 验证重复审批幂等

对同一个审批再次发送相同请求：

```bash
curl -X POST "$BASE_URL/api/approvals/$APPROVAL_ID/approve" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"comment":"duplicate approve","requestId":"resume-check-1"}'
```

预期：

- 返回成功
- `data.idempotent == true`
- 工单状态不会被错误回滚或重复推进

再发送相反动作验证冲突保护：

```bash
curl -i -X POST "$BASE_URL/api/approvals/$APPROVAL_ID/reject" \
  -H "Authorization: Bearer $APPROVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"comment":"conflict check","requestId":"resume-check-2"}'
```

预期：

- 返回 `409 Conflict`

### 9.4 验证审批历史

```bash
curl "$BASE_URL/api/approvals/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

预期：

- 能看到每个阶段的 `stageOrder / stageKey / status / requestedAt / decidedAt`

### 9.5 验证 dashboard 聚合接口

```bash
curl "$BASE_URL/api/observability/dashboard" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

预期返回中至少包含：

- `totalTickets`
- `ticketStatusDistribution`
- `aiSuggestionSuccessRate`
- `averageAiLatencyMs`
- `averageRetrievalLatencyMs`
- `averageApprovalWaitMs`
- `pendingApprovals`
- `aiManualReviewRequiredCount`

### 9.6 验证人工复核阻断逻辑

如果 Thread 5 返回的 AI 结果包含以下任一条件：

- `needsHumanHandoff=true`
- `fallbackUsed=true`
- `retrievalStatus=ERROR`
- `retrievalStatus=UNAVAILABLE`

则预期行为为：

- `ApprovalWorkflowService` 返回 `manualReviewRequired=true`
- 不自动启动 Temporal workflow
- 工单时间线新增 `AI_REVIEW_REQUIRED`

验证工单时间线：

```bash
curl "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN"
```

如果安装了 `jq`，可直接筛选事件：

```bash
curl -s "$BASE_URL/api/tickets/$TICKET_ID" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" | jq '.data.timeline[] | select(.eventType=="AI_REVIEW_REQUIRED")'
```

### 9.7 验证 Prometheus 指标

```bash
curl "$BASE_URL/actuator/prometheus" | rg 'ticketing_(ticket|ai|workflow)_'
```

如果本机没有 `rg`，可用：

```bash
curl "$BASE_URL/actuator/prometheus" | grep 'ticketing_'
```

关注新增指标：

```bash
curl "$BASE_URL/actuator/prometheus" | grep 'ticketing_workflow_ai_decision'
```

### 9.8 验证 Temporal / Jaeger / Grafana

本地默认地址：

```bash
open http://localhost:8088
open http://localhost:16686
open http://localhost:3001
```

如果不想用浏览器命令，也可以直接手动打开：

- Temporal UI：`http://localhost:8088`
- Jaeger：`http://localhost:16686`
- Grafana：`http://localhost:3001`

在 Grafana 中可导入或直接使用仓库中的 dashboard 文件：

- `infra/grafana/dashboards/enterprise-ai-ticketing-workflow.json`

## 10. 其他 thread 的接入建议

### Thread 3

- 继续保持所有状态变化只通过 `TicketService`
- 不要直接修改 `tickets.status`

### Thread 5

- 继续只负责产出 `AiDecisionResult`
- 触发审批时调用 `ApprovalWorkflowService.handleAiDecision(...)`

### Thread 7

- 审批页使用 `/api/approvals/pending`
- 工单详情页审批历史使用 `/api/approvals/tickets/{ticketId}`
- 监控页概览使用 `/api/observability/dashboard`
- 更细粒度监控可直接接 Grafana / Prometheus

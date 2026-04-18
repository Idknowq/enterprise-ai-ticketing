# AI 编排模块说明

本文档用于说明当前仓库中 Thread 5 交付的 AI 编排模块的设计边界、使用方式、验证方法，以及其他 thread 如何通过稳定契约接入 AI 结果。

## 1. 模块目标

本模块负责提供企业级 AI 工单编排系统 MVP 中的 AI 主链路，覆盖：

- AI 编排主链路
- 分类节点 `Classifier`
- 字段抽取节点 `Extractor`
- 检索适配节点 `Retriever adapter`
- 处理建议节点 `Resolution`
- 结构化 AI 输出 schema
- LLM Provider 抽象与接入
- RetrievalService 对接
- `ai_runs` 执行记录落库
- 调试接口：
  - `POST /api/ai/tickets/{id}/run`
  - `GET /api/ai/tickets/{id}/runs`

本模块不是聊天层，不直接变更工单状态，不直接驱动审批流，只输出结构化、可消费、可审计的 AI 结论。

## 2. 关键边界

- AI 模块只能输出结构化结论，不能直接写工单状态
- AI 模块不能绕过 Ticket Core 或 Workflow
- 所有 AI 输出必须 schema 化
- 后端核心逻辑不能依赖裸文本
- Retrieval 只负责提供证据与引用，不负责审批判断或状态判断
- 最终状态变化必须由 Ticket 模块或 Workflow 模块完成

## 3. 当前主链路

当前主链路由 `AiOrchestrationService.runForTicket(ticketId)` 驱动，按以下顺序执行：

1. 读取工单详情
2. 分类节点输出 `category / priority / confidence`
3. 字段抽取节点输出 `extractedFields`
4. 检索节点调用 `RetrievalService`
5. 处理建议节点输出审批判断、人工接管判断、草稿回复和建议动作
6. 聚合为最终 `AiDecisionResult`
7. 将每个节点与最终编排结果写入 `ai_runs`

当前实现入口：

- `backend/src/main/java/com/enterprise/ticketing/ai/service/AiOrchestrationService.java`
- `backend/src/main/java/com/enterprise/ticketing/ai/service/impl/DefaultAiOrchestrationService.java`

## 4. 节点职责

### 4.1 Classifier

职责：

- 基于工单标题和描述做分类
- 输出工单优先级
- 输出置信度

输出字段：

- `category`
- `priority`
- `confidence`

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketClassifierNode.java`

### 4.2 Extractor

职责：

- 基于工单内容和分类结果抽取结构化字段
- 输出扁平 key-value 字段集合

示例字段：

- `system`
- `issueType`
- `environment`
- `accessLevel`
- `resourceType`
- `errorCode`

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketExtractorNode.java`

### 4.3 Retriever adapter

职责：

- 将工单内容和抽取字段组装成检索查询
- 调用 Thread 4 提供的 `RetrievalService.search(...)`
- 将检索结果统一转换为 AI 侧 `AiCitation`

当前行为：

- 若仓库中有可用的 `RetrievalService` 实现，则优先调用检索服务
- 若检索服务未实现或调用异常，则回退到启发式 citations，保证 MVP 主链路可跑通

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketRetrieverNode.java`

### 4.4 Resolution

职责：

- 基于分类、字段抽取、检索引用输出最终建议
- 判断是否需要审批
- 判断是否需要人工接管
- 输出回复草稿与建议动作

输出字段：

- `requiresApproval`
- `needsHumanHandoff`
- `draftReply`
- `suggestedActions`

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketResolutionNode.java`

## 5. 最终 schema

当前 AI 最终结果定义为 `AiDecisionResult`：

```json
{
  "workflowId": "ai-7a2c...",
  "ticketId": 123,
  "category": "VPN_ISSUE",
  "priority": "MEDIUM",
  "confidence": 0.92,
  "requiresApproval": false,
  "needsHumanHandoff": false,
  "draftReply": "AI triage suggests category VPN_ISSUE...",
  "suggestedActions": [
    "Verify whether the local VPN certificate or token has expired."
  ],
  "extractedFields": {
    "system": "VPN",
    "issueType": "CERTIFICATE_EXPIRED"
  },
  "citations": [
    {
      "sourceType": "RETRIEVAL_SERVICE",
      "documentId": 1,
      "chunkId": "chunk-1",
      "title": "VPN Certificate Renewal SOP",
      "snippet": "Steps to renew expired VPN certificates...",
      "score": 0.88,
      "sourceRef": "citation:1001"
    }
  ],
  "generatedAt": "2026-04-16T10:00:00Z"
}
```

Java 定义位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/dto/AiDecisionResult.java`
- `backend/src/main/java/com/enterprise/ticketing/ai/dto/AiCitation.java`

## 6. AI 执行日志

当前使用 `ai_runs` 表记录节点级执行日志。

### 6.1 表结构

migration 文件：

- `backend/src/main/resources/db/migration/V5__init_ai_orchestration.sql`

核心字段：

- `ticket_id`
- `workflow_id`
- `node_name`
- `status`
- `model_name`
- `latency_ms`
- `token_input`
- `token_output`
- `result_summary`
- `result_payload`
- `error_message`
- `created_at`

### 6.2 记录范围

以下节点都会写入 `ai_runs`：

- `CLASSIFIER`
- `EXTRACTOR`
- `RETRIEVER`
- `RESOLUTION`
- `ORCHESTRATION`

### 6.3 指标

当前同时写入基础指标：

- `ticketing.ai.node.runs`
- `ticketing.ai.node.latency`

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/service/impl/AiRunLogService.java`

## 7. LLM Provider 设计

当前 Provider 抽象：

- `StructuredLlmProvider`
- `RuleBasedStructuredLlmProvider`
- `DeepSeekStructuredLlmProvider`
- `LlmProviderRouter`

### 7.1 默认行为

默认使用规则型 provider：

- `app.ai.provider.type=rule-based`
- `app.ai.provider.model=mvp-rule-based`

适用场景：

- 本地未配置真实模型服务
- 希望先验证完整 AI 主链路
- 单元测试和 demo 环境

### 7.2 DeepSeek Provider

若配置：

- `app.ai.provider.type=deepseek`
- `app.ai.provider.base-url`

则会调用 DeepSeek 兼容 Chat Completions 的模型服务。

若调用失败，会自动回退到规则型 provider，避免整条链路不可用。

## 8. 配置项

配置位置：

- `backend/src/main/java/com/enterprise/ticketing/config/ApplicationProperties.java`
- `backend/src/main/resources/application.yml`

当前 AI 配置：

- `app.ai.enabled`
- `app.ai.retrieval-top-k`
- `app.ai.provider.type`
- `app.ai.provider.model`
- `app.ai.provider.base-url`
- `app.ai.provider.api-key`
- `app.ai.provider.chat-path`
- `app.ai.provider.timeout`

环境变量示例：

```bash
APP_AI_ENABLED=true
APP_AI_RETRIEVAL_TOP_K=4
APP_AI_PROVIDER_TYPE=rule-based
APP_AI_PROVIDER_MODEL=mvp-rule-based
```

DeepSeek 示例：

```bash
APP_AI_PROVIDER_TYPE=deepseek
APP_AI_PROVIDER_MODEL=deepseek-chat
APP_AI_PROVIDER_BASE_URL=
APP_AI_PROVIDER_API_KEY=your-key
APP_AI_PROVIDER_CHAT_PATH=/v1/chat/completions
APP_AI_PROVIDER_TIMEOUT=20s
```

## 9. 对外接口

当前提供以下调试接口：

### 9.1 手动触发 AI 主链路

```http
POST /api/ai/tickets/{id}/run
Authorization: Bearer <token>
```

返回：

- `Result<AiDecisionResult>`

### 9.2 查询 AI 运行记录

```http
GET /api/ai/tickets/{id}/runs
Authorization: Bearer <token>
```

返回：

- `Result<List<AiWorkflowRunResponse>>`

说明：

- `runs` 结果会按 `workflowId` 分组
- 每组包含最终结果和节点级执行记录

控制器位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/controller/AiDebugController.java`

## 10. 如何使用

### 10.1 启动前准备

确保以下基础设施可用：

- PostgreSQL
- 认证模块
- Ticket Core 模块

如果本地使用仓库默认方式，可在项目根目录执行：

```bash
docker compose up -d
```

再启动后端：

```bash
cd backend
mvn spring-boot:run
```

### 10.2 准备测试数据

1. 使用 Thread 2 的账号登录
2. 使用 Thread 3 的接口创建工单
3. 调用 AI 调试接口执行主链路

### 10.3 登录并获取 token

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
```

### 10.4 创建测试工单

```bash
TICKET_ID=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 连接失败",
    "description": "我在家连接公司 VPN 失败，客户端提示证书失效。",
    "category": "IT",
    "priority": "MEDIUM"
  }' | jq -r '.data.id')
```

### 10.5 运行 AI 主链路

```bash
curl -X POST "$BASE_URL/api/ai/tickets/$TICKET_ID/run" \
  -H "Authorization: Bearer $TOKEN"
```

预期：

- 返回 `category`
- 返回 `priority`
- 返回 `confidence`
- 返回 `extractedFields`
- 返回 `citations`
- 返回 `suggestedActions`
- 返回 `requiresApproval`
- 返回 `needsHumanHandoff`

### 10.6 查询运行记录

```bash
curl "$BASE_URL/api/ai/tickets/$TICKET_ID/runs" \
  -H "Authorization: Bearer $TOKEN"
```

预期：

- 至少能看到一次 `workflowId`
- 节点列表包含：
  - `CLASSIFIER`
  - `EXTRACTOR`
  - `RETRIEVER`
  - `RESOLUTION`
  - `ORCHESTRATION`

## 11. 如何验证

### 11.1 编译与单测

进入后端目录执行：

```bash
cd backend
mvn test
```

当前已覆盖的单测：

- VPN 分类
- 权限申请字段抽取
- 需要审批场景的建议生成

测试位置：

- `backend/src/test/java/com/enterprise/ticketing/ai/provider/RuleBasedStructuredLlmProviderTest.java`

### 11.2 手工验收项

建议按以下验收：

1. 输入 `ticketId` 能跑通完整 AI 主链路
2. 能返回结构化分类结果
3. 能返回结构化字段抽取结果
4. 能返回 citations
5. 能返回处理建议和草稿回复
6. 能判断是否需要审批
7. 能判断是否需要人工接管
8. 每次执行都能在 `ai_runs` 中追踪

### 11.3 数据库验证

可直接在 PostgreSQL 中检查：

```sql
select id, ticket_id, workflow_id, node_name, status, model_name, latency_ms, created_at
from ai_runs
order by id desc;
```

预期：

- 同一次执行会有多个相同 `workflow_id` 的节点记录
- 每个节点有明确的 `node_name`
- 失败时会记录 `error_message`

## 12. 其他 thread 如何使用

### 12.1 Thread 3：Ticket Core

建议方式：

- 使用 `AiOrchestrationService.runForTicket(ticketId)` 获取结构化结果
- 根据 `AiDecisionResult` 再决定是否调用 Ticket Core service 做状态变化

可以使用的结果字段：

- `category`
- `priority`
- `requiresApproval`
- `needsHumanHandoff`
- `draftReply`
- `suggestedActions`

不要做的事：

- 不要让 AI 模块直接改 `tickets.status`
- 不要绕过 `TicketService`

### 12.2 Thread 4：Knowledge / Retrieval

建议方式：

- 实现 `RetrievalService.search(RetrievalSearchRequest request)`
- 继续返回标准 `RetrievalSearchResponse`
- AI 模块会自动消费 Thread 4 的检索结果

当前 AI 侧已传递的输入：

- `query`
- `ticketId`
- `category`
- `department`
- `limit`
- `aiRunId`

不要做的事：

- 不要在 RetrievalService 中做审批判断
- 不要直接写 Ticket 状态

### 12.3 Thread 6：Workflow / Approval / Observability

建议方式：

- 消费 `AiDecisionResult.requiresApproval`
- 消费 `AiDecisionResult.needsHumanHandoff`
- 消费 `AiDecisionResult.citations`
- 消费 `AiDecisionResult.suggestedActions`

推荐流程：

1. 调用 `runForTicket(ticketId)`
2. 若 `requiresApproval=true`，由 Workflow 层发起审批
3. 若 `requiresApproval=false`，由后续模块决定是人工处理还是进入下一步处理链路
4. 最终状态变化通过 Ticket Core 完成

不要做的事：

- 不要假设 AI 已经修改过工单状态
- 不要直接依赖 `draftReply` 文本做核心后端逻辑

### 12.4 Thread 7：前端

当前前端可以直接对接：

- `POST /api/ai/tickets/{id}/run`
- `GET /api/ai/tickets/{id}/runs`

建议展示：

- AI 分类结果
- 结构化字段
- 建议动作
- 是否需要审批
- 是否建议人工接管
- 引用来源
- 节点级执行时间和状态

## 13. 协作规范

- AI 输出必须只作为结构化决策输入，不是最终业务动作
- 其他模块消费 AI 结果时，应优先依赖字段而不是文案
- `draftReply` 只适合展示或人工编辑，不应作为后端核心逻辑判断依据
- 所有状态更新只能通过 TicketService 或 WorkflowService
- schema 变更要优先保持向后兼容，避免破坏其他 thread 对接

## 14. 当前限制与后续扩展点

当前实现刻意保持 MVP 范围，以下内容可由后续迭代补充：

- 真正的 LangGraph Java 集成
- 更复杂的 prompt 模板和版本管理
- 更完整的 token 统计
- 节点级 trace 关联到 workflow / approval
- 更细的 citation source type
- 与工单详情聚合接口深度集成
- 模型输出 schema 的显式校验与版本化

当前原则是先冻结 AI 编排入口、schema 和运行日志结构，保证其他 thread 能稳定接入。

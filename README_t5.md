# AI 编排模块说明

本文档用于说明当前仓库中 Thread 5 交付的 AI 编排模块的设计边界、基础接口，以及其他 thread 如何通过稳定契约接入 AI 结果。

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
2. 校验工单标准 `category / priority`，分类节点只输出 `confidence`
3. 字段抽取节点输出 `extractedFields`
4. 检索节点调用 `RetrievalService`
5. 处理建议节点基于 citations 和 extracted fields 输出审批判断、人工接管判断、草稿回复和建议动作
6. 聚合为最终 `AiDecisionResult`
7. 将每个节点与最终编排结果写入 `ai_runs`

当前实现入口：

- `backend/src/main/java/com/enterprise/ticketing/ai/service/AiOrchestrationService.java`
- `backend/src/main/java/com/enterprise/ticketing/ai/service/impl/DefaultAiOrchestrationService.java`

## 4. 节点职责

### 4.1 Classifier

职责：

- 校验并沿用 Ticket Core 提供的标准 `category`
- 校验并沿用 Ticket Core 提供的 `priority`
- 输出置信度
- 不允许 LLM、local model 或 rule-based 改写 category 或 priority

输出字段：

- `category`：来自 `ticket.category` 的标准 code
- `priority`：来自 `ticket.priority`
- `confidence`

实现位置：

- `backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketClassifierNode.java`

### 4.2 Extractor

职责：

- 基于工单内容和标准 category 抽取结构化字段
- 输出扁平 key-value 字段集合
- 不允许产出或覆盖 category

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

- 只消费真实 `RetrievalService` 返回结果
- 检索命中时输出真实 citations
- 检索空结果时输出空 citations，并标记 `retrievalStatus=EMPTY`
- 检索异常时输出空 citations，并标记 `retrievalStatus=ERROR`
- 若容器内不存在 `RetrievalService`，则标记 `retrievalStatus=UNAVAILABLE`

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
  "schemaVersion": "v2",
  "workflowId": "ai-7a2c...",
  "ticketId": 123,
  "category": "REMOTE_ACCESS",
  "priority": "MEDIUM",
  "confidence": 0.82,
  "providerType": "deepseek",
  "modelName": "deepseek-chat",
  "analysisMode": "REMOTE_LLM",
  "fallbackUsed": false,
  "fallbackReason": null,
  "requiresApproval": false,
  "needsHumanHandoff": false,
  "draftReply": "AI triage suggests next steps for the standardized category REMOTE_ACCESS...",
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
      "retrievalScore": 0.88,
      "rerankScore": 0.91,
      "sourceRef": "citation:1001",
      "metadata": {
        "category": "REMOTE_ACCESS",
        "department": "GLOBAL"
      }
    }
  ],
  "retrievalStatus": "HIT",
  "retrievalDiagnostics": {
    "retrievalMode": "HYBRID_CANDIDATES_WITH_RERANK",
    "candidateCount": 24,
    "returnedCount": 4,
    "filterSummary": {
      "category": "REMOTE_ACCESS",
      "departments": ["GLOBAL", "IT"]
    },
    "message": "Retrieval completed successfully"
  },
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
- `backend/src/main/resources/db/migration/V7__upgrade_ai_run_audit_fields.sql`

核心字段：

- `ticket_id`
- `workflow_id`
- `node_name`
- `status`
- `provider_type`
- `model_name`
- `latency_ms`
- `token_input`
- `token_output`
- `fallback_used`
- `fallback_reason`
- `retrieval_status`
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
- `LocalStructuredLlmProvider`
- `LlmProviderRouter`

### 7.1 默认行为

默认推荐生产使用 DeepSeek：

- `app.ai.provider.type=deepseek`
- `app.ai.provider.model=deepseek-chat`

`rule-based` 仅保留为：

- 本地无密钥测试
- Provider 故障兜底
- 单元测试 stub
- 关键词命中的保守通用策略输出

### 7.2 DeepSeek Provider

若配置：

- `app.ai.provider.type=deepseek`
- `app.ai.provider.base-url`

则会调用 DeepSeek 兼容 Chat Completions 的模型服务。

当前路由策略：

- `Classifier / Extractor / Resolution` 优先调用 DeepSeek
- `Classifier` 的 category / priority 不由模型决定，模型返回值会被 `ticket.category / ticket.priority` 覆盖
- 若本地模型已启用，则在 DeepSeek 失败后优先切到 `LocalStructuredLlmProvider`
- 模型返回非法 JSON 或缺字段时会执行有限重试
- 远端和本地模型都失败时才会回退到 `rule-based`
- 回退会写入 `ai_runs`，并回传 `fallbackUsed / fallbackReason`
- `rule-based` 不返回伪 citations，不输出旧分类名，只沿用标准 category 并输出保守的关键词通用建议

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
- `app.ai.provider.local.enabled`
- `app.ai.provider.local.type`
- `app.ai.provider.local.model`
- `app.ai.provider.local.base-url`
- `app.ai.provider.local.api-key`
- `app.ai.provider.local.chat-path`
- `app.ai.provider.local.timeout`

环境变量示例：

```bash
APP_AI_ENABLED=true
APP_AI_RETRIEVAL_TOP_K=4
APP_AI_PROVIDER_TYPE=deepseek
APP_AI_PROVIDER_MODEL=deepseek-chat
```

DeepSeek 示例：

```bash
APP_AI_PROVIDER_TYPE=deepseek
APP_AI_PROVIDER_MODEL=deepseek-chat
APP_AI_PROVIDER_BASE_URL=https://api.deepseek.com
APP_AI_PROVIDER_API_KEY=your-key
APP_AI_PROVIDER_CHAT_PATH=/v1/chat/completions
APP_AI_PROVIDER_TIMEOUT=20s
```

本地模型示例：

```bash
APP_AI_PROVIDER_LOCAL_ENABLED=true
APP_AI_PROVIDER_LOCAL_TYPE=openai-compatible
APP_AI_PROVIDER_LOCAL_MODEL=qwen2.5:3b
APP_AI_PROVIDER_LOCAL_BASE_URL=http://127.0.0.1:11434
APP_AI_PROVIDER_LOCAL_CHAT_PATH=/v1/chat/completions
APP_AI_PROVIDER_LOCAL_TIMEOUT=20s
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

## 10. 前端验证

当前使用与验证优先通过前端页面完成。前端可创建或选择已有工单，触发 AI 分析，并查看最终决策结果、引用、节点运行记录和 fallback 状态。

后端只保留基础接口契约；不在本文档维护 curl、SQL 或本地启动脚本。

## 11. 其他 thread 如何使用

### 11.1 Thread 3：Ticket Core

建议方式：

- 使用 `AiOrchestrationService.runForTicket(ticketId)` 获取结构化结果
- 根据 `AiDecisionResult` 再决定是否调用 Ticket Core service 做状态变化
- `ticket.category` 必须提前写入全项目标准 category code

可以使用的结果字段：

- `category`：标准 category code，直接来自 Ticket Core
- `priority`：直接来自 Ticket Core
- `analysisMode`
- `requiresApproval`
- `needsHumanHandoff`
- `draftReply`
- `suggestedActions`

不要做的事：

- 不要让 AI 模块直接改 `tickets.status`
- 不要绕过 `TicketService`
- 不要把旧自由文本 category 传给 AI，例如 `VPN`、`IT`、`Finance`

### 11.2 Thread 4：Knowledge / Retrieval

建议方式：

- 实现 `RetrievalService.search(RetrievalSearchRequest request)`
- 返回升级后的 `RetrievalSearchResponse`
- AI 模块会自动消费真实 citations 与检索诊断字段

当前 AI 侧已传递的输入：

- `query`
- `ticketId`
- `ticketContext`
- `category`：仅标准 code；`OTHER` 时不传 category filter
- `department`
- `limit`
- `aiRunId`

当前 AI 侧期望的输出：

- `results[].docId`
- `results[].chunkId`
- `results[].title`
- `results[].contentSnippet`
- `results[].score`
- `results[].retrievalScore`
- `results[].rerankScore`
- `results[].sourceRef`
- `results[].metadataMap`
- `diagnostics.retrievalMode`
- `diagnostics.candidateCount`
- `diagnostics.returnedCount`
- `diagnostics.filterSummary`

不要做的事：

- 不要在 RetrievalService 中做审批判断
- 不要直接写 Ticket 状态
- 不要要求 Thread 5 在编排层维护旧值映射或 `AI category -> KB category` 的硬编码映射

### 11.3 Thread 6：Workflow / Approval / Observability

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

### 11.4 Thread 7：前端

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

## 12. 协作规范

- AI 输出必须只作为结构化决策输入，不是最终业务动作
- 其他模块消费 AI 结果时，应优先依赖字段而不是文案
- `draftReply` 只适合展示或人工编辑，不应作为后端核心逻辑判断依据
- 所有状态更新只能通过 TicketService 或 WorkflowService
- schema 变更要优先保持向后兼容，避免破坏其他 thread 对接
- 其他 thread 应优先消费 `retrievalStatus / retrievalDiagnostics / fallbackUsed`，不要把空 citations 误判为“AI 未运行”
- `analysisMode=RULE_BASED` 表示当前结果来自关键词兜底，只应视为低可信保守建议

## 13. 当前限制与后续扩展点

当前实现刻意保持 MVP 范围，以下内容可由后续迭代补充：

- 真正的 LangGraph Java 集成
- 更复杂的 prompt 模板和版本管理
- 更完整的 token 统计
- 节点级 trace 关联到 workflow / approval
- 更细的 citation source type
- 与工单详情聚合接口深度集成
- 更强的 prompt 模板版本管理与 A/B rollout
- 真正的 hybrid retrieval / rerank 底层实现由 Thread 4 继续完善

当前原则是先冻结 AI 编排入口、schema 和运行日志结构，保证其他 thread 能稳定接入。

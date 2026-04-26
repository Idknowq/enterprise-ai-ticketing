# Thread 4：知识库与检索模块说明

本文档说明 Thread 4 的模块边界、核心接口、数据契约、类别规范、Embedding/Qdrant 封装，以及其他 thread 的对齐方式。使用与验证优先通过前端完成，本文档不再维护 curl 级别的操作脚本。

## 1. 模块边界

Thread 4 负责企业 IT 服务台知识文档的处理与证据检索，目标是给工单处理和 AI 编排提供可解释、可引用、可过滤的知识片段。

负责范围：

- 文档模型设计：`documents`、`document_chunks`、`citations`
- 文档上传接口
- Markdown / PDF / TXT 解析
- 文档切分
- Embedding 调用封装与路由
- Qdrant 向量写入与检索
- 元数据过滤检索
- 检索结果标准化
- Citation 落库能力

不负责范围：

- 不决定工单状态
- 不做审批判断
- 不实现 LangGraph / AI 编排
- 不直接修改 Ticket Core 状态
- 不负责前端页面

## 2. 全项目标准类别

`category` 已升级为全项目统一的 IT 服务类别标准，不再只是知识库内部字段。数据库仍使用 `varchar` 存储，但所有后端模块的新入参、新输出和新写入值都应统一使用以下标准 code。

当前 Thread 4 代码中该标准暂由 `KnowledgeDocumentCategory` 承载；后续如迁移到公共 domain，可重命名为 `ItServiceCategory` 或等价类型，但 code 清单和语义必须保持稳定。

| Code | 名称 | 典型文档 |
|---|---|---|
| `REMOTE_ACCESS` | 远程访问 / VPN | VPN 证书失效、远程办公连接失败、客户端配置 |
| `IDENTITY_ACCOUNT` | 账号与身份 | 账号开通、账号锁定、离职账号禁用、AD/LDAP 问题 |
| `PASSWORD_MFA` | 密码与 MFA | 密码重置、MFA 绑定、验证码异常、SSO 登录失败 |
| `ACCESS_REQUEST` | 权限申请 | 系统权限申请、权限变更、临时授权、审批流程 |
| `EMAIL_COLLABORATION` | 邮件与协作 | 邮箱异常、邮件组、日历、Teams/Slack/飞书协作 |
| `DEVICE_HARDWARE` | 终端与硬件 | 电脑、显示器、打印机、外设、资产更换 |
| `OPERATING_SYSTEM` | 操作系统 | Windows/macOS 系统故障、补丁、启动异常 |
| `SOFTWARE_APPLICATION` | 软件与应用 | 办公软件、业务应用安装、客户端异常 |
| `NETWORK_CONNECTIVITY` | 网络连接 | Wi-Fi、有线网络、DNS、代理、内网访问异常 |
| `SECURITY_INCIDENT` | 安全事件 | 钓鱼邮件、恶意软件、账号异常登录、安全上报 |
| `DATA_BACKUP_RECOVERY` | 数据备份与恢复 | 文件恢复、备份策略、误删数据恢复 |
| `CLOUD_INFRASTRUCTURE` | 云与基础设施 | 云资源、服务器、容器、存储、基础设施运维 |
| `DATABASE_DATA_PLATFORM` | 数据库与数据平台 | 数据库连接、数据权限、数据任务、BI 平台 |
| `DEV_ENGINEERING` | 开发与工程工具 | Git、CI/CD、制品库、开发环境、测试环境 |
| `ITSM_PROCESS` | ITSM 流程 | 工单流转、SLA、升级路径、服务目录 |
| `ASSET_PROCUREMENT` | 资产与采购 | 设备领用、采购申请、资产归还、库存流程 |
| `CHANGE_RELEASE` | 变更与发布 | 变更申请、发布窗口、回滚 SOP、维护公告 |
| `POLICY_COMPLIANCE` | 政策与合规 | 信息安全制度、审计要求、合规流程 |
| `GENERAL_FAQ` | 通用 FAQ | 通用服务台问答、常见问题合集 |
| `OTHER` | 其他 | 暂时无法归类但仍需入库的文档，需后续治理 |

类别规则：

- 新上传文档、检索过滤以及后续工单/AI/审批相关 category 入参都必须使用标准 code。
- API 新入参严格拒绝旧值或自由文本，例如 `VPN`、`VPN_ISSUE`、`Finance`、`IT` 都不应作为合法 category。
- `OTHER` 仅作为兜底，不建议长期大量使用。
- 历史数据可以通过 Flyway migration 归一化，但不要把旧值作为长期兼容 API。
- Qdrant 旧 payload 不会被数据库 migration 自动更新，已有旧索引需要重建 collection 或重新上传文档。

核心代码：

- 枚举：`backend/src/main/java/com/enterprise/ticketing/knowledge/domain/KnowledgeDocumentCategory.java`
- 历史迁移：`backend/src/main/resources/db/migration/V8__normalize_knowledge_document_categories.sql`

## 3. 数据模型

Migration：

- `backend/src/main/resources/db/migration/V4__init_knowledge_module.sql`
- `backend/src/main/resources/db/migration/V8__normalize_knowledge_document_categories.sql`

核心表：

- `documents`：文档主表，保存标题、来源文件、类别、部门、访问级别、版本、全文、索引状态、embedding 模型等。
- `document_chunks`：文档分块表，保存 chunk 内容、snippet、业务 chunkId、Qdrant pointId。
- `citations`：引用表，保存检索命中的证据片段，可关联 `ticketId` 或 `aiRunId`。

关键元数据：

- `title`
- `category`
- `department`
- `accessLevel`
- `version`
- `updatedAt`

## 4. 核心接口

### 4.1 上传文档

`POST /api/documents/upload`

权限：

- `ADMIN`

Content-Type：

- `multipart/form-data`

表单字段：

- `file`：必填，支持 `.md`、`.markdown`、`.txt`、`.pdf`
- `title`：可选，不传时默认取文件名
- `category`：必填，标准知识类别 code，例如 `REMOTE_ACCESS`
- `department`：可选，不传时默认 `GLOBAL`
- `accessLevel`：必填，`PUBLIC` / `INTERNAL` / `RESTRICTED` / `CONFIDENTIAL`
- `version`：必填
- `updatedAt`：必填，ISO-8601 时间

处理链路：

1. 校验权限与入参。
2. 解析文档内容。
3. 切分 chunk。
4. 调用 `EmbeddingProvider`。
5. 写入 Qdrant。
6. 写入 `documents` 与 `document_chunks`。
7. 返回 `DocumentResponse`。

### 4.2 获取标准类别

`GET /api/documents/categories`

返回字段：

- `code`
- `displayName`
- `description`

前端上传页面和其他 thread 应通过该接口获取类别选项，不要硬编码自由文本类别。后续如果平台层新增全局类别接口，可以迁移到全局路径，但返回 code 必须保持一致。

### 4.3 列出文档

`GET /api/documents`

权限：

- `ADMIN`
- `SUPPORT_AGENT`

Query 参数：

- `keyword`
- `category`
- `department`
- `accessLevel`
- `indexStatus`
- `page`
- `size`

说明：

- `category` 只能使用标准 code。
- 列表接口同样经过部门与访问级别过滤。
- 当过滤条件超出当前用户权限范围时，返回空列表。

### 4.4 检索知识证据

`POST /api/retrieval/search`

请求字段：

- `query`：原始检索文本，可选
- `ticketId`：工单 ID，可选；当 `query` 为空时用于读取工单标题、描述、分类并构造 query
- `ticketContext`：AI 侧准备的工单上下文，可选
- `category`：标准知识类别 code，可选
- `department`：部门过滤，可选
- `accessLevel`：访问级别过滤，可选
- `limit`：返回条数，默认 5，最大 10
- `saveCitations`：是否落 citation，默认 `false`
- `aiRunId`：AI 运行 ID，可选

请求规则：

- `query` 和 `ticketId` 至少提供一个。
- 若传 `ticketId`，Thread 4 只读取 Ticket 信息，不修改 Ticket 状态。
- `category` 只能接收标准类别 code；Ticket Core 完成统一前，调用方不要把自由文本 `ticket.category` 直接传入该字段。

核心返回 schema：

```json
{
  "query": "VPN 连接失败 证书失效",
  "ticketId": 123,
  "diagnostics": {
    "retrievalMode": "VECTOR_WITH_METADATA_FILTERS",
    "candidateCount": 3,
    "returnedCount": 3,
    "filterSummary": {
      "category": "REMOTE_ACCESS",
      "departments": ["IT"],
      "accessLevels": ["INTERNAL", "PUBLIC"],
      "ticketContextProvided": true
    }
  },
  "results": [
    {
      "docId": 1,
      "title": "VPN 证书失效处理 SOP",
      "chunkId": "doc-1-chunk-0",
      "contentSnippet": "当用户在远程办公时遇到证书失效...",
      "score": 0.82,
      "retrievalScore": 0.82,
      "rerankScore": null,
      "sourceRef": "citation:10",
      "metadata": {
        "docId": 1,
        "title": "VPN 证书失效处理 SOP",
        "category": "REMOTE_ACCESS",
        "department": "IT",
        "accessLevel": "INTERNAL",
        "version": "v1.0",
        "updatedAt": "2026-04-16T00:00:00Z"
      },
      "metadataMap": {
        "docId": 1,
        "title": "VPN 证书失效处理 SOP",
        "category": "REMOTE_ACCESS",
        "department": "IT",
        "accessLevel": "INTERNAL",
        "version": "v1.0",
        "updatedAt": "2026-04-16T00:00:00Z"
      },
      "whyMatched": "Matched keywords: vpn, 证书, 失效",
      "citationId": 10
    }
  ]
}
```

字段说明：

- `score`：兼容旧调用方的总分字段。
- `retrievalScore`：当前向量检索原始分数。
- `rerankScore`：后续 rerank 预留字段，当前通常为 `null`。
- `sourceRef`：若已落 citation，优先返回 `citation:<id>`。
- `metadataMap`：给 AI 模块直接消费的扁平 metadata。
- `diagnostics`：用于空结果诊断、AI 运行审计和可观测性。

## 5. 权限与过滤

权限过滤由 `DocumentAccessPolicy` 统一处理。

访问级别：

- `PUBLIC`
- `INTERNAL`
- `RESTRICTED`
- `CONFIDENTIAL`

默认可见范围：

- `EMPLOYEE`：`PUBLIC`、`INTERNAL`
- `APPROVER`、`SUPPORT_AGENT`：`PUBLIC`、`INTERNAL`、`RESTRICTED`
- `ADMIN`：全部可见

部门规则：

- `ADMIN`、`SUPPORT_AGENT` 可跨部门读取。
- 其他用户默认只可读本部门和 `GLOBAL` 文档。
- 显式传入超出权限范围的 `department` 时返回空结果。

依赖 Thread 2：

- `UserContext`
- `UserPrincipal`
- 用户角色
- 用户部门

其他模块不需要重复解析 JWT，也不应自行拼接权限过滤。

## 6. Embedding 与 Qdrant

### 6.1 EmbeddingProvider

对外统一抽象：

- `EmbeddingProvider`

当前实现：

- `RoutingEmbeddingProvider`：唯一对外注入实现，负责按配置路由。
- `LocalOllamaEmbeddingProvider`：本地 Ollama provider，默认模型 `nomic-embed-text:latest`。
- `OpenAiEmbeddingProvider`：商用 OpenAI provider，预留模型 `text-embedding-3-large`。

配置入口：

- `app.knowledge.embedding.routing.mode`
- `app.knowledge.embedding.local.*`
- `app.knowledge.embedding.commercial.*`

规则：

- 当前默认使用本地 Ollama。
- 上传和检索链路只依赖 `EmbeddingProvider`，不感知 provider 细节。
- 切换模型后，Qdrant collection 维度必须与当前 provider 的 `dimension()` 一致。

### 6.2 QdrantClient

能力：

- 检查 collection 是否存在。
- collection 不存在时自动创建。
- 写入 chunk 向量。
- 基于 payload filter 检索。
- 检查已有 collection vector size，维度不一致时直接报错。

当前 payload 字段：

- `docId`
- `title`
- `chunkId`
- `chunkIndex`
- `contentSnippet`
- `category`
- `department`
- `accessLevel`
- `version`
- `updatedAt`

维度一致性要求：

- collection vector size 由当前活动 embedding provider 决定。
- 切换本地/商用模型前，应删除或重建对应 Qdrant collection。
- 本轮不支持多 embedding 模型共用同一个 collection。

## 7. 关键代码位置

Controller：

- `backend/src/main/java/com/enterprise/ticketing/knowledge/controller/DocumentController.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/controller/RetrievalController.java`

Service 契约：

- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/DocumentService.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/RetrievalService.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/EmbeddingProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/CitationService.java`

核心实现：

- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/DocumentServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/RetrievalServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/CitationServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/DocumentAccessPolicy.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/RoutingEmbeddingProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/LocalOllamaEmbeddingProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/OpenAiEmbeddingProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/qdrant/QdrantClient.java`

解析器：

- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/DocumentParserRegistry.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/MarkdownDocumentParser.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/TextDocumentParser.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/PdfDocumentParser.java`

## 8. 其他 Thread 对齐规范

### 8.1 Thread 2：认证与权限

Thread 2 需要继续保证：

- `UserContext` 可用。
- `UserPrincipal.department` 可用。
- 用户角色稳定。

Thread 4 会直接复用这些上下文做文档列表和检索过滤。

### 8.2 Thread 3：Ticket Core

Thread 4 只读取 Ticket 信息：

- 通过 `TicketQueryService#getTicketDetail` 读取标题、描述、分类。
- 不修改 ticket 状态。
- 不写 `tickets`。
- 不绕过 Ticket Core 写事件日志。

Ticket Core 代码由对应 thread 修改，Thread 4 不直接实施。但目标契约必须统一到本标准类别，不能长期保留自由文本 category。

Thread 3 需要修改的具体部分：

- `CreateTicketRequest.category`：从自由文本 `String` 改为标准类别枚举或标准 code，只接受 `REMOTE_ACCESS` 等标准值。
- `TicketListQuery.category`：从自由文本筛选改为标准类别筛选，非法值返回 400。
- `TicketEntity.category`：数据库字段可继续 `varchar(128)`，但写入值必须是标准 code。
- `TicketCoreServiceImpl#createTicket`：创建时不再 `trim()` 任意文本，而是校验并存储标准 code。
- `TicketQueryServiceImpl`：列表查询按标准 code 精确匹配，不再做自由文本大小写兼容。
- `TicketResponse.category`、`TicketSummaryResponse.category`：继续可序列化为字符串，但值必须是标准 code。
- 历史 migration：新增 Ticket Core migration，将 `VPN`、`VPN_ISSUE` 映射到 `REMOTE_ACCESS`，`PASSWORD_RESET` 映射到 `PASSWORD_MFA`，`SOFTWARE_LICENSE` 映射到 `SOFTWARE_APPLICATION`，无法识别的旧值映射到 `OTHER`。
- 测试：旧测试中的 `VPN`、`Finance`、`IT` 作为新建/筛选入参应改为标准 code，或改成非法值 400 测试。

### 8.3 Thread 5：AI 编排

Thread 5 是检索接口的主要下游。

推荐调用：

- HTTP：`POST /api/retrieval/search`
- Spring Service：注入 `RetrievalService`

Thread 5 应消费的字段：

- `ticketContext`
- `diagnostics.retrievalMode`
- `diagnostics.candidateCount`
- `diagnostics.returnedCount`
- `diagnostics.filterSummary`
- `results[].retrievalScore`
- `results[].rerankScore`
- `results[].sourceRef`
- `results[].metadataMap`
- `results[].citationId`

协作要求：

- AI 分类输出应直接使用标准 category code，例如 `REMOTE_ACCESS`、`PASSWORD_MFA`、`ACCESS_REQUEST`。
- Retriever 不应长期维护 `AI category -> KB category` 的硬编码映射；标准化后应直接把 AI 分类结果作为检索 category filter。
- 分类失败或不确定时，Thread 5 可选择输出 `OTHER`，或不传检索 category filter，让检索在权限范围内基于 `query` 和 `ticketContext` 召回。
- 不要直接访问 `document_chunks` 拼接 AI 证据。
- 不要自行判断 citation 是否越权。
- 若要记录证据链，优先使用 `saveCitations=true`。
- 若 `results` 为空，不要直接判定系统故障，应结合 `diagnostics` 和 HTTP 状态区分空命中与调用异常。

### 8.4 前端

前端测试与使用应优先走页面流程。

前端需要对齐：

- 上传页面的类别下拉框从 `GET /api/documents/categories` 获取。
- 上传文档时只能提交标准 category code。
- 工单创建/筛选页面后续也应改为标准类别下拉，不能继续使用自由文本输入。
- 检索结果展示优先使用 `title`、`contentSnippet`、`sourceRef`、`metadataMap`。
- Ticket 详情或 AI 运行记录如需展示证据链，应使用 citation 相关字段。

## 9. 当前限制

- citation 已支持落库，但还没有单独的 citation 查询接口。
- 文档上传当前限制为 `ADMIN`。
- 当前是 vector search + metadata filter，hybrid recall 和 rerank 仍是后续演进方向。
- 当前环境默认只支持一个活动 embedding 模型对应一个 Qdrant collection，不支持多模型共存。

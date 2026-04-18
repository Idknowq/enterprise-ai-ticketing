# 知识库与检索模块说明

本文档用于说明当前仓库中知识库与检索模块的设计边界、使用方式、验证方法，以及其他 thread 如何复用该模块能力。

## 1. 模块目标

本模块负责提供企业级 AI 工单编排系统的知识文档处理与证据检索能力，覆盖：

- 文档模型设计
- 文档上传接口
- 文档解析（Markdown / PDF / TXT）
- 文档切分
- embedding 调用封装
- 向 Qdrant 写入向量
- 检索接口实现
- 基于元数据过滤检索
- 检索结果标准化
- citation 落库能力

本模块的目标不是做普通问答，而是为工单处理与 AI 决策提供可解释、可引用、可过滤的证据片段。

## 2. 关键边界

- 本模块只负责“检索证据”，不负责 AI 最终判断
- 不决定工单状态
- 不做审批判断
- 不实现 LangGraph / 工作流编排
- 不直接修改 Ticket 核心状态
- 可读取 Thread 3 的 ticket 信息来构造检索 query
- 检索结果必须兼容用户权限上下文
- 检索结果必须可解释，且可落 citation

## 3. 当前实现范围

当前已实现：

- `POST /api/documents/upload`
- `GET /api/documents`
- `POST /api/retrieval/search`
- 文档入库主表 `documents`
- 文档分块表 `document_chunks`
- 引用表 `citations`
- 文档解析器注册与自动识别
- 文本切分与摘要 snippet 生成
- 本地 `EmbeddingProvider` 抽象与默认 hashing 实现
- Qdrant collection 自动创建、向量 upsert、带 payload 的过滤检索
- 按 `department` / `accessLevel` 做权限兼容过滤
- 检索结果统一 schema 输出

## 4. 数据模型

数据库 migration 文件：

- `backend/src/main/resources/db/migration/V4__init_knowledge_module.sql`

当前新增三张表：

### 4.1 documents

核心字段：

- `id`
- `title`
- `source_filename`
- `content_type`
- `document_type`
- `category`
- `department`
- `access_level`
- `version`
- `updated_at`
- `content_text`
- `chunk_count`
- `index_status`
- `last_indexed_at`
- `embedding_model`
- `created_by_user_id`
- `created_at`

### 4.2 document_chunks

核心字段：

- `id`
- `document_id`
- `chunk_id`
- `chunk_index`
- `vector_point_id`
- `content`
- `content_snippet`
- `created_at`

说明：

- `chunk_id` 是业务稳定 ID，例如 `doc-12-chunk-3`
- `vector_point_id` 与 Qdrant point id 对齐
- `content_snippet` 用于接口返回和 citation 展示

### 4.3 citations

核心字段：

- `id`
- `ticket_id`
- `ai_run_id`
- `document_id`
- `chunk_id`
- `title`
- `content_snippet`
- `score`
- `category`
- `department`
- `access_level`
- `version`
- `document_updated_at`
- `search_query`
- `why_matched`
- `created_by_user_id`
- `created_at`

说明：

- 可同时支持 ticket 详情证据展示和 AI 运行记录追溯
- `ticket_id`、`ai_run_id` 允许为空，便于通用检索场景复用

## 5. 关键代码位置

### 控制器

- `backend/src/main/java/com/enterprise/ticketing/knowledge/controller/DocumentController.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/controller/RetrievalController.java`

### Service 契约

- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/DocumentService.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/RetrievalService.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/EmbeddingProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/CitationService.java`

### Service 实现

- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/DocumentServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/RetrievalServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/CitationServiceImpl.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/DocumentAccessPolicy.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/TextChunker.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/HashingEmbeddingProvider.java`

### 解析器

- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/DocumentParserRegistry.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/MarkdownDocumentParser.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/TextDocumentParser.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/parser/PdfDocumentParser.java`

### 持久化与向量库

- `backend/src/main/java/com/enterprise/ticketing/knowledge/entity/DocumentEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/entity/DocumentChunkEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/entity/CitationEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/repository/DocumentRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/repository/DocumentChunkRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/repository/CitationRepository.java`
- `backend/src/main/java/com/enterprise/ticketing/knowledge/qdrant/QdrantClient.java`

### 配置

- `backend/src/main/java/com/enterprise/ticketing/config/ApplicationProperties.java`
- `backend/src/main/resources/application.yml`

## 6. 接口说明

## 6.1 上传文档

接口：

```http
POST /api/documents/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

权限：

- 当前实现限制为 `ADMIN`

表单字段：

- `file`: 文档文件，必填
- `title`: 文档标题，可选，不传时默认取文件名
- `category`: 文档类别，必填
- `department`: 所属部门，可选，不传时使用 `GLOBAL`
- `accessLevel`: 访问级别，必填
- `version`: 文档版本，必填
- `updatedAt`: 文档更新时间，必填

支持格式：

- `.md`
- `.markdown`
- `.txt`
- `.pdf`

示例：

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@./samples/knowledge/vpn_cert_sop.md" \
  -F "title=VPN 证书失效处理 SOP" \
  -F "category=VPN" \
  -F "department=IT" \
  -F "accessLevel=INTERNAL" \
  -F "version=v1.0" \
  -F "updatedAt=2026-04-16T00:00:00Z"
```

处理链路：

1. 鉴权并校验角色
2. 解析文档内容
3. 切分 chunk
4. 调用 `EmbeddingProvider`
5. 向 Qdrant upsert 向量
6. 写入 `documents` 和 `document_chunks`
7. 返回统一 `DocumentResponse`

## 6.2 列出文档

接口：

```http
GET /api/documents
Authorization: Bearer <token>
```

权限：

- 当前实现允许 `ADMIN`、`SUPPORT_AGENT`

支持 query 参数：

- `keyword`
- `category`
- `department`
- `accessLevel`
- `indexStatus`
- `page`
- `size`

说明：

- 列表接口也会经过访问级别与部门范围约束
- 若请求的过滤条件超出当前用户权限，会直接返回空列表

## 6.3 检索知识证据

接口：

```http
POST /api/retrieval/search
Content-Type: application/json
Authorization: Bearer <token>
```

请求体字段：

- `query`: 原始检索文本，可选
- `ticketId`: 工单 ID，可选；当 `query` 为空时使用
- `category`: 可选元数据过滤
- `department`: 可选元数据过滤
- `accessLevel`: 可选元数据过滤
- `limit`: 返回条数，默认 5，最大 10
- `saveCitations`: 是否落 citation，默认 `false`
- `aiRunId`: AI 运行 ID，可选

请求规则：

- `query` 和 `ticketId` 至少要有一个
- 若只传 `ticketId`，服务会通过 Thread 3 的 `TicketQueryService` 读取工单标题、描述、分类来构造 query

示例 1：直接按文本检索

```bash
curl -X POST http://localhost:8080/api/retrieval/search \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "VPN 连接失败 证书失效",
    "category": "VPN",
    "limit": 5,
    "saveCitations": true,
    "aiRunId": "run-001"
  }'
```

示例 2：按工单检索

```bash
curl -X POST http://localhost:8080/api/retrieval/search \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": 123,
    "limit": 5,
    "saveCitations": true,
    "aiRunId": "run-002"
  }'
```

返回结构：

```json
{
  "success": true,
  "code": "COMMON_SUCCESS",
  "message": "Success",
  "data": {
    "query": "VPN 连接失败 证书失效",
    "ticketId": 123,
    "results": [
      {
        "docId": 1,
        "title": "VPN 证书失效处理 SOP",
        "chunkId": "doc-1-chunk-0",
        "contentSnippet": "当用户在远程办公时遇到证书失效...",
        "score": 0.82,
        "metadata": {
          "docId": 1,
          "title": "VPN 证书失效处理 SOP",
          "category": "VPN",
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
}
```

## 7. 权限与过滤规则

当前权限策略由 `DocumentAccessPolicy` 统一处理。

### 7.1 访问级别

当前定义：

- `PUBLIC`
- `INTERNAL`
- `RESTRICTED`
- `CONFIDENTIAL`

默认可见范围：

- `EMPLOYEE`: `PUBLIC`、`INTERNAL`
- `APPROVER`、`SUPPORT_AGENT`: `PUBLIC`、`INTERNAL`、`RESTRICTED`
- `ADMIN`: 全部可见

### 7.2 部门范围

规则：

- `ADMIN`、`SUPPORT_AGENT` 可跨部门读取
- 其他用户默认只可读本部门和 `GLOBAL` 文档
- 若显式传入超出权限范围的 `department`，返回空结果

### 7.3 与用户上下文的关系

本模块依赖 Thread 2 的：

- `UserContext`
- `UserPrincipal`
- 角色信息
- 当前用户 `department`

因此其他模块不需要自行重复解析 JWT，也不需要单独传用户部门给知识模块。

## 8. Embedding 与 Qdrant 说明

## 8.1 EmbeddingProvider

当前实现：

- 接口：`EmbeddingProvider`
- 默认实现：`HashingEmbeddingProvider`

说明：

- 当前是本地 hashing 向量化实现，优点是无外部模型依赖、便于本地开发和链路联调
- 这是一个占位但可运行的实现，不代表最终生产 embedding 方案
- 后续如需接入真实模型，只需新增或替换 `EmbeddingProvider` 实现，不需要改上传/检索主链路

## 8.2 QdrantClient

能力：

- 自动检查 collection 是否存在
- 不存在时自动创建
- upsert points
- search with payload filter

当前 collection 配置：

- `app.knowledge.collection-name`
- `app.knowledge.embedding-dimension`

当前 payload 字段包括：

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

这些字段支持检索结果返回和元数据过滤。

## 9. 配置项

当前新增配置位于 `app.knowledge.*`：

- `app.knowledge.collection-name`
- `app.knowledge.embedding-dimension`
- `app.knowledge.chunk-size`
- `app.knowledge.chunk-overlap`
- `app.knowledge.default-top-k`
- `app.knowledge.max-top-k`
- `app.knowledge.global-department`
- `app.knowledge.embedding.provider`
- `app.knowledge.embedding.model`

可通过环境变量覆盖：

- `APP_KNOWLEDGE_COLLECTION_NAME`
- `APP_KNOWLEDGE_EMBEDDING_DIMENSION`
- `APP_KNOWLEDGE_CHUNK_SIZE`
- `APP_KNOWLEDGE_CHUNK_OVERLAP`
- `APP_KNOWLEDGE_DEFAULT_TOP_K`
- `APP_KNOWLEDGE_MAX_TOP_K`
- `APP_KNOWLEDGE_GLOBAL_DEPARTMENT`
- `APP_KNOWLEDGE_EMBEDDING_PROVIDER`
- `APP_KNOWLEDGE_EMBEDDING_MODEL`

## 10. 如何启动与使用

### 10.1 启动基础设施

以下命令均在项目根目录执行。

先启动基础设施：

```bash
docker compose up -d
```

需要至少保证以下依赖可用：

- PostgreSQL
- Qdrant

建议立刻确认关键容器已启动：

```bash
docker compose ps postgres qdrant
```

预期：

- `postgres` 状态为 `Up`
- `qdrant` 状态为 `Up`

### 10.2 启动后端

下面开始，除非特别说明，示例命令默认都在**仓库根目录**执行。

启动后端时再进入 `backend/` 目录，建议显式指定数据库地址为 `127.0.0.1`，避免本机 `localhost` 解析差异：

```bash
cd backend
APP_DB_HOST=127.0.0.1 \
APP_DB_PORT=5432 \
APP_DB_NAME=ticketing \
APP_DB_USERNAME=ticketing \
APP_DB_PASSWORD=ticketing \
APP_QDRANT_HOST=127.0.0.1 \
APP_QDRANT_HTTP_PORT=6333 \
mvn clean spring-boot:run
```

说明：

- 这里使用 `mvn clean spring-boot:run`，避免旧的 `target/classes` 中残留过期 migration
- 如果你已经执行过 `mvn clean`，也可以使用 `mvn spring-boot:run`
- 若后端成功启动，终端中会出现 `Tomcat started on port 8080`

### 10.3 登录获取 token

先使用 Thread 2 提供的登录接口获取 JWT。

建议先准备基础变量：

```bash
BASE_URL='http://localhost:8080'
ADMIN_USERNAME='admin01'
ADMIN_PASSWORD='ChangeMe123!'
EMPLOYEE_USERNAME='employee01'
EMPLOYEE_PASSWORD='ChangeMe123!'
SUPPORT_USERNAME='support01'
SUPPORT_PASSWORD='ChangeMe123!'
```

获取管理员 token：

```bash
curl -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"password\": \"$ADMIN_PASSWORD\"
  }"
```

如果你本机没有 `jq`，可用下面这段命令直接提取 token：

```bash
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"password\": \"$ADMIN_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

echo "$ADMIN_TOKEN"
```

同理，获取普通员工 token：

```bash
EMPLOYEE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$EMPLOYEE_USERNAME\",
    \"password\": \"$EMPLOYEE_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

echo "$EMPLOYEE_TOKEN"
```

### 10.4 上传示例知识文档

仓库内已提供一份可直接上传的示例文档：

- `samples/knowledge/vpn_cert_sop.md`

如果你当前在仓库根目录，直接执行：

```bash
curl -X POST "$BASE_URL/api/documents/upload" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@./samples/knowledge/vpn_cert_sop.md" \
  -F "title=VPN 证书失效处理 SOP" \
  -F "category=VPN" \
  -F "department=IT" \
  -F "accessLevel=INTERNAL" \
  -F "version=v1.0" \
  -F "updatedAt=2026-04-17T00:00:00Z"
```

如果你当前在 `backend/` 目录执行，上面的文件路径改成：

```bash
-F "file=@../samples/knowledge/vpn_cert_sop.md"
```

如果成功，预期响应中会出现：

- `success: true`
- `data.indexStatus: INDEXED`
- `data.chunkCount` 大于 0
- `data.metadata.category: VPN`

### 10.5 列出已上传文档

使用管理员或支持人员调用：

```bash
curl "$BASE_URL/api/documents?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

如果只想筛选 VPN 类别：

```bash
curl "$BASE_URL/api/documents?page=0&size=20&category=VPN" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 10.6 直接按文本检索

使用任意已登录用户调用 `/api/retrieval/search`。

```bash
curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "今天在家连接 VPN 失败，提示证书失效",
    "category": "VPN",
    "limit": 5,
    "saveCitations": true,
    "aiRunId": "demo-run-001"
  }'
```

预期：

- 返回 `results`
- 至少有 1 条结果命中 `VPN 证书失效处理 SOP`
- `whyMatched` 不为空
- `citationId` 不为空，因为 `saveCitations=true`

### 10.7 创建工单并按工单检索

如果你要验证 Thread 3 联动，先创建工单。

先获取员工 token：

```bash
EMPLOYEE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$EMPLOYEE_USERNAME\",
    \"password\": \"$EMPLOYEE_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

创建工单：

```bash
TICKET_RESPONSE=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 证书失效，无法连接",
    "description": "今天在家连接公司 VPN 失败，客户端提示证书失效。",
    "category": "VPN",
    "priority": "HIGH"
  }')

echo "$TICKET_RESPONSE"
```

提取工单 ID：

```bash
TICKET_ID=$(echo "$TICKET_RESPONSE" | tr -d '\n' | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')
echo "$TICKET_ID"
```

按工单检索：

```bash
curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"ticketId\": $TICKET_ID,
    \"limit\": 5,
    \"saveCitations\": true,
    \"aiRunId\": \"demo-run-ticket-001\"
  }"
```

## 11. 如何验证

下面给出一套可以从零复制执行的完整验证流程。

## 11.1 一次性准备环境

在项目根目录执行：

```bash
docker compose up -d postgres qdrant redis rabbitmq
docker compose ps postgres qdrant redis rabbitmq
```

确认 PostgreSQL 可登录：

```bash
docker exec -it enterprise-ai-ticketing-postgres \
  psql -U ticketing -d ticketing -c 'select 1'
```

确认 Qdrant 可访问：

```bash
curl http://127.0.0.1:6333/collections
```

启动后端：

```bash
cd backend
APP_DB_HOST=127.0.0.1 \
APP_DB_PORT=5432 \
APP_DB_NAME=ticketing \
APP_DB_USERNAME=ticketing \
APP_DB_PASSWORD=ticketing \
APP_QDRANT_HOST=127.0.0.1 \
APP_QDRANT_HTTP_PORT=6333 \
mvn clean spring-boot:run
```

### 11.2 验证文档上传成功

在另一个终端执行。以下命令假设你当前在**仓库根目录**：

```bash
cd /Users/qianhaoda/Documents/Codex/企业级AI工单编排系统

BASE_URL='http://localhost:8080'
ADMIN_USERNAME='admin01'
ADMIN_PASSWORD='ChangeMe123!'

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"password\": \"$ADMIN_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -X POST "$BASE_URL/api/documents/upload" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@./samples/knowledge/vpn_cert_sop.md" \
  -F "title=VPN 证书失效处理 SOP" \
  -F "category=VPN" \
  -F "department=IT" \
  -F "accessLevel=INTERNAL" \
  -F "version=v1.0" \
  -F "updatedAt=2026-04-17T00:00:00Z"
```

预期检查点：

- HTTP 返回成功
- `indexStatus` 为 `INDEXED`
- `chunkCount` 大于 0

### 11.3 验证 PostgreSQL 落库

查看 `documents` 表：

```bash
docker exec -it enterprise-ai-ticketing-postgres \
  psql -U ticketing -d ticketing -c 'select id, title, category, department, access_level, chunk_count, index_status from documents order by id desc limit 5;'
```

查看 `document_chunks` 表：

```bash
docker exec -it enterprise-ai-ticketing-postgres \
  psql -U ticketing -d ticketing -c 'select document_id, chunk_id, chunk_index from document_chunks order by id desc limit 10;'
```

预期：

- `documents` 中出现刚上传的 SOP
- `document_chunks` 中出现 `doc-<id>-chunk-<index>` 形式的 chunk

### 11.4 验证 Qdrant 已写入向量

先查看 collection：

```bash
curl http://localhost:6333/collections/knowledge_chunks
```

如果返回 200，说明 collection 已创建。

如果想进一步看集合列表：

```bash
curl http://localhost:6333/collections
```

### 11.5 验证直接检索

```bash
EMPLOYEE_USERNAME='employee01'
EMPLOYEE_PASSWORD='ChangeMe123!'

EMPLOYEE_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$EMPLOYEE_USERNAME\",
    \"password\": \"$EMPLOYEE_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "VPN 证书失效 无法连接",
    "category": "VPN",
    "limit": 5,
    "saveCitations": true,
    "aiRunId": "verify-direct-search-001"
  }'
```

预期检查点：

- 返回 `results`
- `results[0].title` 大概率为 `VPN 证书失效处理 SOP`
- `results[*].metadata.category` 为 `VPN`
- `citationId` 不为空

### 11.6 验证 citations 落库

```bash
docker exec -it enterprise-ai-ticketing-postgres \
  psql -U ticketing -d ticketing -c 'select id, ticket_id, ai_run_id, document_id, chunk_id, score from citations order by id desc limit 10;'
```

预期：

- 出现 `ai_run_id = verify-direct-search-001`
- `document_id`、`chunk_id`、`score` 均有值

### 11.7 验证按工单检索

```bash
TICKET_RESPONSE=$(curl -s -X POST "$BASE_URL/api/tickets" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "VPN 客户端提示证书失效",
    "description": "今天在家连接公司 VPN 失败，客户端提示 certificate expired。",
    "category": "VPN",
    "priority": "HIGH"
  }')

TICKET_ID=$(echo "$TICKET_RESPONSE" | tr -d '\n' | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')

curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"ticketId\": $TICKET_ID,
    \"limit\": 5,
    \"saveCitations\": true,
    \"aiRunId\": \"verify-ticket-search-001\"
  }"
```

预期：

- 即使不显式传 `query` 也能返回证据
- 结果中依然能命中 VPN SOP
- citations 表中会新增带 `ticket_id` 的记录

### 11.8 验证权限过滤

当前建议用角色和部门组合做基础验证。

验证思路：

1. 先上传一份 `department=IT`、`accessLevel=INTERNAL` 的文档
2. 用普通员工账号执行检索，确认能搜到
3. 再上传一份 `accessLevel=RESTRICTED` 的文档
4. 用普通员工账号检索，预期搜不到
5. 用 `support01` 或 `admin01` 检索，预期可搜到

如果你想新增一个受限文档，可先复制已有示例文件：

```bash
cp ./samples/knowledge/vpn_cert_sop.md ./samples/knowledge/vpn_restricted_runbook.md
```

再上传：

```bash
curl -X POST "$BASE_URL/api/documents/upload" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@./samples/knowledge/vpn_restricted_runbook.md" \
  -F "title=VPN 受限排障 Runbook" \
  -F "category=VPN" \
  -F "department=IT" \
  -F "accessLevel=RESTRICTED" \
  -F "version=v1.0" \
  -F "updatedAt=2026-04-17T00:00:00Z"
```

员工检索：

```bash
curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "VPN Runbook 受限排障",
    "category": "VPN",
    "limit": 5
  }'
```

支持人员检索：

```bash
SUPPORT_TOKEN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"$SUPPORT_USERNAME\",
    \"password\": \"$SUPPORT_PASSWORD\"
  }" | tr -d '\n' | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl -X POST "$BASE_URL/api/retrieval/search" \
  -H "Authorization: Bearer $SUPPORT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "VPN Runbook 受限排障",
    "category": "VPN",
    "limit": 5
  }'
```

预期：

- 员工账号看不到 `RESTRICTED` 文档
- 支持人员或管理员可以看到

### 11.9 常见问题排查

如果启动时报 Flyway migration 冲突，先执行：

```bash
cd backend
mvn clean compile
```

如果数据库连接异常，先确认：

```bash
docker compose ps postgres
docker exec -it enterprise-ai-ticketing-postgres \
  psql -U ticketing -d ticketing -c 'select 1'
```

如果 Qdrant 未启动，先确认：

```bash
docker compose ps qdrant
curl http://localhost:6333/collections
```

如果上传返回 403，检查是否使用了 `admin01` 登录。

如果检索返回空数组，优先检查：

- 文档是否已成功上传并 `INDEXED`
- `category` 是否与上传元数据一致，例如 `VPN`
- 当前账号角色是否具备对应 `accessLevel`
- `department` 过滤是否超出当前用户范围

## 12. 其他 thread 如何使用

## 12.1 Thread 2：认证与权限

Thread 2 无需改动知识模块，只需继续保证：

- `UserContext` 可用
- `UserPrincipal.department` 可用
- 角色信息稳定

本模块会直接复用这些能力做访问过滤。

## 12.2 Thread 3：Ticket Core

Thread 3 只需要提供稳定只读能力即可。

当前知识模块通过：

- `TicketQueryService#getTicketDetail`

读取工单标题、描述、分类来构造检索 query。

协作规范：

- 知识模块不会修改 ticket 状态
- 不会直接写 `tickets`
- 不会绕过 Ticket Core 改事件日志

## 12.3 Thread 5：AI 编排

这是最直接的下游调用方。

推荐使用方式：

### 方式 A：通过 HTTP

调用：

- `POST /api/retrieval/search`

建议请求：

```json
{
  "ticketId": 123,
  "category": "VPN",
  "limit": 5,
  "saveCitations": true,
  "aiRunId": "run-xxx"
}
```

说明：

- `results` 可直接作为 AI 输入证据
- `citationId` 可回填到 AI 运行记录或前端展示
- 建议 Thread 5 不自行拼装权限过滤，而是直接复用本服务

### 方式 B：通过 Spring Service

可直接注入：

```java
private final RetrievalService retrievalService;
```

优点：

- 直接复用统一 schema
- 不重复实现向量检索与权限过滤

### Thread 5 协作规范

- 只消费检索结果，不在 AI 模块重复查询底层表
- 不直接访问 `document_chunks` 拼返回
- 不自行决定引用是否越权
- 若要记录证据链，优先使用 `saveCitations=true`

## 12.4 前端或其他模块

如果未来前端或 ticket 详情页需要展示证据，可直接使用：

- `POST /api/retrieval/search`

或者后续基于 `citations` 增加详情接口。

## 13. 当前限制与后续建议

当前限制：

- 还没有接入真实 embedding 模型
- citation 目前只实现落库，没有额外查询接口
- 文档上传当前限制为 `ADMIN`
- 尚未补自动化测试

后续建议：

1. 增加 citation 查询接口，供 ticket 详情直接读取
2. 用真实 embedding 模型替换 hashing provider
3. 增加批量导入和重建索引能力
4. 增加文档删除、重传、版本管理接口
5. 增加针对检索准确率和权限过滤的集成测试

## 14. 总结

当前 Thread 4 已提供一条完整、可编译的最小闭环：

- 文档上传
- 解析
- 切分
- embedding
- Qdrant 写入
- 检索
- 元数据过滤
- citation 落库

该模块已经满足 MVP 中“给定工单内容，返回带来源的可过滤检索结果”的核心目标，并且保持了与 Ticket Core、权限模块、AI 模块之间的职责边界。

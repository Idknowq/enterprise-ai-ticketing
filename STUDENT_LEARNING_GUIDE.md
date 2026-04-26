# 企业级 AI 工单编排系统学习指南

## 1. 文档定位

本文档面向有基础编程能力、但尚未系统学习软件开发的计算机学院学生。

项目已有的文档主要是开发交付文档：

- `enterprise_ai_ticketing_mvp_dev_doc.md`：总体需求、模块边界、技术选型和开发拆分。
- `README_t1.md` 到 `README_t7.md`：各开发线程的模块说明。
- `USER_GUIDE.md`：面向使用者的操作说明。
- `TESTING_GUIDE_FOR_CODEX.md`：测试和验证说明。

这些文档适合查阅，但不适合直接作为入门学习顺序。本文档的目标是把这个项目整理成一套可学习、可复盘、可用于面试表达的课程路线。

学习完成后，应能回答三个层面的问题：

1. 业务层：这个系统解决什么问题，核心流程是什么。
2. 技术层：每个框架、组件、数据库、测试工具为什么被使用。
3. 代码层：从一次前端点击到后端落库、AI 编排、审批流转，代码如何串起来。

## 2. 推荐学习方式

推荐采用“主线实践 + 分模块讲义 + 代码追踪 + 小改造任务”的方式。

不建议一开始逐文件阅读代码。这个项目虽然是 MVP，但已经包含前端、后端、数据库、AI、知识库、审批、监控和测试。直接看代码会很容易迷失在细节里。

推荐顺序如下：

```text
先理解业务主线
再理解项目结构
再按模块学习
再追踪真实请求链路
再做小改造
最后总结成面试表达
```

每一讲都按同一模板学习：

```text
1. 业务目标：这个模块解决什么问题
2. 技术知识：这里涉及哪些框架和概念
3. 代码入口：先看哪些文件
4. 请求链路：一次真实调用经过哪些类和函数
5. 数据模型：数据库表和对象如何对应
6. 验证方法：如何用页面、接口或测试证明它可用
7. 实践任务：做一个小修改，巩固理解
8. 面试表达：如何向面试官讲清楚
```

## 3. 总体学习路线

建议按 9 讲学习：

```text
第 0 讲：项目全局地图
第 1 讲：后端基础设施与 Spring Boot 分层结构
第 2 讲：认证、JWT、用户与权限
第 3 讲：工单核心模块
第 4 讲：知识库、文档切分、Embedding 与 Qdrant
第 5 讲：AI 编排链路
第 6 讲：审批工作流、Temporal 与可观测性
第 7 讲：前端 Next.js 控制台
第 8 讲：测试、联调、部署与大厂工程实践
```

推荐学习节奏：

```text
第一轮：只理解业务和目录，不深究实现细节。
第二轮：每个模块追一条完整代码链路。
第三轮：做小改造，跑测试，修问题。
第四轮：整理项目介绍、技术亮点和面试问答。
```

## 4. 项目全局地图

### 4.1 项目是什么

这是一个企业级 AI 工单编排系统 MVP。它模拟企业内部 IT 服务台场景：

```text
员工提交问题
系统创建工单
AI 自动分类和抽取字段
知识库检索相关 SOP
AI 生成处理建议
高风险请求进入审批
审批完成后更新工单状态
监控系统记录指标和链路
```

可以把它理解为一个简化版的：

```text
ServiceNow / Jira Service Management + 企业知识库 + AI 助手 + 审批流 + 监控平台
```

### 4.2 目录结构

```text
backend/        Java + Spring Boot 后端服务
frontend/       Next.js + React 前端控制台
infra/          Prometheus / Grafana / OpenTelemetry 等基础设施配置
scripts/        冒烟测试、发布检查、接口验证脚本
samples/        示例知识库文档
```

### 4.3 后端模块

```text
common          通用响应、异常、错误码、Trace ID
config          Spring 配置、OpenAPI、安全、Temporal 配置
auth            登录、JWT、用户、角色、权限
ticket          工单创建、查询、评论、分配、状态流转
knowledge       文档上传、切分、Embedding、向量检索、引用记录
ai              工单分类、字段抽取、检索编排、解决方案生成
approval        审批记录、审批命令、待办审批
workflow        Temporal 工作流定义和活动实现
observability   指标、监控面板、运行数据聚合
```

### 4.4 前端模块

```text
app/(auth)/login/page.tsx          登录页
app/(console)/tickets/page.tsx     工单列表与详情
app/(console)/approvals/page.tsx   审批页
app/(console)/documents/page.tsx   文档管理页
app/(console)/monitoring/page.tsx  监控页
lib/services/                      HTTP 接口封装
components/                        通用组件
types/api.ts                       前端接口类型
```

### 4.5 主业务链路

学习时要始终记住这条主线：

```text
登录
  -> 创建工单
  -> AI 分析
  -> 检索知识库
  -> 生成建议
  -> 触发审批
  -> 审批处理
  -> 状态更新
  -> 前端展示
  -> 监控记录
```

## 5. 第 0 讲：项目全局地图

### 学习目标

建立对项目的整体认识，不急着进入代码细节。

### 需要掌握的技术和概念

- MVP：最小可用产品。
- 前后端分离：前端负责界面，后端负责业务和数据。
- 模块化开发：按业务领域拆分代码。
- 企业应用：认证、权限、审计、监控、数据一致性都很重要。
- 工单系统：状态流转、评论、事件日志、分配、审批。

### 建议阅读

```text
enterprise_ai_ticketing_mvp_dev_doc.md
README_t1.md
README_t7.md
USER_GUIDE.md
```

### 重点问题

阅读时重点回答：

1. 这个项目解决的真实业务问题是什么？
2. 为什么它不是一个简单聊天机器人？
3. 为什么要把认证、工单、知识库、AI、审批、监控拆成不同模块？
4. MVP 阶段哪些功能做了，哪些功能故意不做？

### 实践任务

画出一张系统图，至少包括：

```text
浏览器
Next.js 前端
Spring Boot 后端
PostgreSQL
Qdrant
Temporal
Prometheus / Grafana / Jaeger
```

### 面试表达

可以这样介绍项目：

```text
这是一个面向企业 IT 服务台场景的 AI 工单编排系统。用户提交工单后，系统会完成工单分类、字段抽取、知识库检索、AI 处理建议生成，并在高风险请求中触发审批流。项目采用 Spring Boot 和 Next.js 前后端分离架构，后端按 auth、ticket、knowledge、ai、approval、workflow、observability 等领域模块拆分，同时接入 PostgreSQL、Qdrant、Temporal、Prometheus、Grafana 和 Jaeger，覆盖了企业应用中的认证、权限、业务状态机、知识检索、AI 编排、审批和可观测性。
```

## 6. 第 1 讲：后端基础设施与 Spring Boot 分层结构

### 学习目标

理解一个 Spring Boot 企业后端项目如何启动、如何组织代码、如何管理配置、如何统一接口响应和异常处理。

### 需要掌握的技术和框架

- Java 17：后端主语言。
- Maven：依赖管理和构建工具。
- Spring Boot：应用启动、自动配置和依赖注入。
- Spring Web MVC：HTTP 接口开发。
- Spring Validation：请求参数校验。
- Spring Data JPA / Hibernate：ORM 和数据库访问。
- Flyway：数据库版本迁移。
- springdoc-openapi：生成 OpenAPI 和 Swagger UI。
- Actuator：健康检查和运行状态暴露。
- Logback：日志配置。

### 重点文件

```text
backend/pom.xml
backend/src/main/java/com/enterprise/ticketing/EnterpriseAiTicketingApplication.java
backend/src/main/resources/application.yml
backend/src/main/resources/application-dev.yml
backend/src/main/resources/db/migration/
backend/src/main/java/com/enterprise/ticketing/common/api/Result.java
backend/src/main/java/com/enterprise/ticketing/common/handler/GlobalExceptionHandler.java
backend/src/main/java/com/enterprise/ticketing/common/error/ErrorCode.java
backend/src/main/java/com/enterprise/ticketing/config/OpenApiConfig.java
```

### 核心内容

#### 6.1 Spring Boot 启动过程

从 `EnterpriseAiTicketingApplication` 开始理解：

```text
main 方法
  -> SpringApplication.run
  -> 扫描 com.enterprise.ticketing 包
  -> 创建 Controller / Service / Repository Bean
  -> 加载 application.yml
  -> 连接数据库和基础设施
  -> 暴露 HTTP 接口
```

#### 6.2 Maven 依赖

重点理解 `backend/pom.xml` 中这些依赖：

```text
spring-boot-starter-web             开发 REST API
spring-boot-starter-validation      参数校验
spring-boot-starter-actuator        健康检查、指标端点
spring-boot-starter-data-jpa        JPA 数据访问
spring-boot-starter-security        安全认证
springdoc-openapi                   Swagger / OpenAPI 文档
flyway-core                         数据库 migration
postgresql                          PostgreSQL 驱动
micrometer-registry-prometheus      Prometheus 指标
micrometer-tracing-bridge-otel      链路追踪桥接
temporal-sdk                        Temporal 工作流
pdfbox                              PDF 文档解析
```

#### 6.3 企业项目常见分层

后端大部分模块遵循：

```text
Controller
  -> Service
  -> Repository
  -> Database
```

各层职责：

```text
Controller：处理 HTTP 请求和响应，不写复杂业务。
DTO：定义请求和响应结构。
Service：编排业务逻辑、权限检查、状态流转。
Repository：封装数据库查询。
Entity：映射数据库表。
Domain：定义业务枚举和领域概念。
```

#### 6.4 统一返回和异常处理

企业项目通常不会让每个接口随意返回格式，而是统一返回：

```text
{
  "success": true,
  "code": "...",
  "message": "...",
  "data": {}
}
```

本项目通过 `Result<T>` 和 `GlobalExceptionHandler` 统一响应格式和错误处理。

### 实践任务

1. 打开 Swagger UI，查看所有后端接口。
2. 找到 `/api/platform/info` 接口，从 Controller 追到 DTO。
3. 故意传一个非法请求，观察统一异常返回格式。

### 面试表达

```text
后端采用 Spring Boot 3 和 Java 17，按 Controller、Service、Repository、Entity、DTO 分层组织。公共能力放在 common 和 config 模块，例如统一响应 Result、统一异常处理、错误码、OpenAPI 配置和应用配置。数据库结构使用 Flyway 管理，避免手工改表导致环境不一致。
```

## 7. 第 2 讲：认证、JWT、用户与权限

### 学习目标

理解前后端分离项目中用户如何登录，后端如何识别当前用户，如何做基于角色的权限控制。

### 需要掌握的技术和概念

- Spring Security：认证和授权框架。
- JWT：无状态登录令牌。
- RBAC：基于角色的访问控制。
- Filter：请求进入 Controller 之前的过滤器。
- SecurityContext：Spring Security 保存当前用户身份的位置。
- PasswordEncoder：密码加密和校验。

### 重点文件

```text
README_t2.md
backend/src/main/java/com/enterprise/ticketing/auth/controller/AuthController.java
backend/src/main/java/com/enterprise/ticketing/auth/service/AuthService.java
backend/src/main/java/com/enterprise/ticketing/auth/service/AuthServiceImpl.java
backend/src/main/java/com/enterprise/ticketing/auth/security/JwtTokenProvider.java
backend/src/main/java/com/enterprise/ticketing/auth/security/JwtAuthenticationFilter.java
backend/src/main/java/com/enterprise/ticketing/auth/security/UserPrincipal.java
backend/src/main/java/com/enterprise/ticketing/auth/security/UserPrincipalService.java
backend/src/main/java/com/enterprise/ticketing/auth/context/UserContext.java
backend/src/main/java/com/enterprise/ticketing/auth/access/AccessControlService.java
backend/src/main/java/com/enterprise/ticketing/auth/domain/SystemRole.java
backend/src/main/java/com/enterprise/ticketing/auth/entity/UserEntity.java
backend/src/main/java/com/enterprise/ticketing/auth/entity/RoleEntity.java
backend/src/main/resources/db/migration/V2__init_auth_tables.sql
```

### 核心内容

#### 7.1 登录链路

```text
前端提交账号密码
  -> AuthController
  -> AuthServiceImpl
  -> 查询 UserEntity
  -> 校验密码
  -> 生成 JWT
  -> 返回 LoginResponse
  -> 前端保存 token
```

#### 7.2 后续请求如何识别用户

```text
浏览器请求携带 Authorization: Bearer <token>
  -> JwtAuthenticationFilter 拦截
  -> JwtTokenProvider 解析 token
  -> UserPrincipalService 加载用户信息
  -> 写入 SecurityContext
  -> Controller / Service 可获取当前用户
```

#### 7.3 RBAC 权限模型

RBAC 的核心思想：

```text
用户 User
  -> 拥有角色 Role
  -> 角色决定能访问哪些资源和操作
```

本项目中的典型角色：

```text
普通员工
一线支持人员
审批人
管理员
```

### 实践任务

1. 用前端或 curl 登录，拿到 JWT。
2. 不带 JWT 访问需要登录的接口，观察 401。
3. 换不同角色账号访问工单或审批接口，观察数据范围差异。
4. 在代码里找到“当前用户是谁”是如何获取的。

### 面试表达

```text
认证模块使用 Spring Security 和 JWT 实现前后端分离登录。用户登录后后端签发 access token，前端在后续请求中通过 Authorization 头携带。后端通过 JwtAuthenticationFilter 解析 token，把用户身份写入 SecurityContext。业务层通过 UserContext 和 AccessControlService 获取当前用户并执行 RBAC 权限校验。
```

## 8. 第 3 讲：工单核心模块

### 学习目标

理解企业业务系统最常见的 CRUD、状态机、事件日志和权限过滤。

### 需要掌握的技术和概念

- REST API：资源式接口设计。
- DTO 和 Entity 的区别。
- JPA Repository：数据库访问。
- 业务状态机：限制状态如何流转。
- 事件日志：记录关键业务动作。
- 事务：保证多表写入一致性。
- 分页查询：列表接口常见能力。

### 重点文件

```text
README_t3.md
backend/src/main/java/com/enterprise/ticketing/ticket/controller/TicketController.java
backend/src/main/java/com/enterprise/ticketing/ticket/service/TicketService.java
backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketCoreServiceImpl.java
backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketQueryServiceImpl.java
backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketDtoMapper.java
backend/src/main/java/com/enterprise/ticketing/ticket/service/impl/TicketAccessPolicy.java
backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketEntity.java
backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketEventEntity.java
backend/src/main/java/com/enterprise/ticketing/ticket/entity/TicketCommentEntity.java
backend/src/main/java/com/enterprise/ticketing/ticket/repository/TicketRepository.java
backend/src/main/java/com/enterprise/ticketing/ticket/domain/TicketStatus.java
backend/src/main/java/com/enterprise/ticketing/ticket/domain/TicketPriority.java
backend/src/main/resources/db/migration/V3__init_ticket_core.sql
```

### 核心内容

#### 8.1 工单为什么是核心模块

其他模块大多围绕工单工作：

```text
认证模块决定谁能看工单
知识库模块根据工单内容检索文档
AI 模块分析工单
审批模块根据工单发起审批
前端围绕工单展示业务流程
监控模块统计工单处理情况
```

#### 8.2 创建工单链路

```text
前端提交 CreateTicketRequest
  -> TicketController.create
  -> TicketService.create
  -> TicketCoreServiceImpl
  -> 校验当前用户
  -> 保存 TicketEntity
  -> 写入 TicketEventEntity
  -> 返回 TicketResponse
```

#### 8.3 状态机

工单状态不是随意修改的。典型状态包括：

```text
OPEN
AI_PROCESSING
WAITING_APPROVAL
IN_PROGRESS
RESOLVED
CLOSED
REJECTED
```

学习时要重点理解：

```text
哪些状态可以转到哪些状态
谁有权限改变状态
状态变化是否写入事件日志
状态变化是否影响前端显示和审批流程
```

#### 8.4 事件日志

事件日志用于回答：

```text
谁在什么时候做了什么
工单为什么从一个状态变成另一个状态
处理过程是否可审计
```

这是企业系统和课堂 CRUD demo 的重要区别。

### 实践任务

1. 创建一个工单。
2. 查询工单列表和详情。
3. 添加评论。
4. 分配工单。
5. 修改状态。
6. 查看事件时间线是否完整。

### 面试表达

```text
工单模块是系统的主业务模块，负责工单创建、查询、评论、分配和状态流转。实现上采用 Controller、Service、Repository 分层，核心状态通过 TicketStatus 管理，所有关键动作都会写入 TicketEventEntity 形成时间线，保证业务过程可追踪、可审计。
```

## 9. 第 4 讲：知识库、文档切分、Embedding 与 Qdrant

### 学习目标

理解企业知识库如何从文档上传变成可检索的知识片段，以及向量数据库在 AI 应用中的作用。

### 需要掌握的技术和概念

- RAG：Retrieval-Augmented Generation，检索增强生成。
- 文档解析：Markdown、TXT、PDF 转文本。
- Chunk：把长文档切成小片段。
- Embedding：把文本转换成向量。
- 向量相似度：用向量距离查找相关内容。
- Qdrant：向量数据库。
- Citation：AI 答案引用来源。
- 权限过滤：不同用户只能检索允许访问的知识。

### 重点文件

```text
README_t4.md
samples/knowledge/
backend/src/main/java/com/enterprise/ticketing/knowledge/controller/DocumentController.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/DocumentService.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/RetrievalService.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/DocumentServiceImpl.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/RetrievalServiceImpl.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/TextChunker.java
backend/src/main/java/com/enterprise/ticketing/knowledge/service/impl/HashingEmbeddingProvider.java
backend/src/main/java/com/enterprise/ticketing/knowledge/qdrant/QdrantClient.java
backend/src/main/java/com/enterprise/ticketing/knowledge/entity/KnowledgeDocumentEntity.java
backend/src/main/java/com/enterprise/ticketing/knowledge/entity/DocumentChunkEntity.java
backend/src/main/java/com/enterprise/ticketing/knowledge/entity/CitationEntity.java
backend/src/main/resources/db/migration/V4__init_knowledge_module.sql
```

### 核心内容

#### 9.1 文档上传链路

```text
管理员上传文档
  -> DocumentController
  -> DocumentServiceImpl
  -> 解析文本
  -> TextChunker 切分
  -> EmbeddingProvider 生成向量
  -> 保存 documents / document_chunks
  -> 写入 Qdrant
```

#### 9.2 检索链路

```text
输入工单内容或查询文本
  -> RetrievalServiceImpl
  -> 生成 query embedding
  -> QdrantClient 查询相似片段
  -> 根据部门和访问级别过滤
  -> 返回 citations
```

#### 9.3 为什么需要 Citation

AI 系统不能只给一个没有依据的答案。Citation 用来记录：

```text
答案参考了哪篇文档
参考了哪个片段
相似度是多少
是否符合权限
```

### 实践任务

1. 上传 `samples/knowledge/` 下的示例文档。
2. 用工单内容检索相关知识。
3. 查看返回的 citation。
4. 修改文档内容后重新上传，观察检索结果变化。

### 面试表达

```text
知识库模块实现了一个简化 RAG 流程。文档上传后会被解析成文本，再按 chunk 切分，使用 EmbeddingProvider 转成向量并写入 Qdrant。检索时，系统把查询文本也转成向量，到 Qdrant 中查找相似片段，并结合部门和访问级别做权限过滤，最终以 citation 的形式返回证据来源。
```

## 10. 第 5 讲：AI 编排链路

### 学习目标

理解 AI 在企业系统中不是孤立聊天，而是作为业务流程中的一个可观测、可追踪、可结构化输出的模块。

### 需要掌握的技术和概念

- AI Orchestration：AI 编排。
- Rule-based provider：规则模拟的 AI Provider。
- LLM Provider：可替换的大模型调用层。
- Schema 化输出：AI 输出必须结构化，便于后续系统消费。
- Run log：记录每次 AI 运行过程。
- 节点化设计：Classifier、Extractor、Retriever、Resolution。

### 重点文件

```text
README_t5.md
backend/src/main/java/com/enterprise/ticketing/ai/controller/AiController.java
backend/src/main/java/com/enterprise/ticketing/ai/service/AiOrchestrationService.java
backend/src/main/java/com/enterprise/ticketing/ai/service/impl/DefaultAiOrchestrationService.java
backend/src/main/java/com/enterprise/ticketing/ai/service/impl/AiRunLogService.java
backend/src/main/java/com/enterprise/ticketing/ai/workflow/AiWorkflowState.java
backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketClassifierNode.java
backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketExtractorNode.java
backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketRetrieverNode.java
backend/src/main/java/com/enterprise/ticketing/ai/workflow/TicketResolutionNode.java
backend/src/main/java/com/enterprise/ticketing/ai/domain/AiRunStatus.java
backend/src/main/java/com/enterprise/ticketing/ai/domain/AiNodeName.java
backend/src/main/resources/db/migration/V5__init_ai_orchestration.sql
```

### 核心内容

#### 10.1 AI 主链路

```text
手动或系统触发 AI 分析
  -> AiController
  -> DefaultAiOrchestrationService
  -> 读取工单
  -> Classifier 分类
  -> Extractor 抽取字段
  -> Retriever 检索知识库
  -> Resolution 生成处理建议
  -> 保存 ai_run 日志
  -> 可能触发审批
```

#### 10.2 为什么 AI 输出要结构化

企业系统不能只依赖自然语言，因为后续模块需要消费 AI 输出：

```text
工单类别
优先级
置信度
抽取字段
建议动作
是否需要审批
审批原因
引用来源
```

如果 AI 只输出一段话，审批模块、前端页面、监控统计都很难稳定工作。

#### 10.3 为什么要记录 AI 运行日志

AI 运行日志用于：

```text
排查 AI 为什么给出某个建议
统计 AI 成功率和失败率
记录模型名称、耗时、节点状态
支持后续审计
```

### 实践任务

1. 创建一个 VPN 或权限申请工单。
2. 手动触发 AI 分析。
3. 查看 AI 返回的分类、字段、引用和建议。
4. 查询 AI run log。
5. 修改工单描述，观察 AI 输出变化。

### 面试表达

```text
AI 模块采用节点化编排设计，把一次 AI 分析拆成分类、字段抽取、知识检索和解决方案生成。每个节点都输出结构化结果，最终形成可被前端、审批和监控模块消费的 schema。系统还记录 AI run log，用于追踪节点状态、耗时和失败原因。
```

## 11. 第 6 讲：审批工作流、Temporal 与可观测性

### 学习目标

理解复杂业务流程为什么需要工作流引擎，以及企业系统如何监控运行状态。

### 需要掌握的技术和概念

- Approval：审批。
- Workflow：长流程状态管理。
- Temporal：工作流引擎。
- Activity：工作流中的可重试业务步骤。
- 幂等：重复调用不会造成重复副作用。
- Micrometer：指标采集。
- Prometheus：指标存储和查询。
- Grafana：监控面板。
- OpenTelemetry：链路追踪标准。
- Jaeger：链路追踪查看工具。

### 重点文件

```text
README_t6.md
backend/src/main/java/com/enterprise/ticketing/approval/controller/ApprovalController.java
backend/src/main/java/com/enterprise/ticketing/approval/service/ApprovalCommandService.java
backend/src/main/java/com/enterprise/ticketing/approval/service/ApprovalQueryService.java
backend/src/main/java/com/enterprise/ticketing/approval/service/ApprovalWorkflowService.java
backend/src/main/java/com/enterprise/ticketing/approval/service/impl/
backend/src/main/java/com/enterprise/ticketing/approval/entity/ApprovalEntity.java
backend/src/main/java/com/enterprise/ticketing/workflow/ApprovalWorkflow.java
backend/src/main/java/com/enterprise/ticketing/workflow/impl/ApprovalWorkflowImpl.java
backend/src/main/java/com/enterprise/ticketing/workflow/activity/ApprovalWorkflowActivities.java
backend/src/main/java/com/enterprise/ticketing/workflow/impl/ApprovalWorkflowActivitiesImpl.java
backend/src/main/java/com/enterprise/ticketing/observability/controller/ObservabilityController.java
backend/src/main/java/com/enterprise/ticketing/observability/service/TelemetryService.java
backend/src/main/resources/db/migration/V6__init_approval_workflow.sql
infra/prometheus/prometheus.yml
infra/grafana/dashboards/
infra/otel/otel-collector-config.yml
```

### 核心内容

#### 11.1 审批为什么需要工作流

审批不是普通的一次性接口调用。它可能经历：

```text
AI 判断需要审批
  -> 发起审批
  -> 等待直属主管
  -> 等待系统管理员
  -> 审批通过或驳回
  -> 恢复工单流程
```

这个过程可能跨越几分钟、几小时甚至几天。用普通函数调用很难管理这种长时间等待状态，因此引入 Temporal。

#### 11.2 审批链路

```text
AI 输出需要审批
  -> ApprovalWorkflowService
  -> 启动 Temporal workflow
  -> 创建 ApprovalEntity
  -> 工单进入 WAITING_APPROVAL
  -> 审批人处理
  -> ApprovalCommandService
  -> Temporal workflow 收到 signal
  -> 更新审批状态
  -> 更新工单状态
```

#### 11.3 可观测性

企业系统上线后，不能只知道“能不能跑”，还要知道：

```text
接口是否健康
请求耗时是多少
错误率是多少
AI 分析成功率是多少
审批积压多少
哪一次请求在哪个模块慢
```

本项目通过以下组件实现：

```text
Actuator：暴露健康检查和指标端点
Micrometer：在应用内采集指标
Prometheus：拉取和存储指标
Grafana：展示监控面板
OpenTelemetry：采集链路追踪
Jaeger：查看请求链路
```

### 实践任务

1. 创建一个会触发审批的工单。
2. 触发 AI 分析。
3. 查看待审批列表。
4. 审批通过或驳回。
5. 观察工单状态变化。
6. 打开 Prometheus / Grafana / Jaeger 查看指标和链路。

### 面试表达

```text
审批模块使用 Temporal 管理长流程状态，把审批启动、等待、恢复和状态更新封装成 workflow 和 activity。这样可以避免把长时间等待逻辑散落在普通业务代码中。可观测性方面，系统通过 Actuator 和 Micrometer 暴露指标，Prometheus 采集，Grafana 展示，OpenTelemetry 和 Jaeger 用于查看链路追踪。
```

## 12. 第 7 讲：前端 Next.js 控制台

### 学习目标

理解前端如何组织页面、调用后端接口、管理登录态、展示业务状态，并通过测试保证页面行为。

### 需要掌握的技术和框架

- Next.js 15：React 全栈框架和 App Router。
- React 19：组件化 UI。
- TypeScript：类型系统。
- Ant Design：企业后台 UI 组件库。
- fetch 封装：统一处理 HTTP 请求。
- localStorage：保存登录 token。
- 受保护路由：未登录跳转登录页。
- 组件测试：验证页面交互。

### 重点文件

```text
README_t7.md
frontend/package.json
frontend/app/layout.tsx
frontend/app/page.tsx
frontend/app/(auth)/login/page.tsx
frontend/app/(console)/layout.tsx
frontend/app/(console)/tickets/page.tsx
frontend/app/(console)/approvals/page.tsx
frontend/app/(console)/documents/page.tsx
frontend/app/(console)/monitoring/page.tsx
frontend/components/console-shell.tsx
frontend/components/app-provider.tsx
frontend/components/page-state.tsx
frontend/components/status-tags.tsx
frontend/lib/http.ts
frontend/lib/auth-storage.ts
frontend/lib/services/
frontend/types/api.ts
```

### 核心内容

#### 12.1 前端页面结构

```text
登录页
  -> 获取 JWT
  -> 保存登录态
  -> 进入控制台

控制台布局
  -> 左侧导航
  -> 顶部用户信息
  -> 子页面内容

工单页
  -> 查询工单列表
  -> 创建工单
  -> 查看详情
  -> 添加评论
  -> 触发 AI

审批页
  -> 查询待审批
  -> 通过或驳回

文档页
  -> 上传知识文档
  -> 查询文档列表

监控页
  -> 查看系统指标和仪表盘数据
```

#### 12.2 前端请求链路

```text
页面组件点击按钮
  -> 调用 lib/services/*.ts
  -> 调用 lib/http.ts
  -> 自动带上 JWT
  -> 请求 Spring Boot API
  -> 解析 Result<T>
  -> 更新页面状态
```

#### 12.3 TypeScript 的价值

`types/api.ts` 用来约束前端拿到的数据结构。这样后端接口字段变化时，前端更容易在编译阶段发现问题。

### 实践任务

1. 找到登录页，追踪登录按钮点击后的代码。
2. 找到工单列表页，追踪它如何调用接口。
3. 在页面上新增一个字段展示，例如工单优先级或创建时间。
4. 修改对应测试，确保页面仍然通过。

### 面试表达

```text
前端采用 Next.js App Router 和 React 组件化开发，页面按登录区和控制台区拆分。接口请求统一封装在 lib/services 和 lib/http 中，自动处理 JWT 和统一响应格式。UI 使用 Ant Design 构建企业后台页面，TypeScript 类型定义保证前后端数据结构一致，组件测试和 E2E 测试覆盖主要交互。
```

## 13. 第 8 讲：测试、联调、部署与大厂工程实践

### 学习目标

理解真实项目不是写完代码就结束，还需要测试、联调、自动化检查、环境管理和发布前验证。

### 需要掌握的技术和概念

- 单元测试：测试一个函数或服务。
- 组件测试：测试前端组件渲染和交互。
- MSW：前端测试中 mock 后端 API。
- E2E 测试：模拟用户从浏览器操作完整流程。
- Playwright：浏览器自动化测试。
- 冒烟测试：快速验证核心功能是否可用。
- Docker Compose：本地一键启动依赖。
- OpenAPI：前后端接口契约。
- CI 思维：自动化执行构建、测试和检查。

### 重点文件

```text
TESTING_GUIDE_FOR_CODEX.md
TEST_PROGRESS.md
RELEASE_CHECK.md
scripts/openapi-smoke.sh
scripts/release-check.sh
scripts/k6-smoke.js
docker-compose.yml
frontend/tests/
frontend/playwright.config.ts
frontend/vitest.config.ts
backend/src/test/
```

### 核心内容

#### 13.1 测试分层

```text
后端单元测试
  -> 验证 service、权限、状态机等业务规则

前端组件测试
  -> 验证页面在不同数据状态下如何展示

服务测试
  -> 验证 API client 和 mock 服务协作

E2E 测试
  -> 从浏览器模拟真实用户操作

冒烟测试
  -> 发布前快速验证核心接口和页面可用
```

#### 13.2 Docker Compose 的作用

`docker-compose.yml` 让开发者本地快速启动完整依赖：

```text
PostgreSQL
Redis
RabbitMQ
Qdrant
Temporal
Prometheus
Grafana
Jaeger
OpenTelemetry Collector
```

这体现了现代软件开发的重要理念：环境应该可复制，而不是依赖某个人电脑上的手工配置。

#### 13.3 OpenAPI 的作用

OpenAPI 是前后端之间的接口契约。后端通过 springdoc 生成接口文档，前端和测试可以据此确认：

```text
接口路径
请求方法
请求参数
响应结构
错误返回
鉴权要求
```

### 实践任务

1. 运行后端测试。
2. 运行前端组件测试。
3. 运行 E2E 测试。
4. 执行 release check。
5. 故意改坏一个字段，观察测试如何失败。

### 面试表达

```text
项目采用多层测试策略。后端测试关注业务规则，前端使用 Vitest 和 Testing Library 做组件测试，使用 MSW mock API，E2E 使用 Playwright 模拟真实浏览器流程。基础设施通过 Docker Compose 管理，发布前通过脚本执行 OpenAPI 冒烟测试和 release check，保证核心功能可用。
```

## 14. 每讲通用代码追踪方法

学习任何一个功能时，都按下面顺序追代码：

```text
1. 从页面或接口开始
2. 找到请求路径和 HTTP 方法
3. 找到 Controller
4. 找到 Request DTO 和 Response DTO
5. 找到 Service 接口
6. 找到 Service 实现
7. 找到 Repository
8. 找到 Entity
9. 找到数据库 migration
10. 找到前端 service 封装
11. 找到相关测试
```

例如学习“创建工单”：

```text
frontend/app/(console)/tickets/page.tsx
  -> frontend/lib/services/tickets.ts
  -> backend ticket/controller/TicketController.java
  -> CreateTicketRequest.java
  -> TicketService.java
  -> TicketCoreServiceImpl.java
  -> TicketRepository.java
  -> TicketEntity.java
  -> V3__init_ticket_core.sql
  -> frontend/tests/components/tickets-page.test.tsx
```

## 15. 学习时不要陷入的误区

### 15.1 不要一开始逐行读所有代码

真实项目代码量大，逐行阅读效率很低。应先建立业务主线，再按功能追代码。

### 15.2 不要只看 Controller

Controller 通常只是入口。真正的业务规则往往在 Service、Policy、Workflow、Repository 和数据库约束中。

### 15.3 不要忽略测试

测试是理解项目行为的好入口。一个好的测试会告诉你：

```text
输入是什么
期望输出是什么
边界情况是什么
业务规则是什么
```

### 15.4 不要把 AI 当成孤立能力

企业 AI 系统重点不只是“调用大模型”，而是：

```text
AI 输出是否结构化
是否有知识来源
是否可审计
是否能接入业务状态机
失败时系统如何降级
```

### 15.5 不要忽略权限和审计

企业项目和个人 demo 的重要区别是：

```text
谁能看什么
谁能操作什么
操作过程是否可追踪
错误是否可定位
系统是否可监控
```

## 16. 建议学习产出

每学完一讲，建议产出以下内容：

```text
一张模块图
一条请求链路图
一段 3 分钟口述总结
一个小改造 commit
一组验证截图或测试结果
```

最后形成一份项目复盘：

```text
1. 项目背景和业务目标
2. 技术架构图
3. 核心模块说明
4. 一条完整业务链路
5. 最有技术含量的 3 个设计点
6. 遇到的问题和解决方式
7. 后续可优化方向
```

## 17. 推荐面试准备方向

### 17.1 项目介绍

准备一个 1 分钟版本和一个 5 分钟版本。

1 分钟版本讲：

```text
项目做什么
用了什么技术栈
你负责或理解的核心模块
解决了哪些工程问题
```

5 分钟版本讲：

```text
业务背景
系统架构
主流程
核心模块
技术难点
测试和部署
改进方向
```

### 17.2 高频问题

建议准备以下问题：

```text
为什么用 Spring Boot？
JWT 登录流程是什么？
RBAC 怎么实现？
工单状态机怎么设计？
为什么要记录事件日志？
Flyway 解决什么问题？
RAG 的流程是什么？
Embedding 和向量数据库是什么？
AI 输出为什么要结构化？
Temporal 解决什么问题？
Prometheus、Grafana、Jaeger 分别做什么？
前端如何封装 API 请求？
组件测试和 E2E 测试有什么区别？
如果线上出现接口变慢，你如何排查？
如果 AI 调用失败，系统如何降级？
```

### 17.3 可重点强调的技术亮点

```text
前后端分离架构
Spring Boot 分层设计
JWT + RBAC 权限控制
工单状态机和事件日志
Flyway 数据库版本管理
RAG 知识库检索
Qdrant 向量数据库
AI 节点化编排和结构化输出
Temporal 长流程审批
Prometheus / Grafana / Jaeger 可观测性
Vitest / Playwright 自动化测试
Docker Compose 本地环境
```

## 18. 建议学习顺序清单

可以按下面清单推进：

```text
[ ] 读完本文档，理解学习方法
[ ] 读 enterprise_ai_ticketing_mvp_dev_doc.md 的项目背景和模块划分
[ ] 跑通本地依赖和后端
[ ] 跑通前端登录
[ ] 创建第一个工单
[ ] 追踪创建工单的完整代码链路
[ ] 学习 JWT 登录和权限判断
[ ] 学习工单状态机和事件日志
[ ] 上传知识文档并执行检索
[ ] 触发 AI 分析并查看结构化输出
[ ] 触发审批并完成审批
[ ] 查看监控指标和链路追踪
[ ] 运行前后端测试
[ ] 做一个小功能改造
[ ] 整理项目复盘和面试表达
```

## 19. 下一步建议

完成本文档的学习框架后，建议从第 1 讲开始进行详细代码讲解。每一讲都应该边看代码边运行项目，不要只读文字。

推荐第一个实践目标：

```text
跑通登录 -> 创建工单 -> 查询工单详情 -> 添加评论
```

这条链路短、清晰、覆盖前端、后端、认证、数据库和接口封装，是进入整个项目最合适的起点。

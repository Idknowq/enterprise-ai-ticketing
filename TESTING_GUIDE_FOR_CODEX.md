# 企业级 AI 工单编排系统测试方案（Codex 协作版）

## 1. 文档目标

本文档用于指导本仓库在 MVP 完成后建立一套接近大厂工程实践的测试体系，并且特别适配 Codex / 多 agent 协作开发场景。

目标不是“把测试说全”，而是形成一套可以直接执行的规范：

- 明确测试分层与职责边界
- 明确每一层建议使用的具体技术
- 明确测试代码应放在哪、如何命名、如何运行
- 明确 CI 门禁和质量阈值
- 明确 Codex 在修改代码时必须补哪些测试

本文档默认面向当前仓库技术栈：

- 后端：Java 17、Spring Boot 3.x、JPA、Flyway、PostgreSQL、Redis、RabbitMQ、Temporal、OpenTelemetry
- 前端：Next.js 15、React 19、TypeScript、Ant Design
- 基础设施：Docker Compose、Prometheus、Grafana、Jaeger

---

## 2. 测试总体原则

### 2.1 测试目标

测试体系需要覆盖以下风险：

- 领域逻辑错误
- 接口行为回归
- 数据库 migration 破坏
- 安全与权限回归
- AI 输出结构回归
- 工作流挂起 / 恢复 / 幂等问题
- 前端页面关键路径失效
- 性能退化
- 依赖升级导致的兼容性问题

### 2.2 测试分层

推荐固定为 7 层：

1. 单元测试
2. 切片测试
3. 集成测试
4. 合同 / API 测试
5. 端到端测试
6. 非功能测试
7. 发布前回归测试

### 2.3 测试金字塔

强制遵守以下比例倾向：

- 单元测试最多
- 切片 / 集成测试适中
- E2E 最少但必须覆盖关键路径

不允许把所有验证都堆到 E2E，否则执行慢、维护成本高、定位困难。

### 2.4 Codex 协作原则

Codex 在修改代码时，必须优先补“离改动最近”的测试，而不是默认只写冒烟测试。

例如：

- 修改纯业务规则：优先补单元测试
- 修改 Controller / DTO 校验：优先补 WebMvc slice 测试
- 修改 Repository / migration：优先补 JPA + Testcontainers 测试
- 修改工作流：优先补 Temporal 测试
- 修改前端页面：优先补组件测试与 Playwright 场景

---

## 3. 推荐测试技术栈

## 3.1 后端核心测试

### 必选

- `JUnit 5`
  - 后端测试基座
  - 用于单元测试、参数化测试、嵌套测试、生命周期管理

- `Spring Boot Test`
  - 用于 `@SpringBootTest`
  - 用于 `@WebMvcTest`、`@DataJpaTest`、`@JsonTest`

- `Mockito`
  - 用于 mock 外部依赖与失败路径
  - 只用于单元测试或切片测试，不要滥用到所有测试

- `AssertJ`
  - 已包含在 `spring-boot-starter-test` 中
  - 统一断言风格，提升可读性

### 强烈建议新增

- `Testcontainers`
  - 使用真实 PostgreSQL、Redis、RabbitMQ 容器
  - 避免 H2 与真实 PostgreSQL 行为不一致
  - 适用于 repository、service 集成测试、migration 测试

- `temporal-testing`
  - 用于 Temporal Workflow / Activity 测试
  - 建议使用 `TestWorkflowEnvironment`

- `Awaitility`
  - 用于异步状态轮询
  - 适合消息消费、异步 AI 流程、审批状态等待

- `WireMock`
  - 用于模拟外部 HTTP 依赖
  - 适合 LLM Provider、Embedding Provider、外部知识服务

### 可选

- `ArchUnit`
  - 用于约束架构边界
  - 例如禁止 `ai` 模块直接依赖 `ticket.repository`

---

## 3.2 API / 合同测试

### 推荐

- `REST Assured`
  - 适合 Java 内部 API 验证
  - 用于接口响应、认证头、错误码、状态码校验

- `Schemathesis`
  - 基于 OpenAPI 自动生成 API 测试与 fuzz 测试
  - 特别适合本项目已有 Swagger / OpenAPI 的现状
  - 用于发现 schema 与实现不一致、边界输入处理不足

### 可选

- `Pact`
  - 当前适合在前后端接口稳定后再引入
  - 若后续拆服务或多前端客户端，可以再上

---

## 3.3 前端测试

### 推荐

- `Vitest`
  - 前端单元测试与组件测试基座
  - 比 Jest 更适合当前 Vite / TS 生态思路

- `@testing-library/react`
  - 组件级行为测试
  - 验证用户交互，不测试内部实现细节

- `@testing-library/user-event`
  - 模拟真实用户输入

- `MSW`
  - Mock 前端请求
  - 避免前端测试依赖真实后端

- `Playwright`
  - 端到端测试
  - 覆盖登录、工单创建、审批、知识检索、AI 调试、监控页等关键路径

---

## 3.4 性能 / 稳定性测试

### 推荐

- `k6`
  - HTTP 压测、并发测试、soak test、stress test
  - 可直接纳入 CI 的 nightly job

### 适用场景

- 工单创建接口
- 工单列表分页接口
- AI 调试接口
- 文档检索接口
- 审批待办列表接口

---

## 3.5 安全测试

### 推荐

- `OWASP ZAP`
  - Web / API 动态安全扫描
  - 扫 XSS、未授权访问、弱配置、常见漏洞模式

- `Trivy`
  - 扫 Docker 镜像、依赖漏洞、IaC 配置风险

### 可选

- `gitleaks`
  - 扫描密钥泄露

---

## 3.6 质量门禁

### 推荐

- `JaCoCo`
  - Java 覆盖率报告

- `SonarQube`
  - 静态分析、质量门禁、重复代码、覆盖率趋势

- `ESLint`
  - 前端静态检查

- `TypeScript`
  - 前端类型门禁

---

## 4. 仓库测试目录规范

## 4.1 后端目录

```text
backend/
  src/test/java/com/enterprise/ticketing/
    common/
    auth/
    ticket/
    knowledge/
    ai/
    approval/
    workflow/
    observability/
    architecture/
  src/test/resources/
    application-test.yml
    sql/
    payloads/
```

### 命名约定

- 单元测试：`*Test.java`
- 集成测试：`*IntegrationTest.java`
- 切片测试：`*WebMvcTest.java`、`*DataJpaTest.java`
- 工作流测试：`*WorkflowTest.java`
- 架构测试：`*ArchitectureTest.java`

---

## 4.2 前端目录

```text
frontend/
  src/
  tests/
    unit/
    component/
    e2e/
    fixtures/
    mocks/
```

### 命名约定

- 单元 / 组件测试：`*.test.ts`、`*.test.tsx`
- E2E：`*.spec.ts`

---

## 5. 后端测试分层方案

## 5.1 单元测试

### 目标

验证纯逻辑，不启动 Spring 容器，不连真实数据库，不连 MQ。

### 适合测试的对象

- 状态流转判断
- DTO mapper
- 权限校验逻辑
- 文本切分逻辑
- Prompt 模板组装逻辑
- AI 输出 schema 校验逻辑
- 错误码映射逻辑

### 技术方案

- JUnit 5
- Mockito
- AssertJ

### 编写要求

- 一个测试类只测一个核心对象
- 一个测试方法只验证一个行为
- 覆盖正常路径、边界路径、异常路径

### 示例模块

- `ticket.service.impl`
- `knowledge.service.impl.TextChunker`
- `ai.provider.RuleBasedStructuredLlmProvider`
- `auth.access.RoleChecker`

---

## 5.2 Web 层切片测试

### 目标

验证 Controller、参数校验、异常处理、统一返回结构、认证拦截。

### 技术方案

- `@WebMvcTest`
- `MockMvc`
- Mockito

### 必测内容

- 200 / 400 / 401 / 403 / 404 / 409
- `Result<T>` 返回结构
- 字段校验注解是否生效
- 全局异常处理是否输出稳定错误码

### 适用接口

- `AuthController`
- `TicketController`
- `DocumentController`
- `ApprovalController`
- `AiDebugController`
- `ObservabilityController`

---

## 5.3 数据层切片测试

### 目标

验证 Repository、自定义查询、JPA 映射、索引假设、枚举持久化。

### 技术方案

- `@DataJpaTest`
- PostgreSQL Testcontainer
- Flyway

### 不建议

不要使用 H2 替代 PostgreSQL 做主验证。当前项目使用：

- `jsonb`
- PostgreSQL 枚举 / 时间语义
- Flyway migration

这些都可能和 H2 不一致。

### 必测内容

- 实体映射是否正确
- 自定义查询是否返回正确结果
- 状态筛选 / 排序 / 分页是否正确
- 审批待办查询与权限范围过滤是否正确

---

## 5.4 后端集成测试

### 目标

验证模块协作，而不是单个类。

### 技术方案

- `@SpringBootTest`
- Testcontainers
- `@AutoConfigureMockMvc`
- Awaitility

### 建议容器

- PostgreSQL
- Redis
- RabbitMQ

### 适用场景

- 创建工单后事件是否落库
- 审批回调后状态是否正确恢复
- 登录后访问受保护接口是否正确
- 知识上传后文档、chunk、citation 是否一致
- AI workflow 记录是否正确写入 `ai_runs`

---

## 5.5 Migration 测试

### 目标

验证 Flyway 脚本在空库和增量升级场景下都能成功。

### 技术方案

- Testcontainers PostgreSQL
- 启动 Spring 上下文执行 Flyway

### 必测内容

- 从空数据库执行全部 migration
- 从旧版本迁移到最新版本
- 关键表是否存在
- 索引与约束是否按预期建立

### 规则

每新增一个 migration，至少新增一个 migration 测试或更新已有断言。

---

## 5.6 工作流测试

### 目标

验证 Temporal workflow 的可靠性、幂等性、挂起/恢复行为。

### 技术方案

- Temporal Java Testing
- `TestWorkflowEnvironment`
- Mockito / fake Activity

### 必测内容

- 审批 workflow 正常通过
- 审批 workflow 驳回
- 重复 signal 不导致状态错乱
- 超时路径
- activity 异常重试

### 特别要求

工作流测试不要依赖真实 Temporal 服务，优先使用官方 testing environment。

---

## 5.7 AI 模块测试

### 目标

验证 AI 模块“结构化输出正确”，而不是只验证模型是否“看起来聪明”。

### 技术方案

- 单元测试
- WireMock 或 mock provider
- Golden file 测试

### 必测内容

- 分类输出字段完整
- 抽取输出字段完整
- 引用列表结构正确
- `requiresApproval` / `needsHumanHandoff` 逻辑稳定
- provider 降级逻辑
- 外部 LLM 失败时的 fallback

### 推荐方案

对 `RuleBasedStructuredLlmProvider` 和 `DeepSeekStructuredLlmProvider` 分层处理：

- 规则 provider：做强单元测试
- 真实 provider：做接口契约测试，不依赖真实线上模型作为 CI 阶段强依赖

---

## 6. 前端测试方案

## 6.1 组件测试

### 目标

验证页面组件与状态展示，不依赖真实后端。

### 技术方案

- Vitest
- React Testing Library
- MSW

### 必测对象

- 登录表单
- 工单列表
- 工单详情时间线
- 审批列表与审批动作
- 文档上传页
- 监控页关键指标卡片

### 验证重点

- 加载态
- 空态
- 错误态
- 权限差异展示
- 提交按钮禁用 / 可用状态

---

## 6.2 E2E 测试

### 目标

覆盖关键用户路径，验证“系统真的能用”。

### 技术方案

- Playwright

### MVP 必测流程

1. 用户登录
2. 创建工单
3. 查看工单详情
4. 支持人员分配工单
5. 审批人审批 / 驳回
6. 文档上传与检索验证
7. AI 调试接口触发与结果查看
8. 监控页可见核心指标

### 执行层级

- PR 阶段：只跑 smoke E2E
- nightly：跑完整 E2E 回归

---

## 7. API 测试方案

## 7.1 OpenAPI 一致性测试

### 目标

确保 Swagger 文档不是摆设，接口实现与 schema 一致。

### 技术方案

- Schemathesis

### 执行方式

- 启动本地后端
- 基于 `/api-docs` 读取 OpenAPI
- 自动生成边界请求并校验响应

### 必测重点

- required 字段
- enum 合法值
- 错误响应结构
- 非法请求处理

---

## 7.2 接口回归测试

### 技术方案

- REST Assured

### 适合做的内容

- 登录拿 token
- 鉴权访问控制
- 工单创建 / 查询 / 状态更新
- 审批通过 / 驳回
- 文档上传与检索

---

## 8. 非功能测试

## 8.1 性能测试

### 技术方案

- k6

### 最低覆盖接口

- `POST /api/tickets`
- `GET /api/tickets`
- `GET /api/tickets/{id}`
- `GET /api/approvals/pending`
- `POST /api/retrieval/search`
- `POST /api/ai/tickets/{id}/run`

### 目标建议

- 工单创建 API：P95 < 300ms
- 工单列表：P95 < 500ms
- 检索接口：P95 < 1.5s
- AI 调试接口：单独统计异步与同步耗时

### 测试类型

- smoke load：小并发，PR 前后手动验证
- stress test：找系统瓶颈
- soak test：长时间稳定性

---

## 8.2 安全测试

### 技术方案

- OWASP ZAP
- Trivy

### ZAP 重点

- 未授权访问
- 越权访问
- 常见输入注入路径
- 安全头缺失

### Trivy 重点

- Docker 镜像漏洞
- Maven / npm 依赖漏洞
- Docker Compose 风险配置

---

## 9. Observability 验证

这个项目不是普通 CRUD，必须验证可观测性。

### 必测内容

- 接口请求是否生成 trace
- 指标是否暴露到 `/actuator/prometheus`
- Jaeger 中是否能看到关键请求
- Grafana dashboard 是否能展示核心指标
- AI / 审批 / 检索路径是否带 traceId

### 技术方案

- 集成测试验证 metrics endpoint
- 手动或 E2E 验证 Jaeger / Grafana
- 对日志中 traceId 做断言或采样检查

---

## 10. 建议新增依赖与工具清单

## 10.1 后端 Maven

建议后续加入以下测试依赖：

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>rabbitmq</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>redis</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.temporal</groupId>
  <artifactId>temporal-testing</artifactId>
  <version>${temporal.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.awaitility</groupId>
  <artifactId>awaitility</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.wiremock</groupId>
  <artifactId>wiremock-standalone</artifactId>
  <version>3.9.1</version>
  <scope>test</scope>
</dependency>
```

### Maven Plugin

建议加入：

- `maven-surefire-plugin`
- `maven-failsafe-plugin`
- `jacoco-maven-plugin`

---

## 10.2 前端 npm

建议后续加入：

```bash
npm install -D vitest @testing-library/react @testing-library/user-event jsdom msw playwright
```

如果继续保持 Next.js 原生风格，也可以使用 Jest，但对当前仓库更推荐 Vitest。

---

## 11. 测试执行约定

## 11.1 本地执行

### 后端

```bash
cd backend
mvn test
```

### 前端

```bash
cd frontend
npm test
```

### E2E

```bash
cd frontend
npx playwright test
```

### 性能

```bash
k6 run tests/performance/ticket-create.js
```

---

## 11.2 CI 分层执行

### PR 必跑

- 后端单元测试
- 后端切片测试
- 前端单元 / 组件测试
- lint
- type check
- OpenAPI schema smoke

### merge/main 必跑

- PR 全部内容
- 后端集成测试
- Flyway migration 测试
- Temporal workflow 测试
- Playwright smoke
- JaCoCo 覆盖率

### nightly 必跑

- 全量 E2E
- k6 压测
- OWASP ZAP 基础扫描
- Trivy 镜像与依赖扫描

---

## 12. 质量门禁建议

MVP 阶段建议使用“务实但不放水”的标准。

### 后端

- 新增或修改类必须有测试
- 核心领域模块覆盖率不低于 70%
- `ticket`、`approval`、`workflow` 模块覆盖率目标 80%
- 不允许新增 blocker / critical 静态扫描问题

### 前端

- 关键页面至少有 smoke 级别组件测试
- 登录、工单详情、审批页必须有 E2E 场景

### API

- OpenAPI 生成成功
- 至少 1 轮 schema smoke 测试通过

---

## 13. 各模块最低测试要求

## 13.1 auth

- 登录成功 / 失败
- token 解析
- 角色权限校验
- 未登录访问受保护接口返回 401
- 越权访问返回 403

## 13.2 ticket

- 创建工单
- 状态流转合法 / 非法路径
- 评论追加
- 指派
- 列表筛选 / 分页 / 排序
- 时间线事件记录

## 13.3 knowledge

- 文档解析
- 文本切分
- embedding 调用 mock
- citation 存储
- 检索过滤
- 权限范围过滤

## 13.4 ai

- 分类输出结构
- 抽取输出结构
- 检索结果整合
- 建议回复结构
- fallback 路径
- AI 运行日志写入

## 13.5 approval / workflow

- 审批发起
- 审批通过
- 审批驳回
- 重复回调幂等
- workflow 挂起 / 恢复
- 状态同步到工单

## 13.6 observability

- metrics endpoint 可访问
- traceId 注入
- dashboard 聚合接口正确

---

## 14. Codex 编码时的测试准则

本节是给 Codex / agent 直接执行的。

### 14.1 修改后端代码时

如果改动涉及：

- 纯逻辑：至少补单元测试
- Controller / DTO / 错误码：至少补 WebMvc 测试
- Repository / SQL / migration：至少补 JPA 或集成测试
- Workflow / MQ / 异步：至少补异步或 workflow 测试

### 14.2 修改前端代码时

如果改动涉及：

- 组件渲染：至少补组件测试
- 用户交互：至少补 user-event 测试
- 关键页面流程：至少补 Playwright smoke

### 14.3 修改 OpenAPI / DTO 时

必须同时更新：

- schema 测试
- 接口回归测试
- 前端类型或调用层断言

### 14.4 修改配置与基础设施时

至少验证：

- 本地可启动
- 健康检查通过
- 关键依赖可连接
- Prometheus / Jaeger / Grafana 链路不被破坏

---

## 15. 推荐实施顺序

考虑当前项目已基本完成 MVP，建议按以下顺序补测试体系。

### Phase 1：后端基础测试

- JUnit 5 规范化
- Mockito
- JaCoCo
- `ticket` / `auth` / `approval` 单元测试
- WebMvc 接口测试

### Phase 2：真实依赖集成测试

- Testcontainers
- PostgreSQL / Redis / RabbitMQ 集成测试
- Flyway migration 测试
- Temporal 测试

### Phase 3：前端与 API

- Vitest + Testing Library
- MSW
- Playwright smoke
- Schemathesis

### Phase 4：非功能

- k6
- ZAP
- Trivy
- SonarQube

---

## 16. 当前仓库的现实建议

基于当前仓库状态，建议优先做以下增量，而不是一次性铺满所有工具：

1. 后端补 `Testcontainers + JaCoCo + Awaitility + temporal-testing`
2. 前端补 `Vitest + Testing Library + MSW + Playwright`
3. 新增 `application-test.yml`
4. 新增 GitHub Actions 或本地脚本执行：
   - backend unit
   - backend integration
   - frontend unit
   - frontend e2e
5. 引入 `Schemathesis` 对 `/api-docs` 做 schema smoke

这是最适合当前 MVP 阶段、收益最高的一组投入。

---

## 17. 结论

本项目推荐的测试体系不是单工具，而是分层组合：

- 后端：`JUnit 5 + Spring Boot Test + Mockito + Testcontainers + Temporal Testing + JaCoCo`
- API：`REST Assured + Schemathesis`
- 前端：`Vitest + React Testing Library + MSW + Playwright`
- 性能：`k6`
- 安全：`OWASP ZAP + Trivy`
- 质量门禁：`SonarQube`

对 Codex 来说，最重要的不是记住所有工具名，而是遵守这条原则：

**每次改动都必须补与改动层级匹配的测试，并且优先使用最靠近问题边界的测试手段。**


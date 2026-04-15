# 企业级 AI 工单编排系统（MVP）

本仓库当前提供的是 MVP 的基础工程骨架，目标是给后续 thread 提供稳定、可运行、低冲突的开发底座，而不是提前实现业务逻辑。

详细需求、模块边界和 thread 协作规则见 [enterprise_ai_ticketing_mvp_dev_doc.md](./enterprise_ai_ticketing_mvp_dev_doc.md)。

## 当前目录

```text
backend/    Spring Boot 3.x 后端骨架
frontend/   前端占位目录（Thread 7 / 前端 thread 接手）
infra/      OTel / Prometheus / Grafana / Temporal 等本地依赖配置
```

## 已完成的基础设施

- 单仓目录初始化
- Spring Boot 3.x + Java 17 后端骨架
- 统一 API 返回结构 `Result<T>`
- 统一错误码与全局异常处理
- OpenAPI / Swagger UI
- Flyway migration 基础接入
- Docker Compose 本地依赖环境
- PostgreSQL / Redis / Qdrant / RabbitMQ / Temporal / OTel / Jaeger / Prometheus / Grafana 配置
- 为 `auth`、`ticket`、`knowledge`、`ai`、`workflow`、`approval`、`observability` 预留 module package

## 本地启动

### 1. 准备环境变量

```bash
cp .env.example .env
```

默认端口已避开前端常用的 `3000`，Grafana 默认使用 `3001`。

### 2. 启动本地依赖

```bash
docker compose up -d
```

启动后可访问：

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator Health: `http://localhost:8080/actuator/health`
- RabbitMQ: `http://localhost:15672`
- Temporal UI: `http://localhost:8088`
- Jaeger: `http://localhost:16686`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001`
- Qdrant: `http://localhost:6333/dashboard`

### 3. 启动后端

推荐任选一种方式。

方式 A：本地安装 Maven 3.9+

```bash
cd backend
mvn spring-boot:run
```

方式 B：使用 Docker 里的 Maven

```bash
docker run --rm -it \
  --env-file .env \
  -p 8080:8080 \
  -v "$(pwd)/backend":/workspace \
  -w /workspace \
  maven:3.9.9-eclipse-temurin-17 \
  mvn spring-boot:run
```

### 4. 首次验证

- 打开 `http://localhost:8080/api/platform/info`
- 打开 `http://localhost:8080/swagger-ui/index.html`
- 打开 `http://localhost:8080/actuator/prometheus`
- 在 Jaeger 中查看 `enterprise-ai-ticketing` 服务

## 关键约定

### 包结构

后端基础包固定为 `com.enterprise.ticketing`，后续 thread 在此基础上扩展：

```text
config/
common/
auth/
ticket/
knowledge/
ai/
workflow/
approval/
observability/
```

### API 约定

- 所有业务接口统一返回 `Result<T>`
- 所有异常统一走 `GlobalExceptionHandler`
- 错误码统一定义在 `common/error/ErrorCode.java`
- 新增控制器优先挂在 `/api/**`

### 配置约定

- 默认 profile：`dev`
- 业务配置统一挂在 `app.*`
- 基础设施配置统一通过环境变量覆盖
- 生产和本地差异通过 profile 处理，不要把业务常量硬编码在代码里

### 数据库约定

- 表结构统一通过 Flyway 管理
- 当前仅提供 baseline migration，业务表由对应 thread 增量添加
- 不要手工改线上或共享环境表结构

## 其他 thread 如何继续开发

### Thread 2：认证与权限

- 在 `auth/` 中补充 Spring Security、JWT、RBAC
- 保持 `Result<T>`、错误码和 `app.*` 配置结构不变
- 若替换当前放行型安全配置，请只改 `config/SecurityBaselineConfig.java` 或增加更明确的安全配置类

### Thread 3：工单核心

- 在 `ticket/` 中实现领域模型、状态机、事件日志
- 通过 Flyway 新增 `tickets`、`ticket_events` 等表
- 不要改动公共异常与响应包装约定

### Thread 4：知识库与检索

- 在 `knowledge/` 中接入文档处理与 Qdrant 客户端
- 优先复用 `app.qdrant.*` 配置
- 检索结果对外返回时继续使用 `Result<T>`

### Thread 5：AI 编排

- 在 `ai/` 中定义 schema 化输出和编排服务
- 不直接修改工单状态，只输出结构化结论
- AI 相关耗时、token、模型名应接入 `observability/` 约定

### Thread 6：审批与可观测性

- 在 `workflow/`、`approval/`、`observability/` 中补充 Temporal workflow、审批恢复和链路打点
- Temporal / OTel / Prometheus / Grafana 的本地依赖已经就位，可直接接入

## 说明

- 当前骨架不包含认证、工单 CRUD、知识检索、AI workflow、审批逻辑和前端页面实现
- 这些内容由各自 thread 在既定边界内继续开发


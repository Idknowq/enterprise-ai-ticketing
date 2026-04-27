# 运行与运维文档

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是本地启动、环境变量、依赖服务、观测和排障的事实源。  
Related Docs: [README](../README.md), [Architecture](ARCHITECTURE.md), [Testing](TESTING.md), [Security](SECURITY.md)

## 适用范围

- 指导本地开发、联调、演示和基础排障。
- 记录 Docker Compose 依赖服务、后端和前端启动方式、关键环境变量。
- 说明可观测性入口和常见问题。

## 非目标

- 不定义生产级高可用部署。
- 不维护完整 Kubernetes 或云平台方案。
- 不替代安全策略；敏感配置要求见 [Security](SECURITY.md)。

## 本地依赖

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Java | Runtime | 是 | 17 | 后端运行和测试 | 影响 `mvn test` |
| Maven | Tool | 是 | 系统安装 | 后端构建 | 影响后端启动 |
| Node.js | Runtime | 是 | 适配 Next.js 15 | 前端运行和测试 | 影响前端 |
| npm | Tool | 是 | 随 Node | 前端依赖和脚本 | 影响前端 |
| Docker | Tool | 是 | Docker Compose | 本地依赖服务 | 影响联调 |

## Docker Compose

工作目录：仓库根目录。

```bash
docker compose up -d
```

```bash
docker compose ps
```

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `postgres` | Service | 是 | `5432` | 业务 PostgreSQL | 影响后端启动 |
| `redis` | Service | 是 | `6379` | Redis | 影响缓存依赖 |
| `rabbitmq` | Service | 是 | `5672` / `15672` | RabbitMQ 和管理 UI | 影响异步依赖 |
| `qdrant` | Service | 是 | `6333` / `6334` | 向量库 | 影响知识检索 |
| `temporal` | Service | 是 | `7233` | workflow service | 影响审批 |
| `temporal-ui` | Service | 否 | `8088` | Temporal UI | 影响排障 |
| `jaeger` | Service | 否 | `16686` | Trace UI | 影响观测 |
| `otel-collector` | Service | 否 | `4317` / `4318` | OTel collector | 影响 trace |
| `prometheus` | Service | 否 | `9090` | 指标采集 | 影响 dashboard |
| `grafana` | Service | 否 | compose 配置 | 可视化看板 | 影响演示 |

## 后端启动

工作目录：`backend`。

```bash
mvn spring-boot:run
```

验证：

```bash
curl -s http://127.0.0.1:8080/api/platform/info
```

```bash
curl -s http://127.0.0.1:8080/api-docs
```

## 前端启动

工作目录：`frontend`。

```bash
npm install
npm run dev
```

默认访问 `http://localhost:3000`。前端默认通过 `.env.local` 使用：

```bash
NEXT_PUBLIC_API_BASE_URL=/backend-api
BACKEND_API_ORIGIN=http://localhost:8080
```

## 环境变量

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `APP_PORT` | Integer | 否 | `8080` | 后端端口 | 影响 README 和 smoke |
| `APP_DB_HOST` | String | 否 | `127.0.0.1` | PostgreSQL host | 影响后端启动 |
| `APP_DB_PORT` | Integer | 否 | `5432` | PostgreSQL port | 影响 compose |
| `APP_DB_NAME` | String | 否 | `ticketing` | 业务库名 | 影响 Flyway |
| `APP_DB_USERNAME` | String | 否 | `ticketing` | 数据库用户 | 敏感配置 |
| `APP_DB_PASSWORD` | Secret | 否 | `ticketing` | 数据库密码 | 不得生产复用默认值 |
| `APP_REDIS_HOST` | String | 否 | `127.0.0.1` | Redis host | 影响缓存 |
| `APP_RABBITMQ_HOST` | String | 否 | `127.0.0.1` | RabbitMQ host | 影响异步依赖 |
| `APP_AUTH_JWT_SECRET` | Secret | 是 | dev secret | JWT 签名密钥 | 生产必须替换 |
| `APP_AUTH_JWT_ACCESS_TOKEN_TTL` | Duration | 否 | `8h` | 访问 token 有效期 | 影响认证 |
| `APP_AI_ENABLED` | Boolean | 否 | `true` | AI 模块开关 | 影响演示 |
| `APP_AI_PROVIDER_TYPE` | String | 否 | `rule-based` | AI provider 类型 | 影响 AI 输出 |
| `APP_AI_PROVIDER_API_KEY` | Secret | 否 | 空 | 外部 LLM key | 不得提交 |
| `APP_QDRANT_HOST` | String | 否 | `127.0.0.1` | Qdrant host | 影响检索 |
| `APP_KNOWLEDGE_EMBEDDING_LOCAL_BASE_URL` | URL | 否 | `http://127.0.0.1:11434` | 本地 embedding 服务 | 影响文档索引 |
| `APP_KNOWLEDGE_EMBEDDING_COMMERCIAL_API_KEY` | Secret | 否 | 空 | 商业 embedding key | 不得提交 |
| `APP_TEMPORAL_HOST` | String | 否 | `127.0.0.1` | Temporal host | 影响 workflow |
| `APP_OTEL_EXPORTER_TRACES_ENDPOINT` | URL | 否 | `http://127.0.0.1:4318/v1/traces` | trace exporter | 影响观测 |
| `NEXT_PUBLIC_API_BASE_URL` | URL Path | 否 | `/backend-api` | 前端浏览器 API base | 影响前端 |
| `BACKEND_API_ORIGIN` | URL | 否 | `http://localhost:8080` | Next.js 代理目标 | 影响前端联调 |

## 可观测性入口

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Actuator health | URL | 是 | `/actuator/health` | 后端健康检查 | 影响部署 |
| Actuator prometheus | URL | 是 | `/actuator/prometheus` | Prometheus scrape | 影响指标 |
| Observability dashboard | URL | 是 | `/api/observability/dashboard` | 业务聚合指标 | 影响前端监控页 |
| Jaeger UI | URL | 否 | `http://localhost:16686` | trace 查询 | 影响排障 |
| Temporal UI | URL | 否 | `http://localhost:8088` | workflow 查询 | 影响审批排障 |

## 常见故障

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 数据库连接失败 | Troubleshooting | 否 | 无 | 检查 `docker compose ps`、`APP_DB_*` 和端口占用 | 影响后端启动 |
| Flyway 失败 | Troubleshooting | 否 | 无 | 检查 migration 顺序和是否编辑旧 migration | 影响数据安全 |
| Qdrant 不可用 | Troubleshooting | 否 | 无 | 检查 `APP_QDRANT_HOST`、端口和容器日志 | 影响知识检索 |
| Temporal 连接失败 | Troubleshooting | 否 | 无 | 检查 `APP_TEMPORAL_*` 和 `temporal-ui` | 影响审批 |
| 前端 401 | Troubleshooting | 否 | 无 | 清理本地 token，重新登录演示账号 | 影响联调 |
| OpenAPI smoke 失败 | Troubleshooting | 否 | 无 | 检查后端是否启动、admin 用户是否可登录、schema 是否和 DTO 一致 | 影响质量门禁 |

## 维护规则

- 新增环境变量、端口、服务或启动步骤时，必须更新本文件。
- 生产化部署前必须替换所有 dev secret 和默认密码。
- 命令必须标明工作目录并保持可复制执行。

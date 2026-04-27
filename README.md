# 企业级 AI 工单编排系统

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件是项目入口；产品、架构、接口、数据和测试事实以 `docs/` 下对应文档为准。  
Related Docs: [PRD](docs/PRD.md), [Architecture](docs/ARCHITECTURE.md), [API Contracts](docs/API_CONTRACTS.md), [Data Model](docs/DATA_MODEL.md), [Modules](docs/MODULES.md), [Operations](docs/OPERATIONS.md), [Testing](docs/TESTING.md), [Doc Writing Guide](docs/DOC_WRITING_GUIDE.md)

企业级 AI 工单编排系统是面向企业 IT 服务台的 MVP：用户提交工单后，系统完成分类、字段抽取、知识检索、AI 处理建议、审批流挂起/恢复、审计和基础可观测性。

## 适用范围

- 面向开发者、评审者和后续协作 thread 的项目总入口。
- 提供项目定位、本地启动、演示账号、常用命令和正式文档索引。
- 深入需求、架构、接口、数据、测试和安全内容只维护在 `docs/`。

## 非目标

- 不在本文件维护完整 PRD、完整 API 列表、完整表结构或模块实现细节。
- 不记录个人 thread 工作日志。
- 不替代 `docs/CHANGELOG.md` 或 git 历史。

## 核心能力

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 工单管理 | Capability | 是 | Enabled | 创建、查询、分配、评论、状态流转 | 影响 `docs/API_CONTRACTS.md`、`docs/DATA_MODEL.md` |
| 认证与权限 | Capability | 是 | Enabled | JWT + RBAC，内置演示用户 | 影响 `docs/SECURITY.md`、`docs/API_CONTRACTS.md` |
| 知识库与检索 | Capability | 是 | Enabled | 文档上传、切分、embedding、Qdrant 检索、引用 | 影响 `docs/MODULES.md`、`docs/OPERATIONS.md` |
| AI 编排 | Capability | 是 | Enabled | 分类、抽取、检索适配、处理建议、审计 | 影响 AI schema、测试和安全文档 |
| 审批工作流 | Capability | 是 | Enabled | 两级审批、幂等回调、Temporal workflow | 影响状态机和接口契约 |
| 可观测性 | Capability | 是 | Enabled | Actuator、Prometheus、OTel、Jaeger、Grafana | 影响运维和测试文档 |

## 技术栈

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Java / Spring Boot | Backend | 是 | Java 17 / Spring Boot 3.3.6 | 后端 Web、Security、JPA、Actuator | 影响后端构建和部署 |
| PostgreSQL / Flyway | Data | 是 | PostgreSQL 16 | 业务库和 schema migration | 影响 `docs/DATA_MODEL.md` |
| Redis / RabbitMQ | Infra | 是 | Docker Compose | 缓存与异步基础设施 | 影响 `docs/OPERATIONS.md` |
| Qdrant | Vector Store | 是 | v1.9.5 | 知识向量检索 | 影响知识模块 |
| Temporal | Workflow | 是 | 1.25.2 | 审批 workflow | 影响审批和运维 |
| Next.js / React / Ant Design | Frontend | 是 | Next.js 15 / React 19 / Ant Design 5 | 控制台 UI | 影响前端测试和构建 |
| Prometheus / Grafana / Jaeger / OTel | Observability | 是 | Docker Compose | 指标、追踪、看板 | 影响可观测性 |

## 快速启动

工作目录：仓库根目录。

```bash
docker compose up -d
```

启动后端：

```bash
cd backend
mvn spring-boot:run
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Frontend | URL | 是 | `http://localhost:3000` | 控制台入口 | 变更时更新前端和运维文档 |
| Backend API | URL | 是 | `http://localhost:8080/api` | 后端 API base path | 影响前端代理和 OpenAPI smoke |
| OpenAPI | URL | 是 | `http://localhost:8080/api-docs` | 代码生成接口事实源 | 影响 `docs/API_CONTRACTS.md` |
| Temporal UI | URL | 否 | `http://localhost:8088` | workflow 查看 | 影响运维文档 |
| Prometheus | URL | 否 | `http://localhost:9090` | 指标查询 | 影响运维文档 |
| Grafana | URL | 否 | `http://localhost:3001` | 看板入口，端口以 compose 为准 | 影响运维文档 |
| Jaeger | URL | 否 | `http://localhost:16686` | trace 查询 | 影响运维文档 |

## 演示账号

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `employee01` | User | 是 | `ChangeMe123!` | 普通员工 | 影响认证测试 |
| `support01` | User | 是 | `ChangeMe123!` | 一线支持人员 | 影响工单处理测试 |
| `approver01` | User | 是 | `ChangeMe123!` | 审批人 | 影响审批测试 |
| `admin01` | User | 是 | `ChangeMe123!` | 管理员 | 影响管理和 OpenAPI smoke |

## 常用命令

工作目录：仓库根目录，除非命令中显式 `cd`。

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run lint
npm run typecheck
npm test
npm run build
npm run test:e2e
```

```bash
./scripts/openapi-smoke.sh
```

## 文档索引

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| [docs/PRD.md](docs/PRD.md) | Product | 是 | Active | 产品目标、范围、验收标准 | 功能变更必须更新 |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture | 是 | Active | 架构、链路、技术决策 | 跨模块设计变更必须更新 |
| [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) | Contract | 是 | Active | API、错误码、跨模块契约 | 接口变更必须更新 |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Data | 是 | Active | 表、枚举、状态机、migration | 数据变更必须更新 |
| [docs/MODULES.md](docs/MODULES.md) | Engineering | 是 | Active | 模块职责和边界 | 模块边界变更必须更新 |
| [docs/OPERATIONS.md](docs/OPERATIONS.md) | Operations | 是 | Active | 启动、配置、排障、监控 | 运行方式变更必须更新 |
| [docs/TESTING.md](docs/TESTING.md) | Quality | 是 | Active | 测试策略和质量门禁 | 测试策略变更必须更新 |
| [docs/SECURITY.md](docs/SECURITY.md) | Security | 是 | Active | 权限、敏感信息、审计、AI 安全 | 安全策略变更必须更新 |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | Collaboration | 是 | Active | 多 thread 协作和 PR 规则 | 协作方式变更必须更新 |
| [docs/DOC_WRITING_GUIDE.md](docs/DOC_WRITING_GUIDE.md) | Documentation | 是 | Active | 文档编写规范和模板 | 文档规范变更必须更新 |
| [docs/ADR](docs/ADR) | Decision Log | 是 | Active | 架构决策记录 | 重大决策必须新增 ADR |
| [docs/archive](docs/archive) | Archive | 否 | Active | 旧 thread 文档和历史记录 | 只读参考，不再作为事实源 |

## 维护规则

- 新增或修改 API、状态机、数据表、权限、环境变量、AI 输出 schema 时，必须同步更新对应 `docs/` 文档。
- 正式文档禁止复制同一份事实；需要引用时写链接。
- 后续不再新增按 thread 编号的模块 README。模块协作内容统一进入 `docs/MODULES.md` 和 `docs/API_CONTRACTS.md`。

# Enterprise AI Ticketing Frontend

企业级 AI 工单编排系统前端控制台，基于 Next.js + TypeScript + Ant Design。

## 本地启动

1. 安装依赖

```bash
cd frontend
npm install
```

2. 配置环境变量

```bash
cp .env.example .env.local
```

默认后端地址为 `http://localhost:8080/api`。
默认通过 Next.js 同源代理转发到 `http://localhost:8080/api`，避免浏览器跨域问题。

3. 启动开发环境

```bash
npm run dev
```

打开 `http://localhost:3000`。

## 演示账号

- `employee01 / ChangeMe123!`
- `support01 / ChangeMe123!`
- `approver01 / ChangeMe123!`
- `admin01 / ChangeMe123!`

## 当前页面

- 登录页
- 工单列表页
- 工单详情页
- 审批页
- 文档管理页
- 基础监控页

## 对接说明

- 已对接：`auth`、`tickets`、`documents`、`retrieval`、`ai/tickets`
- 已对接：`approvals`、`observability`
- 已对接：`GET /api/documents/categories`，工单和文档页面共用后端标准 category code

## Thread 3/4/5/6 兼容说明

- 前端已兼容 thread5 扩展后的 AI DTO，工单详情页会展示 `providerType`、`modelName`、`analysisMode`、`fallbackUsed`、`fallbackReason`、`retrievalStatus` 等摘要字段。
- `requiresApproval=true` 不再等于一定进入审批流。只有在 `needsHumanHandoff=false`、`fallbackUsed=false` 且 `retrievalStatus` 不为 `ERROR/UNAVAILABLE` 时，后端才会自动发起审批。
- 若 AI 触发 `AI_REVIEW_REQUIRED`，详情页会提示“需要人工复核”，审批页不会出现该工单待办。
- 文档管理页不感知 embedding provider 细节。thread4 的“本地 embedding 优先”由后端路由控制，前端仅保持上传与列表能力。
- category 已升级为全局统一 IT 服务类别。前端上传文档、文档筛选、创建工单、工单筛选均使用 `/api/documents/categories` 返回的标准 code，不再提交 `VPN`、`VPN_ISSUE`、`权限申请` 等自由文本。
- 监控页继续直接展示 `/api/observability/dashboard` 的后端聚合指标，本轮未新增更细的 diagnostics 面板。

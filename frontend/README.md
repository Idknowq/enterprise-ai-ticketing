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

# Enterprise AI Ticketing Frontend

Status: Active  
Owner: Frontend Owner  
Last Verified: 2026-04-27  
Source of Truth: 本文件只维护前端本地启动、脚本和目录入口；项目级产品、架构、接口和测试事实以 `../docs/` 为准。  
Related Docs: [Project README](../README.md), [API Contracts](../docs/API_CONTRACTS.md), [Modules](../docs/MODULES.md), [Testing](../docs/TESTING.md)

企业级 AI 工单编排系统前端控制台，基于 Next.js、TypeScript、React 和 Ant Design。

## 适用范围

- 前端开发者本地启动和脚本入口。
- 前端目录结构和页面入口说明。
- 前端与后端 API 对接的最小提示。

## 非目标

- 不维护完整 API 契约；见 `../docs/API_CONTRACTS.md`。
- 不维护产品需求或架构；见 `../docs/PRD.md` 和 `../docs/ARCHITECTURE.md`。
- 不记录 thread 兼容历史。

## 本地启动

工作目录：`frontend`。

```bash
npm install
cp .env.example .env.local
npm run dev
```

默认打开 `http://localhost:3000`。

## 环境变量

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | URL Path | 否 | `/backend-api` | 浏览器侧 API base | 影响服务层 |
| `BACKEND_API_ORIGIN` | URL | 否 | `http://localhost:8080` | Next.js 代理到后端的 origin | 影响本地联调 |

## 脚本

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `npm run dev` | Command | 是 | 无 | 启动开发服务 | 影响本地开发 |
| `npm run lint` | Command | 是 | 无 | ESLint 门禁 | 影响 PR |
| `npm run typecheck` | Command | 是 | 无 | Next typegen + TypeScript | 影响 PR |
| `npm test` | Command | 是 | 无 | Vitest 测试 | 影响 PR |
| `npm run build` | Command | 是 | 无 | 生产构建 | 影响发布 |
| `npm run test:e2e` | Command | 是 | 无 | Playwright smoke | 影响主链路 |

## 当前页面

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| `/login` | Page | 是 | 无 | 登录页 | 影响认证 |
| `/tickets` | Page | 是 | 无 | 工单列表 | 影响工单 |
| `/tickets/[id]` | Page | 是 | 无 | 工单详情 | 影响 AI、审批和引用展示 |
| `/approvals` | Page | 是 | 无 | 审批中心 | 影响 workflow |
| `/documents` | Page | 是 | 无 | 文档管理 | 影响知识库 |
| `/monitoring` | Page | 是 | 无 | 基础监控 | 影响观测 |

## 维护规则

- 前端 API 调用统一放在 `frontend/lib/services`。
- 新增页面必须补充组件测试或 E2E 覆盖理由。
- API 字段变化先更新 `../docs/API_CONTRACTS.md`，再改服务层和页面。

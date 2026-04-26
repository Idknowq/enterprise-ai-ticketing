# 前端控制台模块说明

本文档用于说明当前仓库中 Thread 7 交付的前端控制台模块，包括页面范围、接口对接情况、运行方式，以及其他 thread 如何继续与前端联调。

## 1. 模块目标

本模块负责提供企业级 AI 工单编排系统 MVP 的后台控制台，覆盖：

- 登录页
- 工单列表页
- 工单详情页
- 审批页
- 文档管理页
- 基础监控页
- 统一 API client
- 基础鉴权状态管理
- 页面级 loading / error 处理

本模块不负责：

- 后端业务逻辑实现
- 数据库结构设计
- AI workflow 内部推理
- 审批引擎后端实现

## 2. 当前技术选型

- Next.js 15
- React 19
- TypeScript
- Ant Design 5

前端目录：

- `frontend/`

## 3. 当前页面

### 3.1 登录页

- 路由：`/login`
- 能力：
  - 调用 `POST /api/auth/login`
  - 登录后写入本地 token
  - 自动拉取 `GET /api/auth/me`

### 3.2 工单列表页

- 路由：`/tickets`
- 能力：
  - 工单列表查询
  - 状态 / 优先级 / 分类筛选
  - 新建工单
  - 跳转工单详情
  - 分类筛选与新建工单使用后端标准 category code

对应接口：

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/documents/categories`

### 3.3 工单详情页

- 路由：`/tickets/{id}`
- 能力：
  - 展示基础信息
  - 评论
  - 状态更新
  - 指派处理人
  - 时间线
  - AI 建议与节点执行记录
  - 检索引用
  - 审批记录
  - 展示 AI provider / model / analysis mode / fallback / retrieval status 摘要
  - 识别 `AI_REVIEW_REQUIRED` 时间线事件并提示人工复核

对应接口：

- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/assign`
- `POST /api/tickets/{id}/status`
- `POST /api/ai/tickets/{id}/run`
- `GET /api/ai/tickets/{id}/runs`
- `POST /api/retrieval/search`
- `GET /api/approvals/tickets/{ticketId}`

### 3.4 审批页

- 路由：`/approvals`
- 能力：
  - 查看待审批列表
  - 审批通过
  - 审批驳回
  - 对“为何工单未进入审批流”给出引导文案

对应接口：

- `GET /api/approvals/pending`
- `POST /api/approvals/{id}/approve`
- `POST /api/approvals/{id}/reject`

### 3.5 文档管理页

- 路由：`/documents`
- 能力：
  - 文档列表
  - 上传文档
  - 元数据筛选
  - 展示“embedding 由后端本地优先路由控制”的说明
  - 分类筛选与上传文档使用后端标准 category code

对应接口：

- `GET /api/documents`
- `POST /api/documents/upload`
- `GET /api/documents/categories`

### 3.6 基础监控页

- 路由：`/monitoring`
- 能力：
  - 工单总数
  - 状态分布
  - AI 建议成功率
  - 平均 AI 耗时
  - 平均检索耗时
  - 平均审批等待时长
  - 待审批数
  - workflow 失败/重试次数

对应接口：

- `GET /api/observability/dashboard`

## 4. 关键代码位置

### 页面与布局

- `frontend/app/(auth)/login/page.tsx`
- `frontend/app/(console)/layout.tsx`
- `frontend/app/(console)/tickets/page.tsx`
- `frontend/app/(console)/tickets/[id]/page.tsx`
- `frontend/app/(console)/approvals/page.tsx`
- `frontend/app/(console)/documents/page.tsx`
- `frontend/app/(console)/monitoring/page.tsx`

### 全局状态与 UI 基础

- `frontend/components/app-provider.tsx`
- `frontend/components/console-shell.tsx`
- `frontend/components/page-state.tsx`
- `frontend/components/status-tags.tsx`
- `frontend/app/globals.css`

### API client

- `frontend/lib/http.ts`
- `frontend/lib/services/auth.ts`
- `frontend/lib/services/tickets.ts`
- `frontend/lib/services/ai.ts`
- `frontend/lib/services/documents.ts`
- `frontend/lib/services/approvals.ts`
- `frontend/lib/services/monitoring.ts`

### 类型

- `frontend/types/api.ts`

## 5. 鉴权与权限

当前使用后端 JWT：

1. 登录获取 `accessToken`
2. 保存到 `localStorage`
3. 后续请求统一带 `Authorization: Bearer <token>`
4. 收到 `401` 时清理本地会话并回到登录页

页面权限策略：

- 审批页：`APPROVER` / `ADMIN`
- 文档页：`SUPPORT_AGENT` / `ADMIN`
- 文档上传：仅 `ADMIN`
- 工单状态更新 / 指派：`SUPPORT_AGENT` / `ADMIN`

## 6. 当前实现说明

### 6.1 已改为真实接口

当前前端不再依赖审批或监控 mock / adapter：

- 审批页直接使用真实 approval controller
- 监控页直接使用真实 observability controller

### 6.2 当前保留的 MVP 限制

- 后端暂无用户列表接口，因此工单详情中的“指派处理人”仍使用手工输入 `assigneeId`
- 检索引用仍以实时检索结果或 AI 返回 citations 为主，没有额外 citation 明细接口
- 本轮只做 thread3/4/5/6 的最小兼容展示，不新增 AI diagnostics 详情面板或 provider 时间线

### 6.3 Thread 3 / 4 / 5 / 6 兼容点

- thread4 已将 `category` 升级为全项目统一 IT 服务类别。前端通过 `GET /api/documents/categories` 获取选项，并在工单创建、工单筛选、文档上传、文档筛选中提交标准 code
- thread5 扩展了 AI DTO，前端已同步兼容 `schemaVersion`、`providerType`、`modelName`、`analysisMode`、`fallbackUsed`、`fallbackReason`、`retrievalStatus`、`retrievalDiagnostics`
- thread3 / thread6 调整了自动审批进入规则
  - `requiresApproval=true`
  - 且 `needsHumanHandoff=false`
  - 且 `fallbackUsed=false`
  - 且 `retrievalStatus` 不为 `ERROR / UNAVAILABLE`
  时，后端才会自动发起审批流
- 若不满足上述条件，后端会写入 `AI_REVIEW_REQUIRED` 事件，前端在工单详情中提示人工复核，审批页不会出现该工单
- thread4 的 embedding 模型路由由后端控制，当前为本地优先，前端不提供 provider 选择器，也不再提供自由文本 category 输入

## 7. 本地启动

### 7.1 安装依赖

```bash
cd frontend
npm install
```

### 7.2 配置环境变量

```bash
cp .env.example .env.local
```

默认：

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

### 7.3 启动开发环境

```bash
npm run dev
```

访问：

- `http://localhost:3000/login`

## 8. 验证情况

当前已完成：

- `npm install`
- `npm run build`
- `next start` 本地冒烟
- `/login` 返回 `200`
- `/tickets` 返回 `200`

## 9. 推荐联调路径

推荐使用以下演示路径：

1. 用 `employee01` 登录
2. 创建分类为 `ACCESS_REQUEST` 的权限申请类工单
3. 在工单详情触发 `运行 AI 分析`
4. 若满足自动审批条件，工单进入 `WAITING_APPROVAL`；否则在时间线中显示 `AI_REVIEW_REQUIRED`
5. 用 `approver01` 或 `admin01` 进入审批页处理
6. 回到工单详情查看审批记录与状态变化
7. 进入监控页查看指标变化

## 10. 对其他 thread 的协作建议

### Thread 3 / Ticket

- 若新增用户查询或处理人选择接口，前端可将当前手输 `assigneeId` 替换为下拉选择

### Thread 4 / Knowledge

- 若新增 citation 查询接口，前端可在工单详情页直接展示持久化证据链，而非仅依赖实时检索

### Thread 5 / AI

- 若扩展 AI schema，优先保持 `AiDecisionResult` 向后兼容
- 当前前端已消费分类、优先级、审批判断、人工接管、provider、模型、analysis mode、fallback、retrieval status、建议动作、结构化字段、引用来源、节点明细

### Thread 6 / Approval / Observability

- 当前已直接消费：
  - `GET /api/approvals/pending`
  - `GET /api/approvals/tickets/{ticketId}`
  - `POST /api/approvals/{id}/approve`
  - `POST /api/approvals/{id}/reject`
  - `GET /api/observability/dashboard`
- 若未来 dashboard 新增更多字段，建议以追加字段方式演进，避免破坏现有前端

## 11. 总结

当前 Thread 7 已交付一个可运行、可构建、可联调的企业后台型前端控制台，能完整展示：

- 登录
- 工单主链路
- AI 建议
- 审批处理
- 文档管理
- 基础监控

当前重点是保证主业务闭环清晰、信息结构稳定、与后端契约对齐，而不是做复杂 UI 装饰。

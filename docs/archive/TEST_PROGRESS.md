# MVP 测试推进记录

## 当前结论

- 已阅读 `TESTING_GUIDE_FOR_CODEX.md`，当前仓库更适合按阶段推进测试，而不是一次性铺满全部层级。
- 当前测试目标按 MVP 上线口径收敛：优先覆盖登录、工单、审批、文档、OpenAPI 契约、基础安全/性能 smoke 等上线阻断风险；过细的边界组合、完整 Testcontainers、完整 Temporal SDK 集成和大规模性能压测先放到上线后。
- 当前基线：
  - `backend`: `mvn test` 可通过，但仅有 1 个测试类、3 个测试用例。
  - `frontend`: `npm run build` 可通过。
  - `frontend`: `npm run lint` 会进入 Next.js 的交互式 ESLint 初始化，说明 lint 门禁尚未真正落地。

## 已完成的第一阶段

- 补充后端单元测试：
  - `RoleCheckerTest`
  - `TextChunkerTest`
  - `TicketAccessPolicyTest`
- 补充后端 WebMvc 切片测试：
  - `AuthControllerWebMvcTest`

这批测试覆盖了测试指南中的第一阶段目标：

- 纯逻辑测试
- Web 层参数校验与统一返回结构验证

## 已完成的第二阶段

- 建立前端测试与质量门禁基线：
  - `frontend/eslint.config.mjs`
  - `frontend/vitest.config.ts`
  - `frontend/tests/setup.ts`
- 调整前端脚本：
  - `npm run lint`
  - `npm run typecheck`
  - `npm test`
- 补充首批前端测试：
  - `frontend/tests/components/page-state.test.tsx`
  - `frontend/tests/components/status-tags.test.tsx`
  - `frontend/tests/lib/http.test.ts`
- 为通过 lint，修正了控制台页面里 4 处 `useEffect` 依赖写法

当前前端验证结果：

- `npm run lint` 通过
- `npm run typecheck` 通过
- `npm test` 通过，当前共 10 个测试全部通过
- `npm run build` 通过

## 已完成的第三阶段

- 建立 Playwright E2E smoke 基线：
  - `frontend/playwright.config.ts`
  - `frontend/tests/e2e/smoke.spec.ts`
- 覆盖主链路 smoke：
  - 管理员登录并创建工单
  - 运行 AI 分析并命中“需要审批”分支
  - 进入审批中心并完成审批通过动作
  - 进入审批中心并完成审批驳回动作
  - 管理员上传知识文档并在列表中过滤校验
  - 访问监控页并验证核心指标区块展示
  - 回到工单列表进入详情页，校验审批记录已写入
- 为提高 E2E 稳定性，补充审批页测试钩子：
  - `approval-ticket-{ticketId}`
  - `approve-ticket-{ticketId}`
  - `reject-ticket-{ticketId}`
  - `approval-comment-input`
  - `approval-submit-button`
- 补充文档上传测试夹具：
  - `frontend/tests/fixtures/knowledge-upload.txt`

当前 E2E 验证结果：

- `frontend` 执行 `npm run test:e2e` 通过
- 当前共 5 条 Playwright smoke 全部通过

## 已完成的第四阶段

- 建立 Schemathesis OpenAPI smoke：
  - `scripts/openapi-smoke.sh`
  - 默认读取 `http://127.0.0.1:8080/api-docs`
  - 自动登录 `admin01` 获取 JWT
  - 正向 fuzz 低副作用只读接口：`listTickets`、`currentUser`、`info`、`dashboard`、`listDocuments`、`pending`
  - 生成 JUnit 报告到 `test-results/schemathesis`
- 修复 Schemathesis 暴露的 OpenAPI 契约问题：
  - 为 smoke 覆盖接口补充具体 `Result<T>` wrapper schema，避免 `Result.data` 被错误固定为 `TicketResponse`
  - 标记工单响应里的 `category`、`assignee`、用户 `department` 为可空
  - 标记文档响应里的 `lastIndexedAt` 为可空
- 补充本地测试产物忽略规则：
  - `test-results/`
  - `frontend/test-results/`
  - `frontend/playwright-report/`
  - `schemathesis-report/`

当前 OpenAPI smoke 验证结果：

- `scripts/openapi-smoke.sh` 通过
- Schemathesis 选中 6 个 operation，生成 10 个 case，全部通过

## 已完成的第五阶段

- 补充 `ticket` / `approval` / `workflow` 核心状态流转测试：
  - `TicketStatusTest`
  - `ApprovalCommandServiceImplTest`
  - `ApprovalWorkflowActivitiesImplTest`
- 覆盖重点：
  - 工单状态机合法迁移、非法迁移、重复迁移、空目标和终态行为
  - 审批命令的权限边界、管理员代审、幂等重复审批、终态冲突处理
  - workflow activity 的审批阶段创建、replay 幂等、二级审批恢复事件、审批通过/驳回落地、workflow 归属冲突、缺少审批人错误

当前后端验证结果：

- `backend` 执行 `mvn test` 通过
- 当前共 32 个测试全部通过

## 已完成的第六阶段

- 补齐 `PlatformController`、`TicketController`、`DocumentController` 的 WebMvc 切片测试：
  - `PlatformControllerWebMvcTest`
  - `TicketControllerWebMvcTest`
  - `DocumentControllerWebMvcTest`
- 覆盖重点：
  - 平台基础信息接口的统一 `Result` 包装、profile、apiBasePath 和模块开关输出
  - 工单创建、列表查询、详情、评论、分配、状态更新的 URL 映射、JSON 参数绑定、service 调用和响应结构
  - 工单创建和评论接口的 Bean Validation 错误响应
  - 文档列表查询的过滤参数绑定、分页校验错误响应
  - 文档上传 multipart 表单绑定、必填字段校验和响应结构

当前后端验证结果：

- `backend` 执行 `mvn test` 通过
- 当前共 45 个测试全部通过

## 已完成的第七阶段

- 按 MVP 上线优先级补齐前端高价值组件测试：
  - `login-page.test.tsx`
  - `tickets-page.test.tsx`
  - `approvals-page.test.tsx`
- 引入基于 MSW 的服务层 mock 场景：
  - `msw-services.test.ts`
- 覆盖重点：
  - 登录页使用演示账号登录，并跳转到工单列表
  - 工单列表可加载核心字段、详情链接正确
  - 新建工单提交后调用创建接口，并跳转到详情页
  - 审批页加载待办，并可提交通过/驳回决策
  - 服务层对登录、工单查询、审批通过的 HTTP path、query、body 进行真实 mock 校验
- 调整测试配置：
  - `vitest` 排除 `tests/e2e/**`，避免单测误收集 Playwright 用例
  - 测试 setup 显式清理 DOM，并补充 `ResizeObserver` mock

当前前端验证结果：

- `frontend` 执行 `npm test` 通过
- 当前共 7 个测试文件、18 个测试全部通过
- `npm run lint` 通过
- `npm run typecheck` 通过

## 当前未覆盖但应尽快推进的内容

- 后端：
  - OpenAPI 错误响应 schema 补齐，至少覆盖 400 / 401 / 403 / 404 / 409 / 500 的统一错误 envelope
  - Schemathesis 负向 fuzz smoke，限制在低副作用接口和错误响应契约，不做大规模破坏性 fuzz
  - Flyway migration smoke，可先用轻量启动校验替代完整 Repository + Testcontainers
- 前端：
  - 可选补充：文档上传页的最小组件测试
- 非功能：
  - k6 小流量 smoke，验证核心接口无明显性能退化
  - Trivy 依赖/镜像扫描
  - ZAP baseline scan，仅做被动扫描和高危项检查

## 上线后再推进的深度测试

- Repository + PostgreSQL Testcontainers 的完整数据访问测试
- Temporal SDK TestWorkflowEnvironment 的完整 workflow replay / signal / timer 测试
- 大规模边界值、权限矩阵和并发幂等测试
- k6 分层负载测试和容量评估
- ZAP 深度主动扫描

建议下一阶段优先顺序：

1. 补齐 OpenAPI 错误响应 schema，并开启受控 Schemathesis 负向 fuzz smoke
2. 增加 k6 小流量性能 smoke
3. 增加 Trivy 和 ZAP baseline 安全 smoke
4. 可选补充文档上传页最小组件测试

## 已完成的第八阶段

- 新增 MVP 发布检查脚本：
  - `scripts/release-check.sh`
  - 默认覆盖后端测试、前端测试、lint、typecheck、production build、Docker 依赖启动、OpenAPI smoke、Playwright E2E、发布巡检录屏、k6 小流量 smoke
- 新增前端发布巡检录屏用例：
  - `frontend/playwright.video.config.ts`
  - `frontend/tests/e2e/release-video.spec.ts`
  - 录制登录、工单创建、AI 分析、审批中心、文档管理、基础监控、退出登录等页面跳转和状态变化
- 新增 k6 性能 smoke：
  - `scripts/k6-smoke.js`
  - 覆盖登录、平台信息、工单列表、待审批、监控 dashboard
- 新增发布检查说明：
  - `RELEASE_CHECK.md`

当前 MVP 发布检查结果：

- `./scripts/release-check.sh` 默认 MVP 门禁通过
- 后端：45 个测试全部通过
- 前端：7 个测试文件、18 个测试全部通过
- Playwright E2E smoke：5 个主流程测试全部通过
- 发布巡检录屏：1 个录屏测试通过
- k6 smoke：322 个 checks 全部通过，失败率 0%，p95 响应时间低于 1000ms 阈值
- 录屏输出目录：`frontend/test-results/release-video`

当前决策：

- Trivy 和 ZAP 不作为 MVP 默认阻塞项，已保留为可选强化检查
- Trivy 首次下载漏洞库依赖外部网络，本地运行曾因 `mirror.gcr.io` 超时失败
- ZAP 本地 `127.0.0.1` quick scan 曾出现未真正攻击目标但命令返回成功的日志，因此更适合在 staging URL 上显式运行

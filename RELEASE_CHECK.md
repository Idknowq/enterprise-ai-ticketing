# MVP 发布检查脚本

## 默认检查

```bash
./scripts/release-check.sh
```

默认模式面向 MVP 尽快上线，覆盖当前最关键的发布门禁：

- 后端单元测试、领域测试、WebMvc 测试
- 前端单元测试、组件测试、MSW 服务 mock 测试
- 前端 lint、typecheck、production build
- Docker Compose 启动后端依赖服务
- 后端和前端生产模式启动健康检查
- OpenAPI Schemathesis smoke
- Playwright E2E 主流程 smoke
- Playwright 前端发布巡检录屏
- k6 小流量性能 smoke

录屏文件输出到：

```text
frontend/test-results/release-video
```

## 可选强化检查

Trivy 和 ZAP 对 MVP 上线不是当前阻塞项，默认跳过。原因是这两项更依赖稳定网络、本地缓存或可访问的 staging 地址；本地 `127.0.0.1` 下 ZAP 也可能出现扫描器未真正访问目标但命令返回成功的情况。

需要上线前强化时再显式开启：

```bash
RUN_SECURITY=1 ./scripts/release-check.sh
RUN_ZAP=1 ZAP_TARGET_URL=http://localhost:3000/login ./scripts/release-check.sh
RUN_SECURITY=1 RUN_ZAP=1 ZAP_TARGET_URL=http://localhost:3000/login ./scripts/release-check.sh
```

如果 Trivy 首次运行下载漏洞库超时，可以先跳过，不影响 MVP 功能上线判断；等网络稳定或 CI/CD 环境缓存好漏洞库后再把 `RUN_SECURITY=1` 作为安全门禁。

## 常用开关

```bash
RUN_PERF=0 ./scripts/release-check.sh      # 跳过 k6
RUN_VIDEO=0 ./scripts/release-check.sh     # 跳过录屏
START_STACK=0 ./scripts/release-check.sh   # 不启动 Docker Compose，使用已有服务
KEEP_SERVERS=1 ./scripts/release-check.sh  # 检查结束后保留脚本启动的前后端服务
```

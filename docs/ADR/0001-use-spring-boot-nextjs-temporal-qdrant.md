# ADR 0001: 使用 Spring Boot、Next.js、Temporal 和 Qdrant 构建 MVP

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件记录本项目第一组关键技术选型决策。  
Related Docs: [Architecture](../ARCHITECTURE.md), [Operations](../OPERATIONS.md), [Modules](../MODULES.md)

## 适用范围

- 记录 MVP 当前后端、前端、工作流和向量存储技术选择。
- 为后续替换技术栈或拆分架构提供决策背景。

## 非目标

- 不比较所有可选框架。
- 不作为性能评测报告。
- 不锁死未来技术演进。

## Context

项目需要在 MVP 阶段快速交付一个可运行、可演示、可扩展的企业 IT 服务台系统，覆盖工单、认证、知识检索、AI 编排、审批 workflow 和可观测性。

## Decision

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| Spring Boot | Backend Decision | 是 | 3.3.6 | 企业后端生态成熟，Security/JPA/Actuator/OpenAPI 集成直接 | 替换需重写后端架构 |
| Next.js | Frontend Decision | 是 | 15.x | 支持企业控制台快速开发和 API proxy | 替换需重写前端 |
| Ant Design | UI Decision | 是 | 5.x | 适合后台管理界面 | 替换影响所有页面 |
| Temporal | Workflow Decision | 是 | 1.25.2 | 审批长流程、signal、replay 和幂等语义清晰 | 替换需重写 workflow |
| Qdrant | Vector Store Decision | 是 | 1.9.5 | 独立向量库，便于 RAG 检索演进 | 替换影响知识模块 |
| Flyway | Data Decision | 是 | Enabled | migration 可追踪，适合多人协作 | 替换影响数据库发布 |

## Consequences

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 正向影响 | Consequence | 是 | 无 | 开发速度快，企业后端能力完整，长流程边界清晰 | 支撑 MVP |
| 负向影响 | Consequence | 是 | 无 | 本地依赖服务较多，Temporal 和 Qdrant 增加运维复杂度 | 需要完善 `OPERATIONS.md` |
| 约束 | Consequence | 是 | 无 | 模块化单体内仍需保持 service/API/workflow 契约清晰 | 影响 `MODULES.md` |

## 维护规则

- 替换上述任一核心技术前，必须新增 ADR。
- ADR 一旦 Active，不直接改写历史结论；需要用新 ADR supersede。

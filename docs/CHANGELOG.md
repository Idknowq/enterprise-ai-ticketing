# 变更记录

Status: Active  
Owner: Project Lead  
Last Verified: 2026-04-27  
Source of Truth: 本文件记录面向用户和协作者的重要项目变更，不替代 git log。  
Related Docs: [README](../README.md), [CONTRIBUTING](CONTRIBUTING.md), [Testing](TESTING.md)

## 适用范围

- 记录重要功能、架构、接口、数据、文档和测试门禁变化。
- 帮助后续 thread 快速理解项目演进。
- 作为发布说明的基础材料。

## 非目标

- 不记录每个提交。
- 不记录个人工作日志。
- 不替代 PR 描述、测试报告或 ADR。

## Unreleased

| 名称 | 类型 | 是否必填 | 默认值 | 说明 | 变更影响 |
| --- | --- | --- | --- | --- | --- |
| 文档体系重构 | Documentation | 是 | Active | 新增 `docs/` 正式文档体系，根目录新增 `README.md` | 后续 thread 统一维护 |
| 旧 thread 文档归档 | Documentation | 是 | Active | 旧模块 README、MVP 开发文档和测试推进记录移入 `docs/archive/` | 旧文档不再作为事实源 |
| 文档规范落地 | Documentation | 是 | Active | 新增 `DOC_WRITING_GUIDE.md`，统一元信息头、source-of-truth、表格和跨模块契约格式 | 影响所有文档 PR |

## 维护规则

- 用户可感知功能、接口、数据、运维、测试门禁和文档体系变化应记录在 `Unreleased`。
- 发布版本时，将 `Unreleased` 内容移动到对应版本号和日期下。
- 细碎内部重命名不需要记录，除非影响协作者使用。

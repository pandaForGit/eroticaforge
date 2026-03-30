# EroticaForge 技术文档索引

**更新日期**：2026-03-28  

本目录为仓库内**设计与实现**的权威说明入口；若与代码冲突，以**运行中的 `src` / `dataAnalysisModule` 与 `application.yml`** 为准，并应回写修正本文档体系。

---

## 1. 仓库与模块

| 路径 | 说明 |
|------|------|
| `src/main/java/com/eroticaforge/` | 主后端（Spring Boot）：故事、章节、文档、RAG、生成、StoryState |
| `dataAnalysisModule/` | 独立 Maven 工程：语料清洗、分类、人物卡批处理等（端口与主应用错开） |
| `frontend/` | 前端工程；实施顺序见 [`development/前端开发计划.md`](development/前端开发计划.md) |
| `sql/` | PostgreSQL 初始化与迁移脚本（与部署文档配合使用） |

---

## 2. 按主题阅读

### 架构与目录

- [`architecture/项目目录结构说明.md`](architecture/项目目录结构说明.md) — 实际包结构、关键类、多模块边界  
- [`architecture/项目架构设计.md`](architecture/项目架构设计.md) — 分层原则、RAG 与生成数据流（概念层）  
- [`architecture/核心功能流程图.md`](architecture/核心功能流程图.md) — 流程说明  

### API 与后端实现

- [`api/API 接口定义.md`](api/API 接口定义.md) — REST 路径、请求/响应、**实现状态**、与代码差异说明  
- [`development/后端代码计划文档.md`](development/后端代码计划文档.md) — 分阶段实现顺序与验收  
- [`development/核心 Service 实现指南.md`](development/核心 Service 实现指南.md) — RAG、生成、StoryState 等约定  

### 前端

- [`development/前端开发计划.md`](development/前端开发计划.md) — 技术选型、阶段 0～7、与 REST/SSE 的映射与验收；仓库内 `frontend/` 当前为 **Vite + React + TypeScript + Tailwind + DaisyUI**（见 `frontend/package.json`）  

### 数据、部署与安全

- [`deployment/完整 PostgreSQL 与 pgvector 数据模型.md`](deployment/完整%20PostgreSQL%20与%20pgvector%20数据模型.md)  
- [`deployment/EroticaForge 项目量身定制的向量数据库设计方案.md`](deployment/EroticaForge%20项目量身定制的向量数据库设计方案.md)  
- [`deployment/安装部署指南.md`](deployment/安装部署指南.md)  
- [`security/安全与隐私说明.md`](security/安全与隐私说明.md)  

### 模型、Prompt 与数据分析

- [`development/模型管理与配置.md`](development/模型管理与配置.md)  
- [`development/数据分析与人物卡提取规划.md`](development/数据分析与人物卡提取规划.md)  
- [`product/小说进行分类.md`](product/小说进行分类.md)  
- [`prompts/完整 Prompt 模板.md`](prompts/完整%20Prompt%20模板.md)  

### 产品与规划

- [`product/本地 NSFW 角色扮演小说生成工具 - 产品需求文档 (PRD).md`](product/本地%20NSFW%20角色扮演小说生成工具%20-%20产品需求文档%20(PRD).md)  
- [`planning/EroticaForge 项目后续开发计划.md`](planning/EroticaForge%20项目后续开发计划.md)  
- [`planning/未来路线图与待办事项.md`](planning/未来路线图与待办事项.md)  

### 质量与协作

- [`quality/测试计划与测试用例.md`](quality/测试计划与测试用例.md)  
- [`guides/用户操作手册.md`](guides/用户操作手册.md)  
- `prompts/` 下交接类文档 — 给后续 Agent / 开发者的上下文说明  

---

## 3. 已知文档与代码差异（维护时请同步更新）

- **主应用默认端口**：`8090`（`server.port`，可用 `SERVER_PORT` 覆盖），不是 8080。  
- **文档上传**：当前实现为 **UTF-8 文本（`.txt`）**；PDF 会在接口层拒绝。  
- **多模型链**：`useMultiModel: true` 时接口会抛出「尚未实现」，与阶段 6 计划一致。  
- **专题参考库**：`CorpusJsonlReferenceImporter` 已实现；`erotica.corpus-import.enable=true` 并配置 `jsonl-path` / `corpus-root`（或 `CORPUS_IMPORT_JSONL`、`CORPUS_IMPORT_ROOT`）时，启动会执行一次 `CorpusJsonlImportApplicationRunner`。**尚无 REST** 管理接口；检索侧需 `erotica.rag.include-reference-corpus: true`。  
- **Lorebook**：当前为**内存存储**，重启后丢失（见 `LorebookController` 类注释）。  

---

## 4. 文档维护约定

1. 变更对外行为（路径、端口、字段）时，同步修改 `api/API 接口定义.md` 与本索引「已知差异」小节。  
2. 新增顶层 Markdown 时，在本 `README.md` 中归入合适小节并添加链接。  
3. 数据分析批处理以 `dataAnalysisModule/` 内说明与 `数据分析模块开发计划.md`（模块根目录）为准，主工程文档仅描述接入与 RAG 侧行为。  

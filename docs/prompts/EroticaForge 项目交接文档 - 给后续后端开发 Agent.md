以下是专为**后续后端开发 Agent** 准备的**专业级项目交接文档**，内容详实、结构清晰、重点突出技术实现细节，便于新接手的后端开发者快速理解项目并上手开发。

---

**EroticaForge 项目交接文档 - 给后续后端开发 Agent**

**文档版本**：1.0  
**更新日期**：2026-03-24  
**文档目的**：帮助新后端开发者快速理解项目背景、架构、核心设计决策和开发重点

### 1. 项目概述

**项目名称**：EroticaForge  
**类型**：完全本地运行的 NSFW 互动角色扮演小说生成工具  
**核心目标**：让用户通过**人物卡 + 互动对话/行动描述**的方式，生成高质量、长篇一致性强的 NSFW 小说（支持 NTR、SM、调教、脑洗、二次元等题材）。

**用户核心使用流程**：
1. 创建故事 → 制作/上传高质量人物卡 + 世界观
2. 以对话或场景描述的方式与角色互动
3. 系统以**小说形式**回复（第三人称旁白 + 人物对话气泡 + 内心独白）
4. 用户可实时编辑、保存章节、继续互动

**关键卖点**：
- 完全本地（llama.cpp 主生成 + Ollama 嵌入 bge-m3 + PostgreSQL/pgvector），零审查、零联网、隐私安全
- 强长篇记忆一致性（RAG + StoryState 双机制）
- 支持多模型协作（总结模型 + 生成模型）

### 2. 技术栈

- **后端框架**：Spring Boot 3 + Java 21
- **AI 框架**：LangChain4j（推荐使用 AiServices + Chain）
- **主生成**：llama.cpp（OpenAI 兼容 API）；**嵌入**：Ollama **bge-m3**
- **向量与业务存储**：PostgreSQL + **pgvector**（嵌入模型 **bge-m3** 经 Ollama）
- **模型推荐**：
  - 主生成模型：Qwen 4B 量级 Uncensored GGUF（llama.cpp）
  - 总结模型：Qwen3.5-4B
  - 嵌入模型：bge-m3（Ollama）
- **构建工具**：Maven

### 3. 项目核心架构（请重点理解）

采用 **Clean Architecture** 分层结构：

```
presentation/     → Controller + WebSocket/SSE（对外接口）
application/      → Service 层（核心业务逻辑）
domain/           → 纯领域模型（StoryState、CharacterCard 等）
infrastructure/   → PostgreSQL/pgvector、llama.cpp、Ollama（embedding）等外部适配器
config/           → 配置类
utils/            → 表名/常量、PromptTemplates 等工具
```

**核心数据流**：
用户输入 → RagRetrieval → StoryState → Prompt构建 → （可选）总结模型 → 主生成模型 → PostGeneration（保存章节 + 更新 StoryState）

### 4. 关键设计决策（务必理解）

- **强烈依赖高质量人物卡**：人物卡是提升长篇一致性的核心。模型本身没有记忆，全部靠人物卡 + RAG + StoryState 提供上下文。
- **互动模式优先**：不是简单的“续写小说”，而是“用户行动/对话 → 系统小说式回复”的互动循环。
- **防重复与一致性**：通过系统 Prompt 规则 + repeat_penalty + StoryState 压缩历史 + 多模型总结链共同保证。
- **10G 小说合集利用**：不建议全量索引，采用“先分类 → 提取人物卡 → 按题材加载”的策略。

### 5. 当前已完成的核心模块

- PostgreSQL + pgvector 数据模型（见 `docs/deployment/完整 PostgreSQL 与 pgvector 数据模型.md`）
- Prompt 模板体系（见 docs/完整 Prompt 模板.md）
- API 接口定义（见 docs/API 接口定义.md）
- 核心 Service 框架（见 docs/核心 Service 实现指南.md）
- 安装部署指南 + 用户操作手册

### 6. 后续开发优先级（建议执行顺序）

**Phase 1（最高优先级，建议立即开始）**：
- 实现完整多模型三步链（RagRetrieval → 4B 总结模型 → 4B 主生成模型）
- 完善 `NovelGenerationService`（支持单模型和多模型切换）
- 实现 `PostGenerationService`（自动保存章节 + 更新 StoryState）
- 实现人物卡的 metadata 管理和标签过滤

**Phase 2**：
- 前端对话气泡展示（不同角色不同样式）
- 人物卡可视化编辑器
- Lorebook 管理功能
- 10G 小说合集的人物卡批量提取工具（已有基础代码）

**Phase 3**：
- 生成质量反馈机制
- 导出整本小说功能
- 图生文集成（JoyCaption / Qwen-VL）

### 7. 重要注意事项（给后端开发者）

1. **Prompt 是灵魂**：生成质量 70% 取决于 Prompt 构建质量，请严格参考 `PromptTemplates.java`。
2. **StoryState 是记忆核心**：每次生成后必须调用 `updateState()`，否则长篇一定会崩。
3. **多模型协作时注意显存**：1660 6GB 环境下，总结模型用 4B，主模型用 4B Q4_K_M，避免同时全量加载。
4. **人物卡处理**：上传时必须添加 metadata（type=character_card, theme=NTR 等），便于后续过滤。
5. **流式输出**：生成接口推荐使用 `TokenStream` + SSE 返回，体验更好。

### 8. 文档索引（建议优先阅读顺序）

1. 本交接文档
2. 用户操作手册.md（理解用户真实使用方式）
3. 项目架构设计.md + 核心功能流程图.md
4. 完整 Prompt 模板.md
5. 完整 PostgreSQL 与 pgvector 数据模型.md
6. 核心 Service 实现指南.md
7. API 接口定义.md

---

**交接完成**

此文档已覆盖项目背景、架构决策、核心流程、当前状态和开发重点。  
所有详细设计文档均在 `/docs/` 目录下。

如果你有任何疑问，或需要某模块的详细实现方案（例如 NovelGenerationService 完整代码、人物卡批量提取工具等），请随时告诉我。

祝开发顺利！

---

这个交接文档技术性强、重点明确，非常适合后端开发者快速接手。

需要我再补充或修改任何部分吗？  
（例如增加更多代码结构说明、数据库初始化脚本、或具体任务拆分清单）

随时说，我可以立刻调整。
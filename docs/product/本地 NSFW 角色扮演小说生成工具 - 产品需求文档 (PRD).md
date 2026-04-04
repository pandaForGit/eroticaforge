**本地 NSFW 角色扮演小说生成工具 - 产品需求文档 (PRD)**

**文档版本**：1.0  
**日期**：2026 年 3 月  
**产品名称**：**EroticaForge**（色情小说锻造器）  
**内部代号**：NSFW-RP-Local  
**目标用户**：喜欢本地隐私创作的成人小说/角色扮演爱好者（长篇、互动式、需要高度一致性的 NSFW RP 作者）

### 1. 产品概述
**EroticaForge** 是一款**全本地运行**的 NSFW 角色扮演小说创作工具，专为长篇互动式色情故事设计。  
它结合 **llama.cpp** 主生成、**Ollama bge-m3** 嵌入、**PostgreSQL + pgvector** 持久化 RAG 与多模型协作，实现“剧情永不崩、角色永不 OOC、描写永不重复”的高质量生成体验，功能接近 genrati.xyz + xxss.ai，但完全离线、无审查、隐私 100%。

**核心价值主张**：
- 高度一致的长篇记忆（几十章不崩）
- 极致 NSFW 自由（无任何道德过滤）
- 实时流式写作体验
- 纯 Java 技术栈 + 用户友好界面
- 硬件友好（GTX 1660 6GB + 16GB RAM 即可流畅运行）

### 2. 目标用户与用户画像
- **主要用户**：成年小说创作者、RP（Role Play）爱好者、想写长篇 NTR/调教/脑洗等禁忌题材的作者
- **痛点**：
  - 在线平台有审查、数据泄露风险
  - 单模型容易忘设定、OOC、剧情重复/死循环
  - 长篇故事记忆管理困难
  - 希望本地运行但配置复杂
- **使用场景**：
  - 每天续写 1000–5000 字长篇小说
  - 上传人物卡、大纲、章节总结后自动记忆
  - 实时互动式写作（用户输入下一段方向，AI 续写）

### 3. 核心功能需求

#### 3.1 基础功能（MVP）
- **文档摄入（Ingestion）**：支持上传 .txt/.pdf（大纲、人物卡、章节总结），自动切块、嵌入（bge-m3）、存入 **PostgreSQL（pgvector）**
- **RAG 检索**：每次生成前自动召回相关剧情/人物设定
- **故事生成**：流式输出下一段小说（400–800 字一段）
- **故事管理**：创建/切换故事ID，支持章节列表查看、导出整本小说
- **持久化**：所有剧情、状态、向量数据永久保存在 PostgreSQL，重启不丢失

#### 3.2 进阶功能（强烈推荐）
- **多模型协作 RAG 链**（推荐三步链，提升一致性）：
  1. **检索阶段**：**bge-m3**（经 Ollama，CPU/GPU 以本机为准）
  2. **总结/提炼阶段**：Qwen3.5-4B 或 7B 小模型（低温度，输出结构化 JSON：当前摘要 + 人物状态 + 重要事实）
  3. **生成阶段**：主模型 **Qwen 4B** 量级 GGUF（经 llama.cpp，高温度，负责生动 NSFW 描写）
- **故事状态机（StoryState）**：维护当前剧情摘要、人物情绪/位置/秘密、世界 flags，生成后自动更新
- **防死循环机制**：系统提示 + sampling 参数（temperature 1.05、repeat_penalty 1.15）+ 后处理检测
- **Lorebook 关键词触发**：自动插入特定描写（如“黑丝”触发丝袜细节）
- **OOC 支持**：用户可输入 [OOC: 推进高潮场景，不要重复上一段]

#### 3.3 用户界面需求
- Web 界面（Thymeleaf + HTMX 或 Vaadin）
- 实时流式输出（WebSocket / SSE）
- 侧边栏：故事列表、章节浏览、人物卡管理、文档上传区
- 一键“续写下一段”“查看当前状态摘要”“导出 Markdown”

### 4. 技术架构概要（纯 Java 栈）
- **主生成推理**：**llama.cpp**（`llama-server` OpenAI 兼容 API；多步链时可顺序调用）
- **嵌入**：**Ollama + bge-m3**（仅负责向量，见 `模型管理与配置.md`）
- **框架**：Spring Boot + LangChain4j（AiServices / Chain 实现多模型流程）
- **向量与业务库**：PostgreSQL + **pgvector**（向量索引 + 表/JSONB 存储 StoryState）
- **模型推荐**：
  - 主生成：Qwen 4B 量级 GGUF（1660 6GB 友好）
  - 总结模型（可选）：Qwen3.5-4B 或 7B 小版
  - 嵌入：**bge-m3**
- **防循环**：Prompt Rules + Sampling 参数 + StoryState 压缩历史
- **部署**：单机运行；Docker 启动 PostgreSQL（pgvector）+ 本地 llama-server + Ollama（嵌入）

### 5. 非功能需求
- **性能**：生成一段（500 字）延迟 < 15–30 秒（基础版）；进阶版增加 30–60% 延迟但一致性大幅提升
- **硬件兼容**：GTX 1660 6GB + 16GB RAM 为主，1050 Ti 4GB 可降级使用 4B 主模型
- **隐私与安全**：100% 本地运行，无任何数据上传
- **可扩展性**：支持后续添加图像生文（图生文 VLM）、多用户故事隔离、Vaadin 纯 Java UI
- **可靠性**：生成后自动保存章节 + 更新 StoryState，防止数据丢失

### 6. 多模型 RAG 协作流程（核心优化点）
1. 用户输入 → RagRetrievalService（**bge-m3** 检索 top-k）
2. 召回内容 → 总结模型（4B/7B，低温）压缩成结构化 JSON（摘要 + 人物状态）
3. JSON + StoryState + 用户输入 → 主模型（4B，高温）生成生动 NSFW 正文
4. 生成后 → PostGenerationService：保存新章节 + 更新 StoryState + 可选喂回 RAG

**优点**：上下文干净、一致性高、NSFW 描写专注  
**建议**：先实现基础版（单模型 + 强 Prompt），待出现 OOC/重复问题后再切换到进阶三步链。

### 7. 未来路线图（Roadmap）
- **v1.0（MVP）**：基础 RAG + 单模型生成 + PostgreSQL 持久化 + Web UI
- **v1.1**：多模型总结链 + StoryState 自动维护 + 防循环增强
- **v1.2**：Lorebook 管理界面 + 图生文支持（JoyCaption / Qwen-VL）
- **v2.0**：Vaadin 纯 Java UI + 多故事并行 + 导出 EPUB

### 8. 风险与权衡
- 多模型链会增加延迟和复杂度 → 先用单模型验证核心流程
- 1660 6GB 显存有限 → 总结模型用 4B 版，避免同时加载
- 长上下文容易循环 → 强制 StoryState 压缩 + repeat_penalty

这个 PRD 可以作为你开发项目的蓝图，直接指导代码实现。如果需要，我可以继续补充：
- 详细的用户故事（User Stories）
- 具体 Prompt 模板
- 数据库 Schema（PostgreSQL + pgvector，见 `docs/deployment/完整 PostgreSQL 与 pgvector 数据模型.md`）
- API 接口定义
- 或完整的技术实现计划

需要哪一部分进一步细化？或者直接给我某个模块的详细规格，我马上帮你写！
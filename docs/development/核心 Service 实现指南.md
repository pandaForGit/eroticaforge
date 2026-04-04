以下是直接可以使用的完整文档模板，复制后保存为 `docs/核心 Service 实现指南.md`

---

**核心 Service 实现指南.md**

**文档版本**：1.0  
**更新日期**：2026-03-24  
**项目名称**：EroticaForge - 本地 NSFW 角色扮演小说生成工具

### 1. 目的
本文档详细说明项目中**核心 Service** 的职责、实现逻辑、关键方法和注意事项，便于开发、维护和后续扩展。

### 2. 核心 Service 列表与职责

| Service 类名                        | 主要职责                                   | 是否推荐使用 LangChain4j AiServices |
|-------------------------------------|--------------------------------------------|-------------------------------------|
| RagIngestionService                | 文档上传、切分、嵌入、存入 PostgreSQL（pgvector） | 否                                  |
| RagRetrievalService                | RAG 向量检索 + 结果过滤                   | 否                                  |
| StoryStateService                  | 维护故事状态（摘要、人物状态、世界 flags）| 否                                  |
| NovelGenerationService             | 完整 Prompt 构建 + 调用 Ollama 生成       | 是（推荐）                          |
| PostGenerationService              | 生成后处理：保存章节 + 更新 StoryState    | 否                                  |
| LorebookService                    | 关键词触发管理                             | 否                                  |

### 3. 详细实现指南

#### 3.1 RagIngestionService（文档摄入）

**核心方法**：
- `ingestDocument(String storyId, MultipartFile file, Map metadata)`
- `ingestText(String storyId, String text, Map metadata)`

**实现要点**：
- 使用 `DocumentSplitter.recursive(512, 50)`
- 调用 `EmbeddingModel.embed()` 生成向量
- 使用基于 **pgvector** 的 `EmbeddingStore.addAll()` 持久化（嵌入模型 **bge-m3** 经 Ollama）
- 保存原始文档元数据到 `erotica:story:{storyId}:doc:{docId}`

#### 3.2 RagRetrievalService（RAG 检索）

**核心方法**：
- `String retrieveRelevantContext(String query, StoryState state)`

**实现要点**：
- 调用 `ContentRetriever.retrieve()` 获取 top-k（默认 8-12）
- 按相似度过滤（阈值 ≥ 0.75）
- 格式化为带标签的文本（[回忆1]、[人物卡] 等）
- 如果开启多模型模式，返回原始 chunks 供总结模型使用

#### 3.3 StoryStateService（故事状态管理）

**核心方法**：
- `StoryState getCurrentState(String storyId)`
- `void updateState(String storyId, String generatedText)`
- `void mergeDelta(String storyId, StoryDelta delta)`

**关键逻辑**：
- 从 PostgreSQL 读取 `erotica_story_states`（或等价表）中 `storyId` 对应行
- 使用小模型（Qwen3.5-4B）对新生成内容进行结构化总结
- 合并新旧状态，更新 version
- 持久化回 PostgreSQL

**StoryState POJO 结构**（推荐字段）：
- current_summary
- character_states（Map）
- important_facts（List）
- world_flags（List）
- last_chapter_ending
- version

#### 3.4 NovelGenerationService（生成核心，最重要）

**推荐使用 LangChain4j AiServices 方式**（代码最简洁）

```java
@AiService
public interface NovelGenerationService {

    @SystemMessage("""
        {GLOBAL_SYSTEM_PROMPT}
        当前故事状态：{state}
        RAG 召回内容：{ragContext}
        Lorebook 触发：{loreTriggers}
        """)
    TokenStream generateNextChapter(
        @UserMessage String userPrompt,
        @MemoryId String storyId,
        @V("state") String state,
        @V("ragContext") String ragContext,
        @V("loreTriggers") String loreTriggers
    );
}
```

**手动构建 Prompt 方式**（更灵活，推荐用于多模型链）：

```java
public TokenStream generate(String storyId, String userPrompt, boolean useMultiModel) {
    StoryState state = storyStateService.getCurrentState(storyId);
    String ragContext = ragRetrievalService.retrieveRelevantContext(userPrompt, state);
    String loreTriggers = lorebookService.getTriggeredDescriptions(userPrompt + state.getSummary());

    String fullPrompt = buildFullPrompt(state, ragContext, loreTriggers, userPrompt);

    if (useMultiModel) {
        // 三步链：先总结 → 再生成
        String summaryJson = summarizerModel.generate(buildSummarizerPrompt(ragContext));
        fullPrompt = buildGenerationPromptWithSummary(state, summaryJson, userPrompt);
    }

    return ollamaStreamingChatModel.generate(fullPrompt);
}
```

#### 3.5 PostGenerationService（生成后处理）

**核心方法**：
- `void processGeneratedContent(String storyId, String generatedText, String userPrompt)`

**执行顺序**：
1. 保存新章节到 `erotica_chapters`（或等价表）
2. 更新 StoryState（调用 StoryStateService.updateState）
3. （可选）将新章节内容喂给 RAG 重新索引
4. 返回 chapterId 给前端

### 4. 多模型协作实现流程（进阶版）

1. 用户请求 → RagRetrievalService（**bge-m3** 嵌入）
2. 召回结果 → Summarizer Model（Qwen3.5-4B，低温度）生成结构化 JSON
3. JSON + StoryState + 用户输入 → Main Generation Model（Qwen3.5-9B，高温度）
4. 生成结果 → PostGenerationService（保存 + 更新状态）

**注意**：
- 两个模型建议使用不同 Ollama 实例（不同端口）或顺序调用，避免显存冲突
- 总结模型 temperature 建议设为 0.3~0.5
- 生成模型 temperature 建议设为 1.05~1.12

### 5. 关键工具类与常量

- `DbTableNames.java` 或 JPA 实体：与 `完整 PostgreSQL 与 pgvector 数据模型.md` 一致
- `PromptTemplates.java`：存放所有 Prompt 模板（GLOBAL_SYSTEM_PROMPT、SUMMARIZER_PROMPT 等）
- `AntiLoopRules.java`：防重复规则常量

### 6. 开发建议与最佳实践

1. **优先实现单模型版本**（NovelGenerationService + 完整 Prompt），验证流程通畅后再加入多模型链。
2. **所有 Service 必须注入对应的数据访问组件或 LangChain4j Bean**。
3. **异常处理**：生成失败时记录日志并返回友好提示，不要直接抛出给前端。
4. **日志记录**：重要操作（生成、状态更新、RAG 索引）必须打印 storyId 和耗时。
5. **单元测试**：重点测试 StoryState 合并逻辑和 Prompt 构建结果。

### 7. 后续扩展点

- 支持图像输入（图生文）
- 多用户故事隔离（storyId 前增加 userId）
- 自动章节标题生成
- 生成质量评分与人工反馈循环

---

此文档可直接作为开发手册使用，建议与 `API 接口定义.md`、`完整 Prompt 模板.md`、`完整 PostgreSQL 与 pgvector 数据模型.md` 配合阅读。

---

需要我继续帮你写以下文档吗？
- 用户操作手册.md
- 项目目录结构说明.md
- 测试计划与测试用例.md

或者你有其他想补充的内容，也可以告诉我，我立刻修改或新增！
# API 接口定义

**文档版本**：1.2  
**更新日期**：2026-04-03  
**项目名称**：EroticaForge（本地 NSFW 角色扮演小说生成工具）

本文与 `src/main/java/com/eroticaforge/presentation` 下控制器保持一致；若实现变更，请同步更新本节「实现状态」。

---

## 1. 总览

| 项 | 说明 |
|----|------|
| 风格 | REST JSON；生成支持 **SSE**（`text/event-stream`） |
| Base URL | `http://localhost:{port}/api`，默认 `port=8090`（`server.port` / `SERVER_PORT`） |
| 认证 | 当前版本**无** Token；单机本地使用 |
| WebSocket | **未提供**；流式场景请用 SSE |

---

## 2. 通用响应格式

### 2.1 成功（多数 REST 接口）

HTTP 2xx，体为：

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

`data` 可为对象、数组或 `null`（如删除故事）。

对应类型：`com.eroticaforge.application.dto.api.ApiResponse`。

### 2.2 错误

HTTP 4xx/5xx，体为：

```json
{
  "code": 400,
  "message": "参数错误",
  "error": "具体原因（异常消息或摘要）"
}
```

对应类型：`ApiErrorResponse`。常见：`400` 参数、`404` 资源不存在、`409` 乐观锁冲突、`413` 上传过大、`501` 能力未实现、`502` 上游 LLM/嵌入失败。

### 2.3 健康检查（特例）

`GET /api/health` **不**使用 `ApiResponse` 封装，直接返回扁平 JSON（见 3.9）。

### 2.4 SSE 错误

当请求的 `Accept` **仅**包含 `text/event-stream` 时，部分错误会以 SSE `event: error` 形式写出（见 `RestExceptionHandler`）。

---

## 3. 接口列表

下表「状态」：`已完成` / `部分` / `未实现`。

### 3.1 故事 CRUD

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `POST` | `/api/stories` | 创建故事 |
| 已完成 | `GET` | `/api/stories` | 列表（按更新时间倒序） |
| 已完成 | `GET` | `/api/stories/{storyId}` | 详情 |
| 已完成 | `DELETE` | `/api/stories/{storyId}` | 删除（级联子表） |

**POST `/api/stories` 请求体**

```json
{
  "title": "标题",
  "tags": ["标签1", "标签2"],
  "libraryCharacterIds": ["库卡UUID-1", "库卡UUID-2"]
}
```

`tags` 可省略或 `null`。`libraryCharacterIds` 可选：有序；服务端为每个 ID 在故事下插入**人物快照**（深拷贝库中 payload）；重复 ID 会去重保序。非法 ID 返回 **400**。

**POST 响应**：HTTP **201**，`data` 为：

```json
{
  "storyId": "uuid",
  "title": "标题",
  "createdAt": "2026-03-28T12:00:00Z"
}
```

（`createdAt` 为 ISO-8601 瞬时时间，实际序列化以 Jackson 为准。）

---

### 3.2 人物卡库与故事人物快照

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `GET` | `/api/character-library` | 人物卡库列表/搜索 |
| 已完成 | `GET` | `/api/stories/{storyId}/character-snapshots` | 本故事快照列表（已排序） |
| 已完成 | `POST` | `/api/stories/{storyId}/character-snapshots` | 新增快照（从库克隆或手写 `payload`） |
| 已完成 | `PATCH` | `/api/stories/{storyId}/character-snapshots/{snapshotId}` | 改 `sortOrder` 或整体替换 `payload` |
| 已完成 | `DELETE` | `/api/stories/{storyId}/character-snapshots/{snapshotId}` | 删除快照 |
| 已完成 | `PUT` | `/api/stories/{storyId}/character-snapshots/order` | 重排：body `{"snapshotIds":["…"]}` 须覆盖本故事全部快照 |

**GET `/api/character-library` 查询参数**：`query`（可选，模糊匹配展示名/源路径）、`limit`（默认 200，最大 500）。

**POST 快照请求体示例（从库克隆）**：`{"libraryCharacterId":"uuid"}`。  
**POST 快照请求体示例（手写）**：`{"payload":{"name":"角色","personality":"…"}}`（`payload` 须为对象，可为 `{}`）。

人物卡库数据由 `sql/002_character_library_and_snapshots.sql` 建表后，通过配置 `erotica.character-cards-import.enable=true` 在启动时导入 JSONL，或后续扩展管理端导入。

---

### 3.3 文档上传与列表（RAG）

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `POST` | `/api/stories/{storyId}/documents` | `multipart/form-data` 上传 |
| 已完成 | `GET` | `/api/stories/{storyId}/documents` | 已上传文档列表 |

**POST 表单字段**

| 字段 | 必填 | 说明 |
|------|------|------|
| `file` | 是 | **UTF-8 文本 `.txt`** |
| `metadata` | 否 | JSON **字符串**，合并为业务 metadata |

**限制**：单文件与总请求大小见 `spring.servlet.multipart`（默认约 10MB / 12MB）。**不支持 PDF**（上传 `.pdf` 返回 400）。

**响应 `data` 示例**

```json
{
  "docId": "…",
  "fileName": "人物卡.txt",
  "chunkCount": 12,
  "status": "indexed"
}
```

---

### 3.4 生成（SSE / 同步）

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `POST` | `/api/stories/{storyId}/generate/stream` | SSE 流式 |
| 已完成 | `POST` | `/api/stories/{storyId}/generate` | 阻塞式一次生成 |

**请求体（共用）**

```json
{
  "prompt": "续写指令或正文",
  "maxTokens": 800,
  "useMultiModel": false
}
```

| 字段 | 说明 |
|------|------|
| `prompt` | **必填** |
| `maxTokens` | 可选；当前由服务端模型配置主导，字段为预留 |
| `useMultiModel` | 为 `true` 时返回 **HTTP 501**（多模型链未实现） |

**SSE 事件**（`data:` 后为 JSON 字符串）

- 流式片段：`{"type":"token","content":"..."}`  
- 结束：`{"type":"done","done":true,"chapterId":"..."}`  
- 模型/后处理错误：`{"error":"..."}`  

流结束后服务端会落库章节并更新 StoryState（见 `PostGenerationService`）。

**同步响应 `data`**

```json
{
  "text": "完整生成文本",
  "chapterId": "…"
}
```

---

### 3.5 章节

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `GET` | `/api/stories/{storyId}/chapters` | 章节摘要列表 |
| 已完成 | `GET` | `/api/stories/{storyId}/chapters/{chapterId}` | 单章正文 |

---

### 3.6 故事状态（StoryState）

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `GET` | `/api/stories/{storyId}/state` | 当前状态 |
| 已完成 | `PUT` | `/api/stories/{storyId}/state` | 手动覆盖（调试） |

**PUT 请求体**（`null` 字段表示不修改该项；`version` **必填**，与当前不一致时 **409**）

```json
{
  "version": 1,
  "currentSummary": "…",
  "characterStates": { "角色": "状态" },
  "importantFacts": ["事实"],
  "worldFlags": ["标记"],
  "lastChapterEnding": "…"
}
```

---

### 3.7 Lorebook

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 部分 | `GET` | `/api/lorebook` | 列出条目 |
| 部分 | `POST` | `/api/lorebook` | 新增条目 |

**说明**：当前为**内存存储**，进程重启后丢失；持久化与 PRD 完整对齐属后续迭代。

---

### 3.8 专题参考库（JSONL 导入）

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 未实现 | — | — | **无**对外 REST |

业务类 `CorpusJsonlReferenceImporter#importFromJsonl` 已实现。运维入口：配置 **`erotica.corpus-import.enable=true`** 及 `jsonl-path`、`corpus-root`（或环境变量 `CORPUS_IMPORT_JSONL`、`CORPUS_IMPORT_ROOT`），启动时由 `CorpusJsonlImportApplicationRunner` 执行一次导入（示例见 `application-corpus-import.yml`）。仍无对外 REST。检索侧需配置 `erotica.rag.include-reference-corpus: true`（见 `docs/README.md`）。

---

### 3.9 健康检查

| 状态 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 已完成 | `GET` | `/api/health` | DB、llama OpenAI 兼容端、Ollama（嵌入）探测 |

**响应示例**（无 `ApiResponse` 包装）

```json
{
  "status": "ok",
  "database": "up",
  "llm": "up",
  "embedding": "up"
}
```

失败项中对应字段可能为 `down: …` 等字符串说明。

另：Spring Boot Actuator 可按 `management.endpoints.web.exposure.include` 暴露 `health`、`info`（路径通常为 `/actuator/*`，与上表独立）。

---

## 4. 与历史文档/前端的差异提示

- 默认端口 **8090**，不是 8080。  
- 上传仅保证 **`.txt`**；勿再宣传 PDF 已支持。  
- `useMultiModel: true` → **501**，直至阶段 6 实现多模型链。  

---

## 5. 相关代码索引

| 文档概念 | 代码位置 |
|----------|----------|
| 故事 CRUD | `StoryCrudController` |
| 文档 | `StoryDocumentController` |
| 生成 | `StoryGenerationController` |
| 章节 | `StoryChapterController` |
| 状态 | `StoryStateController` |
| Lorebook | `LorebookController` |
| 健康 | `HealthController` |
| 错误体 | `RestExceptionHandler`、`ApiErrorResponse` |

---

## 6. 维护说明

新增或变更接口时：更新本节表格与示例，并在 `docs/README.md`「已知差异」中检查是否需同步。

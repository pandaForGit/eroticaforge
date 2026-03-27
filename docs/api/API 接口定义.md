以下是直接可以使用的 **API 接口定义.md** 模板，内容完整、结构清晰、适合你的 EroticaForge 项目。

你可以直接复制保存为 `docs/API 接口定义.md`

---

**API 接口定义.md**

**文档版本**：1.0  
**更新日期**：2026-03-24  
**项目名称**：EroticaForge - 本地 NSFW 角色扮演小说生成工具

### 1. 接口总览

本项目采用 **RESTful API + WebSocket/SSE** 混合架构：

- **REST API**：用于文档上传、故事管理、章节查询等非实时操作
- **WebSocket / SSE**：用于小说实时流式生成（推荐使用 SSE，更简单）

**Base URL**：`http://localhost:8080/api`

### 2. 认证方式
当前版本为单机本地工具，**暂不使用 Token 认证**。  
未来多用户版本可增加 JWT 或简单 API Key。

### 3. 核心接口列表

#### 3.1 故事管理接口

**1. 创建新故事**
- **POST** `/api/stories`
```json
Request Body:
{
  "title": "黑丝人妻的堕落调教",
  "tags": ["NTR", "调教", "黑丝"]
}

Response 201:
{
  "storyId": "abc123",
  "title": "黑丝人妻的堕落调教",
  "createdAt": "2026-03-24T10:53:00"
}
```

**2. 获取故事列表**
- **GET** `/api/stories`

**3. 获取单个故事信息**
- **GET** `/api/stories/{storyId}`

**4. 删除故事**
- **DELETE** `/api/stories/{storyId}`

#### 3.2 文档上传与 RAG 管理

**5. 上传文档（大纲、人物卡、章节总结等）**
- **POST** `/api/stories/{storyId}/documents`
- 支持 multipart/form-data（文件上传）
```json
Request:
- file: 文件（.txt 或 .pdf）
- metadata: {"type": "character_card", "characterName": "林晓曼"}

Response 200:
{
  "docId": "doc_001",
  "fileName": "林晓曼人物卡.txt",
  "chunkCount": 12,
  "status": "indexed"
}
```

**6. 查看已上传文档列表**
- **GET** `/api/stories/{storyId}/documents`

#### 3.3 小说生成接口（核心）

**7. 流式生成下一段内容（推荐）**
- **POST** `/api/stories/{storyId}/generate/stream` （SSE）
```json
Request Body:
{
  "prompt": "继续写下一段，推进到女主开始主动求欢",
  "maxTokens": 800,
  "useMultiModel": true          // true = 开启三步链（总结+生成）
}

Response: text/event-stream
data: {"content": "女主颤抖着伸出手……", "type": "token"}
data: {"content": "……", "type": "token"}
data: {"done": true, "chapterId": "chap_016"}
```

**8. 非流式生成（调试用）**
- **POST** `/api/stories/{storyId}/generate`
- 返回完整一段文本

#### 3.4 章节与状态查询

**9. 获取章节列表**
- **GET** `/api/stories/{storyId}/chapters`

**10. 获取单章内容**
- **GET** `/api/stories/{storyId}/chapters/{chapterId}`

**11. 获取当前故事状态**
- **GET** `/api/stories/{storyId}/state`

**12. 手动更新故事状态（调试用）**
- **PUT** `/api/stories/{storyId}/state`

#### 3.5 其他辅助接口

**13. 获取 Lorebook 列表**
- **GET** `/api/lorebook`

**14. 添加/修改 Lorebook 条目**
- **POST** `/api/lorebook`

**15. 系统健康检查**
- **GET** `/api/health`
  返回 llama-server、Ollama（embedding）、PostgreSQL、GPU 使用情况等

### 4. 通用响应格式

**成功响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**错误响应**：
```json
{
  "code": 400,
  "message": "参数错误",
  "error": "storyId cannot be null"
}
```

### 5. WebSocket 接口（备选方案）

如果后续想使用 WebSocket，可增加：
- `/ws/story/{storyId}`  
  客户端连接后，发送 JSON 指令即可触发生成，服务端实时推送 token。

### 6. 注意事项
- 所有生成接口均支持 `useMultiModel` 参数，控制是否开启总结模型 + 生成模型三步链。
- 生成接口默认开启防重复机制（repeat_penalty + anti-loop prompt）。
- 文件上传大小限制建议设为 10MB。
- SSE 流结束时会返回 `done: true` 和本次生成的 `chapterId`。

---

**下一步建议**：
1. 将此文件保存到 `docs/API 接口定义.md`
2. 根据实际开发进度，在每个接口后面补充「实现状态」字段（如：未实现 / 开发中 / 已完成）

需要我继续帮你写下面任意一个文档的完整模板吗？
- 安装部署指南.md
- 核心 Service 实现指南.md
- 用户操作手册.md

直接告诉我你要哪一个，我马上给你写好！
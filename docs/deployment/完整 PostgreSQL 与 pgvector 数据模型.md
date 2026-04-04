以下是为 **EroticaForge** 项目设计的 **PostgreSQL + pgvector 数据模型**（2026 年推荐版本），替代原 Redis Key 方案：关系数据与向量检索统一在 Postgres 中管理，便于备份、查询与扩展。

### 1. 整体设计原则

- **故事隔离**：所有业务表通过 `story_id`（UUID 或业务 ID）关联；RAG 向量行必须带 `story_id` 与 metadata，便于过滤。
- **向量存储**：使用 **pgvector** 扩展，`embedding` 列类型为 `vector(dimension)`；**bge-m3** _dense 向量维度一般为 **1024**（以实际模型与 LangChain4j 输出为准，部署前用一次 `embed` 校验）。
- **结构化数据**：故事元信息、章节、StoryState、文档元数据、Lorebook 均使用标准表 + JSONB 字段（等价于原「Redis JSON」能力）。
- **TTL / 清理**：用表字段 `expires_at` 或定时任务删除旧向量行，替代 Redis TTL。

### 2. 核心表一览

| 表名 | 说明 |
|------|------|
| `erotica_stories` | 故事元信息（标题、标签、统计等） |
| `erotica_chapters` | 章节正文与元数据 |
| `erotica_story_states` | StoryState（当前摘要、人物状态、version 等，JSONB） |
| `erotica_documents` | 上传文档元数据（原始路径或内容摘要） |
| `erotica_rag_chunks` | RAG 向量块：文本片段 + **embedding** + metadata（JSONB） |
| `erotica_chapter_seq` | 可按故事维度的章节序号（也可用序列或 `stories` 内计数器列） |
| `erotica_lorebook` | 全局或按故事的 Lorebook 关键词 → 描写文本 |
| `erotica_character_library` | 人物卡库（JSONL 导入母卡；`UNIQUE(content_sha256, role_index)`） |
| `erotica_story_character_snapshots` | 故事人物快照（可编辑；随故事 `ON DELETE CASCADE`） |

建表脚本：`sql/002_character_library_and_snapshots.sql`（在 `001_init_pgvector.sql` 之后执行）。

### 3. 详细字段示例

#### 3.1 `erotica_stories`

| 列 | 类型 | 说明 |
|----|------|------|
| id | UUID / VARCHAR | 主键，即 `storyId` |
| title | TEXT | 标题 |
| tags | JSONB / TEXT[] | 标签 |
| total_chapters | INT | 章节数 |
| main_model | VARCHAR | 主模型标识（如 llama.cpp 使用的模型名） |
| created_at / updated_at | TIMESTAMPTZ | 时间戳 |

#### 3.2 `erotica_chapters`

| 列 | 类型 | 说明 |
|----|------|------|
| id | UUID / VARCHAR | `chapterId` |
| story_id | FK | 关联故事 |
| seq | INT | 章节序号 |
| title | TEXT | 章标题 |
| content | TEXT | 正文 |
| metadata | JSONB | temperature、tags 等 |

#### 3.3 `erotica_story_states`（对应原 `...:state`）

整行或单列 `payload` JSONB，示例：

```json
{
  "storyId": "abc123",
  "version": 23,
  "current_summary": "...",
  "character_states": {},
  "important_facts": [],
  "world_flags": [],
  "last_chapter_ending": "..."
}
```

更新 StoryState 时使用 `version` 做乐观锁（`WHERE story_id = ? AND version = ?`）。

#### 3.4 `erotica_rag_chunks`（pgvector）

| 列 | 类型 | 说明 |
|----|------|------|
| id | BIGSERIAL | 主键 |
| story_id | VARCHAR | 必填，隔离故事 |
| content | TEXT | 文本片段 |
| embedding | vector(1024) | bge-m3 向量（维度按实际调整） |
| metadata | JSONB | `type`, `theme`, `seq`, `character_names` 等 |
| created_at | TIMESTAMPTZ | 可选 |

创建索引示例：

```sql
CREATE INDEX ON erotica_rag_chunks USING hnsw (embedding vector_cosine_ops);
CREATE INDEX ON erotica_rag_chunks (story_id);
```

### 4. 与原「Redis Key」概念对照

| 原 Redis 概念 | PostgreSQL 等价 |
|---------------|-----------------|
| `erotica:story:{id}` | `erotica_stories` 行 |
| `erotica:story:{id}:chapter:{cid}` | `erotica_chapters` 行 |
| `erotica:story:{id}:state` | `erotica_story_states` |
| `erotica:story:{id}:doc:{docId}` | `erotica_documents` |
| `erotica:novel-rag-index` | `erotica_rag_chunks` + pgvector 索引 |
| `erotica:lorebook:global` | `erotica_lorebook`（`scope='global'`） |

### 5. Java 侧命名建议

- 原 `RedisKeys.java` 可改为 `DbTableNames` / `StoryColumns` 等，或直接使用 JPA 实体 `@Table`。
- 若使用 LangChain4j：**pgvector 存储**需使用对应 `EmbeddingStore` 实现（如 `PgVectorEmbeddingStore` 或项目封装的 JDBC/pgvector 适配器），配置中 `dimension` 与 **bge-m3** 一致。

### 6. 备份与运维

- 使用 `pg_dump` 备份全库；向量与业务数据一次导出。
- 监控表大小与 `erotica_rag_chunks` 行数，按文档《向量数据库设计方案》做章节向量保留策略。

---

**与嵌入模型配置**：嵌入通过 **Ollama** 调用 **bge-m3**（见 `docs/development/模型管理与配置.md`），与主生成 **llama.cpp** 分离部署时，注意在 `application.yml` 中分别配置 chat 与 embedding。

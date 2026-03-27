**以下是为 EroticaForge 项目量身定制的向量数据库设计方案**（完整版，2026 年实用版）。

本方案已改为 **PostgreSQL + pgvector**，嵌入模型为 **bge-m3**（经 Ollama 调用，见 `docs/development/模型管理与配置.md`）。

### 1. 总体设计原则

- **一个主向量表 + 多层 metadata 过滤**（`story_id` + JSONB metadata）
- **故事专属记忆轻量化**（只存总结 + 关键片段）
- **10G 合集采用只读专题库**（按需加载，不污染主索引）
- **严格控制总量**：向量与章节元数据统一在 Postgres 中监控
- **支持动态增删**：增量存储，旧内容可自动/手动清理

### 2. PostgreSQL + pgvector 整体架构

```text
erotica_rag_chunks（及关联表）
├── 当前故事向量（Layer 1+2，metadata 区分）
├── 10G 合集参考库（Layer 3，只读，type=reference）
└── 历史归档（Layer 4，可选）
```

#### 推荐配置（application.yml 片段）

```yaml
langchain4j:
  ollama:
    base-url: http://localhost:11434
    embedding:
      options:
        model: bge-m3

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/erotica
    username: erotica
    password: erotica

# pgvector 列维度须与 bge-m3 输出一致（常见 1024，以实测为准）
```

### 3. 详细分层设计

| 层级 | 内容类型 | 存储策略 | metadata 关键字段 | 是否长期保留 |
|------|----------|----------|-------------------|--------------|
| **Layer 1** | StoryState + 当前剧情摘要 | 表 `erotica_story_states` + 可选向量行 | `storyId`, `type: state`, `version` | 是 |
| **Layer 2** | 当前故事章节 | 表 `erotica_chapters` + 向量仅存总结 chunk | `storyId`, `type: chapter`, `seq`, `theme` | 保留最近若干章向量 |
| **Layer 3** | 10G 合集参考库 | 只读批量导入，metadata `type: reference` | `theme`, `source` | 是（静态） |
| **Layer 4** | 历史旧故事 | 归档表或冷存储 | `storyId`, `status: archived` | 按需 |

### 4. 关键 metadata 设计（必须严格执行）

每个向量 chunk 在 **JSONB** 中须包含：

```json
{
  "storyId": "abc123",
  "type": "chapter / character_card / summary / reference",
  "theme": "NTR / SM / 二次元 / 现代",
  "sub_theme": ["人妻", "调教"],
  "character_names": ["林晓曼"],
  "seq": 15,
  "intensity": 4,
  "timestamp": 1742801234567,
  "ttl_days": 90
}
```

### 5. 新增内容时的存储策略（核心）

每次生成一段内容后，`PostGenerationService` 建议执行：

1. 保存完整章节文本到 **`erotica_chapters`**（用于展示和导出）
2. 用 4B 模型生成**高质量总结**（200–400 字）
3. 将**总结 + 关键片段** 写入 **`erotica_rag_chunks`**（带完整 metadata + embedding）
4. 更新 StoryState

**坚决不做**：把每一次生成的全文都直接扔进向量库。

### 6. 清理与控制机制

- 每个故事只保留最近 **25–30 章** 的向量 chunk（SQL 删除旧行即可）
- 删除故事时 `DELETE` 该 `story_id` 下所有向量与章节
- 10G 参考库：单独 `type=reference`，用户按专题加载

### 7. 预计资源占用（1660S + 16GB RAM）

向量存在磁盘-backed 的 Postgres 中，内存压力主要来自 **shared_buffers** 与 **HNSW 索引**；仍建议控制单故事 chunk 数量。

---

**下一步**：实现 `PostGenerationService`（总结 + 带 metadata 写入 pgvector + 清理逻辑）时，以 `docs/deployment/完整 PostgreSQL 与 pgvector 数据模型.md` 为准。

-- EroticaForge：PostgreSQL + pgvector 初始化脚本
--
-- 说明：应用内 LangChain4j 另使用表 erotica_lc4j_embeddings（见 application.yml / LangChainConfig），
-- 由库在首次启动时自动 CREATE；本脚本中的 erotica_rag_chunks 供后续业务统一或双写策略使用。
-- 在云库 vectordb 上以超级用户或有权限用户执行；若扩展已存在可跳过第一行。
-- bge-m3 稠密向量维度通常为 1024；若你实测维度不同，请全局替换 1024 后重建表/索引。

CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------------
-- 故事
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_stories (
    id              VARCHAR(64) PRIMARY KEY,
    title           TEXT NOT NULL DEFAULT '',
    tags            JSONB DEFAULT '[]'::jsonb,
    total_chapters  INT NOT NULL DEFAULT 0,
    next_chapter_seq INT NOT NULL DEFAULT 0,
    main_model      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- 章节
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_chapters (
    id          VARCHAR(64) PRIMARY KEY,
    story_id    VARCHAR(64) NOT NULL REFERENCES erotica_stories(id) ON DELETE CASCADE,
    seq         INT NOT NULL,
    title       TEXT,
    content     TEXT,
    metadata    JSONB DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (story_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_erotica_chapters_story ON erotica_chapters (story_id);

-- ---------------------------------------------------------------------------
-- StoryState（乐观锁 version）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_story_states (
    story_id    VARCHAR(64) PRIMARY KEY REFERENCES erotica_stories(id) ON DELETE CASCADE,
    payload     JSONB NOT NULL DEFAULT '{}'::jsonb,
    version     INT NOT NULL DEFAULT 1,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- 上传文档元数据
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_documents (
    id          VARCHAR(64) PRIMARY KEY,
    story_id    VARCHAR(64) NOT NULL REFERENCES erotica_stories(id) ON DELETE CASCADE,
    file_name   TEXT,
    metadata    JSONB DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_erotica_documents_story ON erotica_documents (story_id);

-- ---------------------------------------------------------------------------
-- RAG 向量块（pgvector）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_rag_chunks (
    id          BIGSERIAL PRIMARY KEY,
    story_id    VARCHAR(64) NOT NULL REFERENCES erotica_stories(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    embedding   vector(1024) NOT NULL,
    metadata    JSONB DEFAULT '{}'::jsonb,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_erotica_rag_chunks_story ON erotica_rag_chunks (story_id);
CREATE INDEX IF NOT EXISTS idx_erotica_rag_chunks_embedding
    ON erotica_rag_chunks
    USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- Lorebook（全局或按故事）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_lorebook (
    id          BIGSERIAL PRIMARY KEY,
    scope       VARCHAR(32) NOT NULL DEFAULT 'global',
    story_id    VARCHAR(64) REFERENCES erotica_stories(id) ON DELETE CASCADE,
    keyword     TEXT NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_erotica_lorebook_story ON erotica_lorebook (story_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_erotica_lorebook_scope_story_kw
    ON erotica_lorebook (scope, keyword, (COALESCE(story_id, '')));

-- 人物卡库（JSONL 导入母卡）与故事人物快照（生成唯一信源）
-- 在已执行 001_init_pgvector.sql 的库上追加执行。

-- ---------------------------------------------------------------------------
-- 人物卡库（按 content_sha256 + 行内角色下标唯一）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_character_library (
    id                    VARCHAR(64) PRIMARY KEY,
    schema_version        VARCHAR(32)  NOT NULL DEFAULT '1',
    source_relative_path  TEXT         NOT NULL DEFAULT '',
    content_sha256        VARCHAR(64)  NOT NULL,
    role_index            INT          NOT NULL DEFAULT 0,
    display_name          TEXT         NOT NULL DEFAULT '',
    payload               JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_erotica_character_library_sha_role
    ON erotica_character_library (content_sha256, role_index);

CREATE INDEX IF NOT EXISTS idx_erotica_character_library_display_name
    ON erotica_character_library (display_name);

CREATE INDEX IF NOT EXISTS idx_erotica_character_library_source_path
    ON erotica_character_library (source_relative_path);

-- ---------------------------------------------------------------------------
-- 故事人物快照（随故事删除而级联删除）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erotica_story_character_snapshots (
    id                       VARCHAR(64) PRIMARY KEY,
    story_id                 VARCHAR(64) NOT NULL REFERENCES erotica_stories(id) ON DELETE CASCADE,
    sort_order               INT         NOT NULL DEFAULT 0,
    cloned_from_library_id   VARCHAR(64) REFERENCES erotica_character_library(id) ON DELETE SET NULL,
    payload                  JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_erotica_story_character_snapshots_story
    ON erotica_story_character_snapshots (story_id, sort_order);

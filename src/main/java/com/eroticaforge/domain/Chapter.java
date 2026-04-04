package com.eroticaforge.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 章节实体，对应表 {@code erotica_chapters}。
 *
 * @author EroticaForge
 */
public final class Chapter {

    /** 章节主键 ID。 */
    private final String id;

    /** 所属故事 ID。 */
    private final String storyId;

    /** 章节序号（同一故事内唯一、有序）。 */
    private final int seq;

    /** 章节标题。 */
    private final String title;

    /** 章节正文。 */
    private final String content;

    /** 扩展元数据（JSONB，如生成参数摘要等）。 */
    private final Map<String, Object> metadata;

    /** 创建时间。 */
    private final Instant createdAt;

    /**
     * 全字段构造。
     *
     * @param id        章节 ID，不可为 null
     * @param storyId   故事 ID，不可为 null
     * @param seq       章节序号
     * @param title     标题
     * @param content   正文
     * @param metadata  元数据，可为 null（按空 Map 处理）
     * @param createdAt 创建时间
     */
    public Chapter(
            String id,
            String storyId,
            int seq,
            String title,
            String content,
            Map<String, Object> metadata,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.storyId = Objects.requireNonNull(storyId, "storyId 不能为空");
        this.seq = seq;
        this.title = title;
        this.content = content;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.createdAt = createdAt;
    }

    /**
     * @return 章节主键 ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return 所属故事 ID
     */
    public String getStoryId() {
        return storyId;
    }

    /**
     * @return 章节序号
     */
    public int getSeq() {
        return seq;
    }

    /**
     * @return 章节标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 章节正文
     */
    public String getContent() {
        return content;
    }

    /**
     * @return 元数据副本视图
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}

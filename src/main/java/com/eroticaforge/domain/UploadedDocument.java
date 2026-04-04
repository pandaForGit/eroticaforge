package com.eroticaforge.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 上传文档元数据，对应表 {@code erotica_documents}。
 *
 * @author EroticaForge
 */
public final class UploadedDocument {

    /** 文档记录主键 ID。 */
    private final String id;

    /** 所属故事 ID。 */
    private final String storyId;

    /** 原始文件名。 */
    private final String fileName;

    /** 业务元数据（如存储路径、MIME、大小等，JSONB）。 */
    private final Map<String, Object> metadata;

    /** 上传/记录创建时间。 */
    private final Instant createdAt;

    /**
     * 全字段构造。
     *
     * @param id        文档 ID，不可为 null
     * @param storyId   故事 ID，不可为 null
     * @param fileName  文件名，可为 null（按空串处理）
     * @param metadata  元数据，可为 null（按空 Map 处理）
     * @param createdAt 创建时间
     */
    public UploadedDocument(
            String id,
            String storyId,
            String fileName,
            Map<String, Object> metadata,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.storyId = Objects.requireNonNull(storyId, "storyId 不能为空");
        this.fileName = fileName != null ? fileName : "";
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.createdAt = createdAt;
    }

    /**
     * @return 文档 ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return 故事 ID
     */
    public String getStoryId() {
        return storyId;
    }

    /**
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return 元数据
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

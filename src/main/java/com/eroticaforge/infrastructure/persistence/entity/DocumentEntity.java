package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eroticaforge.infrastructure.persistence.mybatis.PostgresJsonbJacksonTypeHandler;
import com.eroticaforge.domain.UploadedDocument;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 表 {@code erotica_documents} 映射。
 *
 * @author EroticaForge
 */
@Data
@TableName(value = "erotica_documents", autoResultMap = true)
public class DocumentEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("story_id")
    private String storyId;
    @TableField("file_name")
    private String fileName;
    @TableField(typeHandler = PostgresJsonbJacksonTypeHandler.class)
    private Map<String, Object> metadata;
    @TableField("created_at")
    private Instant createdAt;

    /**
     * 由领域对象构造持久化实体。
     *
     * @param d 上传文档领域对象
     * @return 与表列对应的实体
     */
    public static DocumentEntity from(UploadedDocument d) {
        DocumentEntity e = new DocumentEntity();
        e.setId(d.getId());
        e.setStoryId(d.getStoryId());
        e.setFileName(d.getFileName());
        e.setMetadata(d.getMetadata());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    /**
     * 转为领域对象。
     *
     * @return 上传文档领域对象
     */
    public UploadedDocument toDomain() {
        return new UploadedDocument(id, storyId, fileName, metadata, createdAt);
    }
}

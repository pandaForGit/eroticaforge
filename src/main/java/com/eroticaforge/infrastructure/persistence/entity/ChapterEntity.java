package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eroticaforge.infrastructure.persistence.mybatis.PostgresJsonbJacksonTypeHandler;
import com.eroticaforge.domain.Chapter;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 表 {@code erotica_chapters} 映射。
 *
 * @author EroticaForge
 */
@Data
@TableName(value = "erotica_chapters", autoResultMap = true)
public class ChapterEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("story_id")
    private String storyId;
    private Integer seq;
    private String title;
    private String content;
    @TableField(typeHandler = PostgresJsonbJacksonTypeHandler.class)
    private Map<String, Object> metadata;
    @TableField("created_at")
    private Instant createdAt;

    /**
     * 由领域对象构造持久化实体。
     *
     * @param c 章节领域对象
     * @return 与表列对应的实体
     */
    public static ChapterEntity from(Chapter c) {
        ChapterEntity e = new ChapterEntity();
        e.setId(c.getId());
        e.setStoryId(c.getStoryId());
        e.setSeq(c.getSeq());
        e.setTitle(c.getTitle());
        e.setContent(c.getContent());
        e.setMetadata(c.getMetadata());
        e.setCreatedAt(c.getCreatedAt());
        return e;
    }

    /**
     * 转为领域对象。
     *
     * @return 章节领域对象
     */
    public Chapter toDomain() {
        return new Chapter(id, storyId, seq, title, content, metadata, createdAt);
    }
}

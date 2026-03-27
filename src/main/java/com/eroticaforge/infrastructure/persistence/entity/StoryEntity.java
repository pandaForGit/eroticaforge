package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eroticaforge.infrastructure.persistence.mybatis.PostgresJsonbJacksonTypeHandler;
import com.eroticaforge.domain.Story;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 表 {@code erotica_stories} 映射。
 *
 * @author EroticaForge
 */
@Data
@TableName(value = "erotica_stories", autoResultMap = true)
public class StoryEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String title;
    @TableField(typeHandler = PostgresJsonbJacksonTypeHandler.class)
    private List<String> tags;
    @TableField("total_chapters")
    private Integer totalChapters;
    @TableField("next_chapter_seq")
    private Integer nextChapterSeq;
    @TableField("main_model")
    private String mainModel;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;

    /**
     * 由领域对象构造持久化实体。
     *
     * @param s 故事领域对象
     * @return 与表列对应的实体
     */
    public static StoryEntity from(Story s) {
        StoryEntity e = new StoryEntity();
        e.setId(s.getId());
        e.setTitle(s.getTitle());
        e.setTags(s.getTags());
        e.setTotalChapters(s.getTotalChapters());
        e.setNextChapterSeq(s.getNextChapterSeq());
        e.setMainModel(s.getMainModel());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    /**
     * 转为领域对象。
     *
     * @return 故事领域对象
     */
    public Story toDomain() {
        return new Story(id, title, tags, totalChapters, nextChapterSeq, mainModel, createdAt, updatedAt);
    }
}

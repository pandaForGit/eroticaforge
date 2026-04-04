package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eroticaforge.infrastructure.persistence.mybatis.PostgresJsonbJacksonTypeHandler;

import java.time.Instant;
import java.util.Map;

/**
 * 表 {@code erotica_story_character_snapshots}：故事内人物快照。
 *
 * @author EroticaForge
 */
@TableName(value = "erotica_story_character_snapshots", autoResultMap = true)
public class StoryCharacterSnapshotEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("story_id")
    private String storyId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("cloned_from_library_id")
    private String clonedFromLibraryId;

    @TableField(typeHandler = PostgresJsonbJacksonTypeHandler.class)
    private Map<String, Object> payload;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getClonedFromLibraryId() {
        return clonedFromLibraryId;
    }

    public void setClonedFromLibraryId(String clonedFromLibraryId) {
        this.clonedFromLibraryId = clonedFromLibraryId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

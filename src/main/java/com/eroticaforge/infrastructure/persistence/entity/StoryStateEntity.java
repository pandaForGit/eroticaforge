package com.eroticaforge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.eroticaforge.domain.StoryState;
import lombok.Data;

import java.time.Instant;

/**
 * 表 {@code erotica_story_states} 映射；带版本条件的更新见 {@link com.eroticaforge.infrastructure.persistence.mapper.StoryStateMapper#updatePayloadVersioned}。
 *
 * @author EroticaForge
 */
@Data
@TableName(value = "erotica_story_states", autoResultMap = true)
public class StoryStateEntity {

    @TableId("story_id")
    private String storyId;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private StoryState.Payload payload;
    private Integer version;
    @TableField("updated_at")
    private Instant updatedAt;

    /**
     * 由领域对象构造持久化实体（用于插入或带版本条件的更新）。
     *
     * @param s 故事状态领域对象
     * @return 与表列对应的实体
     */
    public static StoryStateEntity from(StoryState s) {
        StoryStateEntity e = new StoryStateEntity();
        e.setStoryId(s.getStoryId());
        e.setPayload(s.toPayload());
        e.setVersion(s.getVersion());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    /**
     * 转为领域对象（合并表列与 payload）。
     *
     * @return 故事状态领域对象
     */
    public StoryState toDomain() {
        StoryState.Payload p = payload != null ? payload : new StoryState.Payload();
        return StoryState.fromRow(storyId, version, updatedAt, p);
    }
}

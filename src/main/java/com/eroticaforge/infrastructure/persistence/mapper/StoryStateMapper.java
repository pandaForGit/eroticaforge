package com.eroticaforge.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eroticaforge.infrastructure.persistence.entity.StoryStateEntity;

/**
 * {@code erotica_story_states} Mapper。
 *
 * <p>除下列 XML/自定义方法外，通用 CRUD 的参数与返回值语义同 {@link BaseMapper}。
 *
 * @author EroticaForge
 */
public interface StoryStateMapper extends BaseMapper<StoryStateEntity> {

    /**
     * 插入初始行；主键冲突时不报错（0 行写入）。
     *
     * @param row 待插入行
     * @return 影响行数（冲突时为 0）
     */
    int insertIgnore(StoryStateEntity row);

    /**
     * 仅当 {@code version} 与库中一致时更新 payload，并自增 version、刷新 {@code updated_at}。
     *
     * @param row 含当前期望版本与待写入 payload 的行
     * @return 影响行数（0 表示版本冲突或不存在）
     */
    int updatePayloadVersioned(StoryStateEntity row);
}

package com.eroticaforge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eroticaforge.infrastructure.persistence.entity.StoryCharacterSnapshotEntity;
import com.eroticaforge.infrastructure.persistence.mapper.StoryCharacterSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 故事人物快照持久化。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class StoryCharacterSnapshotRepository {

    private final StoryCharacterSnapshotMapper mapper;

    /**
     * 按故事查询，按 sort_order 升序。
     *
     * @param storyId 故事 ID
     * @return 快照列表
     */
    public List<StoryCharacterSnapshotEntity> listByStoryIdOrdered(String storyId) {
        return mapper.selectList(
                Wrappers.<StoryCharacterSnapshotEntity>lambdaQuery()
                        .eq(StoryCharacterSnapshotEntity::getStoryId, storyId)
                        .orderByAsc(StoryCharacterSnapshotEntity::getSortOrder));
    }

    /**
     * 按主键查询。
     *
     * @param id 快照 ID
     * @return optional
     */
    public Optional<StoryCharacterSnapshotEntity> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /**
     * 插入一行。
     *
     * @param row 实体
     */
    public void insert(StoryCharacterSnapshotEntity row) {
        mapper.insert(row);
    }

    /**
     * 按主键更新（非空字段）。
     *
     * @param row 实体
     */
    public void updateById(StoryCharacterSnapshotEntity row) {
        mapper.updateById(row);
    }

    /**
     * 按主键删除。
     *
     * @param id 快照 ID
     * @return 影响行数
     */
    public int deleteById(String id) {
        return mapper.deleteById(id);
    }

    /**
     * 当前故事下最大 sort_order；无数据时返回 empty。
     *
     * @param storyId 故事 ID
     * @return 最大排序值
     */
    public Optional<Integer> maxSortOrder(String storyId) {
        StoryCharacterSnapshotEntity one =
                mapper.selectOne(
                        Wrappers.<StoryCharacterSnapshotEntity>lambdaQuery()
                                .eq(StoryCharacterSnapshotEntity::getStoryId, storyId)
                                .orderByDesc(StoryCharacterSnapshotEntity::getSortOrder)
                                .last("LIMIT 1"));
        return one == null ? Optional.empty() : Optional.of(one.getSortOrder());
    }

    /**
     * 统计故事下快照条数。
     *
     * @param storyId 故事 ID
     * @return 条数
     */
    public long countByStoryId(String storyId) {
        return mapper.selectCount(
                Wrappers.<StoryCharacterSnapshotEntity>lambdaQuery()
                        .eq(StoryCharacterSnapshotEntity::getStoryId, storyId));
    }

    /**
     * 局部更新排序、payload 与时间戳（仅非 null 字段参与 set）。
     *
     * @param id        快照主键
     * @param sortOrder 新排序，可 null 表示不改
     * @param payload   新 payload，可 null 表示不改
     * @param updatedAt 更新时间
     * @return 影响行数
     */
    public int patch(String id, Integer sortOrder, Map<String, Object> payload, Instant updatedAt) {
        var uw =
                Wrappers.<StoryCharacterSnapshotEntity>lambdaUpdate()
                        .eq(StoryCharacterSnapshotEntity::getId, id)
                        .set(StoryCharacterSnapshotEntity::getUpdatedAt, updatedAt);
        if (sortOrder != null) {
            uw.set(StoryCharacterSnapshotEntity::getSortOrder, sortOrder);
        }
        if (payload != null) {
            uw.set(StoryCharacterSnapshotEntity::getPayload, payload);
        }
        return mapper.update(null, uw);
    }
}

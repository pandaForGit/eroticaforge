package com.eroticaforge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eroticaforge.domain.Story;
import com.eroticaforge.infrastructure.persistence.entity.StoryEntity;
import com.eroticaforge.infrastructure.persistence.mapper.StoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 故事持久化（MyBatis-Plus）。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class StoryRepository {

    /** 故事表 Mapper。 */
    private final StoryMapper storyMapper;

    /**
     * 插入一条故事记录。
     *
     * @param story 领域对象（须含主键与时间戳等由调用方赋值的字段）
     */
    public void insert(Story story) {
        storyMapper.insert(StoryEntity.from(story));
    }

    /**
     * 按主键更新故事记录（全字段覆盖语义以实体为准）。
     *
     * @param story 领域对象
     */
    public void update(Story story) {
        storyMapper.updateById(StoryEntity.from(story));
    }

    /**
     * 按主键查询故事。
     *
     * @param id 故事 ID
     * @return 存在则返回领域对象，否则 empty
     */
    public Optional<Story> findById(String id) {
        StoryEntity row = storyMapper.selectById(id);
        return row == null ? Optional.empty() : Optional.of(row.toDomain());
    }

    /**
     * 查询全部故事，按 {@code updated_at} 降序。
     *
     * @return 列表（无数据时为空列表，非 null）
     */
    public List<Story> findAllOrderByUpdatedAtDesc() {
        return storyMapper
                .selectList(Wrappers.<StoryEntity>lambdaQuery().orderByDesc(StoryEntity::getUpdatedAt))
                .stream()
                .map(StoryEntity::toDomain)
                .toList();
    }

    /**
     * 按主键删除故事。
     *
     * @param id 故事 ID
     */
    public void deleteById(String id) {
        storyMapper.deleteById(id);
    }

    /**
     * 原子自增下一章序号并返回新值。
     *
     * @param storyId 故事 ID
     * @return 新序号；故事不存在或未更新任何行时为 empty
     */
    public Optional<Integer> allocateNextChapterSeq(String storyId) {
        return Optional.ofNullable(storyMapper.allocateNextChapterSeq(storyId));
    }

    /**
     * 将已生成章节总数加一。
     *
     * @param storyId 故事 ID
     */
    public void incrementTotalChapters(String storyId) {
        storyMapper.incrementTotalChapters(storyId);
    }
}

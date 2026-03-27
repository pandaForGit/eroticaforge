package com.eroticaforge.infrastructure.persistence;

import com.eroticaforge.domain.OptimisticLockException;
import com.eroticaforge.domain.StoryState;
import com.eroticaforge.infrastructure.persistence.entity.StoryStateEntity;
import com.eroticaforge.infrastructure.persistence.mapper.StoryStateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * StoryState 持久化（MyBatis-Plus + 少量 XML）。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class StoryStateRepository {

    /** 故事状态表 Mapper。 */
    private final StoryStateMapper storyStateMapper;

    /**
     * 按故事 ID 查询状态行。
     *
     * @param storyId 故事 ID（即表主键 {@code story_id}）
     * @return 存在则返回领域对象，否则 empty
     */
    public Optional<StoryState> findByStoryId(String storyId) {
        StoryStateEntity row = storyStateMapper.selectById(storyId);
        return row == null ? Optional.empty() : Optional.of(row.toDomain());
    }

    /**
     * 插入初始状态；主键已存在时静默跳过（幂等）。
     *
     * @param state 领域状态（通常为 {@link StoryState#empty(String, java.time.Instant)} 的返回值）
     */
    public void insertInitialIfAbsent(StoryState state) {
        storyStateMapper.insertIgnore(StoryStateEntity.from(state));
    }

    /**
     * 乐观锁更新：仅当 {@code state.version} 与库中一致时写入 payload 并自增版本。
     *
     * @param state 当前领域状态（含客户端已知的版本号与待持久化内容）
     * @return 数据库更新后的领域状态（含新 version、updated_at）
     * @throws OptimisticLockException 版本不匹配或记录不存在
     * @throws IllegalStateException   更新成功但随后按主键读取失败（极端并发/删除场景）
     */
    public StoryState updateIfVersionMatches(StoryState state) {
        StoryStateEntity row = StoryStateEntity.from(state);
        int affected = storyStateMapper.updatePayloadVersioned(row);
        if (affected == 0) {
            throw new OptimisticLockException(
                    "StoryState 版本冲突或记录不存在，storyId=" + state.getStoryId());
        }
        StoryStateEntity reloaded = storyStateMapper.selectById(state.getStoryId());
        if (reloaded == null) {
            throw new IllegalStateException("StoryState 更新后读取失败，storyId=" + state.getStoryId());
        }
        return reloaded.toDomain();
    }
}

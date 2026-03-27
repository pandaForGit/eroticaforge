package com.eroticaforge.infrastructure.persistence;

import com.eroticaforge.domain.Story;
import com.eroticaforge.domain.StoryState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 新建故事时同时写入故事与初始 StoryState。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class StoryWriteFacade {

    /** 故事仓储。 */
    private final StoryRepository stories;

    /** 故事状态仓储。 */
    private final StoryStateRepository storyStates;

    /**
     * 在同一事务中插入故事行与初始 {@code erotica_story_states} 行。
     *
     * @param story 已构造好的故事领域对象（须含 {@link Story#getId()}、{@link Story#getUpdatedAt()} 等）
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertStoryWithInitialState(Story story) {
        stories.insert(story);
        storyStates.insertInitialIfAbsent(StoryState.empty(story.getId(), story.getUpdatedAt()));
    }
}

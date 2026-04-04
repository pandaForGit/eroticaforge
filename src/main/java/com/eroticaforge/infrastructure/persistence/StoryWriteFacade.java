package com.eroticaforge.infrastructure.persistence;

import com.eroticaforge.application.service.StoryCharacterSnapshotService;
import com.eroticaforge.domain.Story;
import com.eroticaforge.domain.StoryState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 新建故事时同时写入故事与初始 StoryState，以及可选的人物库克隆快照。
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

    /** 故事人物快照（创建时从库克隆）。 */
    private final StoryCharacterSnapshotService storyCharacterSnapshotService;

    /**
     * 插入故事、初始状态，并按顺序从人物卡库克隆快照（{@code libraryCharacterIds} 可 {@code null} 或空）。
     *
     * @param story                 已构造好的故事领域对象
     * @param libraryCharacterIds   人物卡库 ID 列表，有序；重复 ID 会去重保序
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertStoryWithInitialState(Story story, List<String> libraryCharacterIds) {
        stories.insert(story);
        storyStates.insertInitialIfAbsent(StoryState.empty(story.getId(), story.getUpdatedAt()));
        List<String> deduped = StoryCharacterSnapshotService.dedupeLibraryIdsPreserveOrder(libraryCharacterIds);
        if (!deduped.isEmpty()) {
            storyCharacterSnapshotService.insertSnapshotsFromLibrary(
                    story.getId(), deduped, story.getUpdatedAt());
        }
    }
}

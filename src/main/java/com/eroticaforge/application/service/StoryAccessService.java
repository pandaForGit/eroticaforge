package com.eroticaforge.application.service;

import com.eroticaforge.domain.Story;
import com.eroticaforge.domain.StoryNotFoundException;
import com.eroticaforge.infrastructure.persistence.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 为 REST 等入口提供「故事必须存在」的加载语义。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class StoryAccessService {

    private final StoryRepository storyRepository;

    /**
     * 按主键加载故事，不存在时抛出 {@link StoryNotFoundException}。
     *
     * @param storyId 故事 ID
     * @return 领域对象
     */
    public Story requireStory(String storyId) {
        return storyRepository
                .findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));
    }
}

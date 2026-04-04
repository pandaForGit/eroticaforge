package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.StoryStateResponse;
import com.eroticaforge.application.dto.api.UpdateStoryStateRequest;
import com.eroticaforge.application.service.StoryAccessService;
import com.eroticaforge.application.service.StoryStateService;
import com.eroticaforge.domain.StoryState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * /api/stories/{storyId}/state。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories/{storyId}/state")
public class StoryStateController {

    private final StoryAccessService storyAccessService;
    private final StoryStateService storyStateService;

    /**
     * @param storyAccessService 故事存在性
     * @param storyStateService  状态读写
     */
    public StoryStateController(
            StoryAccessService storyAccessService, StoryStateService storyStateService) {
        this.storyAccessService = storyAccessService;
        this.storyStateService = storyStateService;
    }

    /**
     * 当前故事状态快照。
     *
     * @param storyId 故事 ID
     * @return 状态 DTO
     */
    @GetMapping
    public ApiResponse<StoryStateResponse> get(@PathVariable String storyId) {
        storyAccessService.requireStory(storyId);
        StoryState s = storyStateService.getCurrentState(storyId);
        return ApiResponse.ok(toResponse(s));
    }

    /**
     * 手动覆盖状态（调试）；{@code version} 必须与当前一致。
     *
     * @param storyId 故事 ID
     * @param request 请求体
     * @return 更新后的状态
     */
    @PutMapping
    public ApiResponse<StoryStateResponse> put(
            @PathVariable String storyId, @RequestBody UpdateStoryStateRequest request) {
        storyAccessService.requireStory(storyId);
        StoryState updated = storyStateService.replaceStateFromRest(storyId, request);
        return ApiResponse.ok(toResponse(updated));
    }

    private static StoryStateResponse toResponse(StoryState s) {
        Map<String, String> chars = new LinkedHashMap<>(s.getCharacterStates());
        List<String> facts = new ArrayList<>(s.getImportantFacts());
        List<String> flags = new ArrayList<>(s.getWorldFlags());
        return new StoryStateResponse(
                s.getStoryId(),
                s.getVersion(),
                s.getCurrentSummary(),
                chars,
                facts,
                flags,
                s.getLastChapterEnding(),
                s.getUpdatedAt());
    }
}

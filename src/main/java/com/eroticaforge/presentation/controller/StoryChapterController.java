package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.ChapterDetailDto;
import com.eroticaforge.application.dto.api.ChapterSummaryDto;
import com.eroticaforge.application.service.StoryAccessService;
import com.eroticaforge.domain.Chapter;
import com.eroticaforge.domain.ChapterNotFoundException;
import com.eroticaforge.infrastructure.persistence.ChapterRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * /api/stories/{storyId}/chapters。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories/{storyId}/chapters")
public class StoryChapterController {

    private final StoryAccessService storyAccessService;
    private final ChapterRepository chapterRepository;

    /**
     * @param storyAccessService 故事存在性
     * @param chapterRepository  章节仓储
     */
    public StoryChapterController(
            StoryAccessService storyAccessService, ChapterRepository chapterRepository) {
        this.storyAccessService = storyAccessService;
        this.chapterRepository = chapterRepository;
    }

    /**
     * 章节列表（按序号升序）。
     *
     * @param storyId 故事 ID
     * @return 列表
     */
    @GetMapping
    public ApiResponse<List<ChapterSummaryDto>> list(@PathVariable String storyId) {
        storyAccessService.requireStory(storyId);
        List<ChapterSummaryDto> list =
                chapterRepository.findByStoryIdOrderBySeq(storyId).stream()
                        .map(
                                c ->
                                        new ChapterSummaryDto(
                                                c.getId(),
                                                c.getSeq(),
                                                c.getTitle(),
                                                c.getCreatedAt()))
                        .toList();
        return ApiResponse.ok(list);
    }

    /**
     * 单章全文。
     *
     * @param storyId   故事 ID
     * @param chapterId 章节 ID
     * @return 章节详情
     */
    @GetMapping("/{chapterId}")
    public ApiResponse<ChapterDetailDto> get(
            @PathVariable String storyId, @PathVariable String chapterId) {
        storyAccessService.requireStory(storyId);
        Chapter c =
                chapterRepository
                        .findById(storyId, chapterId)
                        .orElseThrow(() -> new ChapterNotFoundException(storyId, chapterId));
        return ApiResponse.ok(toDetail(c));
    }

    private static ChapterDetailDto toDetail(Chapter c) {
        return new ChapterDetailDto(
                c.getId(),
                c.getSeq(),
                c.getTitle(),
                c.getContent(),
                new LinkedHashMap<>(c.getMetadata()),
                c.getCreatedAt());
    }
}

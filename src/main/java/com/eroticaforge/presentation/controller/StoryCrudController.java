package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.CreateStoryRequest;
import com.eroticaforge.application.dto.api.CreateStoryResponse;
import com.eroticaforge.application.dto.api.StoryDetailDto;
import com.eroticaforge.application.dto.api.StoryListItemDto;
import com.eroticaforge.application.service.StoryAccessService;
import com.eroticaforge.domain.Story;
import com.eroticaforge.infrastructure.persistence.StoryRepository;
import com.eroticaforge.infrastructure.persistence.StoryWriteFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * POST/GET/DELETE /api/stories。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories")
public class StoryCrudController {

    private final StoryRepository storyRepository;
    private final StoryWriteFacade storyWriteFacade;
    private final StoryAccessService storyAccessService;
    private final String defaultMainModel;

    /**
     * @param storyRepository     故事仓储
     * @param storyWriteFacade    新建故事与初始状态
     * @param storyAccessService  存在性校验
     * @param defaultMainModel    写入 {@link Story} 的默认模型名（来自配置）
     */
    public StoryCrudController(
            StoryRepository storyRepository,
            StoryWriteFacade storyWriteFacade,
            StoryAccessService storyAccessService,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String defaultMainModel) {
        this.storyRepository = storyRepository;
        this.storyWriteFacade = storyWriteFacade;
        this.storyAccessService = storyAccessService;
        this.defaultMainModel = defaultMainModel;
    }

    /**
     * 创建故事并写入初始 {@code erotica_story_states}。
     *
     * @param request 标题与标签
     * @return HTTP 201 + data
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateStoryResponse>> create(@RequestBody CreateStoryRequest request) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String title = request != null && StringUtils.hasText(request.title()) ? request.title() : "";
        Story story =
                Story.newDraft(
                        id,
                        title,
                        request != null ? request.tags() : null,
                        defaultMainModel,
                        now);
        storyWriteFacade.insertStoryWithInitialState(story);
        CreateStoryResponse body = new CreateStoryResponse(id, story.getTitle(), story.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(body));
    }

    /**
     * 故事列表，按更新时间倒序。
     *
     * @return data 为列表
     */
    @GetMapping
    public ApiResponse<List<StoryListItemDto>> list() {
        List<StoryListItemDto> list =
                storyRepository.findAllOrderByUpdatedAtDesc().stream()
                        .map(
                                s ->
                                        new StoryListItemDto(
                                                s.getId(),
                                                s.getTitle(),
                                                s.getTags(),
                                                s.getTotalChapters(),
                                                s.getUpdatedAt()))
                        .toList();
        return ApiResponse.ok(list);
    }

    /**
     * 单个故事元数据。
     *
     * @param storyId 故事 ID
     * @return data
     */
    @GetMapping("/{storyId}")
    public ApiResponse<StoryDetailDto> get(@PathVariable String storyId) {
        Story s = storyAccessService.requireStory(storyId);
        StoryDetailDto dto =
                new StoryDetailDto(
                        s.getId(),
                        s.getTitle(),
                        s.getTags(),
                        s.getTotalChapters(),
                        s.getNextChapterSeq(),
                        s.getMainModel(),
                        s.getCreatedAt(),
                        s.getUpdatedAt());
        return ApiResponse.ok(dto);
    }

    /**
     * 删除故事（数据库子表 ON DELETE CASCADE）。
     *
     * @param storyId 故事 ID
     * @return data 为 null
     */
    @DeleteMapping("/{storyId}")
    public ApiResponse<Void> delete(@PathVariable String storyId) {
        storyAccessService.requireStory(storyId);
        storyRepository.deleteById(storyId);
        return ApiResponse.ok(null);
    }
}

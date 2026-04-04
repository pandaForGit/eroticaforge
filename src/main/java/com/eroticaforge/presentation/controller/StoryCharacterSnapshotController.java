package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.CreateCharacterSnapshotRequest;
import com.eroticaforge.application.dto.api.PatchCharacterSnapshotRequest;
import com.eroticaforge.application.dto.api.ReorderCharacterSnapshotsRequest;
import com.eroticaforge.application.dto.api.StoryCharacterSnapshotDto;
import com.eroticaforge.application.service.StoryAccessService;
import com.eroticaforge.application.service.StoryCharacterSnapshotService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 故事人物快照 CRUD 与排序。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories/{storyId}/character-snapshots")
public class StoryCharacterSnapshotController {

    private final StoryAccessService storyAccessService;
    private final StoryCharacterSnapshotService storyCharacterSnapshotService;

    /**
     * @param storyAccessService            故事存在性
     * @param storyCharacterSnapshotService 快照业务
     */
    public StoryCharacterSnapshotController(
            StoryAccessService storyAccessService,
            StoryCharacterSnapshotService storyCharacterSnapshotService) {
        this.storyAccessService = storyAccessService;
        this.storyCharacterSnapshotService = storyCharacterSnapshotService;
    }

    /**
     * 列出本故事全部快照（已排序）。
     *
     * @param storyId 故事 ID
     * @return data
     */
    @GetMapping
    public ApiResponse<List<StoryCharacterSnapshotDto>> list(@PathVariable String storyId) {
        storyAccessService.requireStory(storyId);
        return ApiResponse.ok(storyCharacterSnapshotService.listSnapshots(storyId));
    }

    /**
     * 新增快照（从库克隆或手写 payload）。
     *
     * @param storyId 故事 ID
     * @param body    请求体
     * @return data 为新建快照
     */
    @PostMapping
    public ApiResponse<StoryCharacterSnapshotDto> create(
            @PathVariable String storyId, @RequestBody(required = false) CreateCharacterSnapshotRequest body) {
        storyAccessService.requireStory(storyId);
        return ApiResponse.ok(storyCharacterSnapshotService.create(storyId, body));
    }

    /**
     * 更新快照排序或 payload。
     *
     * @param storyId    故事 ID
     * @param snapshotId 快照 ID
     * @param body       请求体
     * @return data
     */
    @PatchMapping("/{snapshotId}")
    public ApiResponse<StoryCharacterSnapshotDto> patch(
            @PathVariable String storyId,
            @PathVariable String snapshotId,
            @RequestBody(required = false) PatchCharacterSnapshotRequest body) {
        storyAccessService.requireStory(storyId);
        return ApiResponse.ok(storyCharacterSnapshotService.patch(storyId, snapshotId, body));
    }

    /**
     * 删除快照。
     *
     * @param storyId    故事 ID
     * @param snapshotId 快照 ID
     * @return data 为 null
     */
    @DeleteMapping("/{snapshotId}")
    public ApiResponse<Void> delete(@PathVariable String storyId, @PathVariable String snapshotId) {
        storyAccessService.requireStory(storyId);
        storyCharacterSnapshotService.delete(storyId, snapshotId);
        return ApiResponse.ok(null);
    }

    /**
     * 重排本故事全部快照。
     *
     * @param storyId 故事 ID
     * @param body    有序 ID 列表
     * @return data 为 null
     */
    @PutMapping("/order")
    public ApiResponse<Void> reorder(
            @PathVariable String storyId, @RequestBody ReorderCharacterSnapshotsRequest body) {
        storyAccessService.requireStory(storyId);
        if (body == null || body.snapshotIds() == null) {
            throw new IllegalArgumentException("请求体须包含 snapshotIds");
        }
        storyCharacterSnapshotService.reorder(storyId, body.snapshotIds());
        return ApiResponse.ok(null);
    }
}

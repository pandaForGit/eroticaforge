package com.eroticaforge.application.service;

import com.eroticaforge.application.dto.api.CreateCharacterSnapshotRequest;
import com.eroticaforge.application.dto.api.PatchCharacterSnapshotRequest;
import com.eroticaforge.application.dto.api.StoryCharacterSnapshotDto;
import com.eroticaforge.infrastructure.persistence.CharacterLibraryRepository;
import com.eroticaforge.infrastructure.persistence.StoryCharacterSnapshotRepository;
import com.eroticaforge.infrastructure.persistence.entity.CharacterLibraryEntity;
import com.eroticaforge.infrastructure.persistence.entity.StoryCharacterSnapshotEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 故事人物快照：CRUD、排序、Prompt 文本。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class StoryCharacterSnapshotService {

    private final StoryCharacterSnapshotRepository snapshotRepository;
    private final CharacterLibraryRepository characterLibraryRepository;
    private final CharacterProfilePromptFormatter characterProfilePromptFormatter;
    private final ObjectMapper objectMapper;

    /**
     * 列出故事下快照（已排序）。
     *
     * @param storyId 故事 ID
     * @return DTO 列表
     */
    public List<StoryCharacterSnapshotDto> listSnapshots(String storyId) {
        return snapshotRepository.listByStoryIdOrdered(storyId).stream()
                .map(StoryCharacterSnapshotService::toDto)
                .toList();
    }

    /**
     * 供生成链路注入 Prompt 的人物设定块（无快照时为空串）。
     *
     * @param storyId 故事 ID
     * @return 格式化文本
     */
    public String buildProfilesTextForPrompt(String storyId) {
        List<StoryCharacterSnapshotEntity> rows = snapshotRepository.listByStoryIdOrdered(storyId);
        return characterProfilePromptFormatter.format(rows);
    }

    /**
     * 新增快照：从库克隆或手写 payload。
     *
     * @param storyId 故事 ID
     * @param req     请求体
     * @return 新建 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public StoryCharacterSnapshotDto create(String storyId, CreateCharacterSnapshotRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        Instant now = Instant.now();
        int nextOrder = snapshotRepository.maxSortOrder(storyId).map(o -> o + 1).orElse(0);
        StoryCharacterSnapshotEntity row = new StoryCharacterSnapshotEntity();
        row.setId(UUID.randomUUID().toString());
        row.setStoryId(storyId);
        row.setSortOrder(nextOrder);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        if (req != null && StringUtils.hasText(req.libraryCharacterId())) {
            CharacterLibraryEntity lib =
                    characterLibraryRepository
                            .findById(req.libraryCharacterId().strip())
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "人物卡库 ID 不存在: " + req.libraryCharacterId()));
            row.setClonedFromLibraryId(lib.getId());
            row.setPayload(deepCopyPayload(lib.getPayload()));
        } else {
            row.setClonedFromLibraryId(null);
            Map<String, Object> payload = req != null ? req.payload() : null;
            if (payload == null) {
                throw new IllegalArgumentException("手写快照须提供 payload（可为空对象 {}）");
            }
            row.setPayload(deepCopyPayload(payload));
        }
        snapshotRepository.insert(row);
        return toDto(row);
    }

    /**
     * 局部更新快照。
     *
     * @param storyId    故事 ID
     * @param snapshotId 快照 ID
     * @param req        请求体
     * @return 更新后 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public StoryCharacterSnapshotDto patch(String storyId, String snapshotId, PatchCharacterSnapshotRequest req) {
        StoryCharacterSnapshotEntity existing = requireSnapshotInStory(storyId, snapshotId);
        if (req == null || (req.sortOrder() == null && req.payload() == null)) {
            return toDto(existing);
        }
        Instant now = Instant.now();
        Map<String, Object> newPayload = req.payload() != null ? deepCopyPayload(req.payload()) : null;
        snapshotRepository.patch(snapshotId, req.sortOrder(), newPayload, now);
        existing.setSortOrder(req.sortOrder() != null ? req.sortOrder() : existing.getSortOrder());
        if (newPayload != null) {
            existing.setPayload(newPayload);
        }
        existing.setUpdatedAt(now);
        return toDto(existing);
    }

    /**
     * 删除快照。
     *
     * @param storyId    故事 ID
     * @param snapshotId 快照 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String storyId, String snapshotId) {
        requireSnapshotInStory(storyId, snapshotId);
        snapshotRepository.deleteById(snapshotId);
    }

    /**
     * 重排本故事全部快照顺序。
     *
     * @param storyId     故事 ID
     * @param orderedIds  完整 ID 列表（新顺序）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorder(String storyId, List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("snapshotIds 不能为空");
        }
        long count = snapshotRepository.countByStoryId(storyId);
        if (orderedIds.size() != count) {
            throw new IllegalArgumentException("snapshotIds 数量须等于本故事快照条数");
        }
        Set<String> expected = new HashSet<>();
        for (StoryCharacterSnapshotEntity e : snapshotRepository.listByStoryIdOrdered(storyId)) {
            expected.add(e.getId());
        }
        if (!expected.equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("snapshotIds 须恰好包含本故事全部快照 ID");
        }
        Instant now = Instant.now();
        for (int i = 0; i < orderedIds.size(); i++) {
            snapshotRepository.patch(orderedIds.get(i), i, null, now);
        }
    }

    /**
     * 创建故事时批量从库克隆快照（须在故事已插入后调用，同一事务内）。
     *
     * @param storyId            故事 ID
     * @param libraryCharacterIds 有序、已去重库卡 ID
     * @param now                时间戳
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertSnapshotsFromLibrary(String storyId, List<String> libraryCharacterIds, Instant now) {
        if (libraryCharacterIds == null || libraryCharacterIds.isEmpty()) {
            return;
        }
        List<CharacterLibraryEntity> ordered = characterLibraryRepository.findAllByIdsPreserveOrder(libraryCharacterIds);
        for (int i = 0; i < libraryCharacterIds.size(); i++) {
            CharacterLibraryEntity lib = ordered.get(i);
            if (lib == null) {
                throw new IllegalArgumentException("人物卡库 ID 不存在: " + libraryCharacterIds.get(i));
            }
            StoryCharacterSnapshotEntity row = new StoryCharacterSnapshotEntity();
            row.setId(UUID.randomUUID().toString());
            row.setStoryId(storyId);
            row.setSortOrder(i);
            row.setClonedFromLibraryId(lib.getId());
            row.setPayload(deepCopyPayload(lib.getPayload()));
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            snapshotRepository.insert(row);
        }
    }

    private StoryCharacterSnapshotEntity requireSnapshotInStory(String storyId, String snapshotId) {
        StoryCharacterSnapshotEntity e =
                snapshotRepository
                        .findById(snapshotId)
                        .orElseThrow(() -> new IllegalArgumentException("快照不存在: " + snapshotId));
        if (!storyId.equals(e.getStoryId())) {
            throw new IllegalArgumentException("快照不属于该故事");
        }
        return e;
    }

    private Map<String, Object> deepCopyPayload(Map<String, Object> src) {
        if (src == null) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(
                    objectMapper.writeValueAsBytes(src), new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("人物 payload 拷贝失败: " + ex.getMessage());
        }
    }

    private static StoryCharacterSnapshotDto toDto(StoryCharacterSnapshotEntity e) {
        return new StoryCharacterSnapshotDto(
                e.getId(),
                e.getStoryId(),
                e.getSortOrder(),
                e.getClonedFromLibraryId(),
                e.getPayload(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    /**
     * 创建故事时库 ID 列表去重保序。
     *
     * @param ids 原始列表
     * @return 去重后列表
     */
    public static List<String> dedupeLibraryIdsPreserveOrder(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                continue;
            }
            String x = id.strip();
            if (seen.add(x)) {
                out.add(x);
            }
        }
        return out;
    }
}

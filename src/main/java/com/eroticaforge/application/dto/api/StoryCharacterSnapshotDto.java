package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.Map;

/**
 * 故事人物快照 API 模型。
 *
 * @param id                   快照主键
 * @param storyId              故事 ID
 * @param sortOrder            排序
 * @param clonedFromLibraryId  克隆来源库卡 ID，纯新增时为 null
 * @param payload              可编辑人物字段
 * @param createdAt            创建时间
 * @param updatedAt            更新时间
 * @author EroticaForge
 */
public record StoryCharacterSnapshotDto(
        String id,
        String storyId,
        int sortOrder,
        String clonedFromLibraryId,
        Map<String, Object> payload,
        Instant createdAt,
        Instant updatedAt) {}

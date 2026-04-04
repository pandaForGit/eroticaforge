package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/stories 列表项。
 *
 * @param storyId       故事 ID
 * @param title         标题
 * @param tags          标签
 * @param totalChapters 已生成章节数
 * @param updatedAt     最后更新时间
 * @author EroticaForge
 */
public record StoryListItemDto(
        String storyId, String title, List<String> tags, int totalChapters, Instant updatedAt) {}

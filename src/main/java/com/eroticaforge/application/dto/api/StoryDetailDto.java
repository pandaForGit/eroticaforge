package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.List;

/**
 * GET /api/stories/{storyId} 响应 data。
 *
 * @param storyId        故事 ID
 * @param title          标题
 * @param tags           标签
 * @param totalChapters  已生成章节数
 * @param nextChapterSeq 下一章将使用的序号
 * @param mainModel      主模型标识
 * @param createdAt      创建时间
 * @param updatedAt      更新时间
 * @author EroticaForge
 */
public record StoryDetailDto(
        String storyId,
        String title,
        List<String> tags,
        int totalChapters,
        int nextChapterSeq,
        String mainModel,
        Instant createdAt,
        Instant updatedAt) {}

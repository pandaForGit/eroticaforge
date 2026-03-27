package com.eroticaforge.application.dto.api;

import java.time.Instant;

/**
 * GET /api/stories/{storyId}/chapters 列表项。
 *
 * @param chapterId 章节 ID
 * @param seq       章节序号
 * @param title     标题
 * @param createdAt 创建时间
 * @author EroticaForge
 */
public record ChapterSummaryDto(String chapterId, int seq, String title, Instant createdAt) {}

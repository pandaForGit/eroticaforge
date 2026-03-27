package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.Map;

/**
 * GET /api/stories/{storyId}/chapters/{chapterId} 响应 data。
 *
 * @param chapterId 章节 ID
 * @param seq       序号
 * @param title     标题
 * @param content   正文
 * @param metadata  元数据
 * @param createdAt 创建时间
 * @author EroticaForge
 */
public record ChapterDetailDto(
        String chapterId,
        int seq,
        String title,
        String content,
        Map<String, Object> metadata,
        Instant createdAt) {}

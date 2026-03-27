package com.eroticaforge.application.dto.api;

/**
 * POST /api/stories/{storyId}/generate 响应 data。
 *
 * @param text      模型全文输出
 * @param chapterId 落库后的章节 ID
 * @author EroticaForge
 */
public record GenerateSyncResponse(String text, String chapterId) {}

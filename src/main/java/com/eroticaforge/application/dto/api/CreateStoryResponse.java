package com.eroticaforge.application.dto.api;

import java.time.Instant;

/**
 * POST /api/stories 响应 data。
 *
 * @param storyId   新建故事 ID
 * @param title     标题
 * @param createdAt 创建时间
 * @author EroticaForge
 */
public record CreateStoryResponse(String storyId, String title, Instant createdAt) {}

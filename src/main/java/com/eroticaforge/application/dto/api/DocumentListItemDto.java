package com.eroticaforge.application.dto.api;

import java.time.Instant;
import java.util.Map;

/**
 * GET /api/stories/{storyId}/documents 列表项。
 *
 * @param docId      文档 ID
 * @param fileName   文件名
 * @param chunkCount 切块数（来自元数据）
 * @param createdAt  创建时间
 * @param metadata   原始元数据副本（JSON）
 * @author EroticaForge
 */
public record DocumentListItemDto(
        String docId, String fileName, int chunkCount, Instant createdAt, Map<String, Object> metadata) {}

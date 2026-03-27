package com.eroticaforge.application.dto.api;

/**
 * POST /api/stories/{storyId}/documents 响应 data。
 *
 * @param docId     文档记录 ID
 * @param fileName  展示文件名
 * @param chunkCount 向量切块数
 * @param status    固定 {@code indexed}
 * @author EroticaForge
 */
public record DocumentUploadResponse(String docId, String fileName, int chunkCount, String status) {}

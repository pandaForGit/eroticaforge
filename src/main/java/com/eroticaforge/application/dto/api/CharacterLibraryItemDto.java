package com.eroticaforge.application.dto.api;

/**
 * GET /api/character-library 列表项。
 *
 * @param id                 库卡主键
 * @param displayName        展示名（多来自 JSON name）
 * @param sourceRelativePath 源路径
 * @param schemaVersion      schema 版本
 * @param contentSha256      源内容哈希
 * @param roleIndex          行内角色下标
 * @author EroticaForge
 */
public record CharacterLibraryItemDto(
        String id,
        String displayName,
        String sourceRelativePath,
        String schemaVersion,
        String contentSha256,
        int roleIndex) {}

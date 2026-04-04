package com.eroticaforge.application.dto.api;

import java.util.Map;

/**
 * PATCH /api/stories/{storyId}/character-snapshots/{snapshotId}。
 *
 * @param sortOrder 新排序，null 表示不改
 * @param payload   整体替换快照 payload，null 表示不改
 * @author EroticaForge
 */
public record PatchCharacterSnapshotRequest(Integer sortOrder, Map<String, Object> payload) {}

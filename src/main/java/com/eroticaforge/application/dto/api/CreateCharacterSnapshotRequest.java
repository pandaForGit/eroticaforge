package com.eroticaforge.application.dto.api;

import java.util.Map;

/**
 * POST /api/stories/{storyId}/character-snapshots。
 *
 * <p>若 {@code libraryCharacterId} 非空：从库克隆 payload，忽略 {@code payload} 初值（可后续 PATCH）。
 * 若为空：须提供 {@code payload}（可为空对象），作为手写快照。
 *
 * @param libraryCharacterId 人物卡库 ID，可选
 * @param payload            手写快照内容；与库克隆二选一场景下必填（克隆时可 null）
 * @author EroticaForge
 */
public record CreateCharacterSnapshotRequest(String libraryCharacterId, Map<String, Object> payload) {}

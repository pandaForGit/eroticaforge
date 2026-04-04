package com.eroticaforge.application.dto.api;

import java.util.List;

/**
 * PUT /api/stories/{storyId}/character-snapshots/order。
 *
 * @param snapshotIds 本故事下全部快照 ID，按期望顺序排列
 * @author EroticaForge
 */
public record ReorderCharacterSnapshotsRequest(List<String> snapshotIds) {}

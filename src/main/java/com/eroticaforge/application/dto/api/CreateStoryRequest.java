package com.eroticaforge.application.dto.api;

import java.util.List;

/**
 * POST /api/stories 请求体。
 *
 * @param title               故事标题
 * @param tags                标签列表，可为 {@code null}（按空列表处理）
 * @param libraryCharacterIds 从人物卡库克隆的快照来源 ID，有序，可 {@code null} 或空
 * @author EroticaForge
 */
public record CreateStoryRequest(String title, List<String> tags, List<String> libraryCharacterIds) {}

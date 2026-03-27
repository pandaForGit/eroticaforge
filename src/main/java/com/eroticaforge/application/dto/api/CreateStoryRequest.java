package com.eroticaforge.application.dto.api;

import java.util.List;

/**
 * POST /api/stories 请求体。
 *
 * @param title 故事标题
 * @param tags  标签列表，可为 {@code null}（按空列表处理）
 * @author EroticaForge
 */
public record CreateStoryRequest(String title, List<String> tags) {}

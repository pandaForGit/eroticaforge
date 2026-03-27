package com.eroticaforge.application.dto.api;

/**
 * POST /api/lorebook 请求体。
 *
 * @param keyword 触发词
 * @param body    注入正文
 * @author EroticaForge
 */
public record LorebookCreateRequest(String keyword, String body) {}

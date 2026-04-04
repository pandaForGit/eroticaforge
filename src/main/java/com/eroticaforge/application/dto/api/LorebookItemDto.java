package com.eroticaforge.application.dto.api;

import java.time.Instant;

/**
 * Lorebook 条目（阶段 5 REST；当前存于内存，阶段 6 可换表持久化）。
 *
 * @param id        条目 ID
 * @param keyword   触发关键词
 * @param body      注入正文的描写/设定
 * @param createdAt 创建时间
 * @author EroticaForge
 */
public record LorebookItemDto(long id, String keyword, String body, Instant createdAt) {}

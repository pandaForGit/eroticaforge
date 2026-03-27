package com.eroticaforge.application.service;

import com.eroticaforge.application.dto.api.LorebookItemDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lorebook：内存维护条目列表 + 关键词触发（阶段 5）；后续可换 {@code erotica_lorebook} 表。
 *
 * @author EroticaForge
 */
@Service
public class LorebookService {

    private final CopyOnWriteArrayList<LorebookEntry> entries = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /**
     * 列出当前全部 Lorebook 条目。
     *
     * @return 列表快照（可能为空，非 null）
     */
    public List<LorebookItemDto> listAll() {
        return entries.stream().map(LorebookEntry::toDto).toList();
    }

    /**
     * 新增一条 Lorebook 条目。
     *
     * @param keyword 触发关键词，不可为空
     * @param body    描写正文，不可为空
     * @return 新建条目 DTO
     */
    public LorebookItemDto addEntry(String keyword, String body) {
        if (!StringUtils.hasText(keyword)) {
            throw new IllegalArgumentException("keyword 不能为空");
        }
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("body 不能为空");
        }
        LorebookEntry e =
                new LorebookEntry(
                        nextId.getAndIncrement(), keyword.strip(), body.strip(), Instant.now());
        entries.add(e);
        return e.toDto();
    }

    /**
     * 根据当前上下文（用户输入 + 摘要等）匹配关键词，拼接应注入 Prompt 的说明文本。
     *
     * @param context 用于匹配的上下文字符串，可为 {@code null}（按空串处理）
     * @return 无命中时返回空串
     */
    public String getTriggeredDescriptions(String context) {
        if (context == null || context.isBlank()) {
            return "";
        }
        String lower = context.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (LorebookEntry e : entries) {
            if (lower.contains(e.keyword.toLowerCase(Locale.ROOT))) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(e.keyword).append("：").append(e.body);
            }
        }
        return sb.toString();
    }

    private record LorebookEntry(long id, String keyword, String body, Instant createdAt) {
        LorebookItemDto toDto() {
            return new LorebookItemDto(id, keyword, body, createdAt);
        }
    }
}

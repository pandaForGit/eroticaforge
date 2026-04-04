package com.eroticaforge.application.service;

import com.eroticaforge.infrastructure.persistence.entity.StoryCharacterSnapshotEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 将故事人物快照格式化为送入 Prompt 的纯文本块。
 *
 * @author EroticaForge
 */
@Component
public class CharacterProfilePromptFormatter {

    /**
     * 将有序快照列表格式化为 Markdown 风格小节。
     *
     * @param snapshots 按 {@code sort_order} 排好序的快照
     * @return 非空串；无数据时返回空串
     */
    public String format(List<StoryCharacterSnapshotEntity> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (StoryCharacterSnapshotEntity s : snapshots) {
            Map<String, Object> p = s.getPayload();
            if (p == null) {
                p = Map.of();
            }
            String title = firstString(p.get("name"), "未命名");
            sb.append("#### ").append(title).append("\n");
            appendLine(sb, "年龄", p.get("age"));
            appendLine(sb, "身份", p.get("identity"));
            appendLine(sb, "外貌", p.get("appearance"));
            appendLine(sb, "性格", p.get("personality"));
            appendLine(sb, "背景", p.get("background"));
            appendLine(sb, "心理与关系", p.get("psychology_and_relations"));
            appendLine(sb, "NSFW 侧写", p.get("nsfw_profile"));
            appendDialogues(sb, p.get("sample_dialogues"));
            sb.append("\n");
        }
        return sb.toString().strip();
    }

    private static void appendLine(StringBuilder sb, String label, Object value) {
        String t = stringify(value);
        if (!StringUtils.hasText(t)) {
            return;
        }
        sb.append("- **").append(label).append("**：").append(t).append("\n");
    }

    private static void appendDialogues(StringBuilder sb, Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        String joined =
                list.stream()
                        .map(CharacterProfilePromptFormatter::stringify)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(" / "));
        if (StringUtils.hasText(joined)) {
            sb.append("- **示例台词**：").append(joined).append("\n");
        }
    }

    private static String stringify(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof String s) {
            return s.strip();
        }
        return Objects.toString(v).strip();
    }

    private static String firstString(Object v, String fallback) {
        String s = stringify(v);
        return StringUtils.hasText(s) ? s : fallback;
    }
}

package com.eroticaforge.application.service;

import com.eroticaforge.config.PromptProperties;
import com.eroticaforge.domain.StoryState;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据 {@link PromptProperties} 拼装送入 ChatModel 的提示词。
 *
 * @author EroticaForge
 */
@Component
public class PromptComposer {

    private final PromptProperties promptProperties;

    /**
     * @param promptProperties 可外部化配置的提示词与占位符
     */
    public PromptComposer(PromptProperties promptProperties) {
        this.promptProperties = promptProperties;
    }

    /**
     * 组装单轮生成用完整 user 侧 Prompt（含系统规则与状态、人物快照、RAG、Lorebook、用户输入）。
     *
     * @param state              当前故事状态，不可为 {@code null}
     * @param characterProfiles  本故事人物快照格式化文本，可为空串
     * @param ragContext         RAG 检索结果文本，可为空串
     * @param loreTriggers       Lorebook 触发说明，可为空串
     * @param taskConstraints    本轮结构化任务（意图解析），可为空串
     * @param userPrompt         用户本轮指令或续写提示，不可为 {@code null}
     * @return 送入流式/同步聊天的完整字符串
     */
    public String buildFullPrompt(
            StoryState state,
            String characterProfiles,
            String ragContext,
            String loreTriggers,
            String taskConstraints,
            String userPrompt) {
        PromptProperties.Placeholders ph = promptProperties.getPlaceholders();
        String profiles = characterProfiles != null ? characterProfiles : "";
        String rag = ragContext != null ? ragContext : "";
        String lore = loreTriggers != null ? loreTriggers : "";
        String user = userPrompt != null ? userPrompt : "";
        String task = taskConstraints != null ? taskConstraints : "";

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("taskConstraints", task);
        vars.put("globalSystem", promptProperties.getGeneration().getGlobalSystem());
        vars.put("currentSummary", blankToPlaceholder(state.getCurrentSummary(), ph.getEmptyField()));
        vars.put(
                "characterStates",
                formatCharacterStates(state.getCharacterStates(), ph.getEmptyCharacterStates()));
        vars.put("importantFacts", formatImportantFacts(state.getImportantFacts(), ph.getEmptyFacts()));
        vars.put(
                "lastChapterEnding",
                blankToPlaceholder(state.getLastChapterEnding(), ph.getEmptyField()));
        vars.put(
                "characterProfiles",
                blankToPlaceholder(profiles, ph.getEmptyCharacterProfiles()));
        vars.put("ragContext", rag);
        vars.put("loreTriggers", blankToPlaceholder(lore, ph.getEmptyField()));
        vars.put("userPrompt", user);

        return applyTemplate(promptProperties.getGeneration().getUserTemplate(), vars);
    }

    /**
     * 从新生成正文提炼 StoryState 摘要与结尾时送入模型的提示词。
     *
     * @param generatedText 模型刚输出的正文，不可为 {@code null}
     * @return 完整提示字符串
     */
    public String buildStateExcerptPrompt(String generatedText) {
        String body = generatedText != null ? generatedText : "";
        return applyTemplate(
                promptProperties.getStateExcerpt().getTemplate(),
                Map.of("generatedText", body));
    }

    private static String applyTemplate(String template, Map<String, String> variables) {
        String result = template != null ? template : "";
        for (Map.Entry<String, String> e : variables.entrySet()) {
            String value = e.getValue() != null ? e.getValue() : "";
            result = result.replace("{{" + e.getKey() + "}}", value);
        }
        return result;
    }

    private static String formatCharacterStates(
            Map<String, String> characterStates, String emptyPlaceholder) {
        if (characterStates == null || characterStates.isEmpty()) {
            return emptyPlaceholder != null ? emptyPlaceholder : "";
        }
        return characterStates.entrySet().stream()
                .map(e -> e.getKey() + "：" + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static String formatImportantFacts(List<String> facts, String emptyPlaceholder) {
        if (facts == null || facts.isEmpty()) {
            return emptyPlaceholder != null ? emptyPlaceholder : "";
        }
        return String.join("；", facts);
    }

    private static String blankToPlaceholder(String s, String emptyPlaceholder) {
        if (!StringUtils.hasText(s)) {
            return emptyPlaceholder != null ? emptyPlaceholder : "";
        }
        return s.strip();
    }
}

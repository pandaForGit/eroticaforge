package com.eroticaforge.application.service;

import com.eroticaforge.config.RagProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 根据用户短指令推断「本轮任务」文案与 RAG 修饰，减轻提示词自相矛盾与 RAG+状态对单一场景的强化。
 *
 * @author EroticaForge
 */
@Component
public class GenerationIntentAnalyzer {

    private final RagProperties ragProperties;

    public GenerationIntentAnalyzer(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * @param userPrompt 用户本轮输入，不可为 {@code null}
     * @return 任务块、嵌入后缀、RAG 修饰
     */
    public GenerationTurnPlan plan(String userPrompt) {
        String raw = userPrompt != null ? userPrompt.strip() : "";
        if (!StringUtils.hasText(raw)) {
            return GenerationTurnPlan.neutral();
        }

        boolean divergePlot = matchesDivergePlot(raw);
        boolean newCharacter = matchesNewCharacter(raw);
        boolean shortPrompt = raw.length() <= 8;

        String taskConstraints = buildTaskConstraints(raw, divergePlot, newCharacter, shortPrompt);
        String embedSuffix = buildEmbedSuffix(divergePlot, newCharacter);
        RagRetrievalModifiers rag =
                buildRagModifiers(divergePlot || newCharacter);

        return new GenerationTurnPlan(taskConstraints, embedSuffix, rag);
    }

    private static boolean matchesDivergePlot(String p) {
        return containsAny(
                p,
                "其他剧情",
                "推进其他",
                "换场景",
                "换场",
                "换景",
                "换线",
                "离开",
                "出门",
                "出去",
                "支线",
                "转折",
                "别洗澡",
                "不要洗",
                "不在浴室",
                "离开浴室",
                "出浴室",
                "客厅",
                "卧室",
                "门外",
                "外面",
                "楼上",
                "楼下",
                "室外",
                "找人",
                "寻找",
                "找到",
                "女儿",
                "儿子",
                "孩子去哪",
                "新剧情");
    }

    private static boolean matchesNewCharacter(String p) {
        return containsAny(
                p, "新人物", "新人", "角色出场", "出场", "配角", "第三人", "别人", "陌生人");
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private RagRetrievalModifiers buildRagModifiers(boolean reduceBinding) {
        if (!reduceBinding) {
            return RagRetrievalModifiers.defaults();
        }
        int configured = Math.max(1, ragProperties.getTopK());
        int capped = Math.max(3, configured / 2);
        return new RagRetrievalModifiers(Boolean.FALSE, capped);
    }

    private static String buildEmbedSuffix(boolean divergePlot, boolean newCharacter) {
        if (!divergePlot && !newCharacter) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (divergePlot) {
            sb.append("叙事转折 新场景或时间跳跃 避免与上一章同一密闭空间延续 情节信息推进");
        }
        if (newCharacter) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append("新角色互动 对话交代身份或动机");
        }
        return sb.toString();
    }

    private static String buildTaskConstraints(
            String userLiteral,
            boolean divergePlot,
            boolean newCharacter,
            boolean shortPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 本轮任务（优先级高于下文 RAG 片段；与任务冲突时以任务为准）\n");
        sb.append("- **用户原话**：「").append(escapeForPrompt(userLiteral)).append("」\n");

        if (divergePlot) {
            sb.append(
                    """
                    - **换线/拓展**：本段必须相对「上次章节结尾」出现可感知的**场景或时间变化**（例如离开浴室、换房间、次日、打断与插话等），不得在同一镜头内用近似句式、台词模板重复堆砌同一段肉体描写。
                    - **新信息**：至少加入一处上一段未写过的具体细节（物件、对话目的、他人反应、外部事件），避免复读上一章已有段落。
                    """);
        }
        if (newCharacter) {
            sb.append(
                    """
                    - **新人物**：本段须有一名**新登场或此前未正面描写**的角色与剧情发生交集（称呼/关系/一句有信息量的台词即可），不得全段只有单一女性视角独角戏式重复呻吟模板。
                    """);
        }
        if (shortPrompt && !divergePlot && !newCharacter) {
            sb.append(
                    """
                    - **短指令补全**：用户指令较简短，请结合 Story State 自行补全一条可执行的微型剧情目标（地点/冲突/关系变化至少一项），并写进本段正文。
                    """);
        }
        if (!divergePlot && !newCharacter && !shortPrompt) {
            sb.append(
                    """
                    - **连贯续写**：在与人设、摘要、RAG 一致的前提下续写；禁止在同一段落内复制粘贴式重复相同对话或相同比喻链。
                    """);
        }
        return sb.toString().strip();
    }

    private static String escapeForPrompt(String s) {
        return s.replace("###", "＃＃＃").replace("{{", "｛｛");
    }
}

package com.eroticaforge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 主应用内所有送入 LLM 的提示词与相关展示文案（可通过 {@code application.yml} / {@code application-local.yml} 覆盖）。
 *
 * <p>占位符约定（生成主模板 {@link Generation#getUserTemplate()}）：
 * {@code {{taskConstraints}}}、{@code {{globalSystem}}}、{@code {{currentSummary}}}、
 * {@code {{characterStates}}}、{@code {{importantFacts}}}、{@code {{lastChapterEnding}}}、
 * {@code {{characterProfiles}}}、{@code {{ragContext}}}、{@code {{loreTriggers}}}、{@code {{userPrompt}}}。
 *
 * <p>状态摘录模板 {@link StateExcerpt#getTemplate()}：{@code {{generatedText}}}。
 *
 * @author EroticaForge
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "erotica.prompt")
public class PromptProperties {

    private Generation generation = new Generation();
    private StateExcerpt stateExcerpt = new StateExcerpt();
    private RagLabels rag = new RagLabels();
    private Placeholders placeholders = new Placeholders();

    @Getter
    @Setter
    public static class Generation {

        /**
         * 写入用户大包中的全局系统规则与风格。
         */
        private String globalSystem =
                """
                你是男性向色情小说作者，**第三人称**，文笔细腻，感官与心理描写充分，贴合下方人物设定与故事状态。
                - 单段正文目标约 **600～1200 汉字**（随模型上限可略浮动），写成一整段或多段连贯场景，不要分章标题。
                - **Markdown**：**粗体**强调强烈感受或关键动作；*斜体*表示内心独白；普通文本写对白与环境。
                - **去重**：同一段落内不得复述同一句对白或同一串比喻；不得复制粘贴式循环整段（若发现自己在重复，立刻改换动作、角度或信息点）。
                - **优先级**：模板最上方的「本轮任务」高于本段规则；本段规则高于 RAG 片段；RAG 与状态用于补细节，不得绑架成重复上章场景。
                - **禁忌题材**等取向由使用者与平台自担；此处只要求人物行为与上文状态逻辑自洽。
                - 正文后另起一行给出 **3～4 条「剧情推荐选项」**，每条一行、一句话，只点方向不展开。
                - 不要输出创作说明、代码块、OOC、元评论。
                """;

        /**
         * 单轮生成完整 user 侧模板；须包含上述占位符。
         */
        private String userTemplate =
                """
                {{taskConstraints}}

                ### 文风与全局规则
                {{globalSystem}}

                ### 当前故事状态（Story State）
                当前剧情摘要：{{currentSummary}}
                关键人物状态：
                {{characterStates}}
                重要事实列表：{{importantFacts}}
                上次章节结尾：{{lastChapterEnding}}

                ### 本故事人物设定（快照，须一致）
                {{characterProfiles}}

                ### RAG 召回片段（仅供参考）
                下列文本来自向量检索，可能与当前意图部分重合或过时：**若与上方「本轮任务」冲突，以本轮任务为准**；只抽取与人设、时间线相符的细节，勿机械复读旧场景。
                {{ragContext}}

                ### Lorebook 关键词触发（若触发须自然融入）
                {{loreTriggers}}

                ### 用户当前输入（原文）
                {{userPrompt}}

                请直接开始输出小说正文（含文末「剧情推荐选项」）。
                """;
    }

    @Getter
    @Setter
    public static class StateExcerpt {

        /**
         * StoryState 更新前从正文提炼摘要与结尾锚点；须包含 {@code {{generatedText}}}。
         */
        private String template =
                """
                你是剧情摘录助手。根据下列新生成的小说正文，输出严格两行纯文本（不要引号、不要 markdown）：
                第1行：用不超过80字概括本节剧情。
                第2行：摘录结尾约200字以内、适合作为下轮续写锚点的文字（若正文不足200字则全文）。

                正文：
                {{generatedText}}
                """;
    }

    @Getter
    @Setter
    public static class RagLabels {

        /** RAG 命中块在 Prompt 中的标签前缀（用户上传/章节回写等）。 */
        private String recallChunk = "回忆";

        /** 专题参考库命中块在 Prompt 中的标签前缀。 */
        private String referenceChunk = "参考";
    }

    @Getter
    @Setter
    public static class Placeholders {

        /** 无结构化人物状态时的展示文案。 */
        private String emptyCharacterStates = "（暂无结构化人物状态）";

        /** 无重要事实时的展示文案。 */
        private String emptyFacts = "（暂无）";

        /** 摘要/结尾/Lore 等空字段时的展示文案。 */
        private String emptyField = "（暂无）";

        /** 无故事人物快照时的展示文案。 */
        private String emptyCharacterProfiles = "（本故事暂无人物快照）";
    }
}

package com.eroticaforge.config;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;

/**
 * 将默认 {@link ChatRequestParameters} 与可选的每轮 {@code maxOutputTokens} 合并为 {@link ChatRequest}。
 *
 * @author EroticaForge
 */
public final class ChatModelRequests {

    private ChatModelRequests() {}

    /**
     * 单条 user 消息；若 {@code maxOutputTokensOverride} 非空则覆盖默认 max_tokens / max_output_tokens。
     */
    public static ChatRequest userMessage(
            ChatLanguageModel model, String text, Integer maxOutputTokensOverride) {
        return build(model.defaultRequestParameters(), text, maxOutputTokensOverride);
    }

    /**
     * 单条 user 消息（流式模型默认参数与同步模型同源字段）。
     */
    public static ChatRequest userMessage(
            StreamingChatLanguageModel model, String text, Integer maxOutputTokensOverride) {
        return build(model.defaultRequestParameters(), text, maxOutputTokensOverride);
    }

    private static ChatRequest build(
            ChatRequestParameters defaults, String text, Integer maxOutputTokensOverride) {
        ChatRequestParameters params = defaults;
        if (maxOutputTokensOverride != null) {
            params =
                    defaults.overrideWith(
                            OpenAiChatRequestParameters.builder()
                                    .maxOutputTokens(maxOutputTokensOverride)
                                    .build());
        }
        return ChatRequest.builder()
                .messages(UserMessage.from(text))
                .parameters(params)
                .build();
    }
}

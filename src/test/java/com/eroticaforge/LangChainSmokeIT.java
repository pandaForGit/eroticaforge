package com.eroticaforge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 需要：数据库可连、llama OpenAI 兼容嵌入端、llama-server（对话）。
 * PowerShell: {@code $env:EROTICA_RUN_LLM_SMOKE="true"; mvn -Dtest=LangChainSmokeIT test}
 *
 * @author EroticaForge
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "EROTICA_RUN_LLM_SMOKE", matches = "true")
class LangChainSmokeIT {

    /** 注入的同步聊天模型（llama-server OpenAI 兼容）。 */
    @Autowired
    ChatLanguageModel chatLanguageModel;

    /** 注入的嵌入模型（OpenAI 兼容 /v1/embeddings）。 */
    @Autowired
    EmbeddingModel embeddingModel;

    /** 注入的 PgVector 向量存储。 */
    @Autowired
    EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 验证聊天模型能返回包含 “pong” 的回复。
     *
     * <p>无入参。
     */
    @Test
    void chatModelReplies() {
        // 单轮用户消息
        ChatRequest chatRequest =
                ChatRequest.builder()
                        .messages(UserMessage.from("Reply with exactly: pong"))
                        .build();
        // 模型响应（具体类型由 LangChain4j 版本决定，此处用 var 保持兼容）
        var chatResponse = chatLanguageModel.chat(chatRequest);
        String replyLower = chatResponse.aiMessage().text().toLowerCase();
        assertThat(replyLower).contains("pong");
    }

    /**
     * 验证嵌入结果向量非空。
     *
     * <p>无入参。
     */
    @Test
    void embeddingModelHasFixedDimension() {
        // 对固定短文本求嵌入
        Response<Embedding> embeddingResponse = embeddingModel.embed("test");
        float[] vector = embeddingResponse.content().vector();
        assertThat(vector).isNotEmpty();
    }

    /**
     * 验证向量写入后可检索到至少一条匹配。
     *
     * <p>无入参。
     */
    @Test
    void pgVectorStoreAddAndSearch() {
        String segmentText = "hello pgvector";
        Response<Embedding> embeddingResponse = embeddingModel.embed(segmentText);
        Embedding embedding = embeddingResponse.content();
        embeddingStore.add(embedding, TextSegment.from(segmentText));

        EmbeddingSearchRequest searchRequest =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(embedding)
                        .maxResults(3)
                        .minScore(0.0)
                        .build();
        // 相似度检索结果列表
        var searchMatches = embeddingStore.search(searchRequest).matches();
        assertThat(searchMatches).isNotEmpty();
    }
}

package com.eroticaforge.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;

/**
 * LangChain4j 与 llama-server（OpenAI 兼容：对话 + 嵌入）、PgVector 的配置。
 *
 * <p>Bean 命名与构造遵循《阿里巴巴 Java 开发手册》：配置类仅做装配，不包含业务分支。
 *
 * @author EroticaForge
 */
@Configuration
public class LangChainConfig {

    /**
     * 同步聊天模型（指向 llama-server OpenAI 兼容接口）。
     *
     * @param baseUrl     OpenAI 兼容 base-url
     * @param apiKey      API Key（本地可为占位）
     * @param modelName   模型名
     * @param temperature 温度
     * @param maxTokens   最大 token
     * @return {@link ChatLanguageModel} Bean
     */
    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.open-ai.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature}") double temperature,
            @Value("${langchain4j.open-ai.chat-model.max-tokens}") int maxTokens,
            @Value("${langchain4j.open-ai.chat-model.frequency-penalty:0.35}") double frequencyPenalty,
            @Value("${langchain4j.open-ai.chat-model.presence-penalty:0.05}") double presencePenalty) {
        Duration requestTimeout = Duration.ofMinutes(10);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .timeout(requestTimeout)
                .build();
    }

    /**
     * 流式聊天模型。
     *
     * @param baseUrl     OpenAI 兼容 base-url
     * @param apiKey      API Key
     * @param modelName   模型名
     * @param temperature 温度
     * @param maxTokens   最大 token
     * @return {@link StreamingChatLanguageModel} Bean
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${langchain4j.open-ai.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.chat-model.temperature}") double temperature,
            @Value("${langchain4j.open-ai.chat-model.max-tokens}") int maxTokens,
            @Value("${langchain4j.open-ai.chat-model.frequency-penalty:0.35}") double frequencyPenalty,
            @Value("${langchain4j.open-ai.chat-model.presence-penalty:0.05}") double presencePenalty) {
        Duration streamTimeout = Duration.ofMinutes(10);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty)
                .timeout(streamTimeout)
                .build();
    }

    /**
     * 嵌入模型（OpenAI 兼容 {@code /v1/embeddings}，如单独端口的 llama-server）。
     *
     * @param baseUrl   含 {@code /v1} 的 base-url（与 chat 可不同端口）
     * @param apiKey    API Key（本地占位即可）
     * @param modelName 请求体中的 model 字段（llama.cpp 常可填占位或与加载模型一致）
     * @param maxRetries 失败重试次数
     * @return {@link EmbeddingModel} Bean
     */
    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${langchain4j.open-ai.embedding-model.base-url}") String baseUrl,
            @Value("${langchain4j.open-ai.embedding-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.embedding-model.model-name}") String modelName,
            @Value("${langchain4j.open-ai.embedding-model.max-retries:0}") int maxRetries) {
        Duration embedTimeout = Duration.ofMinutes(2);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(embedTimeout)
                .maxRetries(Math.max(0, maxRetries))
                .build();
    }

    /**
     * LangChain4j 自建表结构，默认表名 {@code erotica_lc4j_embeddings}，与 {@code sql/001_init_pgvector.sql} 中
     * {@code erotica_rag_chunks} 并存；后续可在业务层统一或迁移。
     *
     * @param dataSource 数据源
     * @param table      向量表名
     * @param dimension  向量维度
     * @return {@link EmbeddingStore} Bean
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            DataSource dataSource,
            @Value("${erotica.pgvector.table}") String table,
            @Value("${erotica.embedding.dimension:1024}") int dimension) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(table)
                .dimension(dimension)
                .createTable(true)
                .dropTableFirst(false)
                .useIndex(false)
                .build();
    }
}

package com.eroticaforge.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 业务上的 StoryState；{@code version}/{@code updatedAt} 来自表列，
 * 叙事字段来自 {@code payload} JSONB（snake_case）。
 *
 * <p>领域模型保持不可变；与 JSON 交互通过内嵌 {@link Payload}（JavaBean 封装）完成。
 *
 * @author EroticaForge
 */
public final class StoryState {

    /** 故事 ID（与 {@code erotica_story_states.story_id} 一致）。 */
    private final String storyId;

    /** 乐观锁版本号（表列 {@code version}）。 */
    private final int version;

    /** 行最后更新时间（表列 {@code updated_at}）。 */
    private final Instant updatedAt;

    /** 当前故事概要（payload.current_summary）。 */
    private final String currentSummary;

    /** 角色状态快照：角色名 → 状态描述（payload.character_states）。 */
    private final Map<String, String> characterStates;

    /** 重要事实列表（payload.important_facts）。 */
    private final List<String> importantFacts;

    /** 世界/剧情标记（payload.world_flags）。 */
    private final List<String> worldFlags;

    /** 上一章结尾摘录（payload.last_chapter_ending）。 */
    private final String lastChapterEnding;

    /**
     * 全字段构造。
     *
     * @param storyId           故事 ID
     * @param version           版本号
     * @param updatedAt         更新时间
     * @param currentSummary    概要
     * @param characterStates   角色状态，可为 null
     * @param importantFacts    重要事实，可为 null
     * @param worldFlags        世界标记，可为 null
     * @param lastChapterEnding 上章结尾
     */
    public StoryState(
            String storyId,
            int version,
            Instant updatedAt,
            String currentSummary,
            Map<String, String> characterStates,
            List<String> importantFacts,
            List<String> worldFlags,
            String lastChapterEnding) {
        this.storyId = Objects.requireNonNull(storyId, "storyId 不能为空");
        this.version = version;
        this.updatedAt = updatedAt;
        this.currentSummary = currentSummary != null ? currentSummary : "";
        this.characterStates = characterStates != null ? new LinkedHashMap<>(characterStates) : new LinkedHashMap<>();
        this.importantFacts = importantFacts != null ? new ArrayList<>(importantFacts) : new ArrayList<>();
        this.worldFlags = worldFlags != null ? new ArrayList<>(worldFlags) : new ArrayList<>();
        this.lastChapterEnding = lastChapterEnding;
    }

    /**
     * 新建故事时的空状态（version=1，与表默认值一致）。
     *
     * @param storyId 故事 ID
     * @param now     当前时间（写入 {@code updated_at} 语义由调用方与库一致）
     * @return 初始 StoryState
     */
    public static StoryState empty(String storyId, Instant now) {
        return new StoryState(storyId, 1, now, "", Map.of(), List.of(), List.of(), null);
    }

    /**
     * 由表列与 payload 反序列化结果组装领域对象。
     *
     * @param storyId   故事 ID
     * @param version   表列版本
     * @param updatedAt 表列更新时间
     * @param payload   JSON 载荷，可为 null（按空载荷处理）
     * @return 领域对象
     */
    public static StoryState fromRow(String storyId, int version, Instant updatedAt, Payload payload) {
        // 归一化：数据库空对象或反序列化失败时使用空 Payload
        Payload effectivePayload = payload != null ? payload : new Payload();
        return new StoryState(
                storyId,
                version,
                updatedAt,
                effectivePayload.getCurrentSummary(),
                effectivePayload.getCharacterStates(),
                effectivePayload.getImportantFacts(),
                effectivePayload.getWorldFlags(),
                effectivePayload.getLastChapterEnding());
    }

    /**
     * 在乐观锁更新成功后，用数据库返回的新版本号与时间戳刷新视图。
     *
     * @param newVersion    新版本号
     * @param newUpdatedAt  新更新时间
     * @return 更新后的不可变实例
     */
    public StoryState withVersion(int newVersion, Instant newUpdatedAt) {
        return new StoryState(
                storyId,
                newVersion,
                newUpdatedAt,
                currentSummary,
                characterStates,
                importantFacts,
                worldFlags,
                lastChapterEnding);
    }

    /**
     * 生成待写入 {@code payload} 列的 JavaBean（供 Jackson 序列化）。
     *
     * @return 可序列化的载荷副本
     */
    public Payload toPayload() {
        // 为写入数据库构造独立可变 DTO，避免泄露内部集合引用
        Payload payload = new Payload();
        payload.setCurrentSummary(currentSummary);
        payload.setCharacterStates(new LinkedHashMap<>(characterStates));
        payload.setImportantFacts(new ArrayList<>(importantFacts));
        payload.setWorldFlags(new ArrayList<>(worldFlags));
        payload.setLastChapterEnding(lastChapterEnding);
        return payload;
    }

    /**
     * @return 故事 ID
     */
    public String getStoryId() {
        return storyId;
    }

    /**
     * @return 乐观锁版本号
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return 行更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * @return 当前概要
     */
    public String getCurrentSummary() {
        return currentSummary;
    }

    /**
     * @return 角色状态映射（内部不可变语义由构造拷贝保证）
     */
    public Map<String, String> getCharacterStates() {
        return characterStates;
    }

    /**
     * @return 重要事实列表
     */
    public List<String> getImportantFacts() {
        return importantFacts;
    }

    /**
     * @return 世界标记列表
     */
    public List<String> getWorldFlags() {
        return worldFlags;
    }

    /**
     * @return 上一章结尾
     */
    public String getLastChapterEnding() {
        return lastChapterEnding;
    }

    /**
     * 仅用于序列化/反序列化 {@code erotica_story_states.payload} 列；字段私有，通过访问器与 Jackson 协作。
     *
     * @author EroticaForge
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Payload {

        /** JSON 字段 {@code current_summary}。 */
        @JsonProperty("current_summary")
        private String currentSummary = "";

        /** JSON 字段 {@code character_states}。 */
        @JsonProperty("character_states")
        private Map<String, String> characterStates = new LinkedHashMap<>();

        /** JSON 字段 {@code important_facts}。 */
        @JsonProperty("important_facts")
        private List<String> importantFacts = new ArrayList<>();

        /** JSON 字段 {@code world_flags}。 */
        @JsonProperty("world_flags")
        private List<String> worldFlags = new ArrayList<>();

        /** JSON 字段 {@code last_chapter_ending}。 */
        @JsonProperty("last_chapter_ending")
        private String lastChapterEnding;

        /**
         * 无参构造：供 Jackson 与手动实例化使用。
         */
        public Payload() {
            // 刻意保留空构造
        }

        /**
         * @return 当前概要
         */
        public String getCurrentSummary() {
            return currentSummary;
        }

        /**
         * @param currentSummary 当前概要，null 时按空串处理
         */
        public void setCurrentSummary(String currentSummary) {
            this.currentSummary = currentSummary != null ? currentSummary : "";
        }

        /**
         * @return 角色状态映射（可序列化容器）
         */
        public Map<String, String> getCharacterStates() {
            return characterStates;
        }

        /**
         * @param characterStates 角色状态，null 时按空 Map 处理
         */
        public void setCharacterStates(Map<String, String> characterStates) {
            this.characterStates = characterStates != null ? new LinkedHashMap<>(characterStates) : new LinkedHashMap<>();
        }

        /**
         * @return 重要事实列表
         */
        public List<String> getImportantFacts() {
            return importantFacts;
        }

        /**
         * @param importantFacts 重要事实，null 时按空列表处理
         */
        public void setImportantFacts(List<String> importantFacts) {
            this.importantFacts = importantFacts != null ? new ArrayList<>(importantFacts) : new ArrayList<>();
        }

        /**
         * @return 世界标记列表
         */
        public List<String> getWorldFlags() {
            return worldFlags;
        }

        /**
         * @param worldFlags 世界标记，null 时按空列表处理
         */
        public void setWorldFlags(List<String> worldFlags) {
            this.worldFlags = worldFlags != null ? new ArrayList<>(worldFlags) : new ArrayList<>();
        }

        /**
         * @return 上一章结尾
         */
        public String getLastChapterEnding() {
            return lastChapterEnding;
        }

        /**
         * @param lastChapterEnding 上一章结尾，可为 null
         */
        public void setLastChapterEnding(String lastChapterEnding) {
            this.lastChapterEnding = lastChapterEnding;
        }
    }
}

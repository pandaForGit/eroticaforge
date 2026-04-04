package com.eroticaforge.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 故事元数据，对应表 {@code erotica_stories}。
 *
 * @author EroticaForge
 */
public final class Story {

    /** 故事主键 ID。 */
    private final String id;

    /** 标题。 */
    private final String title;

    /** 标签列表（持久化为 JSONB 数组）。 */
    private final List<String> tags;

    /** 已生成章节总数。 */
    private final int totalChapters;

    /** 下一章将使用的序号（单调递增）。 */
    private final int nextChapterSeq;

    /** 主生成模型标识或名称。 */
    private final String mainModel;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 最后更新时间。 */
    private final Instant updatedAt;

    /**
     * 全字段构造。
     *
     * @param id              故事 ID，不可为 null
     * @param title           标题，可为 null（按空串存储）
     * @param tags            标签，可为 null（按空列表处理）
     * @param totalChapters   已生成章节数
     * @param nextChapterSeq  下一章节序号
     * @param mainModel       主模型
     * @param createdAt       创建时间
     * @param updatedAt       更新时间
     */
    public Story(
            String id,
            String title,
            List<String> tags,
            int totalChapters,
            int nextChapterSeq,
            String mainModel,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.title = title != null ? title : "";
        this.tags = tags != null ? List.copyOf(tags) : List.of();
        this.totalChapters = totalChapters;
        this.nextChapterSeq = nextChapterSeq;
        this.mainModel = mainModel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建草稿故事（章节计数与序号为 0）。
     *
     * @param id        故事 ID
     * @param title     标题
     * @param tags      标签，可为 null
     * @param mainModel 主模型
     * @param now       当前时间戳（写入创建/更新时间）
     * @return 草稿故事实例
     */
    public static Story newDraft(String id, String title, List<String> tags, String mainModel, Instant now) {
        return new Story(id, title, tags != null ? tags : new ArrayList<>(), 0, 0, mainModel, now, now);
    }

    /**
     * 在更新章节统计后返回新实例（不可变对象）。
     *
     * @param totalChapters  新的已生成章节总数
     * @param nextChapterSeq 新的下一章序号
     * @param updatedAt      新的最后更新时间
     * @return 携带更新计数的新实例
     */
    public Story withCounts(int totalChapters, int nextChapterSeq, Instant updatedAt) {
        return new Story(id, title, tags, totalChapters, nextChapterSeq, mainModel, createdAt, updatedAt);
    }

    /**
     * @return 故事主键 ID
     */
    public String getId() {
        return id;
    }

    /**
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 标签列表（不可变视图由构造时 copyOf 保证）
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @return 已生成章节总数
     */
    public int getTotalChapters() {
        return totalChapters;
    }

    /**
     * @return 下一章序号
     */
    public int getNextChapterSeq() {
        return nextChapterSeq;
    }

    /**
     * @return 主模型标识
     */
    public String getMainModel() {
        return mainModel;
    }

    /**
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return 最后更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

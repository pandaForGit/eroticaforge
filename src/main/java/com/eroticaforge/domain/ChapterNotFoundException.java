package com.eroticaforge.domain;

/**
 * 章节不存在时由 REST 抛出，映射为 HTTP 404。
 *
 * @author EroticaForge
 */
public final class ChapterNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param storyId   故事 ID
     * @param chapterId 章节 ID
     */
    public ChapterNotFoundException(String storyId, String chapterId) {
        super("章节不存在 storyId=" + storyId + " chapterId=" + chapterId);
    }
}

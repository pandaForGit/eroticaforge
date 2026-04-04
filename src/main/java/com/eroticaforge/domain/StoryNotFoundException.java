package com.eroticaforge.domain;

/**
 * REST 层在故事主键不存在时抛出，映射为 HTTP 404。
 *
 * @author EroticaForge
 */
public final class StoryNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param storyId 请求中的故事 ID
     */
    public StoryNotFoundException(String storyId) {
        super("故事不存在: " + storyId);
    }
}

package com.eroticaforge.application.dto;

/**
 * 生成后落库结果（阶段 4.4）。
 *
 * @param chapterId 新写入的章节主键 {@code erotica_chapters.id}
 * @param seq       该故事内章节序号 {@code erotica_chapters.seq}
 * @author EroticaForge
 */
public record PostGenerationResult(String chapterId, int seq) {}

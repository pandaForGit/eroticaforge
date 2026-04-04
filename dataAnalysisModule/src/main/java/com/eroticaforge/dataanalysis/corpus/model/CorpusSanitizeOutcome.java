package com.eroticaforge.dataanalysis.corpus.model;

/**
 * 单文件清洗结果：成功则携带 UTF-16 正文与检测到的源编码说明。
 *
 * @param utf16Text       供后续 LLM 使用的文本（逻辑上为 UTF-8/Unicode 字符串）
 * @param detectedCharset 解码时采用的字符集标签，如 UTF-8、GBK
 * @author EroticaForge
 */
public record CorpusSanitizeOutcome(String utf16Text, String detectedCharset) {}

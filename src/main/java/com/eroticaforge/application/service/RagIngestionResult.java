package com.eroticaforge.application.service;

/**
 * 文档/文本摄入结果摘要。
 *
 * @param documentId 写入 {@code erotica_documents} 的主键
 * @param chunkCount 实际写入向量库的文本块数量
 * @author EroticaForge
 */
public record RagIngestionResult(String documentId, int chunkCount) {}

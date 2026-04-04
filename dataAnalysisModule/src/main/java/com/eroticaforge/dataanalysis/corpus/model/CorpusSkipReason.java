package com.eroticaforge.dataanalysis.corpus.model;

/**
 * 清洗阶段跳过原因（用于日志统计，不落库）。
 *
 * @author EroticaForge
 */
public enum CorpusSkipReason {

    /** 非 .txt 扩展名（由遍历层处理，此处预留）。 */
    NOT_TXT,

    /** 解码失败。 */
    DECODE_FAILED,

    /** 清洗后过短。 */
    TOO_SHORT,

    /** 去广告等规则后无有效正文。 */
    EMPTY_AFTER_CLEAN
}

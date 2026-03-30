package com.eroticaforge.application.service;

/**
 * 写入 {@link dev.langchain4j.data.segment.TextSegment} 元数据时使用的键名（与检索过滤一致）。
 *
 * @author EroticaForge
 */
public final class RagMetadataKeys {

    /** 故事 ID，用于检索时限定范围。 */
    public static final String STORY_ID = "story_id";

    /** 文档记录 ID（对应 {@code erotica_documents.id}）。 */
    public static final String DOC_ID = "doc_id";

    /** 切块序号（从 0 开始）。 */
    public static final String CHUNK_INDEX = "chunk_index";

    /** 来源：{@code upload}、{@code text} 等。 */
    public static final String SOURCE = "source";

    /** {@link #SOURCE} 取值：文件上传。 */
    public static final String SOURCE_UPLOAD = "upload";

    /** {@link #SOURCE} 取值：纯文本粘贴。 */
    public static final String SOURCE_TEXT = "text";

    /** {@link #SOURCE} 取值：生成章节回写 RAG。 */
    public static final String SOURCE_CHAPTER = "chapter";

    /** {@link #SOURCE} 取值：数据分析模块产出的专题参考语料（见 {@code CorpusJsonlReferenceImporter}）。 */
    public static final String SOURCE_REFERENCE = "reference";

    /** 分类结果：主类型（与 JSONL {@code main_type} 一致）。 */
    public static final String CORPUS_MAIN_TYPE = "corpus_main_type";

    /** 分类结果：标签，逗号分隔（由 JSONL {@code tags} 数组拼接）。 */
    public static final String CORPUS_TAGS = "corpus_tags";

    /** 语料相对根路径（与 JSONL {@code source_relative_path} 一致）。 */
    public static final String CORPUS_RELATIVE_PATH = "corpus_relative_path";

    /** 语料标题（与 JSONL {@code title} 一致）。 */
    public static final String CORPUS_TITLE = "corpus_title";

    /**
     * 检索结果格式化：参考库段落标签的代码侧默认值；实际展示以 {@code erotica.prompt.rag.reference-chunk} 为准。
     */
    public static final String CONTEXT_TAG_REFERENCE = "参考";

    /**
     * 检索结果格式化：默认段落标签的代码侧默认值；实际展示以 {@code erotica.prompt.rag.recall-chunk} 为准。
     */
    public static final String CONTEXT_TAG_RECALL = "回忆";

    /** 元数据中的切块数量（写入 {@code erotica_documents.metadata}）。 */
    public static final String CHUNK_COUNT = "chunk_count";

    private RagMetadataKeys() {}
}

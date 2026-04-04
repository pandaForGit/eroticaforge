package com.eroticaforge.application.service;

import com.eroticaforge.config.RagProperties;
import com.eroticaforge.domain.Story;
import com.eroticaforge.domain.StoryState;
import com.eroticaforge.domain.UploadedDocument;
import com.eroticaforge.infrastructure.persistence.DocumentRepository;
import com.eroticaforge.infrastructure.persistence.StoryRepository;
import com.eroticaforge.infrastructure.persistence.StoryStateRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档/文本摄入：切分、嵌入、写入 LangChain PgVector 表，并记录 {@code erotica_documents} 元数据。
 *
 * @author EroticaForge
 */
@Service
@RequiredArgsConstructor
public class RagIngestionService {

    private static final Logger log = LoggerFactory.getLogger(RagIngestionService.class);

    /** 占位故事标题：仅用于满足 {@code erotica_documents} 对 {@code erotica_stories} 的外键。 */
    private static final String REFERENCE_CORPUS_STORY_TITLE = "专题参考语料（系统）";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final StoryRepository storyRepository;
    private final StoryStateRepository storyStateRepository;
    private final RagProperties ragProperties;

    /**
     * 上传文件：按 UTF-8 解码后走与 {@link #ingestText} 相同的切分与入库流程。
     *
     * @param storyId  故事 ID
     * @param file     上传文件
     * @param metadata 附加元数据（值将转为字符串写入 Document 元数据），可为 null
     * @return 文档 ID 与切块数量
     * @throws IOException 读取文件失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public RagIngestionResult ingestDocument(String storyId, MultipartFile file, Map<String, Object> metadata)
            throws IOException {
        String filename =
                file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                        ? file.getOriginalFilename()
                        : "upload.txt";
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.put("original_filename", filename);
        return ingestTextInternal(storyId, text, RagMetadataKeys.SOURCE_UPLOAD, filename, merged);
    }

    /**
     * 直接摄入纯文本（如粘贴大纲、人物卡）。
     *
     * @param storyId  故事 ID
     * @param text     全文
     * @param metadata 附加元数据，可为 null
     * @return 文档 ID 与切块数量
     */
    @Transactional(rollbackFor = Exception.class)
    public RagIngestionResult ingestText(String storyId, String text, Map<String, Object> metadata) {
        return ingestTextInternal(storyId, text, RagMetadataKeys.SOURCE_TEXT, "inline.txt", metadata);
    }

    /**
     * 将生成章节正文摄入向量库（来源标记为 {@link RagMetadataKeys#SOURCE_CHAPTER}）。
     *
     * @param storyId  故事 ID
     * @param text     章节全文
     * @param metadata 附加元数据（如 {@code chapter_id}），可为 null
     * @return 文档 ID 与切块数
     */
    @Transactional(rollbackFor = Exception.class)
    public RagIngestionResult ingestChapter(String storyId, String text, Map<String, Object> metadata) {
        return ingestTextInternal(storyId, text, RagMetadataKeys.SOURCE_CHAPTER, "chapter.txt", metadata);
    }

    /**
     * 将数据分析模块产出的语料（已分类）写入向量库：{@code source=reference}，使用固定的参考库 {@code storyId}。
     *
     * @param corpusStoryId {@link com.eroticaforge.config.RagProperties#getReferenceCorpusStoryId()}
     * @param text          全文
     * @param fileLabel     展示名（多为相对路径或标题）
     * @param metadata      分类字段等（如 {@link RagMetadataKeys#CORPUS_MAIN_TYPE}），可为 null
     * @return 文档 ID 与切块数
     */
    @Transactional(rollbackFor = Exception.class)
    public RagIngestionResult ingestCorpusReference(
            String corpusStoryId, String text, String fileLabel, Map<String, Object> metadata) {
        if (!StringUtils.hasText(corpusStoryId)) {
            throw new IllegalArgumentException("corpusStoryId 不能为空");
        }
        ensureReferenceCorpusStoryRow(corpusStoryId);
        String label = StringUtils.hasText(fileLabel) ? fileLabel : "corpus.txt";
        return ingestTextInternal(
                corpusStoryId, text, RagMetadataKeys.SOURCE_REFERENCE, label, metadata);
    }

    /**
     * 参考库文档行的 {@code story_id} 须存在于 {@code erotica_stories}（外键）；首次导入前插入占位故事与初始 StoryState。
     */
    private void ensureReferenceCorpusStoryRow(String storyId) {
        if (storyRepository.findById(storyId).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        Story placeholder = Story.newDraft(storyId, REFERENCE_CORPUS_STORY_TITLE, List.of(), null, now);
        try {
            storyRepository.insert(placeholder);
            storyStateRepository.insertInitialIfAbsent(StoryState.empty(storyId, now));
            log.info("已创建参考库占位故事 storyId={} title={}", storyId, REFERENCE_CORPUS_STORY_TITLE);
        } catch (DataIntegrityViolationException ex) {
            if (storyRepository.findById(storyId).isPresent()) {
                log.debug("参考库占位故事已存在（并发或重复创建）storyId={}", storyId);
                storyStateRepository.insertInitialIfAbsent(StoryState.empty(storyId, Instant.now()));
                return;
            }
            throw ex;
        }
    }

    /**
     * 切分、嵌入、写入向量库，再写入文档元数据表。
     *
     * @param storyId    故事 ID
     * @param text       正文
     * @param source     来源标识（{@code upload} / {@code text}）
     * @param fileLabel  写入 {@link UploadedDocument#getFileName()} 的展示名
     * @param userMeta   用户附加元数据，可为 null
     * @return 文档 ID 与切块数
     */
    private RagIngestionResult ingestTextInternal(
            String storyId,
            String text,
            String source,
            String fileLabel,
            Map<String, Object> userMeta) {
        if (!StringUtils.hasText(storyId)) {
            throw new IllegalArgumentException("storyId 不能为空");
        }
        if (text == null) {
            throw new IllegalArgumentException("text 不能为 null");
        }

        long t0 = System.nanoTime();
        String docId = UUID.randomUUID().toString();

        Metadata docLevel = new Metadata();
        putUserMetadata(docLevel, userMeta);

        Document document = Document.from(text, docLevel);
        DocumentSplitter splitter =
                DocumentSplitters.recursive(ragProperties.getChunkSizeChars(), ragProperties.getChunkOverlapChars());
        List<TextSegment> pieces = splitter.split(document);

        List<TextSegment> tagged = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            TextSegment piece = pieces.get(i);
            if (!StringUtils.hasText(piece.text())) {
                continue;
            }
            Metadata meta = piece.metadata().copy();
            meta.put(RagMetadataKeys.STORY_ID, storyId);
            meta.put(RagMetadataKeys.DOC_ID, docId);
            meta.put(RagMetadataKeys.SOURCE, source);
            meta.put(RagMetadataKeys.CHUNK_INDEX, i);
            tagged.add(TextSegment.from(piece.text(), meta));
        }

        if (!tagged.isEmpty()) {
            Response<List<Embedding>> embedResponse = embeddingModel.embedAll(tagged);
            embeddingStore.addAll(embedResponse.content(), tagged);
        }

        Map<String, Object> rowMeta = new LinkedHashMap<>();
        if (userMeta != null) {
            rowMeta.putAll(userMeta);
        }
        rowMeta.put(RagMetadataKeys.SOURCE, source);
        rowMeta.put(RagMetadataKeys.CHUNK_COUNT, tagged.size());

        documentRepository.insert(new UploadedDocument(docId, storyId, fileLabel, rowMeta, Instant.now()));

        long ms = (System.nanoTime() - t0) / 1_000_000L;
        log.info("RAG 摄入完成 storyId={} docId={} chunks={} 耗时{}ms", storyId, docId, tagged.size(), ms);

        return new RagIngestionResult(docId, tagged.size());
    }

    /**
     * 将用户 Map 中的项以字符串形式写入 {@link Metadata}（跳过 null 键值）。
     *
     * @param target 目标元数据
     * @param user   用户附加键值，可为 null
     */
    private static void putUserMetadata(Metadata target, Map<String, Object> user) {
        if (user == null) {
            return;
        }
        user.forEach(
                (key, value) -> {
                    if (key != null && !key.isBlank() && value != null) {
                        target.put(key, value.toString());
                    }
                });
    }
}

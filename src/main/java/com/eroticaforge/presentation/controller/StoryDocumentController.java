package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.DocumentListItemDto;
import com.eroticaforge.application.dto.api.DocumentUploadResponse;
import com.eroticaforge.application.service.RagIngestionService;
import com.eroticaforge.application.service.RagMetadataKeys;
import com.eroticaforge.application.service.StoryAccessService;
import com.eroticaforge.domain.UploadedDocument;
import com.eroticaforge.infrastructure.persistence.DocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /api/stories/{storyId}/documents。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/stories/{storyId}/documents")
public class StoryDocumentController {

    private static final String EXT_PDF = ".pdf";

    private final StoryAccessService storyAccessService;
    private final RagIngestionService ragIngestionService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    /**
     * @param storyAccessService   故事存在性
     * @param ragIngestionService  摄入服务
     * @param documentRepository     文档元数据
     * @param objectMapper           解析 metadata JSON
     */
    public StoryDocumentController(
            StoryAccessService storyAccessService,
            RagIngestionService ragIngestionService,
            DocumentRepository documentRepository,
            ObjectMapper objectMapper) {
        this.storyAccessService = storyAccessService;
        this.ragIngestionService = ragIngestionService;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 上传文本类文档并写入向量库与 {@code erotica_documents}。
     *
     * @param storyId      故事 ID
     * @param file         上传文件（当前仅建议 .txt，UTF-8）
     * @param metadataJson 可选 JSON 字符串，合并为业务 metadata
     * @return docId、切块数等
     * @throws IOException 读取文件失败
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable String storyId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "metadata", required = false) String metadataJson)
            throws IOException {
        storyAccessService.requireStory(storyId);
        String original = file.getOriginalFilename();
        if (original != null && original.toLowerCase(Locale.ROOT).endsWith(EXT_PDF)) {
            throw new IllegalArgumentException("当前版本仅支持 UTF-8 文本（.txt），请勿上传 PDF");
        }
        Map<String, Object> meta = parseMetadata(metadataJson);
        var result = ragIngestionService.ingestDocument(storyId, file, meta);
        String name =
                file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                        ? file.getOriginalFilename()
                        : "upload.txt";
        return ApiResponse.ok(
                new DocumentUploadResponse(
                        result.documentId(), name, result.chunkCount(), "indexed"));
    }

    /**
     * 已上传文档列表。
     *
     * @param storyId 故事 ID
     * @return 列表
     */
    @GetMapping
    public ApiResponse<List<DocumentListItemDto>> list(@PathVariable String storyId) {
        storyAccessService.requireStory(storyId);
        List<DocumentListItemDto> list =
                documentRepository.findByStoryIdOrderByCreatedAt(storyId).stream()
                        .map(StoryDocumentController::toListItem)
                        .toList();
        return ApiResponse.ok(list);
    }

    private Map<String, Object> parseMetadata(String metadataJson) throws IOException {
        if (!StringUtils.hasText(metadataJson)) {
            return new LinkedHashMap<>();
        }
        return objectMapper.readValue(metadataJson.strip(), new TypeReference<>() {});
    }

    private static DocumentListItemDto toListItem(UploadedDocument d) {
        return new DocumentListItemDto(
                d.getId(),
                d.getFileName(),
                chunkCountOf(d),
                d.getCreatedAt(),
                d.getMetadata());
    }

    private static int chunkCountOf(UploadedDocument d) {
        Object c = d.getMetadata().get(RagMetadataKeys.CHUNK_COUNT);
        if (c instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}

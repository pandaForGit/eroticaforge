package com.eroticaforge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eroticaforge.domain.UploadedDocument;
import com.eroticaforge.infrastructure.persistence.entity.DocumentEntity;
import com.eroticaforge.infrastructure.persistence.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 上传文档元数据持久化（MyBatis-Plus）。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    /** 文档元数据表 Mapper。 */
    private final DocumentMapper documentMapper;

    /**
     * 插入文档元数据。
     *
     * @param doc 领域对象
     */
    public void insert(UploadedDocument doc) {
        documentMapper.insert(DocumentEntity.from(doc));
    }

    /**
     * 按故事 ID 与文档主键查询。
     *
     * @param storyId    故事 ID
     * @param documentId 文档记录主键
     * @return 存在则返回领域对象，否则 empty
     */
    public Optional<UploadedDocument> findById(String storyId, String documentId) {
        DocumentEntity row =
                documentMapper.selectOne(
                        Wrappers.<DocumentEntity>lambdaQuery()
                                .eq(DocumentEntity::getStoryId, storyId)
                                .eq(DocumentEntity::getId, documentId));
        return row == null ? Optional.empty() : Optional.of(row.toDomain());
    }

    /**
     * 列出某故事下全部文档元数据，按创建时间升序。
     *
     * @param storyId 故事 ID
     * @return 文档列表（无数据时为空列表）
     */
    public List<UploadedDocument> findByStoryIdOrderByCreatedAt(String storyId) {
        return documentMapper
                .selectList(
                        Wrappers.<DocumentEntity>lambdaQuery()
                                .eq(DocumentEntity::getStoryId, storyId)
                                .orderByAsc(DocumentEntity::getCreatedAt))
                .stream()
                .map(DocumentEntity::toDomain)
                .toList();
    }

    /**
     * 按故事 ID 与文档主键删除。
     *
     * @param storyId    故事 ID
     * @param documentId 文档记录主键
     */
    public void deleteById(String storyId, String documentId) {
        documentMapper.delete(
                Wrappers.<DocumentEntity>lambdaQuery()
                        .eq(DocumentEntity::getStoryId, storyId)
                        .eq(DocumentEntity::getId, documentId));
    }
}

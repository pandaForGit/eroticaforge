package com.eroticaforge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eroticaforge.domain.Chapter;
import com.eroticaforge.infrastructure.persistence.entity.ChapterEntity;
import com.eroticaforge.infrastructure.persistence.mapper.ChapterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 章节持久化（MyBatis-Plus）。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class ChapterRepository {

    /** 章节表 Mapper。 */
    private final ChapterMapper chapterMapper;

    /**
     * 插入章节。
     *
     * @param chapter 领域对象
     */
    public void insert(Chapter chapter) {
        chapterMapper.insert(ChapterEntity.from(chapter));
    }

    /**
     * 按主键更新章节（限定同一 {@code storyId}）。
     *
     * @param chapter 领域对象
     */
    public void update(Chapter chapter) {
        chapterMapper.updateById(ChapterEntity.from(chapter));
    }

    /**
     * 按故事 ID 与章节主键查询。
     *
     * @param storyId   故事 ID
     * @param chapterId 章节 ID
     * @return 存在则返回领域对象，否则 empty
     */
    public Optional<Chapter> findById(String storyId, String chapterId) {
        ChapterEntity row =
                chapterMapper.selectOne(
                        Wrappers.<ChapterEntity>lambdaQuery()
                                .eq(ChapterEntity::getStoryId, storyId)
                                .eq(ChapterEntity::getId, chapterId));
        return row == null ? Optional.empty() : Optional.of(row.toDomain());
    }

    /**
     * 按故事 ID 与章节序号查询。
     *
     * @param storyId 故事 ID
     * @param seq     章节序号
     * @return 存在则返回领域对象，否则 empty
     */
    public Optional<Chapter> findByStoryIdAndSeq(String storyId, int seq) {
        ChapterEntity row =
                chapterMapper.selectOne(
                        Wrappers.<ChapterEntity>lambdaQuery()
                                .eq(ChapterEntity::getStoryId, storyId)
                                .eq(ChapterEntity::getSeq, seq));
        return row == null ? Optional.empty() : Optional.of(row.toDomain());
    }

    /**
     * 列出某故事下全部章节，按 {@code seq} 升序。
     *
     * @param storyId 故事 ID
     * @return 章节列表（无数据时为空列表）
     */
    public List<Chapter> findByStoryIdOrderBySeq(String storyId) {
        return chapterMapper
                .selectList(
                        Wrappers.<ChapterEntity>lambdaQuery()
                                .eq(ChapterEntity::getStoryId, storyId)
                                .orderByAsc(ChapterEntity::getSeq))
                .stream()
                .map(ChapterEntity::toDomain)
                .toList();
    }

    /**
     * 按故事 ID 与章节主键删除。
     *
     * @param storyId   故事 ID
     * @param chapterId 章节 ID
     */
    public void deleteById(String storyId, String chapterId) {
        chapterMapper.delete(
                Wrappers.<ChapterEntity>lambdaQuery()
                        .eq(ChapterEntity::getStoryId, storyId)
                        .eq(ChapterEntity::getId, chapterId));
    }
}

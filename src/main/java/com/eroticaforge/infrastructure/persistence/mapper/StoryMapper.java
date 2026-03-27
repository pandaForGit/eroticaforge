package com.eroticaforge.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eroticaforge.infrastructure.persistence.entity.StoryEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * {@code erotica_stories} Mapper。
 *
 * <p>除下列自定义 SQL 外，通用 CRUD 的参数与返回值语义同 {@link BaseMapper}。
 *
 * @author EroticaForge
 */
public interface StoryMapper extends BaseMapper<StoryEntity> {

    /**
     * 原子自增 {@code next_chapter_seq}，返回新值；无行更新时返回 null。
     *
     * @param id 故事主键
     * @return 自增后的序号，未更新任何行时为 null
     */
    @Select(
            """
            UPDATE erotica_stories
            SET next_chapter_seq = next_chapter_seq + 1, updated_at = now()
            WHERE id = #{id}
            RETURNING next_chapter_seq
            """)
    Integer allocateNextChapterSeq(@Param("id") String id);

    /**
     * 已生成章节总数 +1。
     *
     * @param id 故事主键
     * @return 影响行数
     */
    @Update(
            "UPDATE erotica_stories SET total_chapters = total_chapters + 1, updated_at = now() WHERE id = #{id}")
    int incrementTotalChapters(@Param("id") String id);
}

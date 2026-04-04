package com.eroticaforge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eroticaforge.infrastructure.persistence.entity.CharacterLibraryEntity;
import com.eroticaforge.infrastructure.persistence.mapper.CharacterLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 人物卡库持久化。
 *
 * @author EroticaForge
 */
@Repository
@RequiredArgsConstructor
public class CharacterLibraryRepository {

    private final CharacterLibraryMapper mapper;

    /**
     * 按主键查询。
     *
     * @param id 库卡 ID
     * @return optional
     */
    public Optional<CharacterLibraryEntity> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /**
     * 批量按主键查询（无顺序保证）。
     *
     * @param ids ID 集合
     * @return 列表
     */
    public List<CharacterLibraryEntity> findAllByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }

    /**
     * 按调用方给定 ID 顺序返回库卡；若有缺失则结果长度小于 ids。
     *
     * @param orderedIds 有序 ID
     * @return 与 orderedIds 等长时表示全部存在
     */
    public List<CharacterLibraryEntity> findAllByIdsPreserveOrder(List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            return List.of();
        }
        List<CharacterLibraryEntity> rows = findAllByIds(orderedIds);
        Map<String, CharacterLibraryEntity> map =
                rows.stream().collect(Collectors.toMap(CharacterLibraryEntity::getId, Function.identity()));
        return orderedIds.stream().map(map::get).collect(Collectors.toList());
    }

    /**
     * 列表：可选关键词模糊匹配展示名或源路径，按更新时间倒序，条数上限。
     *
     * @param query  关键词，可空
     * @param maxRows 最大行数（建议 1～500）
     * @return 列表
     */
    public List<CharacterLibraryEntity> search(String query, int maxRows) {
        int limit = Math.min(Math.max(maxRows, 1), 500);
        var w = Wrappers.<CharacterLibraryEntity>lambdaQuery().orderByDesc(CharacterLibraryEntity::getUpdatedAt);
        if (StringUtils.hasText(query)) {
            String q = query.strip();
            String like = "%" + q + "%";
            w.and(
                    x ->
                            x.like(CharacterLibraryEntity::getDisplayName, like)
                                    .or()
                                    .like(CharacterLibraryEntity::getSourceRelativePath, like));
        }
        w.last("LIMIT " + limit);
        return mapper.selectList(w);
    }

    /**
     * 插入或更新（按 content_sha256 + role_index 查找已存在行）。
     *
     * @param row 实体（须含 {@code contentSha256}、{@code roleIndex}）
     * @return {@code true} 表示新插入，{@code false} 表示更新
     */
    public boolean insertOrUpdateByShaAndRole(CharacterLibraryEntity row) {
        CharacterLibraryEntity existing =
                mapper.selectOne(
                        Wrappers.<CharacterLibraryEntity>lambdaQuery()
                                .eq(CharacterLibraryEntity::getContentSha256, row.getContentSha256())
                                .eq(CharacterLibraryEntity::getRoleIndex, row.getRoleIndex()));
        if (existing == null) {
            mapper.insert(row);
            return true;
        }
        row.setId(existing.getId());
        row.setCreatedAt(existing.getCreatedAt());
        mapper.updateById(row);
        return false;
    }
}

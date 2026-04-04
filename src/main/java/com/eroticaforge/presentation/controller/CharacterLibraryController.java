package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.CharacterLibraryItemDto;
import com.eroticaforge.infrastructure.persistence.CharacterLibraryRepository;
import com.eroticaforge.infrastructure.persistence.entity.CharacterLibraryEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/character-library：人物卡库列表与简单搜索。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/character-library")
public class CharacterLibraryController {

    private final CharacterLibraryRepository characterLibraryRepository;

    /**
     * @param characterLibraryRepository 人物卡库仓储
     */
    public CharacterLibraryController(CharacterLibraryRepository characterLibraryRepository) {
        this.characterLibraryRepository = characterLibraryRepository;
    }

    /**
     * 列表：按关键词模糊匹配展示名或源路径，按更新时间倒序，条数有上限。
     *
     * @param query 关键词，可空
     * @param limit 最大条数，默认 200，上限 500
     * @return data 为列表
     */
    @GetMapping
    public ApiResponse<List<CharacterLibraryItemDto>> list(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "200") int limit) {
        List<CharacterLibraryEntity> rows = characterLibraryRepository.search(query, limit);
        List<CharacterLibraryItemDto> dtos =
                rows.stream()
                        .map(
                                e ->
                                        new CharacterLibraryItemDto(
                                                e.getId(),
                                                e.getDisplayName(),
                                                e.getSourceRelativePath(),
                                                e.getSchemaVersion(),
                                                e.getContentSha256(),
                                                e.getRoleIndex()))
                        .toList();
        return ApiResponse.ok(dtos);
    }
}

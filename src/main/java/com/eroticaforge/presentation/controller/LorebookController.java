package com.eroticaforge.presentation.controller;

import com.eroticaforge.application.dto.api.ApiResponse;
import com.eroticaforge.application.dto.api.LorebookCreateRequest;
import com.eroticaforge.application.dto.api.LorebookItemDto;
import com.eroticaforge.application.service.LorebookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET/POST /api/lorebook（当前内存存储，重启丢失）。
 *
 * @author EroticaForge
 */
@RestController
@RequestMapping("/api/lorebook")
public class LorebookController {

    private final LorebookService lorebookService;

    /**
     * @param lorebookService Lorebook 服务
     */
    public LorebookController(LorebookService lorebookService) {
        this.lorebookService = lorebookService;
    }

    /**
     * 列出全部条目。
     *
     * @return data 为列表
     */
    @GetMapping
    public ApiResponse<List<LorebookItemDto>> list() {
        return ApiResponse.ok(lorebookService.listAll());
    }

    /**
     * 新增条目。
     *
     * @param request 关键词与正文
     * @return 新建条目
     */
    @PostMapping
    public ApiResponse<LorebookItemDto> create(@RequestBody LorebookCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        LorebookItemDto created =
                lorebookService.addEntry(request.keyword(), request.body());
        return ApiResponse.ok(created);
    }
}

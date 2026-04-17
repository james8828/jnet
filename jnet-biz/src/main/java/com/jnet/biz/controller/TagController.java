package com.jnet.biz.controller;

import com.jnet.biz.dto.BatchAssignTagsDTO;
import com.jnet.biz.entity.Tag;
import com.jnet.biz.service.ITagService;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签管理 Controller
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理", description = "病理图像标签相关接口")
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;

    /**
     * 获取标签树形结构
     */
    @Operation(summary = "获取标签树", description = "获取标签的树形结构，支持按分类筛选")
    @GetMapping("/tree")
    public Result<String> getTagTree(
            @Parameter(description = "标签分类", example = "组织类型") @RequestParam(required = false) String category) {
        String tree = tagService.getTagTree(category);
        return Result.success(tree);
    }

    /**
     * 获取所有标签
     */
    @Operation(summary = "获取标签列表", description = "获取所有标签（不分页）")
    @GetMapping
    public Result<List<Tag>> listTags() {
        List<Tag> tags = tagService.list();
        return Result.success(tags);
    }

    /**
     * 创建标签
     */
    @Operation(summary = "创建标签", description = "创建新的病理图像标签")
    @PostMapping
    public Result<Tag> createTag(
            @Parameter(description = "标签信息", required = true) @RequestBody Tag tag) {
        boolean success = tagService.save(tag);
        if (success) {
            return Result.success("创建成功", tag);
        } else {
            return Result.error("创建失败");
        }
    }

    /**
     * 批量给资产打标
     */
    @Operation(summary = "批量打标", description = "批量给多个资产添加标签")
    @PostMapping("/batch-assign")
    public Result<Void> batchAssignTags(
            @Parameter(description = "打标信息", required = true) @RequestBody @Validated BatchAssignTagsDTO dto) {
        boolean success = tagService.batchAssignTags(dto.getAssetIds(), dto.getTagIds());
        if (success) {
            return Result.success("打标成功", null);
        } else {
            return Result.error("打标失败");
        }
    }
}

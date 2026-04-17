package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "标签查询条件")
public class TagQueryDTO extends PageQueryDTO {

    /**
     * 标签名称（模糊查询）
     */
    @Schema(description = "标签名称（模糊查询）", example = "肿瘤")
    private String name;

    /**
     * 标签分类
     */
    @Schema(description = "标签分类", example = "组织类型")
    private String category;

    /**
     * 父标签ID
     */
    @Schema(description = "父标签ID", example = "1")
    private Long parentId;

    /**
     * 是否系统标签
     */
    @Schema(description = "是否系统标签", example = "false")
    private Boolean isSystem;
}

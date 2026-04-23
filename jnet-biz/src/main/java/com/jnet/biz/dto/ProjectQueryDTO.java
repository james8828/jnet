package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "项目查询条件")
public class ProjectQueryDTO extends PageQueryDTO {

    /**
     * 项目名称（模糊查询）
     */
    @Schema(description = "项目名称（模糊查询）", example = "肺癌")
    private String name;

    /**
     * 项目编码（精确查询）
     */
    @Schema(description = "项目编码（精确查询）", example = "PROJ-2024-001")
    private String code;

    /**
     * 项目状态（active/archived/deleted）
     */
    @Schema(description = "项目状态", example = "active")
    private String status;

    /**
     * 负责人ID
     */
    @Schema(description = "负责人ID", example = "1001")
    private Long managerId;

    /**
     * 隐私级别
     */
    @Schema(description = "隐私级别", example = "PUBLIC")
    private Integer privacyLevel;
}

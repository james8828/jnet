package com.jnet.biz.dto;

import com.jnet.biz.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 项目创建/更新 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "项目创建/更新请求")
public class ProjectDTO {

    /**
     * 项目ID（更新时必填）
     */
    @Schema(description = "项目ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long projectId;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称", example = "肺癌病理分析项目", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 项目编码
     */
    @NotBlank(message = "项目编码不能为空")
    @Schema(description = "项目编码（唯一）", example = "PROJ-2024-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    /**
     * 负责人ID
     */
    @Schema(description = "负责人ID", example = "1001")
    private Long managerId;

    /**
     * 伦理批件号
     */
    @Schema(description = "伦理批件号", example = "ETHICS-2024-001")
    private String ethicsCode;

    /**
     * 隐私级别（1:公开, 2:脱敏, 3:绝密）
     */
    @Schema(description = "隐私级别", example = "1", allowableValues = {"1", "2", "3"})
    private Integer privacyLevel;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述", example = "基于深度学习的肺癌病理图像分析")
    private String description;

    /**
     * 目标检测类别配置 (JSON)
     */
    @Schema(description = "目标检测类别配置（JSON格式）", example = "{\"classes\":[\"tumor\",\"normal\"]}")
    private String targetClasses;

    /**
     * 状态（active/archived/deleted）
     */
    @Schema(description = "项目状态", example = "active", allowableValues = {"active", "archived", "deleted"})
    private String status;
}

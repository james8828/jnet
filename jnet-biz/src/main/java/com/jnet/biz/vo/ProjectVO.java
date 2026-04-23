package com.jnet.biz.vo;

import com.jnet.biz.enums.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目 VO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "项目信息响应")
public class ProjectVO {

    /**
     * 项目ID
     */
    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    /**
     * 项目名称
     */
    @Schema(description = "项目名称", example = "肺癌病理分析项目")
    private String name;

    /**
     * 项目编码
     */
    @Schema(description = "项目编码", example = "PROJ-2024-001")
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
    @Schema(description = "隐私级别", example = "1")
    private Integer privacyLevel;

    /**
     * 项目描述
     */
    @Schema(description = "项目描述", example = "基于深度学习的肺癌病理图像分析")
    private String description;

    /**
     * 目标检测类别配置
     */
    @Schema(description = "目标检测类别配置（JSON格式）")
    private String targetClasses;

    /**
     * 状态（active/archived/deleted）
     */
    @Schema(description = "项目状态", example = "active")
    private String status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-04-16T10:30:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2024-04-16T15:45:00")
    private LocalDateTime updateTime;
}

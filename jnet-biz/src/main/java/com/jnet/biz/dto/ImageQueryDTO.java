package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 图像查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "图像查询条件")
public class ImageQueryDTO extends PageQueryDTO {

    /**
     * 所属批次ID
     */
    @Schema(description = "所属批次ID", example = "1")
    private Long batchId;

    /**
     * 所属项目ID（通过批次关联）
     */
    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    /**
     * 生命周期状态（Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived）
     */
    @Schema(description = "生命周期状态", example = "Annotated")
    private String lifecycleStatus;

    /**
     * 病理报告号（模糊查询）
     */
    @Schema(description = "病理报告号（模糊查询）", example = "PATH-2024")
    private String pathologyId;

    /**
     * 患者ID（模糊查询）
     */
    @Schema(description = "患者ID（模糊查询）", example = "PAT-001")
    private String patientId;

    /**
     * 图像格式
     */
    @Schema(description = "图像格式", example = "SVS")
    private String format;

    /**
     * 标签ID列表（多标签筛选）
     */
    @Schema(description = "标签ID列表", example = "[1, 2, 3]")
    private List<Long> tagIds;

    /**
     * 标注进度最小值
     */
    @Schema(description = "标注进度最小值（0-100）", example = "50", minimum = "0", maximum = "100")
    private Integer minAnnotationProgress;

    /**
     * 标注进度最大值
     */
    @Schema(description = "标注进度最大值（0-100）", example = "100", minimum = "0", maximum = "100")
    private Integer maxAnnotationProgress;
}

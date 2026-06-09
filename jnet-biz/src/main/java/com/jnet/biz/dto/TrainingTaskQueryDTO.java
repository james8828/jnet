package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 训练任务查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "训练任务查询条件")
public class TrainingTaskQueryDTO extends PageQueryDTO {

    /**
     * 项目ID
     */
    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    /**
     * 任务状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）
     */
    @Schema(description = "任务状态", example = "SUCCESS", allowableValues = {"PENDING", "RUNNING", "SUCCESS", "FAILED", "CANCELLED"})
    private String status;

    /**
     * 任务名称（模糊查询）
     */
    @Schema(description = "任务名称（模糊查询）", example = "病理图像检测模型训练")
    private String taskName;

    /**
     * 模型架构（YOLOv5/YOLOv7/YOLOv8等）
     */
    @Schema(description = "模型架构", example = "YOLOv7")
    private String modelArchitecture;

    /**
     * 数据集任务ID
     */
    @Schema(description = "数据集任务ID", example = "19")
    private Long datasetTaskId;
}

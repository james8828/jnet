package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据集构建任务查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据集构建任务查询条件")
public class DatasetTaskQueryDTO extends PageQueryDTO {

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
    @Schema(description = "任务名称（模糊查询）", example = "病理图像检测")
    private String taskName;

    /**
     * 当前步骤（模糊查询）
     */
    @Schema(description = "当前步骤（模糊查询）", example = "生成YOLO格式标注文件")
    private String currentStep;

    /**
     * 算法类型（YOLO/RCNN等）
     */
    @Schema(description = "算法类型", example = "YOLO")
    private String algorithmType;
}

package com.jnet.api.yolo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 训练任务状态DTO
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "训练任务状态")
public class TrainingTaskStatus implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务ID
     */
    @Schema(description = "任务ID", example = "task_20240513_001")
    private String taskId;
    
    /**
     * 任务状态: pending/running/completed/failed/cancelled
     */
    @Schema(description = "任务状态", example = "running", allowableValues = {"pending", "running", "completed", "failed", "cancelled"})
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    @Schema(description = "进度百分比", example = "45.5")
    private Double progress;
    
    /**
     * 当前epoch
     */
    @Schema(description = "当前epoch", example = "135")
    private Integer currentEpoch;
    
    /**
     * 总epoch数
     */
    @Schema(description = "总epoch数", example = "300")
    private Integer totalEpochs;
    
    /**
     * 训练指标（mAP, loss等）
     */
    @Schema(description = "训练指标")
    private Map<String, Object> metrics;
    
    /**
     * 错误信息（如果失败）
     */
    @Schema(description = "错误信息")
    private String errorMessage;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;
    
    /**
     * 模型输出路径
     */
    @Schema(description = "模型输出路径", example = "/path/to/runs/train/exp/weights/best.pt")
    private String modelPath;
}

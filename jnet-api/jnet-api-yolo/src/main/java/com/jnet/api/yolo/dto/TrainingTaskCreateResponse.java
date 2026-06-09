package com.jnet.api.yolo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 训练任务创建响应DTO
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "训练任务创建响应")
public class TrainingTaskCreateResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否成功
     */
    @Schema(description = "是否成功", example = "true")
    private Boolean success;
    
    /**
     * 任务ID
     */
    @Schema(description = "任务ID", example = "task_20240513_001")
    private String taskId;
    
    /**
     * 消息
     */
    @Schema(description = "消息", example = "训练任务已创建，请调用 /start 接口启动训练")
    private String message;
}

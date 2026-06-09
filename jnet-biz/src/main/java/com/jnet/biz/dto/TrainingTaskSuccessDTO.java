package com.jnet.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练任务成功结果 DTO
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingTaskSuccessDTO {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 模型ID（可选，后续注册时设置）
     */
    private Long modelId;
    
    /**
     * 模型路径
     */
    private String modelPath;
    
    /**
     * 最佳模型路径
     */
    private String bestModelPath;
    
    /**
     * 评估结果（JSON格式）
     */
    private String evaluationResults;
}

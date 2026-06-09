package com.jnet.biz.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 算法任务消息DTO
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class AlgorithmTaskMessage implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID（用于幂等性检查）
     */
    private String messageId;
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 任务编号
     */
    private String taskNo;
    
    /**
     * 算法类型 (YOLO/SAM/CLASSIFICATION)
     */
    private String algorithmType;
    
    /**
     * 任务类型 (DATASET_BUILD/TRAINING/PREDICTION)
     */
    private String taskType;
    
    /**
     * 项目ID
     */
    private Long projectId;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    /**
     * 配置参数（JSON字符串）
     */
    private String configJson;
    
    /**
     * 重试次数
     */
    private Integer retryCount = 0;
    
    /**
     * 创建时间戳
     */
    private Long timestamp;
}

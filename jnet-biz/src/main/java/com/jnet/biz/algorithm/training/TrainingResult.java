package com.jnet.biz.algorithm.training;

import lombok.Data;

import java.util.Map;

/**
 * 训练结果
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class TrainingResult {
    private String modelPath;           // 最终模型路径
    private String bestModelPath;       // 最佳模型路径
    private Long modelSize;             // 模型大小
    private Map<String, Object> metrics; // 训练指标
    private Map<String, Object> bestMetrics; // 最佳指标
    private Integer totalEpochs;        // 总训练轮数
    private Integer completedEpochs;    // 已完成轮数
    private Long trainingTimeSeconds;   // 训练耗时（秒）
    private Long trainingDuration;      // 训练耗时（毫秒，兼容Consumer）
    private String logsPath;            // 日志路径
    private String tensorboardPath;     // TensorBoard日志路径
    private String configFilePath;      // 配置文件路径
    private Object finalMetrics;        // 最终指标（具体类型由实现类决定）
    private EvaluationResult evaluation;  // 评估结果
}

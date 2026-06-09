package com.jnet.biz.algorithm.training;

import lombok.Data;

import java.util.Map;

/**
 * 评估结果
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class EvaluationResult {
    private Float map50;                // mAP@0.5
    private Float map50_95;             // mAP@0.5:0.95
    private Float precision;            // 精确率
    private Float recall;               // 召回率
    private Float f1Score;              // F1分数
    private Object confusionMatrix;     // 混淆矩阵
    private String prCurvePath;         // PR曲线图路径
    private Map<String, Float> perClassMetrics; // 各类别指标
}

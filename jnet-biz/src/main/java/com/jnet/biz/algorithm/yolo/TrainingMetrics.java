package com.jnet.biz.algorithm.yolo;

import lombok.Data;

/**
 * 训练指标
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class TrainingMetrics {
    private Float map50;      // mAP@0.5
    private Float map5095;    // mAP@0.5:0.95
    private Float boxLoss;    // 边界框损失
    private Float clsLoss;    // 分类损失
    private Float objLoss;    // 目标性损失
}

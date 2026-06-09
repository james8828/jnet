package com.jnet.biz.algorithm.training;

import com.jnet.biz.algorithm.config.AlgorithmConfig;
import lombok.Data;

/**
 * 训练配置基类
 * 包含所有模型训练的通用配置项
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public abstract class TrainingConfig implements AlgorithmConfig {
    // ========== 数据源配置 ==========
    private String datasetPath;           // 数据集路径（必需）
    private String customDatasetPath;     // 自定义数据集路径
    
    // ========== 训练超参数 ==========
    private Integer epochs;               // 训练轮数
    private Integer batchSize;            // 批次大小
    private Integer imageSize;            // 图像尺寸
    private Float learningRate;           // 学习率
    private Float momentum;               // 动量
    private Float weightDecay;            // 权重衰减
    private String optimizer;             // 优化器 (SGD/Adam/AdamW)
    private String lrScheduler;           // 学习率调度器
    private Integer warmupEpochs;         // 预热轮数
    private Integer patience;             // 早停耐心值
    
    // ========== 数据增强配置 ==========
    private Float hsvH;                   // HSV色调增强
    private Float hsvS;                   // HSV饱和度增强
    private Float hsvV;                   // HSV亮度增强
    private Float degrees;                // 旋转角度
    private Float translate;              // 平移
    private Float scale;                  // 缩放
    private Float shear;                  // 剪切
    private Float perspective;            // 透视
    private Boolean flipLr;               // 水平翻转
    private Boolean flipUd;               // 垂直翻转
    
    // ========== 硬件配置 ==========
    private String gpuIds;                // GPU设备ID (0,1,2或cpu)
    private Integer numWorkers;           // 数据加载线程数
    private Boolean mixedPrecision;       // 混合精度训练
    
    // ========== 其他配置 ==========
    private String projectName;           // 项目名称
    private String experimentName;        // 实验名称
    private Boolean savePeriodic;         // 定期保存模型
    private Integer saveInterval;         // 保存间隔（轮数）
    
    /**
     * 基础验证逻辑（子类可以调用super.validate()扩展）
     */
    @Override
    public void validate() {
        if (datasetPath == null || datasetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("数据集路径不能为空");
        }
        
        if (epochs != null && epochs <= 0) {
            throw new IllegalArgumentException("训练轮数必须大于0");
        }
        
        if (batchSize != null && batchSize <= 0) {
            throw new IllegalArgumentException("批次大小必须大于0");
        }
        
        if (imageSize != null && imageSize <= 0) {
            throw new IllegalArgumentException("图像尺寸必须大于0");
        }
    }
}

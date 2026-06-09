package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * YOLO模型训练任务实体
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_yolo_training_task")
public class YoloTrainingTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /**
     * 任务编号（唯一）
     */
    private String taskNo;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 关联的数据集构建任务ID
     */
    private Long datasetTaskId;

    /**
     * 数据集路径
     */
    private String datasetPath;

    /**
     * 自定义数据集路径
     */
    private String customDatasetPath;

    /**
     * 数据集配置快照（JSON）
     */
    private String datasetConfig;

    /**
     * 模型架构 (yolov8n/s/m/l/x)
     */
    private String modelArchitecture;

    /**
     * 预训练权重 (coco/imagenet/custom)
     */
    private String pretrainedWeights;

    /**
     * 训练轮数
     */
    private Integer epochs;

    /**
     * 批次大小
     */
    private Integer batchSize;

    /**
     * 图像尺寸
     */
    private Integer imageSize;

    /**
     * 学习率
     */
    private Float learningRate;

    /**
     * 动量
     */
    private Float momentum;

    /**
     * 权重衰减
     */
    private Float weightDecay;

    /**
     * 优化器 (SGD/Adam/AdamW)
     */
    private String optimizer;

    /**
     * 学习率调度器
     */
    private String lrScheduler;

    /**
     * 预热轮数
     */
    private Integer warmupEpochs;

    /**
     * 早停耐心值
     */
    private Integer patience;

    /**
     * 额外参数（JSON）
     */
    private String additionalParams;

    /**
     * 数据增强配置（JSON）
     */
    private String augmentationConfig;

    /**
     * HSV色调增强
     */
    private Float hsvH;

    /**
     * HSV饱和度增强
     */
    private Float hsvS;

    /**
     * HSV亮度增强
     */
    private Float hsvV;

    /**
     * 旋转角度
     */
    private Float degrees;

    /**
     * 平移
     */
    private Float translate;

    /**
     * 缩放
     */
    private Float scale;

    /**
     * 剪切
     */
    private Float shear;

    /**
     * 透视
     */
    private Float perspective;

    /**
     * 水平翻转
     */
    private Boolean flipLr;

    /**
     * 垂直翻转
     */
    private Boolean flipUd;

    /**
     * GPU设备ID (0,1,2或cpu)
     */
    private String gpuIds;

    /**
     * 数据加载线程数
     */
    private Integer numWorkers;

    /**
     * 混合精度训练
     */
    private Boolean mixedPrecision;

    /**
     * 任务状态 (PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)
     */
    private String status;

    /**
     * 进度 0-100
     */
    private Float progress;

    /**
     * 当前训练轮数
     */
    private Integer currentEpoch;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 训练指标（实时，JSON）
     */
    private String metricsJson;

    /**
     * 最佳指标（JSON）
     */
    private String bestMetrics;

    /**
     * 训练日志路径
     */
    private String trainingLogsPath;

    /**
     * TensorBoard日志路径
     */
    private String tensorboardLogPath;

    /**
     * 关联的模型注册ID（biz_model表）
     */
    private Long modelId;

    /**
     * 最终模型路径
     */
    private String modelPath;

    /**
     * 最佳模型路径
     */
    private String bestModelPath;

    /**
     * 最后一轮模型路径
     */
    private String lastModelPath;

    /**
     * 模型文件大小
     */
    private Long modelSize;

    /**
     * 推理时间（毫秒）
     */
    private Float inferenceTimeMs;

    /**
     * 评估结果（JSON）
     */
    private String evaluationResults;

    /**
     * 混淆矩阵图片路径
     */
    private String confusionMatrixPath;

    /**
     * PR曲线图片路径
     */
    private String prCurvePath;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时（秒）
     */
    private Integer durationSeconds;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

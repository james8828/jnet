package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据集构建任务实体（通用，支持多种算法）
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_dataset_build_task")
public class DatasetBuildTask implements Serializable {

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
     * 批次ID列表（JSON数组，可选）
     */
    private String batchIds;

    /**
     * 标签ID列表（JSON数组，可选）
     */
    private String tagIds;

    /**
     * 算法类型（YOLO, RCNN, SSD等）
     */
    private String algorithmType;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 训练集比例
     */
    private Float trainRatio;

    /**
     * 验证集比例
     */
    private Float valRatio;

    /**
     * 测试集比例
     */
    private Float testRatio;

    /**
     * 类别映射配置（JSON对象）
     */
    private String classMapping;

    /**
     * 是否打乱数据
     */
    private Boolean shuffle;

    /**
     * 输出格式（yolov5/yolov8/coco等）
     */
    private String outputFormat;

    /**
     * 是否包含图像文件
     */
    private Boolean includeImages;

    /**
     * 压缩格式 (zip/tar.gz/none)
     */
    private String compressFormat;

    /**
     * 压缩质量（1-100）
     */
    private Integer compressQuality;

    /**
     * 图像最小尺寸过滤
     */
    private Integer minImageSize;

    /**
     * 图像最大尺寸过滤
     */
    private Integer maxImageSize;

    /**
     * 额外配置（JSON，不同算法可有不同配置）
     */
    private String extraConfig;

    /**
     * 任务状态 (PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)
     */
    private String status;

    /**
     * 任务进度（0-100）
     */
    private Float progress;

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 步骤详细信息（JSON）
     */
    private String stepDetail;

    /**
     * 总图像数
     */
    private Integer totalImages;

    /**
     * 总标注数
     */
    private Integer totalAnnotations;

    /**
     * 训练集数量
     */
    private Integer trainCount;

    /**
     * 验证集数量
     */
    private Integer valCount;

    /**
     * 测试集数量
     */
    private Integer testCount;

    /**
     * 类别分布统计（JSON）
     */
    private String classDistribution;

    /**
     * 生成的数据集文件路径
     */
    private String datasetPath;

    /**
     * 数据集文件大小（字节）
     */
    private Long datasetSize;

    /**
     * data.yaml配置文件路径
     */
    private String dataYamlPath;

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
    @TableField(fill = FieldFill.INSERT)
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
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

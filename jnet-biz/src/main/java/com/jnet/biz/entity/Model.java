package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型注册实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_model")
public class Model implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "model_id", type = IdType.AUTO)
    private Long modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 基座模型
     */
    private String baseModel;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 关联的训练任务ID
     */
    private Long trainingTaskId;

    /**
     * 性能指标 (JSONB)
     */
    private String metrics;

    /**
     * 权重文件路径
     */
    private String weightsPath;

    /**
     * 是否为当前最优模型
     */
    private Boolean isBest;

    /**
     * 训练数据集快照 (JSONB)
     */
    private String datasetSnapshot;

    /**
     * 超参数配置 (JSONB)
     */
    private String hyperparams;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

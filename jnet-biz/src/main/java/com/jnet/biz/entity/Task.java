package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_task")
public class Task implements Serializable {

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
     * 任务类型 (TRAINING/PREDICTION/PRE_ANNOTATION)
     */
    private String type;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 关联的模型版本
     */
    private String modelVersion;

    /**
     * 任务配置快照 (JSONB)
     */
    private String configSnapshot;

    /**
     * 当前进度 (0-100)
     */
    private Double progress;

    /**
     * 状态 (PENDING/RUNNING/SUCCESS/FAILED/CANCELLED)
     */
    private String status;

    /**
     * 结果摘要 (JSONB)
     */
    private String resultSummary;

    /**
     * 错误信息
     */
    private String errorMessage;

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
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

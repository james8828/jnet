package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预测结果实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_prediction")
public class Prediction implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "prediction_id", type = IdType.AUTO)
    private Long predictionId;

    /**
     * 关联预测任务ID
     */
    private Long taskId;

    /**
     * 关联图像ID
     */
    private Long imageId;

    /**
     * 检测结果数组 (JSONB)
     */
    private String detections;

    /**
     * GeoJSON文件路径
     */
    private String geojsonPath;

    /**
     * 叠加标注框的可视化图像路径
     */
    private String overlayImgPath;

    /**
     * 检出目标数量
     */
    private Integer objectCount;

    /**
     * 阳性率
     */
    private Double positiveRate;

    /**
     * 推理耗时（毫秒）
     */
    private Integer inferenceTimeMs;

    /**
     * 质检状态 (PENDING/APPROVED/REJECTED)
     */
    private String reviewStatus;

    /**
     * 质检员ID
     */
    private Long reviewerId;

    /**
     * 质检备注
     */
    private String reviewComment;

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

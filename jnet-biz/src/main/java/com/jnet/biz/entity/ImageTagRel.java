package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图像标签关联实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_image_tag_rel")
public class ImageTagRel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "rel_id", type = IdType.AUTO)
    private Long relId;

    /**
     * 图像ID
     */
    private Long imageId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 置信度 (0-1)
     */
    private Double confidence;

    /**
     * 打标人ID
     */
    private Long taggedBy;

    /**
     * 标签来源 (AI_PRE_ANNOTATION/MANUAL/SYSTEM_AUTO)
     */
    private String tagSource;

    /**
     * 关联矢量标注ID（可选）
     */
    private Long vectorAnnotationId;

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
}

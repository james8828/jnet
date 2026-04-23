package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签定义实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_tag")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签编码（唯一）
     */
    private String code;

    /**
     * 标签分类
     */
    private String category;

    /**
     * 父标签ID（实现层级结构）
     */
    private Long parentId;

    /**
     * 前端展示颜色
     */
    private String colorCode;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否系统标签
     */
    private Boolean isSystem;

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

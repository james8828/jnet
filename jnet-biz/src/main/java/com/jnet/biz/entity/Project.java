package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目管理实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_project")
public class Project implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "project_id", type = IdType.AUTO)
    private Long projectId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目编码（唯一）
     */
    private String code;

    /**
     * 负责人ID
     */
    private Long managerId;

    /**
     * 伦理批件号
     */
    private String ethicsCode;

    /**
     * 隐私级别 (1:公开, 2:脱敏, 3:绝密)
     */
    private Integer privacyLevel;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 目标检测类别配置 (JSONB)
     */
    private String targetClasses;

    /**
     * 状态 (active/archived/deleted)
     */
    private String status;

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

    /**
     * 删除标志
     */
    @TableLogic
    private Boolean delFlag;
}

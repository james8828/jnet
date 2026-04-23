package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集批次实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_batch")
public class Batch implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "batch_id", type = IdType.AUTO)
    private Long batchId;

    /**
     * 所属项目ID
     */
    private Long projectId;

    /**
     * 批次编号
     */
    private String batchCode;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 扫描仪型号
     */
    private String scannerModel;

    /**
     * 染色协议
     */
    private String stainingProtocol;

    /**
     * 原始存储根路径
     */
    private String storageRootPath;

    /**
     * 批次内图像总数
     */
    private Integer totalImages;

    /**
     * 上传状态 (pending/uploading/completed/failed)
     */
    private String uploadStatus;

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

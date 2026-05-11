package com.jnet.biz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 图像资产实体
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("biz_image")
public class Image implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（图像ID）
     */
    @TableId(value = "image_id", type = IdType.AUTO)
    private Long imageId;

    /**
     * 所属批次ID
     */
    private Long batchId;

    /**
     * 文件名（存储用）
     */
    private String filename;

    /**
     * 原始文件名（用户上传时的文件名）
     */
    private String originalFilename;

    /**
     * 文件存储路径（兼容旧字段，逐步废弃）
     */
    private String filePath;

    /**
     * 原始文件路径（用户上传的原始文件）
     */
    private String originalFilePath;

    /**
     * 转换后 TIFF 文件路径（仅 JPG/PNG 需要）
     * 如果为空，表示原始文件本身就是 WSI 格式
     */
    private String convertedTiffPath;

    /**
     * 病理报告号
     */
    private String pathologyId;

    /**
     * 患者ID（脱敏处理）
     */
    private String patientId;

    /**
     * 格式 (SVS/NDPI/JPG/PNG)
     */
    private String format;

    /**
     * 生命周期状态 (Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived)
     */
    private String lifecycleStatus;

    /**
     * 标注进度 (0-100)
     */
    private Integer annotationProgress;

    /**
     * 层级
     */
    private Integer levels;

    /**
     * 图像宽度（像素）
     */
    private Integer width;

    /**
     * 图像高度（像素）
     */
    private Integer height;

    /**
     * X轴物理分辨率 (um/px)
     */
    private Double mppX;

    /**
     * Y轴物理分辨率 (um/px)
     */
    private Double mppY;

    /**
     * 放大倍数
     */
    private Integer magnification;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 扫描仪详细信息 (JSONB)
     */
    private String scannerInfo;

    /**
     * 扩展元数据 (JSONB)
     */
    private String metadata;

    /**
     * 缩略图 URL
     */
    private String thumbnailUrl;
    
    /**
     * 是否需要转换（JPG/PNG 为 true，WSI 为 false）
     */
    private Boolean requiresConversion;
    
    /**
     * 转换状态: NONE/PENDING/COMPLETED/FAILED
     */
    private String conversionStatus;

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

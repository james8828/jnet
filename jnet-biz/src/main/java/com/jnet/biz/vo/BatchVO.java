package com.jnet.biz.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批次 VO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "批次信息响应")
public class BatchVO {

    /**
     * 批次ID
     */
    @Schema(description = "批次ID", example = "1")
    private Long batchId;

    /**
     * 所属项目ID
     */
    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    /**
     * 所属项目名称（关联查询）
     */
    @Schema(description = "所属项目名称", example = "肺癌筛查项目")
    private String projectName;

    /**
     * 批次编号
     */
    @Schema(description = "批次编号", example = "BATCH-2024-001")
    private String batchCode;

    /**
     * 批次名称
     */
    @Schema(description = "批次名称", example = "第一批肺癌样本")
    private String batchName;

    /**
     * 扫描仪型号
     */
    @Schema(description = "扫描仪型号", example = "Aperio AT2")
    private String scannerModel;

    /**
     * 染色协议
     */
    @Schema(description = "染色协议", example = "H&E")
    private String stainingProtocol;

    /**
     * 原始存储根路径
     */
    @Schema(description = "原始存储根路径", example = "/data/images/batch-001")
    private String storageRootPath;

    /**
     * 批次内图像总数
     */
    @Schema(description = "批次内图像总数", example = "150")
    private Integer totalImages;

    /**
     * 上传状态（pending/uploading/completed/failed）
     */
    @Schema(description = "上传状态", example = "completed")
    private String uploadStatus;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-04-16T10:30:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2024-04-16T15:45:00")
    private LocalDateTime updateTime;
}

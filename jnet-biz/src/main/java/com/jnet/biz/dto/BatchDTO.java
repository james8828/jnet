package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 批次创建/更新 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "批次创建/更新请求")
public class BatchDTO {

    /**
     * 批次ID（更新时必填）
     */
    @Schema(description = "批次ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long batchId;

    /**
     * 所属项目ID
     */
    @NotNull(message = "项目ID不能为空")
    @Schema(description = "所属项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /**
     * 批次编号
     */
    @NotBlank(message = "批次编号不能为空")
    @Schema(description = "批次编号", example = "BATCH-2024-001", requiredMode = Schema.RequiredMode.REQUIRED)
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
     * 上传状态（pending/uploading/completed/failed）
     */
    @Schema(description = "上传状态", example = "pending", allowableValues = {"pending", "uploading", "completed", "failed"})
    private String uploadStatus;
}

package com.jnet.biz.dto;

import com.jnet.biz.enums.UploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批次查询条件 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批次查询条件")
public class BatchQueryDTO extends PageQueryDTO {

    /**
     * 所属项目ID
     */
    @Schema(description = "所属项目ID", example = "1")
    private Long projectId;

    /**
     * 批次编号（模糊查询）
     */
    @Schema(description = "批次编号（模糊查询）", example = "BATCH-2024")
    private String batchCode;

    /**
     * 批次名称（模糊查询）
     */
    @Schema(description = "批次名称（模糊查询）", example = "第一批")
    private String batchName;

    /**
     * 扫描仪型号
     */
    @Schema(description = "扫描仪型号", example = "Aperio AT2")
    private String scannerModel;

    /**
     * 上传状态
     */
    @Schema(description = "上传状态", example = "COMPLETED")
    private UploadStatus uploadStatus;
}

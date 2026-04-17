package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片上传初始化DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "分片上传初始化请求")
public class ChunkUploadInitDTO {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名", example = "R19-219-RD 20-1346-12 1M.svs", required = true)
    private String filename;

    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件总大小（字节）", example = "1073741824", required = true)
    private Long fileSize;

    @NotNull(message = "批次ID不能为空")
    @Schema(description = "所属批次ID", example = "1", required = true)
    private Long batchId;

    @Schema(description = "病理报告号", example = "P2024-001")
    private String pathologyId;

    @Schema(description = "患者ID（脱敏）", example = "PATIENT_001")
    private String patientId;

    @Schema(description = "文件MD5（由前端计算，用于秒传和去重）", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;

    @Schema(description = "总分片数", example = "100")
    private Integer totalChunks;

    @Schema(description = "每片大小（字节）", example = "10485760")
    private Long chunkSize;
}

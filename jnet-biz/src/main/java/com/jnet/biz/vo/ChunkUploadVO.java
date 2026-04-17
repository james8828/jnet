package com.jnet.biz.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传响应VO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分片上传响应")
public class ChunkUploadVO {

    @Schema(description = "文件唯一标识（MD5）", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;

    @Schema(description = "是否已存在（秒传）", example = "false")
    private Boolean exists;

    @Schema(description = "已存在的图像ID（秒传时返回）", example = "123")
    private Long imageId;

    @Schema(description = "已上传的分片索引列表", example = "[0, 1, 2]")
    private java.util.List<Integer> uploadedChunks;

    @Schema(description = "上传ID（用于后续分片上传）", example = "upload_123456")
    private String uploadId;

    @Schema(description = "临时存储路径", example = "/data/pathology/temp/d41d8cd98f00b204e9800998ecf8427e")
    private String tempPath;

    @Schema(description = "消息", example = "初始化成功")
    private String message;
}

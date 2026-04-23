package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "分片上传请求")
public class ChunkUploadDTO {

    @NotBlank(message = "文件MD5不能为空")
    @Schema(description = "文件唯一标识（MD5）", example = "d41d8cd98f00b204e9800998ecf8427e", required = true)
    private String fileMd5;

    @NotNull(message = "当前分片索引不能为空")
    @Min(value = 0, message = "分片索引从0开始")
    @Schema(description = "当前分片索引（从0开始）", example = "0", required = true)
    private Integer chunkIndex;

    @NotNull(message = "分片文件不能为空")
    @Schema(description = "分片文件", required = true)
    private MultipartFile file;
}

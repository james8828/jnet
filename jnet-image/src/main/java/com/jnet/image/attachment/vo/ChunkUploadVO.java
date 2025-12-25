package com.jnet.image.attachment.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/6/24 11:12:33
 */
@Data
public class ChunkUploadVO {
    @NotBlank(message = "文件名不能为空")
    private String name;

    @NotBlank(message = "uploadId 不能为空")
    private Long uploadId;

    @NotNull(message = "分片索引不能为空")
    private Integer chunkIndex;

    @NotNull(message = "总分片数不能为空")
    private Integer chunkTotal;

    @NotNull(message = "分片大小不能为空")
    private Long chunkSize;

    @NotBlank(message = "MD5 校验值不能为空")
    private String chunkMd5;

    private MultipartFile chunk;

    // 可选：用于合并时的分片信息列表
    private List<ChunkInfo> chunks;

    @Data
    public static class ChunkInfo {
        private Integer chunkIndex;
        private String chunkMd5;
    }
}

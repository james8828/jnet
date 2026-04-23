package com.jnet.biz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 图像状态更新 DTO
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Data
@Schema(description = "图像状态更新请求")
public class ImageStatusDTO {

    /**
     * 生命周期状态（Raw/Indexed/Processing/Annotated/Verified/Predicted/Archived）
     */
    @NotBlank(message = "状态不能为空")
    @Schema(description = "生命周期状态", example = "Annotated", 
            allowableValues = {"Raw", "Indexed", "Processing", "Annotated", "Verified", "Predicted", "Archived"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}

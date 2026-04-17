package com.jnet.biz.dto;

import com.jnet.biz.enums.LifecycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

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
     * 生命周期状态
     */
    @NotNull(message = "状态不能为空")
    @Schema(description = "生命周期状态", example = "ANNOTATED", 
            allowableValues = {"RAW", "INDEXED", "PROCESSING", "ANNOTATED", "VERIFIED", "PREDICTED", "ARCHIVED"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LifecycleStatus status;
}

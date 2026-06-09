package com.jnet.biz.dto;

import com.jnet.biz.validation.ValidAlgorithmType;
import com.jnet.biz.validation.ValidOutputFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 数据集构建请求 DTO
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetBuildRequestDTO {
    
    /**
     * 项目ID（必填）
     */
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 算法类型（必填）
     * 支持的值: YOLO, COCO, VOC, SAM, CLASSIFICATION
     */
    @NotBlank(message = "算法类型不能为空")
    @ValidAlgorithmType
    private String algorithmType;
    
    /**
     * 批次ID列表（可选，不传则使用项目下所有批次）
     */
    private List<Long> batchIds;
    
    /**
     * 标签ID列表（可选，不传则使用所有标签）
     */
    private List<Long> tagIds;
    
    /**
     * 训练集比例（默认 0.7）
     */
    @Builder.Default
    private Double trainRatio = 0.7;
    
    /**
     * 验证集比例（默认 0.2）
     */
    @Builder.Default
    private Double valRatio = 0.2;
    
    /**
     * 测试集比例（默认 0.1）
     */
    @Builder.Default
    private Double testRatio = 0.1;
    
    /**
     * 图像最小尺寸（可选，用于过滤过小的图像）
     */
    private Integer minImageSize;
    
    /**
     * 图像最大尺寸（可选，用于过滤过大的图像）
     */
    private Integer maxImageSize;
    
    /**
     * 是否压缩数据集（默认 false）
     */
    @Builder.Default
    private Boolean compress = false;
    
    /**
     * 压缩质量（1-100，仅当 compress=true 时有效）
     */
    private Integer compressQuality;
    
    /**
     * 输出格式（yolov5/yolov8/coco/voc，默认 yolov8）
     */
    @Builder.Default
    @ValidOutputFormat
    private String outputFormat = "yolov8";
    
    /**
     * 额外配置（JSON格式，不同算法可有不同配置）
     */
    private String extraConfig;
}

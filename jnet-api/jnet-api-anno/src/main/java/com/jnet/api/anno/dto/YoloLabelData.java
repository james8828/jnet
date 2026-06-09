package com.jnet.api.anno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * YOLO格式标注数据DTO
 * 符合 YOLO 训练要求的标注格式
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "YOLO格式标注数据")
public class YoloLabelData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 图像ID
     */
    @Schema(description = "图像ID", example = "2001")
    private Long imageId;
    
    /**
     * YOLO格式标注列表
     * 每条标注格式：class_id x_center y_center width height
     * 所有坐标都是归一化值（0-1之间）
     */
    @Schema(description = "YOLO格式标注列表", example = "[\"0 0.5 0.5 0.2 0.3\", \"1 0.3 0.7 0.15 0.25\"]")
    private List<String> labels;
    
    /**
     * 标签数量
     */
    @Schema(description = "标签数量", example = "2")
    private Integer labelCount;
}

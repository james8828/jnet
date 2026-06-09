package com.jnet.api.yolo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 预测结果DTO
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "YOLO预测结果")
public class PredictionResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 预测ID
     */
    @Schema(description = "预测ID", example = "pred_20240513_001")
    private String predictionId;
    
    /**
     * 检测结果列表
     */
    @Schema(description = "检测结果列表")
    private List<Detection> detections;
    
    /**
     * 推理时间（秒）
     */
    @Schema(description = "推理时间（秒）", example = "0.025")
    private Double inferenceTime;
    
    /**
     * 输出图像路径
     */
    @Schema(description = "输出图像路径", example = "/path/to/output.jpg")
    private String outputImage;
    
    /**
     * 单个检测结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单个检测结果")
    public static class Detection implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 类别ID
         */
        @Schema(description = "类别ID", example = "0")
        private Integer classId;
        
        /**
         * 类别名称
         */
        @Schema(description = "类别名称", example = "person")
        private String className;
        
        /**
         * 置信度
         */
        @Schema(description = "置信度", example = "0.95")
        private Double confidence;
        
        /**
         * 边界框 [x1, y1, x2, y2]
         */
        @Schema(description = "边界框 [x1, y1, x2, y2]", example = "[100, 200, 300, 400]")
        private List<Integer> bbox;
        
        /**
         * 额外信息
         */
        @Schema(description = "额外信息")
        private Map<String, Object> extra;
    }
}

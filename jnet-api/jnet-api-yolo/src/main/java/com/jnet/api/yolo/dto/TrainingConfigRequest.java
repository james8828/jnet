package com.jnet.api.yolo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * YOLO训练配置请求DTO（需要data.yaml）
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "YOLO训练配置请求（需要data.yaml）")
public class TrainingConfigRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 数据集YAML配置文件路径
     */
    @Schema(description = "数据集YAML配置文件路径", example = "/path/to/data.yaml", required = true)
    private String datasetYaml;
    
    /**
     * 训练轮数
     */
    @Schema(description = "训练轮数", example = "300", defaultValue = "300")
    private Integer epochs = 300;
    
    /**
     * 批次大小
     */
    @Schema(description = "批次大小", example = "4", defaultValue = "4")
    private Integer batchSize = 4;
    
    /**
     * 图像尺寸
     */
    @Schema(description = "图像尺寸", example = "1280", defaultValue = "1280")
    private Integer imageSize = 1280;
    
    /**
     * 设备 (cpu 或 GPU ID)
     */
    @Schema(description = "设备", example = "0", defaultValue = "0")
    private String device = "0";
    
    /**
     * 预训练权重文件
     */
    @Schema(description = "预训练权重文件", example = "yolov7x.pt", defaultValue = "yolov7x.pt")
    private String weights = "yolov7x.pt";
    
    /**
     * 是否使用Adam优化器
     */
    @Schema(description = "是否使用Adam优化器", example = "false", defaultValue = "false")
    private Boolean useAdam = false;
    
    /**
     * 超参数配置文件
     */
    @Schema(description = "超参数配置文件", example = "data/hyp.scratch.p5.yaml", defaultValue = "data/hyp.scratch.p5.yaml")
    private String hyp = "data/hyp.scratch.p5.yaml";
    
    /**
     * 数据加载工作进程数
     */
    @Schema(description = "数据加载工作进程数", example = "4", defaultValue = "4")
    private Integer workers = 4;
    
    /**
     * 是否缓存图像到内存
     */
    @Schema(description = "是否缓存图像到内存", example = "false", defaultValue = "false")
    private Boolean cache = false;
}

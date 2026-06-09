package com.jnet.api.yolo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * YOLO训练配置请求DTO（无需data.yaml，自动生成）
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "YOLO训练配置请求（无需data.yaml）")
public class TrainingConfigNoYamlRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 训练集图片目录路径
     */
    @Schema(description = "训练集图片目录路径", example = "/path/to/train/images", required = true)
    private String trainDir;
    
    /**
     * 验证集图片目录路径
     */
    @Schema(description = "验证集图片目录路径", example = "/path/to/val/images", required = true)
    private String valDir;
    
    /**
     * 测试集图片目录路径（可选）
     */
    @Schema(description = "测试集图片目录路径", example = "/path/to/test/images")
    private String testDir;
    
    /**
     * 类别名称列表
     */
    @Schema(description = "类别名称列表", example = "[\"person\", \"car\", \"dog\"]", required = true)
    private List<String> classes;
    
    /**
     * 类别数量（可选，默认根据classes列表计算）
     */
    @Schema(description = "类别数量", example = "3")
    private Integer nc;
    
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

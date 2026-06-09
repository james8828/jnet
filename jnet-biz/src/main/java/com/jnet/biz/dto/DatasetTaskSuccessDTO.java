package com.jnet.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据集任务成功结果 DTO
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetTaskSuccessDTO {
    
    /**
     * 任务ID
     */
    private Long taskId;
    
    /**
     * 数据集路径
     */
    private String datasetPath;
    
    /**
     * 数据集大小（字节）
     */
    private Long datasetSize;
    
    /**
     * 总图像数
     */
    private Integer totalImages;
    
    /**
     * 总标注数
     */
    private Integer totalAnnotations;
    
    /**
     * 训练集数量
     */
    private Integer trainCount;
    
    /**
     * 验证集数量
     */
    private Integer valCount;
    
    /**
     * 测试集数量
     */
    private Integer testCount;
    
    /**
     * 类别分布（JSON格式）
     */
    private String classDistribution;
    
    /**
     * data.yaml 配置文件路径
     */
    private String dataYamlPath;
}

package com.jnet.biz.algorithm.dataset;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据集构建结果
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class DatasetBuildResult {
    private String datasetPath;        // 数据集路径
    private Long datasetSize;          // 数据集大小（字节）
    private Integer totalImages;       // 总图像数
    private Integer totalAnnotations;  // 总标注数
    private Integer trainCount;        // 训练集数量
    private Integer valCount;          // 验证集数量
    private Integer testCount;         // 测试集数量
    private Map<String, Integer> classDistribution; // 类别分布
    private String configFilePath;     // 配置文件路径
    private String format;             // 数据格式
}

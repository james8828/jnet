package com.jnet.biz.algorithm.dataset;

import com.jnet.biz.algorithm.AlgorithmTaskExecutor;
import com.jnet.biz.enums.TaskType;

import java.util.List;
import java.util.Map;

/**
 * 数据集构建器接口
 * 用于将标注数据转换为特定格式的训练数据集
 * 
 * @param <C> 配置类型
 */
public interface DatasetBuilder<C> extends AlgorithmTaskExecutor<C, DatasetBuildResult> {
    
    @Override
    default String getTaskType() {
        return TaskType.DATASET_BUILD.getCode();
    }
    
    /**
     * 查询符合条件的图像和标注数据
     * @param config 配置对象
     * @return 图像标注数据列表
     */
    List<ImageAnnotationData> queryImageData(C config);
    
    /**
     * 划分训练/验证/测试集
     * @param dataList 全部数据
     * @param config 配置对象
     * @return 划分结果
     */
    DatasetSplit splitDataset(List<ImageAnnotationData> dataList, C config);
    
    /**
     * 转换标注格式（已废弃）
     * 现在直接使用 ImageAnnotationData.rawLabels 存储原始标注数据
     * 
     * @param data 图像标注数据
     * @param config 配置对象
     * @return 转换后的标注内容
     * @deprecated 请使用 rawLabels 字段直接获取原始标注数据
     */
    default String convertAnnotationFormat(ImageAnnotationData data, C config) {
        // 默认实现：如果存在 rawLabels，直接返回
        if (data.getRawLabels() instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> labels = (List<String>) data.getRawLabels();
            return String.join("\n", labels);
        }
        return "";
    }
    
    /**
     * 生成配置文件
     * @param config 配置对象
     * @param classDistribution 类别分布
     * @param outputDir 输出目录
     * @return 配置文件路径
     */
    String generateConfigFile(C config, Map<String, Integer> classDistribution, String outputDir);
}

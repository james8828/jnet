package com.jnet.biz.algorithm.training;

import com.jnet.biz.algorithm.AlgorithmTaskExecutor;
import com.jnet.biz.algorithm.TaskExecutionContext;
import com.jnet.biz.enums.TaskType;

/**
 * 模型训练器接口
 * 用于训练机器学习模型
 * 
 * @param <C> 配置类型
 */
public interface ModelTrainer<C> extends AlgorithmTaskExecutor<C, TrainingResult> {
    
    @Override
    default String getTaskType() {
        return TaskType.TRAINING.getCode();
    }
    
    /**
     * 准备训练环境
     * @param config 配置对象
     * @return 准备工作目录路径
     */
    String prepareTrainingEnvironment(C config);
    
    /**
     * 执行训练
     * @param config 配置对象
     * @param workDir 工作目录
     * @param context 执行上下文
     * @return 训练结果
     */
    TrainingResult doTraining(C config, String workDir, TaskExecutionContext context);
    
    /**
     * 评估模型
     * @param modelPath 模型路径
     * @param testDatasetPath 测试数据集路径
     * @return 评估结果
     */
    EvaluationResult evaluateModel(String modelPath, String testDatasetPath);
    
    /**
     * 导出模型
     * @param modelPath 原始模型路径
     * @param exportFormat 导出格式（onnx/tensorrt/openvino）
     * @return 导出后的模型路径
     */
    String exportModel(String modelPath, String exportFormat);
}

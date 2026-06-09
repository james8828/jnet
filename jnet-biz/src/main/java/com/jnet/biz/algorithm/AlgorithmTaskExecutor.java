package com.jnet.biz.algorithm;

import java.util.concurrent.CompletableFuture;

/**
 * 算法任务执行器接口
 * 所有算法任务（数据集构建、训练、预测、评估）都需要实现此接口
 * 
 * @param <C> 配置类型
 * @param <R> 结果类型
 */
public interface AlgorithmTaskExecutor<C, R> {
    
    /**
     * 获取算法类型
     * @return 算法类型标识（如：YOLO, SAM, CLASSIFICATION）
     */
    String getAlgorithmType();
    
    /**
     * 获取任务类型
     * @return 任务类型（DATASET_BUILD, TRAINING, PREDICTION, EVALUATION）
     */
    String getTaskType();
    
    /**
     * 验证配置参数
     * @param config 配置对象
     * @throws IllegalArgumentException 配置无效时抛出
     */
    void validateConfig(C config);
    
    /**
     * 执行任务（同步）
     * @param config 配置对象
     * @param context 执行上下文（包含任务ID、进度回调等）
     * @return 执行结果
     * @throws Exception 执行失败时抛出
     */
    R execute(C config, TaskExecutionContext context) throws Exception;
    
    /**
     * 执行任务（异步）
     * @param config 配置对象
     * @param context 执行上下文
     * @return 未来结果
     */
    default CompletableFuture<R> executeAsync(C config, TaskExecutionContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(config, context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}

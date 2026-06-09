package com.jnet.biz.algorithm.config;

import com.alibaba.fastjson2.JSON;

/**
 * 配置解析器接口
 * 负责将JSON字符串解析为具体的配置对象，并自动验证
 * 
 * @param <C> 配置类型，必须实现AlgorithmConfig接口
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
public interface ConfigParser<C extends AlgorithmConfig> {
    
    /**
     * 支持的算法类型
     * @return 算法类型标识（如：YOLO, COCO等）
     */
    String getSupportedAlgorithmType();
    
    /**
     * 支持的任务类型（可选）
     * 如果返回null或空字符串，表示支持所有任务类型
     * 
     * @return 任务类型标识（如：DATASET_BUILD, TRAINING等），null表示不限制
     */
    default String getSupportedTaskType() {
        return null;
    }
    
    /**
     * 获取配置类的Class对象
     * 用于运行时动态获取配置类型
     * 
     * @return 配置类的Class对象
     */
    Class<C> getConfigClass();
    
    /**
     * 解析配置
     * 从JSON字符串解析为配置对象，并自动调用validate()验证
     * 
     * @param configJson JSON格式的配置字符串
     * @return 已验证的配置对象
     * @throws IllegalArgumentException 配置无效时抛出
     */
    C parse(String configJson);
    
    /**
     * 默认实现：使用FastJSON解析并自动验证
     * 子类可以直接复用此方法，无需重复实现
     * 
     * @param configJson JSON格式的配置字符串
     * @param configClass 配置类的Class对象
     * @return 已验证的配置对象
     */
    default C parseAndValidate(String configJson, Class<C> configClass) {
        try {
            // Step 1: 反序列化JSON
            C config = JSON.parseObject(configJson, configClass);
            
            if (config == null) {
                throw new IllegalArgumentException("配置解析结果为空");
            }
            
            // Step 2: 自动验证配置
            config.validate();
            
            return config;
            
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "配置解析失败: " + e.getMessage(), e);
        }
    }
}

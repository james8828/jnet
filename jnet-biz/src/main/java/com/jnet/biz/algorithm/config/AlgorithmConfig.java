package com.jnet.biz.algorithm.config;

/**
 * 算法配置接口
 * 所有算法配置类必须实现此接口，以确保类型安全和统一验证
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
public interface AlgorithmConfig {
    
    /**
     * 获取算法类型
     * @return 算法类型标识（如：YOLO, COCO, VOC, SAM, CLASSIFICATION）
     */
    String getAlgorithmType();
    
    /**
     * 验证配置参数
     * 在解析配置后自动调用，确保配置的有效性
     * 
     * @throws IllegalArgumentException 配置无效时抛出异常
     */
    void validate();
    
    /**
     * 获取配置类的Class对象
     * 用于运行时动态获取配置类型信息
     * 
     * @return 配置类的Class对象
     */
    Class<? extends AlgorithmConfig> getConfigClass();
    
    /**
     * 获取默认配置
     * 可用于配置合并或初始化
     * 
     * @return 默认配置对象
     */
    default AlgorithmConfig getDefaultConfig() {
        return this;
    }
}

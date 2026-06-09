package com.jnet.biz.algorithm.yolo;

import com.jnet.biz.algorithm.config.AlgorithmConfig;
import com.jnet.biz.algorithm.training.TrainingConfig;
import com.jnet.biz.enums.AlgorithmType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * YOLO训练配置
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class YoloTrainingConfig extends TrainingConfig {
    // 继承TrainingConfig的基础字段
    
    // YOLO特有配置
    private String modelName = "yolov8n";  // 模型名称
    private String device = "0";           // GPU设备
    private Integer workers = 8;           // 数据加载线程数
    private Boolean usePretrained = true;  // 使用预训练模型
    private String pythonScriptPath;       // Python训练脚本路径
    
    /**
     * 获取算法类型
     */
    @Override
    public String getAlgorithmType() {
        return AlgorithmType.YOLO.getCode();
    }
    
    /**
     * 获取配置类的Class对象
     */
    @Override
    public Class<? extends AlgorithmConfig> getConfigClass() {
        return YoloTrainingConfig.class;
    }
    
    /**
     * 验证配置参数（扩展父类验证）
     */
    @Override
    public void validate() {
        // 调用父类基础验证
        super.validate();
        
        // YOLO特有验证
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        
        // 验证模型架构是否合法
        if (!modelName.matches("yolov[58][nsmlx]")) {
            throw new IllegalArgumentException(
                "不支持的YOLO模型架构: " + modelName + 
                "。支持的架构: yolov5n/s/m/l/x, yolov8n/s/m/l/x");
        }
    }
}

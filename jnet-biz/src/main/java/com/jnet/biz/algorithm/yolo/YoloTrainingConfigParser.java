package com.jnet.biz.algorithm.yolo;

import com.jnet.biz.algorithm.config.ConfigParser;
import com.jnet.biz.enums.AlgorithmType;
import com.jnet.biz.enums.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * YOLO训练配置解析器
 * 负责将JSON字符串解析为YoloTrainingConfig对象，并自动验证
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Component
@Slf4j
public class YoloTrainingConfigParser implements ConfigParser<YoloTrainingConfig> {
    
    /**
     * 支持的算法类型
     */
    @Override
    public String getSupportedAlgorithmType() {
        return AlgorithmType.YOLO.getCode();
    }
    
    /**
     * 支持的任务类型：仅用于模型训练
     */
    @Override
    public String getSupportedTaskType() {
        return TaskType.TRAINING.getCode();
    }
    
    /**
     * 获取配置类的Class对象
     */
    @Override
    public Class<YoloTrainingConfig> getConfigClass() {
        return YoloTrainingConfig.class;
    }
    
    /**
     * 解析YOLO训练配置
     * 复用父类的默认实现：反序列化 + 自动验证
     * 
     * @param configJson JSON格式的配置字符串
     * @return 已验证的YOLO训练配置对象
     */
    @Override
    public YoloTrainingConfig parse(String configJson) {
        log.debug("解析YOLO训练配置");
        return parseAndValidate(configJson, YoloTrainingConfig.class);
    }
}

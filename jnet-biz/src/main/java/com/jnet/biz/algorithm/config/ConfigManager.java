package com.jnet.biz.algorithm.config;

import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 配置管理器
 * 通过算法类型+任务类型自动路由到对应的配置解析器，实现类型安全的配置解析
 * 
 * 路由规则：
 * 1. 优先匹配 algorithmType + taskType 的组合键
 * 2. 如果没有精确匹配，则匹配仅指定 algorithmType 的通用解析器
 * 
 * 使用示例：
 * <pre>{@code
 * AlgorithmConfig config = configManager.parseConfigAuto(
 *     msg.getConfigJson(),
 *     msg.getAlgorithmType(),
 *     msg.getTaskType()
 * );
 * }</pre>
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConfigManager {
    
    /**
     * 所有配置解析器（Spring自动注入）
     */
    private final List<ConfigParser<?>> configParsers;
    
    /**
     * 解析器缓存：compositeKey -> ConfigParser
     * compositeKey 格式：
     * - "YOLO:DATASET_BUILD" (精确匹配)
     * - "YOLO:*" (通用匹配，taskType为null)
     */
    private final Map<String, ConfigParser<?>> parserMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化：注册所有解析器
     * Spring容器启动时自动执行
     */
    @PostConstruct
    public void init() {
        for (ConfigParser<?> parser : configParsers) {
            String algorithmType = parser.getSupportedAlgorithmType().toUpperCase();
            String taskType = parser.getSupportedTaskType();
            
            String compositeKey;
            if (taskType != null && !taskType.trim().isEmpty()) {
                compositeKey = buildCompositeKey(algorithmType, taskType.toUpperCase());
                log.info("✅ 注册配置解析器: algorithmType={}, taskType={}, parserClass={}", 
                         algorithmType, taskType, parser.getClass().getSimpleName());
            } else {
                compositeKey = buildGenericKey(algorithmType);
                log.info("✅ 注册通用配置解析器: algorithmType={}, parserClass={}", 
                         algorithmType, parser.getClass().getSimpleName());
            }
            
            parserMap.put(compositeKey, parser);
        }
        
        log.info("📦 配置管理器初始化完成，共注册 {} 个解析器", parserMap.size());
        log.info("📋 已注册的解析器: {}", parserMap.keySet());
    }
    
    /**
     * 解析配置（根据算法类型+任务类型自动路由）
     * 
     * @param configJson JSON字符串
     * @param algorithmType 算法类型（如：YOLO, COCO等）
     * @param taskType 任务类型（如：DATASET_BUILD, TRAINING等）
     * @return 已验证的配置对象
     * @throws IllegalArgumentException 不支持的算法类型或配置无效
     */
    public AlgorithmConfig parseConfigAuto(
            String configJson, 
            String algorithmType,
            String taskType) {
        
        if (configJson == null || configJson.trim().isEmpty()) {
            throw new IllegalArgumentException("配置JSON不能为空");
        }
        
        if (algorithmType == null || algorithmType.trim().isEmpty()) {
            throw new IllegalArgumentException("算法类型不能为空");
        }
        
        String normalizedAlgoType = algorithmType.toUpperCase();
        String normalizedTaskType = taskType != null ? taskType.toUpperCase() : null;
        
        // Step 1: 尝试精确匹配 algorithmType + taskType
        ConfigParser<?> parser = null;
        if (normalizedTaskType != null) {
            String preciseKey = buildCompositeKey(normalizedAlgoType, normalizedTaskType);
            parser = parserMap.get(preciseKey);
            if (parser != null) {
                log.debug("🎯 精确匹配解析器: algorithmType={}, taskType={}", 
                          algorithmType, taskType);
            }
        }
        
        // Step 2: 如果没有精确匹配，尝试通用匹配（仅 algorithmType）
        if (parser == null) {
            String genericKey = buildGenericKey(normalizedAlgoType);
            parser = parserMap.get(genericKey);
            if (parser != null) {
                log.debug("🔍 通用匹配解析器: algorithmType={}", algorithmType);
            }
        }
        
        // Step 3: 如果仍然没有找到，抛出异常
        if (parser == null) {
            throw new BizException(BizErrorCode.ALGORITHM_TYPE_UNSUPPORTED, 
                String.format("不支持的配置组合: algorithmType=%s, taskType=%s。支持的组合: %s",
                    algorithmType, taskType, getSupportedCombinations()));
        }
        
        // Step 4: 委托给具体的解析器处理
        Class<? extends AlgorithmConfig> configClass = parser.getConfigClass();
        log.debug("🔍 解析配置: algorithmType={}, taskType={}, configClass={}", 
                  algorithmType, taskType, configClass.getSimpleName());
        
        return parser.parse(configJson);
    }
    
    /**
     * 检查是否支持指定的算法类型和任务类型组合
     * 
     * @param algorithmType 算法类型
     * @param taskType 任务类型
     * @return true-支持，false-不支持
     */
    public boolean isSupported(String algorithmType, String taskType) {
        if (algorithmType == null) {
            return false;
        }
        
        String normalizedAlgoType = algorithmType.toUpperCase();
        String normalizedTaskType = taskType != null ? taskType.toUpperCase() : null;
        
        // 检查精确匹配
        if (normalizedTaskType != null) {
            String preciseKey = buildCompositeKey(normalizedAlgoType, normalizedTaskType);
            if (parserMap.containsKey(preciseKey)) {
                return true;
            }
        }
        
        // 检查通用匹配
        String genericKey = buildGenericKey(normalizedAlgoType);
        return parserMap.containsKey(genericKey);
    }
    
    /**
     * 获取所有支持的算法类型（向后兼容）
     * 
     * @return 算法类型集合
     */
    public Set<String> getSupportedTypes() {
        return parserMap.values().stream()
            .map(ConfigParser::getSupportedAlgorithmType)
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
    }
    
    /**
     * 获取所有支持的配置组合
     * 
     * @return 配置组合描述列表
     */
    public Set<String> getSupportedCombinations() {
        return parserMap.keySet();
    }
    
    /**
     * 构建复合键
     */
    private String buildCompositeKey(String algorithmType, String taskType) {
        return algorithmType.toUpperCase() + ":" + taskType.toUpperCase();
    }
    
    /**
     * 构建通用键（仅算法类型）
     */
    private String buildGenericKey(String algorithmType) {
        return algorithmType.toUpperCase() + ":*";
    }

}

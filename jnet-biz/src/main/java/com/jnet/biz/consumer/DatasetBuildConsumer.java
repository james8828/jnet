package com.jnet.biz.consumer;

import com.alibaba.fastjson2.JSON;
import com.jnet.biz.algorithm.TaskContextManager;
import com.jnet.biz.algorithm.TaskExecutionContext;
import com.jnet.biz.algorithm.config.AlgorithmConfig;
import com.jnet.biz.algorithm.config.ConfigManager;
import com.jnet.biz.algorithm.dataset.DatasetBuilder;
import com.jnet.biz.algorithm.dataset.DatasetBuildResult;
import com.jnet.biz.config.RabbitMQConfig;
import com.jnet.biz.dto.AlgorithmTaskMessage;
import com.jnet.biz.dto.DatasetTaskSuccessDTO;
import com.jnet.biz.enums.TaskType;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.service.IDatasetBuildTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 数据集构建任务消费者
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetBuildConsumer {

    private final List<DatasetBuilder<?>> datasetBuilders;
    private final TaskContextManager contextManager;
    private final IDatasetBuildTaskService datasetBuildTaskService;
    private final ConfigManager configManager;  // ✅ 新增：配置管理器
    private final RedisTemplate<String, String> redisTemplate;  // ✅ 新增：Redis模板（用于幂等性检查）

    /**
     * 监听数据集构建队列
     * 
     * 流量控制策略（通过 RabbitMQConfig 中的容器工厂配置）：
     * - concurrency: 最小并发数（核心线程数）= 2
     * - maxConcurrent: 最大并发数（最大线程数）= 5
     * - prefetch: 预取数量 = 1（确保负载均衡）
     * 
     * 对于耗时较长的任务，建议：
     * - 降低并发数，避免系统过载
     * - 降低预取数量，确保负载均衡
     */
    @RabbitListener(queues = RabbitMQConfig.DATASET_BUILD_QUEUE)
    public void handleDatasetBuildTask(String messageJson) {
        AlgorithmTaskMessage message = null;
        TaskExecutionContext context = null;

        try {
            // 解析消息
            message = JSON.parseObject(messageJson, AlgorithmTaskMessage.class);
            log.info("收到数据集构建任务: taskId={}, taskNo={}", message.getTaskId(), message.getTaskNo());

            // 幂等性检查
            if (isDuplicateMessage(message.getMessageId())) {
                log.warn("重复消息，跳过处理: messageId={}", message.getMessageId());
                return;
            }

            // 创建任务上下文
            context = contextManager.createContext(message);

            // ✅ 获取算法类型和任务类型
            String algorithmType = message.getAlgorithmType();
            String taskType = message.getTaskType();

            // ✅ 动态解析配置（根据algorithmType + taskType自动选择配置类）
            AlgorithmConfig config = configManager.parseConfigAuto(
                message.getConfigJson(), 
                algorithmType,
                taskType
            );

            log.info("配置解析成功: algorithmType={}, taskType={}, configClass={}",
                    algorithmType, taskType, config.getClass().getSimpleName());

            // ✅ 查找构建器（使用通配符泛型）
            DatasetBuilder<?> builder = findDatasetBuilder(algorithmType);

            if (builder == null) {
                throw new BizException(BizErrorCode.ALGORITHM_BUILDER_NOT_FOUND,
                        "未找到算法构建器: " + algorithmType + "。支持的类型: " + configManager.getSupportedTypes());
            }

            // ✅ 执行构建器（需要SuppressWarnings抑制泛型警告）
            @SuppressWarnings("unchecked")
            DatasetBuildResult result = ((DatasetBuilder<AlgorithmConfig>) builder).execute(config, context);

            // 标记任务成功
            DatasetTaskSuccessDTO successDTO = DatasetTaskSuccessDTO.builder()
                    .taskId(message.getTaskId())
                    .datasetPath(result.getDatasetPath())
                    .datasetSize(result.getDatasetSize())
                    .totalImages(result.getTotalImages())
                    .totalAnnotations(result.getTotalAnnotations())
                    .trainCount(result.getTrainCount())
                    .valCount(result.getValCount())
                    .testCount(result.getTestCount())
                    .classDistribution(JSON.toJSONString(result.getClassDistribution()))
                    .dataYamlPath(result.getConfigFilePath())
                    .build();

            datasetBuildTaskService.markTaskSuccess(successDTO);

            log.info("数据集构建任务完成: taskId={}, path={}", message.getTaskId(), result.getDatasetPath());

        } catch (Exception e) {
            log.error("数据集构建任务失败: taskId={}",
                    message != null ? message.getTaskId() : "unknown", e);

            // 标记任务失败
            if (message != null) {
                datasetBuildTaskService.markTaskFailed(
                        message.getTaskId(),
                        e.getClass().getSimpleName() + ": " + e.getMessage(),
                        getStackTrace(e)
                );
            }

        } finally {
            // 清理资源
            if (context != null && message != null) {
                contextManager.removeContext(String.valueOf(message.getTaskId()));
            }
        }
    }

    /**
     * 查找数据集构建器（使用通配符泛型）
     *
     * @param algorithmType 算法类型
     * @return 匹配的构建器，未找到返回null
     */
    private DatasetBuilder<?> findDatasetBuilder(String algorithmType) {
        for (DatasetBuilder<?> builder : datasetBuilders) {
            if (builder.getAlgorithmType().equalsIgnoreCase(algorithmType) &&
                    TaskType.DATASET_BUILD.getCode().equals(builder.getTaskType())) {
                return builder;
            }
        }
        return null;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 检查是否为重复消息（基于Redis实现）
     */
    private boolean isDuplicateMessage(String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }
        
        String key = "task:processed:" + messageId;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.warn("检测到重复消息: messageId={}", messageId);
            return true;
        }
        
        // 标记为已处理，24小时过期
        redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
        log.debug("标记消息已处理: messageId={}", messageId);
        return false;
    }


}

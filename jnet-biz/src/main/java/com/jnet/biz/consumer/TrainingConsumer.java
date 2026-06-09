package com.jnet.biz.consumer;

import com.alibaba.fastjson2.JSON;
import com.jnet.biz.algorithm.TaskContextManager;
import com.jnet.biz.algorithm.TaskExecutionContext;
import com.jnet.biz.algorithm.config.AlgorithmConfig;
import com.jnet.biz.algorithm.config.ConfigManager;
import com.jnet.biz.algorithm.training.ModelTrainer;
import com.jnet.biz.algorithm.training.TrainingResult;
import com.jnet.biz.config.RabbitMQConfig;
import com.jnet.biz.dto.AlgorithmTaskMessage;
import com.jnet.biz.dto.TrainingTaskSuccessDTO;
import com.jnet.biz.enums.TaskType;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.service.IYoloTrainingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.Map;

/**
 * 模型训练任务消费者
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingConsumer {
    
    private final List<ModelTrainer<?>> modelTrainers;
    private final TaskContextManager contextManager;
    private final IYoloTrainingTaskService trainingTaskService;
    private final ConfigManager configManager;  // ✅ 新增：配置管理器
    
    /**
     * 监听训练任务队列
     */
    @RabbitListener(queues = RabbitMQConfig.TRAINING_QUEUE)
    public void handleTrainingTask(String messageJson) {
        AlgorithmTaskMessage message = null;
        TaskExecutionContext context = null;
        
        try {
            // 解析消息
            message = JSON.parseObject(messageJson, AlgorithmTaskMessage.class);
            log.info("收到模型训练任务: taskId={}, taskNo={}", message.getTaskId(), message.getTaskNo());
            
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
            
            // ✅ 查找训练器（使用通配符泛型）
            ModelTrainer<?> trainer = findModelTrainer(algorithmType);
            
            if (trainer == null) {
                throw new BizException(BizErrorCode.ALGORITHM_TRAINER_NOT_FOUND, 
                    "未找到算法训练器: " + algorithmType + "。支持的类型: " + configManager.getSupportedTypes());
            }
            
            // ✅ 执行训练器（需要SuppressWarnings抑制泛型警告）
            @SuppressWarnings("unchecked")
            TrainingResult result = ((ModelTrainer<AlgorithmConfig>) trainer).execute(config, context);
            
            // 标记任务成功
            TrainingTaskSuccessDTO successDTO = TrainingTaskSuccessDTO.builder()
                .taskId(message.getTaskId())
                .modelId(null) // modelId（后续注册时设置）
                .modelPath(result.getModelPath())
                .bestModelPath(result.getBestModelPath() != null ? result.getBestModelPath() : result.getModelPath())
                .evaluationResults(JSON.toJSONString(result.getEvaluation()))
                .build();
            
            trainingTaskService.markTaskSuccess(successDTO);
            
            log.info("模型训练任务完成: taskId={}, modelPath={}", message.getTaskId(), result.getModelPath());
            
        } catch (Exception e) {
            log.error("模型训练任务失败: taskId={}", 
                    message != null ? message.getTaskId() : "unknown", e);
            
            // 标记任务失败
            if (message != null) {
                trainingTaskService.markTaskFailed(
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
     * 查找模型训练器（使用通配符泛型）
     * 
     * @param algorithmType 算法类型
     * @return 匹配的训练器，未找到返回null
     */
    private ModelTrainer<?> findModelTrainer(String algorithmType) {
        for (ModelTrainer<?> trainer : modelTrainers) {
            if (trainer.getAlgorithmType().equalsIgnoreCase(algorithmType) &&
                TaskType.TRAINING.getCode().equals(trainer.getTaskType())) {
                return trainer;
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
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(String messageId) {
        // TODO: 使用Redis实现幂等性检查
        return false;
    }
    

}

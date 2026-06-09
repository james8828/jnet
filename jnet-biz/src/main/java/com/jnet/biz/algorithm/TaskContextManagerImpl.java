package com.jnet.biz.algorithm;

import com.alibaba.fastjson2.JSON;
import com.jnet.biz.dto.AlgorithmTaskMessage;
import com.jnet.biz.enums.TaskType;
import com.jnet.biz.service.IDatasetBuildTaskService;
import com.jnet.biz.service.IYoloTrainingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 任务上下文管理器实现
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
@Component
public class TaskContextManagerImpl implements TaskContextManager {
    
    private final IDatasetBuildTaskService datasetTaskService;
    private final IYoloTrainingTaskService trainingTaskService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 构造函数（使用@Lazy解决循环依赖）
     */
    public TaskContextManagerImpl(
            @Lazy IDatasetBuildTaskService datasetTaskService,
            @Lazy IYoloTrainingTaskService trainingTaskService,
            SimpMessagingTemplate messagingTemplate,
            RedisTemplate<String, String> redisTemplate) {
        this.datasetTaskService = datasetTaskService;
        this.trainingTaskService = trainingTaskService;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 存储运行中的任务上下文
     */
    private final Map<String, TaskExecutionContextImpl> contextMap = new ConcurrentHashMap<>();
    
    private static final String CANCEL_KEY_PREFIX = "task:cancel:";
    private static final long TTL_HOURS = 24;
    
    @Override
    public TaskExecutionContext createContext(AlgorithmTaskMessage message) {
        String taskId = String.valueOf(message.getTaskId());
        
        TaskExecutionContextImpl context = new TaskExecutionContextImpl(
            taskId,
            message.getTaskNo(),
            message.getProjectId(),
            message.getCreateBy(),
            message.getTaskType(),
            this,
            redisTemplate
        );
        
        contextMap.put(taskId, context);
        log.info("创建任务上下文: taskId={}, taskNo={}", taskId, message.getTaskNo());
        
        return context;
    }
    
    @Override
    public void updateTaskProgress(String taskId, Float progress, String currentStep, String stepDetail) {
        // 根据 taskType 判断是数据集任务还是训练任务
        TaskExecutionContextImpl context = contextMap.get(taskId);
        if (context == null) {
            log.warn("任务上下文不存在: taskId={}", taskId);
            return;
        }
        
        String taskType = context.getTaskType();
        Long taskIdLong = Long.parseLong(taskId);
        
        // 更新数据库
        if (TaskType.DATASET_BUILD.getCode().equals(taskType)) {
            datasetTaskService.updateProgress(taskIdLong, progress, currentStep, stepDetail);
        } else if (TaskType.TRAINING.getCode().equals(taskType)) {
            trainingTaskService.updateProgress(taskIdLong, progress, null, currentStep);
        } else {
            log.warn("未知的任务类型: taskType={}", taskType);
        }
        
        // 通过WebSocket推送进度
        try {
            Map<String, Object> progressData = Map.of(
                "taskId", taskId,
                "progress", progress != null ? progress : 0f,
                "currentStep", currentStep != null ? currentStep : "",
                "stepDetail", stepDetail != null ? JSON.parse(stepDetail) : null,
                "timestamp", LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend("/topic/task/progress/" + taskId, progressData);
            log.debug("推送任务进度: taskId={}, progress={}, taskType={}", taskId, progress, taskType);
        } catch (Exception e) {
            log.warn("WebSocket推送失败: taskId={}", taskId, e);
        }
    }
    
    @Override
    public void removeContext(String taskId) {
        contextMap.remove(taskId);
        clearCancelFlag(taskId);
        log.debug("移除任务上下文: taskId={}", taskId);
    }
    
    @Override
    public boolean cancelTask(String taskId) {
        String key = CANCEL_KEY_PREFIX + taskId;
        redisTemplate.opsForValue().set(key, "1", TTL_HOURS, TimeUnit.HOURS);
        
        TaskExecutionContextImpl localContext = contextMap.get(taskId);
        if (localContext != null) {
            localContext.cancel();
            log.info("触发本地任务取消: taskId={}", taskId);
        } else {
            log.info("标记任务取消（远程节点执行）: taskId={}", taskId);
        }
        
        return true;
    }
    
    /**
     * 清除取消标志
     */
    private void clearCancelFlag(String taskId) {
        String key = CANCEL_KEY_PREFIX + taskId;
        redisTemplate.delete(key);
        log.debug("清除取消标志: taskId={}", taskId);
    }
}

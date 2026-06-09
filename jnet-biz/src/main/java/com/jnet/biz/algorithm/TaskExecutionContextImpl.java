package com.jnet.biz.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务执行上下文实现类
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
public class TaskExecutionContextImpl implements TaskExecutionContext {
    
    private final String taskId;
    private final String taskNo;
    private final Long projectId;
    private final Long createBy;
    private final String taskType;
    private final TaskContextManager contextManager;
    private final RedisTemplate<String, String> redisTemplate;
    
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private Runnable cancelCallback;
    
    private static final String CANCEL_KEY_PREFIX = "task:cancel:";
    
    public TaskExecutionContextImpl(String taskId, String taskNo, Long projectId,
                                    Long createBy, String taskType, TaskContextManager contextManager,
                                    RedisTemplate<String, String> redisTemplate) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.projectId = projectId;
        this.createBy = createBy;
        this.taskType = taskType;
        this.contextManager = contextManager;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public String getTaskId() {
        return taskId;
    }
    
    @Override
    public String getTaskNo() {
        return taskNo;
    }
    
    @Override
    public Long getProjectId() {
        return projectId;
    }
    
    @Override
    public Long getCreateBy() {
        return createBy;
    }
    
    /**
     * 获取任务类型
     */
    @Override
    public String getTaskType() {
        return taskType;
    }
    
    @Override
    public void updateProgress(float progress, String step) {
        updateProgress(progress, step, null);
    }
    
    @Override
    public void updateProgress(float progress, String step, Object detail) {
        log.debug("任务进度更新: taskId={}, progress={}, step={}", taskId, progress, step);
        String detailJson = detail != null ? com.alibaba.fastjson2.JSON.toJSONString(detail) : null;
        contextManager.updateTaskProgress(taskId, progress, step, detailJson);
    }
    
    @Override
    public void log(LogLevel level, String message) {
        switch (level) {
            case DEBUG:
                log.debug("[{}] {}", taskId, message);
                break;
            case INFO:
                log.info("[{}] {}", taskId, message);
                break;
            case WARN:
                log.warn("[{}] {}", taskId, message);
                break;
            case ERROR:
                log.error("[{}] {}", taskId, message);
                break;
        }
    }
    
    @Override
    public boolean isCancelled() {
        if (cancelled.get()) {
            return true;
        }
        
        String key = CANCEL_KEY_PREFIX + taskId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            cancelled.set(true);
            log.info("检测到分布式取消信号: taskId={}", taskId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void onCancel(Runnable callback) {
        this.cancelCallback = callback;
    }
    
    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 取消任务
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            log.info("任务被取消: taskId={}", taskId);
            if (cancelCallback != null) {
                try {
                    cancelCallback.run();
                } catch (Exception e) {
                    log.error("执行取消回调失败", e);
                }
            }
        }
    }
}

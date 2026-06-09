package com.jnet.biz.algorithm;

import java.util.function.Consumer;

/**
 * 任务执行上下文
 * 提供进度更新、日志记录、取消检查等功能
 */
public interface TaskExecutionContext {
    
    /**
     * 获取任务ID
     */
    String getTaskId();
    
    /**
     * 获取任务编号
     */
    String getTaskNo();
    
    /**
     * 获取项目ID
     */
    Long getProjectId();
    
    /**
     * 获取创建人ID
     */
    Long getCreateBy();
    
    /**
     * 获取任务类型
     */
    String getTaskType();
    
    /**
     * 更新任务进度
     * @param progress 进度（0-100）
     * @param step 当前步骤描述
     */
    void updateProgress(float progress, String step);
    
    /**
     * 更新任务进度（带详细信息）
     * @param progress 进度（0-100）
     * @param step 当前步骤描述
     * @param detail 步骤详细信息（JSON）
     */
    void updateProgress(float progress, String step, Object detail);
    
    /**
     * 记录日志
     * @param level 日志级别
     * @param message 日志消息
     */
    void log(LogLevel level, String message);
    
    /**
     * 检查任务是否被取消
     * @return true-已取消，false-未取消
     */
    boolean isCancelled();
    
    /**
     * 注册取消回调
     * @param callback 取消时的回调函数
     */
    void onCancel(Runnable callback);
    
    /**
     * 获取附加属性
     * @param key 属性键
     * @return 属性值
     */
    Object getAttribute(String key);
    
    /**
     * 设置附加属性
     * @param key 属性键
     * @param value 属性值
     */
    void setAttribute(String key, Object value);
    
    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
}

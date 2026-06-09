package com.jnet.biz.algorithm;

import com.jnet.biz.dto.AlgorithmTaskMessage;

/**
 * 任务上下文管理器接口
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
public interface TaskContextManager {
    
    /**
     * 创建任务执行上下文
     * 
     * @param message 任务消息
     * @return 任务执行上下文
     */
    TaskExecutionContext createContext(AlgorithmTaskMessage message);
    
    /**
     * 更新任务进度
     * 
     * @param taskId 任务ID
     * @param progress 进度（0-100）
     * @param currentStep 当前步骤
     * @param stepDetail 步骤详情（JSON）
     */
    void updateTaskProgress(String taskId, Float progress, String currentStep, String stepDetail);
    
    /**
     * 移除任务上下文
     * 
     * @param taskId 任务ID
     */
    void removeContext(String taskId);
    
    /**
     * 取消任务（触发上下文中的取消标志）
     * 支持分布式环境，使用Redis存储取消状态
     * 
     * @param taskId 任务ID
     * @return true-成功触发取消，false-任务不存在
     */
    boolean cancelTask(String taskId);
}

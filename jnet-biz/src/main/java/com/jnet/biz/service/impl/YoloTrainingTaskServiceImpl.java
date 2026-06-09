package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.algorithm.TaskContextManager;
import com.jnet.biz.dto.TrainingTaskQueryDTO;
import com.jnet.biz.dto.TrainingTaskSuccessDTO;
import com.jnet.biz.entity.YoloTrainingTask;
import com.jnet.biz.enums.TaskStatus;
import com.jnet.biz.mapper.YoloTrainingTaskMapper;
import com.jnet.biz.service.IYoloTrainingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * YOLO模型训练任务 Service 实现类
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoloTrainingTaskServiceImpl extends ServiceImpl<YoloTrainingTaskMapper, YoloTrainingTask> implements IYoloTrainingTaskService {

    private final TaskContextManager contextManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Page<YoloTrainingTask> pageTasks(TrainingTaskQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<YoloTrainingTask> page = query.toPage();
        LambdaQueryWrapper<YoloTrainingTask> wrapper = new LambdaQueryWrapper<>();
        
        // 项目ID筛选
        if (query.getProjectId() != null) {
            wrapper.eq(YoloTrainingTask::getProjectId, query.getProjectId());
        }
        
        // 状态筛选
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(YoloTrainingTask::getStatus, query.getStatus());
        }
        
        // 任务名称模糊查询
        if (query.getTaskName() != null && !query.getTaskName().isEmpty()) {
            wrapper.like(YoloTrainingTask::getTaskName, query.getTaskName());
        }
        
        // 模型架构筛选
        if (query.getModelArchitecture() != null && !query.getModelArchitecture().isEmpty()) {
            wrapper.eq(YoloTrainingTask::getModelArchitecture, query.getModelArchitecture());
        }
        
        // 数据集任务ID筛选
        if (query.getDatasetTaskId() != null) {
            wrapper.eq(YoloTrainingTask::getDatasetTaskId, query.getDatasetTaskId());
        }
        
        wrapper.orderByDesc(YoloTrainingTask::getCreateTime);
        
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(YoloTrainingTask task) {
        // 生成任务编号
        String taskNo = "TRAINING_" + System.currentTimeMillis();
        task.setTaskNo(taskNo);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setProgress(0f);
        task.setCurrentEpoch(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        this.save(task);
        
        log.info("创建YOLO训练任务成功: taskId={}, taskNo={}", task.getTaskId(), taskNo);
        return task.getTaskId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Long taskId) {
        YoloTrainingTask task = this.getById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return false;
        }
        
        if (TaskStatus.SUCCESS.getCode().equals(task.getStatus()) || TaskStatus.FAILED.getCode().equals(task.getStatus())) {
            log.warn("任务已结束，无法取消: taskId={}, status={}", taskId, task.getStatus());
            return false;
        }
        
        task.setStatus(TaskStatus.CANCELLED.getCode());
        task.setUpdateTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        
        boolean result = this.updateById(task);
        
        // 触发分布式取消（通知正在执行的线程）
        try {
            contextManager.cancelTask(String.valueOf(taskId));
        } catch (Exception e) {
            log.error("触发任务取消失败: taskId={}", taskId, e);
        }
        
        log.info("取消任务: taskId={}, result={}", taskId, result);
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long taskId, Float progress, Integer currentEpoch, String currentStep) {
        YoloTrainingTask task = this.getById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }
        
        task.setProgress(progress);
        task.setCurrentEpoch(currentEpoch);
        task.setCurrentStep(currentStep);
        task.setUpdateTime(LocalDateTime.now());
        
        // 如果是第一次更新进度，标记为RUNNING
        if (TaskStatus.PENDING.getCode().equals(task.getStatus())) {
            task.setStatus(TaskStatus.RUNNING.getCode());
            task.setStartTime(LocalDateTime.now());
        }
        
        this.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetrics(Long taskId, String metricsJson) {
        YoloTrainingTask task = this.getById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }
        
        task.setMetricsJson(metricsJson);
        task.setUpdateTime(LocalDateTime.now());
        
        this.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskSuccess(TrainingTaskSuccessDTO successDTO) {
        YoloTrainingTask task = this.getById(successDTO.getTaskId());
        if (task == null) {
            log.warn("任务不存在: taskId={}", successDTO.getTaskId());
            return;
        }
        
        task.setStatus(TaskStatus.SUCCESS.getCode());
        task.setProgress(100f);
        task.setModelId(successDTO.getModelId());
        task.setModelPath(successDTO.getModelPath());
        task.setBestModelPath(successDTO.getBestModelPath());
        task.setEvaluationResults(successDTO.getEvaluationResults());
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 计算耗时（秒）
        if (task.getStartTime() != null) {
            long duration = java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            task.setDurationSeconds((int) duration);
        }
        
        this.updateById(task);
        log.info("训练任务执行成功: taskId={}, modelPath={}", successDTO.getTaskId(), successDTO.getModelPath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskFailed(Long taskId, String errorMessage, String errorStack) {
        YoloTrainingTask task = this.getById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }
        
        task.setStatus(TaskStatus.FAILED.getCode());
        task.setErrorMessage(errorMessage);
        task.setErrorStack(errorStack);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 计算耗时（秒）
        if (task.getStartTime() != null) {
            long duration = java.time.Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            task.setDurationSeconds((int) duration);
        }
        
        this.updateById(task);
        log.error("训练任务执行失败: taskId={}, error={}", taskId, errorMessage);
    }
}

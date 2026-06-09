package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.algorithm.TaskContextManager;
import com.jnet.biz.dto.DatasetTaskQueryDTO;
import com.jnet.biz.dto.DatasetTaskSuccessDTO;
import com.jnet.biz.entity.DatasetBuildTask;
import com.jnet.biz.enums.TaskStatus;
import com.jnet.biz.mapper.DatasetBuildTaskMapper;
import com.jnet.biz.service.IDatasetBuildTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 数据集构建任务 Service 实现类（通用，支持多种算法）
 *
 * @author JNet Team
 * @since 2024-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetBuildTaskServiceImpl extends ServiceImpl<DatasetBuildTaskMapper, DatasetBuildTask> implements IDatasetBuildTaskService {

    private final TaskContextManager contextManager;

    @Override
    public Page<DatasetBuildTask> pageTasks(DatasetTaskQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<DatasetBuildTask> page = query.toPage();
        LambdaQueryWrapper<DatasetBuildTask> wrapper = new LambdaQueryWrapper<>();
        
        // 项目ID筛选
        if (query.getProjectId() != null) {
            wrapper.eq(DatasetBuildTask::getProjectId, query.getProjectId());
        }
        
        // 状态筛选
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(DatasetBuildTask::getStatus, query.getStatus());
        }
        
        // 任务名称模糊查询
        if (query.getTaskName() != null && !query.getTaskName().isEmpty()) {
            wrapper.like(DatasetBuildTask::getTaskName, query.getTaskName());
        }
        
        // 当前步骤模糊查询
        if (query.getCurrentStep() != null && !query.getCurrentStep().isEmpty()) {
            wrapper.like(DatasetBuildTask::getCurrentStep, query.getCurrentStep());
        }
        
        // 算法类型筛选
        if (query.getAlgorithmType() != null && !query.getAlgorithmType().isEmpty()) {
            wrapper.eq(DatasetBuildTask::getAlgorithmType, query.getAlgorithmType());
        }
        
        wrapper.orderByDesc(DatasetBuildTask::getCreateTime);
        
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(DatasetBuildTask task) {
        // 生成任务编号
        String taskNo = "DATASET_" + System.currentTimeMillis();
        task.setTaskNo(taskNo);
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setProgress(0f);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        this.save(task);
        
        log.info("创建数据集构建任务成功: taskId={}, taskNo={}, algorithmType={}", 
                task.getTaskId(), taskNo, task.getAlgorithmType());
        return task.getTaskId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Long taskId) {
        DatasetBuildTask task = this.getById(taskId);
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
    public void updateProgress(Long taskId, Float progress, String currentStep, String stepDetail) {
        DatasetBuildTask task = this.getById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }
        
        task.setProgress(progress);
        task.setCurrentStep(currentStep);
        task.setStepDetail(stepDetail);
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
    public void markTaskSuccess(DatasetTaskSuccessDTO successDTO) {
        DatasetBuildTask task = this.getById(successDTO.getTaskId());
        if (task == null) {
            log.warn("任务不存在: taskId={}", successDTO.getTaskId());
            return;
        }
        
        task.setStatus(TaskStatus.SUCCESS.getCode());
        task.setProgress(100f);
        task.setDatasetPath(successDTO.getDatasetPath());
        task.setDatasetSize(successDTO.getDatasetSize());
        task.setTotalImages(successDTO.getTotalImages());
        task.setTotalAnnotations(successDTO.getTotalAnnotations());
        task.setTrainCount(successDTO.getTrainCount());
        task.setValCount(successDTO.getValCount());
        task.setTestCount(successDTO.getTestCount());
        task.setClassDistribution(successDTO.getClassDistribution());
        task.setDataYamlPath(successDTO.getDataYamlPath());
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        // 计算耗时（秒）
        if (task.getStartTime() != null) {
            long duration = Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            task.setDurationSeconds((int) duration);
        }
        
        this.updateById(task);
        log.info("任务执行成功: taskId={}, datasetPath={}", successDTO.getTaskId(), successDTO.getDatasetPath());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markTaskFailed(Long taskId, String errorMessage, String errorStack) {
        DatasetBuildTask task = this.getById(taskId);
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
        log.error("任务执行失败: taskId={}, error={}", taskId, errorMessage);
    }
}

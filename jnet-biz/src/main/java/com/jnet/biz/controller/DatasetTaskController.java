 package com.jnet.biz.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.biz.config.RabbitMQConfig;
import com.jnet.biz.dto.AlgorithmTaskMessage;
import com.jnet.biz.dto.DatasetBuildRequestDTO;
import com.jnet.biz.dto.DatasetTaskQueryDTO;
import com.jnet.biz.entity.DatasetBuildTask;
import com.jnet.biz.enums.TaskStatus;
import com.jnet.biz.enums.TaskType;
import com.jnet.biz.service.IDatasetBuildTaskService;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 数据集构建任务控制器（通用，支持多种算法）
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Tag(name = "数据集构建任务", description = "通用的数据集构建任务管理接口，支持YOLO、RCNN等多种算法")
@Slf4j
@RestController
@RequestMapping("/api/v1/dataset-build-tasks")
@RequiredArgsConstructor
public class DatasetTaskController {
    
    private final IDatasetBuildTaskService datasetBuildTaskService;
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 分页查询任务列表
     */
    @Operation(summary = "分页查询数据集任务列表", description = "支持按项目、状态、任务名称、当前步骤等多条件筛选")
    @PostMapping("/page")
    public Result<Page<DatasetBuildTask>> pageTasks(
            @Parameter(description = "查询条件", required = true) 
            @RequestBody DatasetTaskQueryDTO query) {
        
        Page<DatasetBuildTask> page = datasetBuildTaskService.pageTasks(query);
        return Result.success(page);
    }
    
    /**
     * 获取任务详情
     */
    @Operation(summary = "获取数据集任务详情", description = "根据任务ID获取详细信息")
    @GetMapping("/{taskId}")
    public Result<DatasetBuildTask> getTaskDetail(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        DatasetBuildTask task = datasetBuildTaskService.getById(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }
    
    /**
     * 创建数据集构建任务（统一接口，支持多种算法）
     */
    @Operation(
        summary = "创建数据集构建任务", 
        description = "基于项目、批次、标签筛选图像，支持多种算法类型（YOLO、RCNN等）"
    )
    @PostMapping("/build")
    public Result<Map<String, Object>> buildDataset(
            @Parameter(description = "数据集构建请求", required = true) 
            @RequestBody @Validated DatasetBuildRequestDTO request) {
        
        // 1. 创建任务记录
        DatasetBuildTask task = new DatasetBuildTask();
        task.setProjectId(request.getProjectId());
        task.setTaskName(request.getTaskName());
        task.setDescription(request.getDescription());
        task.setAlgorithmType(request.getAlgorithmType());
        task.setBatchIds(request.getBatchIds() != null ? JSON.toJSONString(request.getBatchIds()) : null);
        task.setTagIds(request.getTagIds() != null ? JSON.toJSONString(request.getTagIds()) : null);
        task.setTrainRatio(request.getTrainRatio().floatValue());
        task.setValRatio(request.getValRatio().floatValue());
        task.setTestRatio(request.getTestRatio().floatValue());
        task.setMinImageSize(request.getMinImageSize());
        task.setMaxImageSize(request.getMaxImageSize());
        task.setCompressFormat(request.getCompress() ? "zip" : "none");
        task.setCompressQuality(request.getCompressQuality());
        task.setOutputFormat(request.getOutputFormat());
        task.setExtraConfig(request.getExtraConfig());
        task.setStatus(TaskStatus.PENDING.getCode());
        
        Long taskId = datasetBuildTaskService.createTask(task);
        
        // 2. 构建算法任务消息
        AlgorithmTaskMessage message = new AlgorithmTaskMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTaskId(taskId);
        message.setTaskNo(task.getTaskNo());
        message.setAlgorithmType(request.getAlgorithmType());
        message.setTaskType(TaskType.DATASET_BUILD.getCode());
        message.setProjectId(request.getProjectId());
        message.setCreateBy(null); // TODO: 从当前用户获取
        // 直接将请求DTO序列化为JSON，包含所有配置参数
        message.setConfigJson(JSON.toJSONString(request));
        message.setTimestamp(System.currentTimeMillis());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TASK_EXCHANGE,
            RabbitMQConfig.DATASET_BUILD_ROUTING_KEY,
            JSON.toJSONString(message)
        );
        
        log.info("创建数据集构建任务: taskId={}, algorithmType={}, projectId={}", 
                taskId, request.getAlgorithmType(), request.getProjectId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("taskNo", task.getTaskNo());
        data.put("algorithmType", request.getAlgorithmType());
        data.put("status", TaskStatus.PENDING.getCode());
        
        return Result.success("创建成功", data);
    }
    
    /**
     * 取消任务
     */
    @Operation(summary = "取消数据集任务", description = "取消正在执行的数据集构建任务")
    @PostMapping("/{taskId}/cancel")
    public Result<Void> cancelTask(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        boolean success = datasetBuildTaskService.cancelTask(taskId);
        if (success) {
            log.info("取消任务: taskId={}", taskId);
            return Result.success("取消成功", null);
        } else {
            return Result.error("取消失败，任务可能已结束或不存在");
        }
    }
    
    /**
     * 删除任务
     */
    @Operation(summary = "删除数据集任务", description = "删除指定的数据集构建任务")
    @DeleteMapping("/{taskId}")
    public Result<Void> deleteTask(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        boolean success = datasetBuildTaskService.removeById(taskId);
        if (success) {
            log.info("删除任务: taskId={}", taskId);
            return Result.success("删除成功", null);
        } else {
            return Result.error("删除失败，任务不存在");
        }
    }
}

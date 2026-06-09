package com.jnet.biz.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.biz.config.RabbitMQConfig;
import com.jnet.biz.dto.AlgorithmTaskMessage;
import com.jnet.biz.dto.TrainingTaskQueryDTO;
import com.jnet.biz.entity.YoloTrainingTask;
import com.jnet.biz.enums.AlgorithmType;
import com.jnet.biz.enums.TaskStatus;
import com.jnet.biz.enums.TaskType;
import com.jnet.biz.service.IYoloTrainingTaskService;
import com.jnet.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * YOLO训练任务控制器
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Tag(name = "YOLO训练任务", description = "YOLO模型训练任务相关接口")
@Slf4j
@RestController
@RequestMapping("/api/v1/yolo/training-tasks")
@RequiredArgsConstructor
public class YoloTrainingTaskController {
    
    private final IYoloTrainingTaskService trainingTaskService;
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 分页查询任务列表
     */
    @Operation(summary = "分页查询训练任务列表", description = "支持按项目、状态、任务名称、模型架构等多条件筛选")
    @PostMapping("/page")
    public Result<Page<YoloTrainingTask>> pageTasks(
            @Parameter(description = "查询条件", required = true) 
            @RequestBody TrainingTaskQueryDTO query) {
        
        Page<YoloTrainingTask> page = trainingTaskService.pageTasks(query);
        return Result.success(page);
    }
    
    /**
     * 获取任务详情
     */
    @Operation(summary = "获取训练任务详情", description = "根据任务ID获取详细信息")
    @GetMapping("/{taskId}")
    public Result<YoloTrainingTask> getTaskDetail(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        YoloTrainingTask task = trainingTaskService.getById(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        return Result.success(task);
    }
    
    /**
     * 创建训练任务
     */
    @Operation(summary = "创建训练任务", description = "创建新的YOLO模型训练任务")
    @PostMapping("/create")
    public Result<Map<String, Object>> createTask(
            @Parameter(description = "任务信息", required = true) @RequestBody YoloTrainingTask task) {
        
        // 1. 验证必填字段
        if (task.getDatasetPath() == null || task.getDatasetPath().trim().isEmpty()) {
            return Result.error("数据集路径不能为空");
        }
        if (task.getModelArchitecture() == null || task.getModelArchitecture().trim().isEmpty()) {
            return Result.error("模型架构不能为空");
        }
        if (task.getEpochs() == null || task.getEpochs() <= 0) {
            return Result.error("训练轮数必须大于0");
        }
        if (task.getBatchSize() == null || task.getBatchSize() <= 0) {
            return Result.error("批次大小必须大于0");
        }
        if (task.getImageSize() == null || task.getImageSize() <= 0) {
            return Result.error("图像尺寸必须大于0");
        }
        
        // 2. 设置默认值
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING.getCode());
        }
        if (task.getOptimizer() == null) {
            task.setOptimizer("SGD");
        }
        if (task.getGpuIds() == null) {
            task.setGpuIds("0");
        }
        if (task.getNumWorkers() == null) {
            task.setNumWorkers(8);
        }
        if (task.getMixedPrecision() == null) {
            task.setMixedPrecision(true);
        }
        
        // 3. 创建任务记录
        Long taskId = trainingTaskService.createTask(task);
        
        // 4. 构建训练配置JSON（关键修复：将YoloTrainingTask转换为YoloTrainingConfig格式）
        String configJson = buildTrainingConfigJson(task);
        
        // 5. 发送消息到RabbitMQ
        AlgorithmTaskMessage message = new AlgorithmTaskMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTaskId(taskId);
        message.setTaskNo(task.getTaskNo());
        message.setAlgorithmType(AlgorithmType.YOLO.getCode());
        message.setTaskType(TaskType.TRAINING.getCode());
        message.setProjectId(task.getProjectId());
        message.setCreateBy(task.getCreateBy());
        message.setConfigJson(configJson); // ✅ 修复：传递完整的配置JSON
        message.setTimestamp(System.currentTimeMillis());
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TASK_EXCHANGE,
            RabbitMQConfig.TRAINING_ROUTING_KEY,
            JSON.toJSONString(message)
        );
        
        log.info("创建训练任务: taskId={}, taskNo={}, datasetPath={}, model={}", 
            taskId, task.getTaskNo(), task.getDatasetPath(), task.getModelArchitecture());
        
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("taskNo", task.getTaskNo());
        data.put("status", TaskStatus.PENDING.getCode());
        
        return Result.success("创建成功", data);
    }
    
    /**
     * 构建训练配置JSON
     * 将YoloTrainingTask转换为YoloTrainingConfig格式的JSON
     */
    private String buildTrainingConfigJson(YoloTrainingTask task) {
        // 使用Map构建配置（与YoloTrainingConfig字段对应）
        Map<String, Object> config = new HashMap<>();
        
        // 数据源配置
        config.put("datasetPath", task.getDatasetPath());
        config.put("customDatasetPath", task.getCustomDatasetPath());
        
        // 训练超参数
        config.put("epochs", task.getEpochs());
        config.put("batchSize", task.getBatchSize());
        config.put("imageSize", task.getImageSize());
        config.put("learningRate", task.getLearningRate());
        config.put("momentum", task.getMomentum());
        config.put("weightDecay", task.getWeightDecay());
        config.put("optimizer", task.getOptimizer());
        config.put("lrScheduler", task.getLrScheduler());
        config.put("warmupEpochs", task.getWarmupEpochs());
        config.put("patience", task.getPatience());
        
        // 数据增强配置
        config.put("hsvH", task.getHsvH());
        config.put("hsvS", task.getHsvS());
        config.put("hsvV", task.getHsvV());
        config.put("degrees", task.getDegrees());
        config.put("translate", task.getTranslate());
        config.put("scale", task.getScale());
        config.put("shear", task.getShear());
        config.put("perspective", task.getPerspective());
        config.put("flipLr", task.getFlipLr());
        config.put("flipUd", task.getFlipUd());
        
        // 硬件配置
        config.put("gpuIds", task.getGpuIds());
        config.put("numWorkers", task.getNumWorkers());
        config.put("mixedPrecision", task.getMixedPrecision());
        
        // YOLO特有配置
        config.put("modelName", task.getModelArchitecture());
        config.put("device", task.getGpuIds());
        config.put("workers", task.getNumWorkers());
        config.put("usePretrained", task.getPretrainedWeights() != null);
        
        // 其他配置
        config.put("projectName", task.getTaskName());
        
        return JSON.toJSONString(config);
    }
    
    /**
     * 取消任务
     */
    @Operation(summary = "取消训练任务", description = "取消正在执行的模型训练任务")
    @PostMapping("/{taskId}/cancel")
    public Result<Void> cancelTask(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        boolean success = trainingTaskService.cancelTask(taskId);
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
    @Operation(summary = "删除训练任务", description = "删除指定的模型训练任务")
    @DeleteMapping("/{taskId}")
    public Result<Void> deleteTask(
            @Parameter(description = "任务ID", required = true, example = "1") @PathVariable Long taskId) {
        boolean success = trainingTaskService.removeById(taskId);
        if (success) {
            log.info("删除任务: taskId={}", taskId);
            return Result.success("删除成功", null);
        } else {
            return Result.error("删除失败，任务不存在");
        }
    }
}

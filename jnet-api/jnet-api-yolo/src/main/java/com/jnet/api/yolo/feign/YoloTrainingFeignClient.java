package com.jnet.api.yolo.feign;

import com.jnet.api.yolo.dto.*;
import com.jnet.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * YOLO训练服务 Feign 客户端
 * 用于其他微服务调用 Python YOLO 训练服务的 REST API
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@FeignClient(
    name = "jnet-yolo-training",
    path = "/api/v1",
    contextId = "yoloTrainingFeignClient",
    fallbackFactory = YoloTrainingFeignFallbackFactory.class
)
public interface YoloTrainingFeignClient {
    
    // ==================== 训练任务管理 ====================
    
    /**
     * 创建训练任务（需要data.yaml）
     * 
     * @param request 训练配置请求
     * @return 任务创建响应，包含task_id
     */
    @PostMapping("/training/tasks")
    Result<TrainingTaskCreateResponse> createTrainingTask(@RequestBody TrainingConfigRequest request);
    
    /**
     * 创建训练任务（无需data.yaml，自动生成）
     * 
     * @param request 训练配置请求（提供图片目录和类别列表）
     * @return 任务创建响应，包含task_id
     */
    @PostMapping("/training/tasks/no-yaml")
    Result<TrainingTaskCreateResponse> createTrainingTaskNoYaml(@RequestBody TrainingConfigNoYamlRequest request);
    
    /**
     * 启动训练任务
     * 
     * @param taskId 任务ID
     * @return 启动结果
     */
    @PostMapping("/training/tasks/{taskId}/start")
    Result<Map<String, Object>> startTrainingTask(@PathVariable("taskId") String taskId);
    
    /**
     * 列出所有训练任务
     * 
     * @param status 状态过滤器（可选）: pending/running/completed/failed/cancelled
     * @return 任务列表
     */
    @GetMapping("/training/tasks")
    Result<List<TrainingTaskStatus>> listTrainingTasks(@RequestParam(value = "status", required = false) String status);
    
    /**
     * 获取训练任务状态
     * 
     * @param taskId 任务ID
     * @return 任务详细状态
     */
    @GetMapping("/training/tasks/{taskId}")
    Result<TrainingTaskStatus> getTrainingTaskStatus(@PathVariable("taskId") String taskId);
    
    /**
     * 取消训练任务
     * 
     * @param taskId 任务ID
     * @return 取消结果
     */
    @PostMapping("/training/tasks/{taskId}/cancel")
    Result<Map<String, Object>> cancelTrainingTask(@PathVariable("taskId") String taskId);
    
    /**
     * 获取训练日志
     * 
     * @param taskId 任务ID
     * @param lines 返回的行数 (1-1000)
     * @return 训练日志内容
     */
    @GetMapping("/training/tasks/{taskId}/log")
    Result<Map<String, Object>> getTrainingLog(
        @PathVariable("taskId") String taskId,
        @RequestParam(value = "lines", defaultValue = "100") Integer lines
    );
    
    // ==================== 系统信息 ====================
    
    /**
     * 获取系统信息
     * 
     * @return 系统和服务配置信息
     */
    @GetMapping("/system/info")
    Result<Map<String, Object>> getSystemInfo();
    
    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    Result<Map<String, Object>> healthCheck();
}

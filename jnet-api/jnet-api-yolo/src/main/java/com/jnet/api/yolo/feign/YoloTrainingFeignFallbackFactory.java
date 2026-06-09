package com.jnet.api.yolo.feign;

import com.jnet.api.yolo.dto.*;
import com.jnet.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YOLO训练服务 Feign 客户端降级工厂
 * 当 YOLO 训练服务不可用时提供降级处理
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Slf4j
@Component
public class YoloTrainingFeignFallbackFactory implements FallbackFactory<YoloTrainingFeignClient> {
    
    @Override
    public YoloTrainingFeignClient create(Throwable cause) {
        log.error("YOLO训练服务调用失败，触发降级处理", cause);
        
        return new YoloTrainingFeignClient() {
            
            @Override
            public Result<TrainingTaskCreateResponse> createTrainingTask(TrainingConfigRequest request) {
                log.warn("创建训练任务接口降级");
                return Result.error("YOLO训练服务暂时不可用，请稍后重试");
            }
            
            @Override
            public Result<TrainingTaskCreateResponse> createTrainingTaskNoYaml(TrainingConfigNoYamlRequest request) {
                log.warn("创建训练任务（无yaml）接口降级");
                return Result.error("YOLO训练服务暂时不可用，请稍后重试");
            }
            
            @Override
            public Result<Map<String, Object>> startTrainingTask(String taskId) {
                log.warn("启动训练任务接口降级, taskId: {}", taskId);
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("success", false);
                errorData.put("message", "YOLO训练服务暂时不可用");
                return Result.error("YOLO训练服务暂时不可用，请稍后重试");
            }
            
            @Override
            public Result<List<TrainingTaskStatus>> listTrainingTasks(String status) {
                log.warn("列出训练任务接口降级");
                return Result.success(Collections.emptyList());
            }
            
            @Override
            public Result<TrainingTaskStatus> getTrainingTaskStatus(String taskId) {
                log.warn("获取训练任务状态接口降级, taskId: {}", taskId);
                return Result.error("无法获取任务状态，YOLO训练服务暂时不可用");
            }
            
            @Override
            public Result<Map<String, Object>> cancelTrainingTask(String taskId) {
                log.warn("取消训练任务接口降级, taskId: {}", taskId);
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("success", false);
                errorData.put("message", "YOLO训练服务暂时不可用");
                return Result.error("YOLO训练服务暂时不可用，请稍后重试");
            }
            
            @Override
            public Result<Map<String, Object>> getTrainingLog(String taskId, Integer lines) {
                log.warn("获取训练日志接口降级, taskId: {}, lines: {}", taskId, lines);
                Map<String, Object> logData = new HashMap<>();
                logData.put("success", true);
                logData.put("task_id", taskId);
                logData.put("log", "无法获取日志，YOLO训练服务暂时不可用");
                return Result.success(logData);
            }
            
            @Override
            public Result<Map<String, Object>> getSystemInfo() {
                log.warn("获取系统信息接口降级");
                return Result.error("YOLO训练服务暂时不可用");
            }
            
            @Override
            public Result<Map<String, Object>> healthCheck() {
                log.warn("健康检查接口降级");
                Map<String, Object> healthData = new HashMap<>();
                healthData.put("status", "unhealthy");
                healthData.put("message", "YOLO训练服务暂时不可用");
                return Result.success(healthData);
            }
        };
    }
}

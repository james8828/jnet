package com.jnet.api.yolo.example;

import com.jnet.api.yolo.dto.*;
import com.jnet.api.yolo.feign.YoloTrainingFeignClient;
import com.jnet.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * YOLO训练服务 Feign 客户端使用示例
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YoloTrainingUsageExample {
    
    private final YoloTrainingFeignClient yoloTrainingClient;
    
    /**
     * 示例1: 创建训练任务（需要data.yaml）
     */
    public void example1_CreateTrainingTaskWithYaml() {
        // 构建训练配置
        TrainingConfigRequest request = TrainingConfigRequest.builder()
            .datasetYaml("/path/to/data.yaml")
            .epochs(300)
            .batchSize(4)
            .imageSize(1280)
            .device("0")
            .weights("yolov7x.pt")
            .useAdam(false)
            .workers(4)
            .cache(false)
            .build();
        
        // 调用 Feign 客户端
        Result<TrainingTaskCreateResponse> result = yoloTrainingClient.createTrainingTask(request);
        
        if (result.isSuccess() && result.getData() != null) {
            String taskId = result.getData().getTaskId();
            log.info("训练任务创建成功, taskId: {}", taskId);
            
            // 启动训练
            startTraining(taskId);
        } else {
            log.error("训练任务创建失败: {}", result.getMessage());
        }
    }
    
    /**
     * 示例2: 创建训练任务（无需data.yaml，自动生成）
     */
    public void example2_CreateTrainingTaskWithoutYaml() {
        // 构建训练配置
        TrainingConfigNoYamlRequest request = TrainingConfigNoYamlRequest.builder()
            .trainDir("/path/to/train/images")
            .valDir("/path/to/val/images")
            .testDir("/path/to/test/images")  // 可选
            .classes(Arrays.asList("person", "car", "dog"))
            .epochs(300)
            .batchSize(4)
            .imageSize(1280)
            .device("0")
            .weights("yolov7x.pt")
            .build();
        
        // 调用 Feign 客户端
        Result<TrainingTaskCreateResponse> result = yoloTrainingClient.createTrainingTaskNoYaml(request);
        
        if (result.isSuccess() && result.getData() != null) {
            String taskId = result.getData().getTaskId();
            log.info("训练任务创建成功（自动生成data.yaml）, taskId: {}", taskId);
            
            // 启动训练
            startTraining(taskId);
        } else {
            log.error("训练任务创建失败: {}", result.getMessage());
        }
    }
    
    /**
     * 启动训练任务
     */
    private void startTraining(String taskId) {
        Result<Map<String, Object>> result = yoloTrainingClient.startTrainingTask(taskId);
        
        if (result.isSuccess()) {
            log.info("训练任务已启动, taskId: {}", taskId);
        } else {
            log.error("训练任务启动失败: {}", result.getMessage());
        }
    }
    
    /**
     * 示例3: 查询训练任务状态
     */
    public void example3_CheckTrainingStatus(String taskId) {
        Result<TrainingTaskStatus> result = yoloTrainingClient.getTrainingTaskStatus(taskId);
        
        if (result.isSuccess() && result.getData() != null) {
            TrainingTaskStatus status = result.getData();
            log.info("任务状态: {}, 进度: {}%, 当前Epoch: {}/{}",
                status.getStatus(),
                status.getProgress(),
                status.getCurrentEpoch(),
                status.getTotalEpochs()
            );
            
            // 如果有训练指标
            if (status.getMetrics() != null) {
                log.info("训练指标: {}", status.getMetrics());
            }
        } else {
            log.error("获取任务状态失败: {}", result.getMessage());
        }
    }
    
    /**
     * 示例4: 列出所有训练任务
     */
    public void example4_ListAllTasks() {
        // 列出所有任务
        Result<List<TrainingTaskStatus>> result = yoloTrainingClient.listTrainingTasks(null);
        
        if (result.isSuccess() && result.getData() != null) {
            List<TrainingTaskStatus> tasks = result.getData();
            log.info("共有 {} 个训练任务", tasks.size());
            
            tasks.forEach(task -> {
                log.info("任务ID: {}, 状态: {}, 进度: {}%",
                    task.getTaskId(),
                    task.getStatus(),
                    task.getProgress()
                );
            });
        }
    }
    
    /**
     * 示例5: 列出特定状态的任务
     */
    public void example5_ListRunningTasks() {
        // 只列出运行中的任务
        Result<List<TrainingTaskStatus>> result = yoloTrainingClient.listTrainingTasks("running");
        
        if (result.isSuccess() && result.getData() != null) {
            List<TrainingTaskStatus> runningTasks = result.getData();
            log.info("当前有 {} 个正在运行的训练任务", runningTasks.size());
        }
    }
    
    /**
     * 示例6: 取消训练任务
     */
    public void example6_CancelTask(String taskId) {
        Result<Map<String, Object>> result = yoloTrainingClient.cancelTrainingTask(taskId);
        
        if (result.isSuccess()) {
            log.info("训练任务已取消, taskId: {}", taskId);
        } else {
            log.error("取消训练任务失败: {}", result.getMessage());
        }
    }
    
    /**
     * 示例7: 获取训练日志
     */
    public void example7_GetTrainingLog(String taskId) {
        Result<Map<String, Object>> result = yoloTrainingClient.getTrainingLog(taskId, 100);
        
        if (result.isSuccess() && result.getData() != null) {
            String logContent = (String) result.getData().get("log");
            log.info("训练日志:\n{}", logContent);
        }
    }
    
    /**
     * 示例8: 健康检查
     */
    public void example8_HealthCheck() {
        Result<Map<String, Object>> result = yoloTrainingClient.healthCheck();
        
        if (result.isSuccess() && result.getData() != null) {
            Map<String, Object> healthInfo = result.getData();
            log.info("YOLO训练服务健康状态: {}", healthInfo.get("status"));
            log.info("GPU可用: {}", healthInfo.get("gpu_available"));
        }
    }
    
    /**
     * 示例9: 获取系统信息
     */
    public void example9_GetSystemInfo() {
        Result<Map<String, Object>> result = yoloTrainingClient.getSystemInfo();
        
        if (result.isSuccess() && result.getData() != null) {
            Map<String, Object> systemInfo = result.getData();
            log.info("系统信息: {}", systemInfo);
        }
    }
}

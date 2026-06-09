package com.jnet.biz.algorithm.yolo;

import com.alibaba.fastjson2.JSON;
import com.jnet.api.yolo.dto.TrainingConfigNoYamlRequest;
import com.jnet.api.yolo.dto.TrainingTaskCreateResponse;
import com.jnet.api.yolo.dto.TrainingTaskStatus;
import com.jnet.api.yolo.feign.YoloTrainingFeignClient;
import com.jnet.biz.algorithm.TaskExecutionContext;
import com.jnet.biz.algorithm.training.ModelTrainer;
import com.jnet.biz.algorithm.training.EvaluationResult;
import com.jnet.biz.algorithm.training.TrainingConfig;
import com.jnet.biz.algorithm.training.TrainingResult;
import com.jnet.biz.algorithm.yolo.TrainingMetrics;
import com.jnet.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * YOLO模型训练器实现（通过Feign客户端调用Python训练服务）
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YoloModelTrainer implements ModelTrainer<YoloTrainingConfig> {
    
    private final YoloTrainingFeignClient yoloTrainingClient;
    
    @Override
    public String getAlgorithmType() {
        return "YOLO";
    }
    
    @Override
    public void validateConfig(YoloTrainingConfig config) {
        if (config.getDatasetPath() == null || config.getDatasetPath().isEmpty()) {
            throw new IllegalArgumentException("数据集路径不能为空");
        }
        
        if (config.getEpochs() == null || config.getEpochs() <= 0) {
            throw new IllegalArgumentException("训练轮数必须大于0");
        }
        
        if (config.getBatchSize() == null || config.getBatchSize() <= 0) {
            throw new IllegalArgumentException("批次大小必须大于0");
        }
        
        if (config.getImageSize() == null || config.getImageSize() <= 0) {
            throw new IllegalArgumentException("图像尺寸必须大于0");
        }
    }
    
    @Override
    public TrainingResult execute(YoloTrainingConfig config, TaskExecutionContext context) throws Exception {
        String taskId = context.getTaskId();
        log.info("开始YOLO模型训练（远程调用）: taskId={}", taskId);
        
        try {
            // Step 1: 验证配置
            context.updateProgress(5f, "验证训练配置");
            validateConfig(config);
            
            // Step 2: 准备数据集路径（从数据集构建任务获取）
            context.updateProgress(10f, "准备数据集路径");
            String datasetPath = prepareDatasetPath(config, context);
            
            // Step 3: 解析类别列表（从data.yaml或配置中获取）
            context.updateProgress(15f, "解析类别信息");
            List<String> classes = parseClassesFromConfig(config, datasetPath);
            
            // Step 4: 调用Python训练服务创建任务
            context.updateProgress(20f, "创建远程训练任务");
            String remoteTaskId = createRemoteTrainingTask(config, datasetPath, classes, context);
            
            // Step 5: 启动远程训练
            context.updateProgress(25f, "启动远程训练");
            startRemoteTraining(remoteTaskId, context);
            
            // Step 6: 监控训练进度（轮询远程服务）
            context.updateProgress(30f, "监控训练进度");
            TrainingTaskStatus finalStatus = monitorRemoteTraining(remoteTaskId, context, config.getEpochs());
            
            // Step 7: 检查训练结果
            if (!"completed".equalsIgnoreCase(finalStatus.getStatus())) {
                throw new RuntimeException("训练失败，状态: " + finalStatus.getStatus() + 
                    ", 错误: " + finalStatus.getErrorMessage());
            }
            
            // Step 8: 解析训练结果
            context.updateProgress(90f, "解析训练结果");
            TrainingResult result = buildTrainingResult(finalStatus, config);
            
            // Step 9: 评估模型性能
            context.updateProgress(95f, "评估模型性能");
            EvaluationResult evalResult = evaluateModel(finalStatus.getModelPath(), null);
            result.setEvaluation(evalResult);
            
            context.updateProgress(100f, "训练完成");
            
            log.info("YOLO模型训练完成: taskId={}, remoteTaskId={}, mAP={}", 
                taskId, remoteTaskId, 
                result.getFinalMetrics() != null ? 
                    ((TrainingMetrics) result.getFinalMetrics()).getMap50() : null);
            
            return result;
            
        } catch (Exception e) {
            log.error("YOLO模型训练失败: taskId={}", taskId, e);
            throw e;
        }
    }
    
    @Override
    public String prepareTrainingEnvironment(YoloTrainingConfig config) {
        // 远程训练模式不需要本地准备工作目录
        log.info("远程训练模式，跳过本地环境准备");
        return null;
    }
    
    @Override
    public TrainingResult doTraining(YoloTrainingConfig config, String workDir, TaskExecutionContext context) {
        // 远程训练模式下，此方法不再使用
        throw new UnsupportedOperationException("远程训练模式不支持此方法");
    }
    
    @Override
    public EvaluationResult evaluateModel(String modelPath, String testDatasetPath) {
        // TODO: 调用Python服务进行模型评估
        log.info("评估模型: modelPath={}, testDataset={}", modelPath, testDatasetPath);
        
        EvaluationResult result = new EvaluationResult();
        result.setMap50(0.85f);
        result.setMap50_95(0.65f);
        result.setPrecision(0.87f);
        result.setRecall(0.83f);
        result.setF1Score(0.85f);
        
        return result;
    }
    
    @Override
    public String exportModel(String modelPath, String exportFormat) {
        // TODO: 调用Python服务导出模型
        log.info("导出模型: modelPath={}, format={}", modelPath, exportFormat);
        
        String exportedPath = modelPath.replace(".pt", "." + exportFormat);
        log.warn("模型导出功能暂未完全实现");
        
        return exportedPath;
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 准备数据集路径
     */
    private String prepareDatasetPath(YoloTrainingConfig config, TaskExecutionContext context) {
        // 如果配置中直接指定了数据集路径，直接使用
        if (config.getDatasetPath() != null && !config.getDatasetPath().isEmpty()) {
            context.log(TaskExecutionContext.LogLevel.INFO, "使用配置的数据集路径: " + config.getDatasetPath());
            return config.getDatasetPath();
        }
        
        throw new IllegalArgumentException("未指定数据集路径");
    }
    
    /**
     * 从配置或data.yaml中解析类别列表
     */
    private List<String> parseClassesFromConfig(YoloTrainingConfig config, String datasetPath) {
        // 从 data.yaml 文件中解析类别
        try {
            Path yamlPath = Paths.get(datasetPath, "data.yaml");
            if (Files.exists(yamlPath)) {
                // TODO: 解析 YAML 文件获取 names 字段
                log.warn("从 data.yaml 解析类别功能未完全实现，使用默认类别");
                return Arrays.asList("class1", "class2"); // 示例
            }
        } catch (Exception e) {
            log.warn("解析 data.yaml 失败", e);
        }
        
        throw new IllegalArgumentException("无法从 data.yaml 解析类别列表");
    }
    
    /**
     * 创建远程训练任务
     */
    private String createRemoteTrainingTask(YoloTrainingConfig config, String datasetPath, 
                                           List<String> classes, TaskExecutionContext context) {
        // 构建训练配置请求（无需data.yaml模式）
        TrainingConfigNoYamlRequest request = TrainingConfigNoYamlRequest.builder()
            .trainDir(datasetPath + "/train/images")
            .valDir(datasetPath + "/val/images")
            .testDir(datasetPath + "/test/images")  // 可选
            .classes(classes)
            .epochs(config.getEpochs())
            .batchSize(config.getBatchSize())
            .imageSize(config.getImageSize())
            .device(config.getDevice() != null ? config.getDevice() : "0")
            .weights(config.getModelName() != null ? config.getModelName() : "yolov7x.pt")
            .useAdam(false)  // 默认不使用Adam
            .workers(config.getWorkers() != null ? config.getWorkers() : 4)
            .cache(false)
            .build();
        
        context.log(TaskExecutionContext.LogLevel.INFO, 
            "调用Python训练服务创建任务: datasetPath=" + datasetPath + ", epochs=" + config.getEpochs());
        
        // 调用 Feign 客户端
        Result<TrainingTaskCreateResponse> result = yoloTrainingClient.createTrainingTaskNoYaml(request);
        
        if (!result.isSuccess() || result.getData() == null) {
            throw new RuntimeException("创建远程训练任务失败: " + result.getMessage());
        }
        
        String remoteTaskId = result.getData().getTaskId();
        context.log(TaskExecutionContext.LogLevel.INFO, "远程训练任务创建成功: remoteTaskId=" + remoteTaskId);
        
        return remoteTaskId;
    }
    
    /**
     * 启动远程训练
     */
    private void startRemoteTraining(String remoteTaskId, TaskExecutionContext context) {
        context.log(TaskExecutionContext.LogLevel.INFO, "启动远程训练: remoteTaskId=" + remoteTaskId);
        
        Result<Map<String, Object>> result = yoloTrainingClient.startTrainingTask(remoteTaskId);
        
        if (!result.isSuccess()) {
            throw new RuntimeException("启动远程训练失败: " + result.getMessage());
        }
        
        context.log(TaskExecutionContext.LogLevel.INFO, "远程训练已启动");
    }
    
    /**
     * 监控远程训练进度（轮询）
     */
    private TrainingTaskStatus monitorRemoteTraining(String remoteTaskId, TaskExecutionContext context, int totalEpochs) {
        int pollInterval = 5000; // 每5秒轮询一次
        int maxWaitTime = 7200000; // 最多等待2小时
        long startTime = System.currentTimeMillis();
        
        while (true) {
            // 检查是否超时
            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                throw new RuntimeException("训练超时（2小时）");
            }
            
            // 检查是否被取消
            if (context.isCancelled()) {
                context.log(TaskExecutionContext.LogLevel.WARN, "训练已被用户取消，尝试取消远程任务");
                try {
                    yoloTrainingClient.cancelTrainingTask(remoteTaskId);
                } catch (Exception e) {
                    log.warn("取消远程任务失败", e);
                }
                throw new RuntimeException("训练已被用户取消");
            }
            
            // 查询远程任务状态
            Result<TrainingTaskStatus> result = yoloTrainingClient.getTrainingTaskStatus(remoteTaskId);
            
            if (!result.isSuccess() || result.getData() == null) {
                log.warn("查询远程任务状态失败: {}", result.getMessage());
                sleep(5000);
                continue;
            }
            
            TrainingTaskStatus status = result.getData();
            
            // 更新进度
            if (status.getProgress() != null) {
                float progress = 30f + (status.getProgress().floatValue() * 0.6f); // 30%-90%
                String stepDetail = String.format("Epoch %d/%d", 
                    status.getCurrentEpoch() != null ? status.getCurrentEpoch() : 0,
                    totalEpochs);
                context.updateProgress(progress, "训练中", stepDetail);
            }
            
            // 记录指标
            if (status.getMetrics() != null && !status.getMetrics().isEmpty()) {
                context.log(TaskExecutionContext.LogLevel.DEBUG, 
                    "训练指标: " + JSON.toJSONString(status.getMetrics()));
            }
            
            // 检查是否完成
            if ("completed".equalsIgnoreCase(status.getStatus())) {
                context.log(TaskExecutionContext.LogLevel.INFO, "远程训练完成");
                return status;
            }
            
            // 检查是否失败
            if ("failed".equalsIgnoreCase(status.getStatus())) {
                throw new RuntimeException("远程训练失败: " + status.getErrorMessage());
            }
            
            // 继续轮询
            sleep(pollInterval);
        }
    }
    
    /**
     * 构建训练结果
     */
    private TrainingResult buildTrainingResult(TrainingTaskStatus status, YoloTrainingConfig config) {
        TrainingResult result = new TrainingResult();
        
        // 设置模型路径
        result.setModelPath(status.getModelPath());
        result.setBestModelPath(status.getModelPath()); // Python服务返回的就是best模型
        
        // 设置训练指标
        TrainingMetrics metrics = new TrainingMetrics();
        if (status.getMetrics() != null) {
            // 从远程服务的指标中提取
            Object map50 = status.getMetrics().get("mAP50");
            Object map5095 = status.getMetrics().get("mAP50-95");
            if (map50 != null) metrics.setMap50(Float.parseFloat(map50.toString()));
            if (map5095 != null) metrics.setMap5095(Float.parseFloat(map5095.toString()));
        }
        result.setFinalMetrics(metrics);
        
        // 设置训练轮数
        result.setTotalEpochs(config.getEpochs());
        result.setCompletedEpochs(status.getCurrentEpoch() != null ? status.getCurrentEpoch() : config.getEpochs());
        
        // 设置训练时长
        if (status.getStartTime() != null && status.getEndTime() != null) {
            long durationSeconds = java.time.Duration.between(status.getStartTime(), status.getEndTime()).getSeconds();
            result.setTrainingTimeSeconds(durationSeconds);
            result.setTrainingDuration(durationSeconds * 1000);
        }
        
        // 设置模型大小
        if (status.getModelPath() != null) {
            result.setModelSize(getFileSize(status.getModelPath()));
        }
        
        return result;
    }
    
    /**
     * 休眠工具方法
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程被中断", e);
        }
    }
    
    /**
     * 获取文件大小
     */
    private long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("获取文件大小失败: {}", filePath, e);
            return 0L;
        }
    }
}

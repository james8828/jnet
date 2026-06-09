# jnet-api-yolo

YOLO训练服务API接口模块，提供Feign客户端供其他微服务调用Python YOLO训练服务。

## 功能特性

- ✅ 创建训练任务（支持需要data.yaml和自动生成data.yaml两种模式）
- ✅ 启动/取消训练任务
- ✅ 查询训练任务状态和进度
- ✅ 获取训练日志
- ✅ 列出所有训练任务
- ✅ 健康检查和系统信息查询
- ✅ 完整的降级处理机制

## 快速开始

### 1. 添加依赖

在需要使用YOLO训练服务的模块的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.jnet</groupId>
    <artifactId>jnet-api-yolo</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 启用Feign客户端

在Spring Boot启动类或配置类上添加注解：

```java
@EnableFeignClients(basePackages = "com.jnet.api.yolo")
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 3. 注入并使用

```java
@Service
@RequiredArgsConstructor
public class YourService {
    
    private final YoloTrainingFeignClient yoloTrainingClient;
    
    public void trainModel() {
        // 创建训练任务（无需data.yaml）
        TrainingConfigNoYamlRequest request = TrainingConfigNoYamlRequest.builder()
            .trainDir("/path/to/train/images")
            .valDir("/path/to/val/images")
            .classes(Arrays.asList("person", "car", "dog"))
            .epochs(300)
            .batchSize(4)
            .build();
        
        Result<TrainingTaskCreateResponse> result = 
            yoloTrainingClient.createTrainingTaskNoYaml(request);
        
        if (result.isSuccess()) {
            String taskId = result.getData().getTaskId();
            // 启动训练
            yoloTrainingClient.startTrainingTask(taskId);
        }
    }
}
```

## API 接口说明

### 训练任务管理

#### 1. 创建训练任务（需要data.yaml）

```java
@PostMapping("/training/tasks")
Result<TrainingTaskCreateResponse> createTrainingTask(@RequestBody TrainingConfigRequest request);
```

**使用场景**：已有标准的YOLO数据集配置文件（data.yaml）

**示例**：
```java
TrainingConfigRequest request = TrainingConfigRequest.builder()
    .datasetYaml("/path/to/data.yaml")
    .epochs(300)
    .batchSize(4)
    .imageSize(1280)
    .device("0")
    .weights("yolov7x.pt")
    .build();

Result<TrainingTaskCreateResponse> result = yoloTrainingClient.createTrainingTask(request);
```

#### 2. 创建训练任务（无需data.yaml）

```java
@PostMapping("/training/tasks/no-yaml")
Result<TrainingTaskCreateResponse> createTrainingTaskNoYaml(@RequestBody TrainingConfigNoYamlRequest request);
```

**使用场景**：只有图片目录和类别列表，需要自动生成data.yaml

**示例**：
```java
TrainingConfigNoYamlRequest request = TrainingConfigNoYamlRequest.builder()
    .trainDir("/path/to/train/images")
    .valDir("/path/to/val/images")
    .testDir("/path/to/test/images")  // 可选
    .classes(Arrays.asList("person", "car", "dog"))
    .epochs(300)
    .batchSize(4)
    .imageSize(1280)
    .build();

Result<TrainingTaskCreateResponse> result = yoloTrainingClient.createTrainingTaskNoYaml(request);
```

#### 3. 启动训练任务

```java
@PostMapping("/training/tasks/{taskId}/start")
Result<Map<String, Object>> startTrainingTask(@PathVariable("taskId") String taskId);
```

#### 4. 查询任务状态

```java
@GetMapping("/training/tasks/{taskId}")
Result<TrainingTaskStatus> getTrainingTaskStatus(@PathVariable("taskId") String taskId);
```

**返回信息**：
- 任务状态（pending/running/completed/failed/cancelled）
- 进度百分比
- 当前epoch / 总epoch
- 训练指标（mAP, loss等）
- 模型输出路径

#### 5. 列出所有任务

```java
@GetMapping("/training/tasks")
Result<List<TrainingTaskStatus>> listTrainingTasks(@RequestParam(value = "status", required = false) String status);
```

**参数**：
- `status`: 可选的状态过滤器（pending/running/completed/failed/cancelled）

#### 6. 取消任务

```java
@PostMapping("/training/tasks/{taskId}/cancel")
Result<Map<String, Object>> cancelTrainingTask(@PathVariable("taskId") String taskId);
```

#### 7. 获取训练日志

```java
@GetMapping("/training/tasks/{taskId}/log")
Result<Map<String, Object>> getTrainingLog(
    @PathVariable("taskId") String taskId,
    @RequestParam(value = "lines", defaultValue = "100") Integer lines
);
```

### 系统信息

#### 健康检查

```java
@GetMapping("/health")
Result<Map<String, Object>> healthCheck();
```

#### 获取系统信息

```java
@GetMapping("/system/info")
Result<Map<String, Object>> getSystemInfo();
```

**返回信息**：
- Python版本
- GPU可用性
- CUDA设备信息
- YOLOv7根目录
- 支持的模型列表

## DTO 说明

### TrainingConfigRequest
需要data.yaml的训练配置请求

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| datasetYaml | String | 是 | - | 数据集YAML配置文件路径 |
| epochs | Integer | 否 | 300 | 训练轮数 |
| batchSize | Integer | 否 | 4 | 批次大小 |
| imageSize | Integer | 否 | 1280 | 图像尺寸 |
| device | String | 否 | "0" | 设备（cpu或GPU ID） |
| weights | String | 否 | "yolov7x.pt" | 预训练权重文件 |
| useAdam | Boolean | 否 | false | 是否使用Adam优化器 |
| hyp | String | 否 | "data/hyp.scratch.p5.yaml" | 超参数配置文件 |
| workers | Integer | 否 | 4 | 数据加载工作进程数 |
| cache | Boolean | 否 | false | 是否缓存图像到内存 |

### TrainingConfigNoYamlRequest
无需data.yaml的训练配置请求（自动生成）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| trainDir | String | 是 | - | 训练集图片目录路径 |
| valDir | String | 是 | - | 验证集图片目录路径 |
| testDir | String | 否 | null | 测试集图片目录路径 |
| classes | List\<String\> | 是 | - | 类别名称列表 |
| nc | Integer | 否 | null | 类别数量（默认根据classes计算） |
| epochs | Integer | 否 | 300 | 训练轮数 |
| batchSize | Integer | 否 | 4 | 批次大小 |
| imageSize | Integer | 否 | 1280 | 图像尺寸 |
| device | String | 否 | "0" | 设备 |
| weights | String | 否 | "yolov7x.pt" | 预训练权重文件 |
| useAdam | Boolean | 否 | false | 是否使用Adam优化器 |
| hyp | String | 否 | "data/hyp.scratch.p5.yaml" | 超参数配置文件 |
| workers | Integer | 否 | 4 | 数据加载工作进程数 |
| cache | Boolean | 否 | false | 是否缓存图像到内存 |

### TrainingTaskStatus
训练任务状态

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务ID |
| status | String | 任务状态（pending/running/completed/failed/cancelled） |
| progress | Double | 进度百分比（0-100） |
| currentEpoch | Integer | 当前epoch |
| totalEpochs | Integer | 总epoch数 |
| metrics | Map\<String, Object\> | 训练指标（mAP, loss等） |
| errorMessage | String | 错误信息（如果失败） |
| createTime | LocalDateTime | 创建时间 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| modelPath | String | 模型输出路径 |

## 降级处理

当YOLO训练服务不可用时，Feign客户端会自动触发降级处理：

- 创建任务：返回错误提示"YOLO训练服务暂时不可用，请稍后重试"
- 查询任务：返回空列表或错误提示
- 健康检查：返回unhealthy状态

降级日志会记录在应用日志中，便于排查问题。

## 完整示例

查看 `src/main/java/com/jnet/api/yolo/example/YoloTrainingUsageExample.java` 获取完整的使用示例。

## 注意事项

1. **服务注册**：确保Python YOLO训练服务已注册到Nacos，服务名为 `jnet-yolo-training`
2. **超时配置**：训练任务可能耗时较长，建议配置合适的Feign超时时间
3. **异步处理**：创建任务后需要单独调用 `/start` 接口启动训练
4. **资源监控**：定期检查GPU使用情况，避免资源耗尽

## 相关文档

- [Python YOLO训练服务API文档](../../../python/yolo/yolo_api.py)
- [OpenFeign官方文档](https://spring.io/projects/spring-cloud-openfeign)
- [Resilience4j熔断器文档](https://resilience4j.readthedocs.io/)

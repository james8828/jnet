package com.jnet.biz.algorithm.yolo;

import com.jnet.biz.algorithm.TaskExecutionContext;
import com.jnet.biz.algorithm.dataset.DatasetBuilder;
import com.jnet.biz.algorithm.dataset.DatasetBuildResult;
import com.jnet.biz.algorithm.dataset.DatasetSplit;
import com.jnet.biz.algorithm.dataset.ImageAnnotationData;
import com.jnet.biz.config.StoragePathConfig;
import com.jnet.biz.entity.Image;
import com.jnet.biz.enums.AlgorithmType;
import com.jnet.biz.enums.OutputFormat;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.api.anno.dto.YoloLabelData;
import com.jnet.api.anno.feign.YoloLabelFeignClient;
import com.jnet.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YOLO数据集构建器实现
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YoloDatasetBuilder implements DatasetBuilder<YoloDatasetConfig> {
    
    private final ImageMapper imageMapper;
    private final YoloLabelFeignClient yoloLabelClient;
    private final StoragePathConfig storagePathConfig;
    
    // 分块查询大小
    private static final int BATCH_SIZE = 100;
    
    @Override
    public String getAlgorithmType() {
        return AlgorithmType.YOLO.getCode();
    }
    
    @Override
    public void validateConfig(YoloDatasetConfig config) {
        if (config.getProjectId() == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        
        double totalRatio = config.getTrainRatio() + config.getValRatio() + config.getTestRatio();
        if (Math.abs(totalRatio - 1.0) > 0.01) {
            throw new IllegalArgumentException("训练/验证/测试集比例之和必须等于1，当前: " + totalRatio);
        }
        
        if (config.getOutputFormat() == null || 
            (!OutputFormat.YOLOV5.getCode().equals(config.getOutputFormat()) && 
             !OutputFormat.YOLOV8.getCode().equals(config.getOutputFormat()))) {
            throw new IllegalArgumentException("输出格式必须是yolov5或yolov8");
        }
    }
    
    @Override
    public DatasetBuildResult execute(YoloDatasetConfig config, TaskExecutionContext context) throws Exception {
        String taskId = context.getTaskId();
        log.info("开始构建YOLO数据集: taskId={}", taskId);
        
        try {
            // Step 1: 验证配置
            context.updateProgress(5f, "验证配置参数");
            validateConfig(config);
            
            // Step 2: 查询数据
            context.updateProgress(10f, "查询图像和标注数据");
            List<ImageAnnotationData> dataList = queryImageData(config);
            
            if (dataList.isEmpty()) {
                throw new IllegalArgumentException("未找到符合条件的图像和标注数据");
            }
            
            context.log(TaskExecutionContext.LogLevel.INFO, 
                String.format("查询到 %d 张图像", dataList.size()));
            
            // Step 3: 统计类别分布
            context.updateProgress(20f, "统计类别分布");
            Map<String, Integer> classDistribution = calculateClassDistribution(dataList);
            context.log(TaskExecutionContext.LogLevel.INFO, 
                String.format("类别分布: %s", classDistribution));
            
            // Step 4: 应用类别映射
            if (config.getClassMapping() != null && !config.getClassMapping().isEmpty()) {
                context.updateProgress(25f, "应用类别映射");
                applyClassMapping(dataList, config.getClassMapping());
                classDistribution = recalculateClassDistribution(dataList);
            }
            
            // Step 5: 划分数据集
            context.updateProgress(30f, "划分训练/验证/测试集");
            DatasetSplit split = splitDataset(dataList, config);
            
            context.log(TaskExecutionContext.LogLevel.INFO, 
                String.format("数据集划分: 训练集=%d, 验证集=%d, 测试集=%d",
                    split.getTrainSet().size(), 
                    split.getValSet().size(),
                    split.getTestSet().size()));
            
            // Step 6: 创建目录结构
            context.updateProgress(40f, "创建目录结构");
            String datasetDir = createDatasetDirectory(config, taskId);
            
            // Step 7: 生成YOLO格式标注文件（直接使用 ImageAnnotationData 中的 rawLabels）
            context.updateProgress(50f, "生成YOLO格式标注文件");
            generateYoloLabels(split.getTrainSet(), datasetDir + "/train/labels", context);
            generateYoloLabels(split.getValSet(), datasetDir + "/val/labels", context);
            if (!split.getTestSet().isEmpty()) {
                generateYoloLabels(split.getTestSet(), datasetDir + "/test/labels", context);
            }
            
            // Step 8: 复制图像文件
            if (Boolean.TRUE.equals(config.getIncludeImages())) {
                context.updateProgress(70f, "复制图像文件");
                copyImagesToDataset(split, datasetDir, context);
            }
            
            // Step 9: 生成data.yaml配置文件
            context.updateProgress(85f, "生成配置文件");
            String dataYamlPath = generateDataYaml(config, classDistribution, datasetDir);
            
            // Step 10: 打包压缩
            context.updateProgress(95f, "打包压缩");
            String zipPath = compressDataset(datasetDir, config.getCompress(), taskId);
            
            // 完成
            context.updateProgress(100f, "完成");
            
            // 构建结果
            DatasetBuildResult result = new DatasetBuildResult();
            result.setDatasetPath(zipPath);
            result.setDatasetSize(Files.size(Paths.get(zipPath)));
            result.setTotalImages(dataList.size());
            result.setTotalAnnotations(classDistribution.values().stream().mapToInt(Integer::intValue).sum());
            result.setTrainCount(split.getTrainSet().size());
            result.setValCount(split.getValSet().size());
            result.setTestCount(split.getTestSet().size());
            result.setClassDistribution(classDistribution);
            result.setConfigFilePath(dataYamlPath);
            result.setFormat(config.getOutputFormat());
            
            log.info("YOLO数据集构建完成: taskId={}, path={}, size={}", 
                    taskId, zipPath, result.getDatasetSize());
            
            return result;
            
        } catch (Exception e) {
            log.error("YOLO数据集构建失败: taskId={}", taskId, e);
            throw e;
        }
    }
    
    @Override
    public List<ImageAnnotationData> queryImageData(YoloDatasetConfig config) {
        log.info("查询图像和标注数据: projectId={}, batchIds={}, tagIds={}", 
            config.getProjectId(), config.getBatchIds(), config.getTagIds());
        
        try {
            // Step 1: 根据条件查询完整图像信息（一次性查询，避免重复）
            List<Image> images = queryImages(config);
            
            if (images.isEmpty()) {
                log.warn("未找到符合条件的图像");
                return Collections.emptyList();
            }
            
            log.info("查询到 {} 张图像，开始分块获取标注数据", images.size());
            
            // 提取 imageIds 用于后续查询
            List<Long> imageIds = images.stream()
                .map(Image::getImageId)
                .collect(Collectors.toList());
            
            // 构建 Image Map 便于快速查找
            Map<Long, Image> imageMap = images.stream()
                .collect(Collectors.toMap(Image::getImageId, img -> img));
            
            // Step 2: 分块查询 anno 服务获取 YOLO 标注数据
            Map<Long, YoloLabelData> labelDataMap = queryLabelsInBatches(imageIds);
            
            // Step 3: 组装 ImageAnnotationData
            List<ImageAnnotationData> result = new ArrayList<>();
            
            for (Long imageId : imageIds) {
                YoloLabelData labelData = labelDataMap.get(imageId);
                Image image = imageMap.get(imageId);
                
                if (image == null) {
                    log.warn("图像不存在: imageId={}", imageId);
                    continue;
                }
                
                // 创建 ImageAnnotationData
                ImageAnnotationData data = new ImageAnnotationData();
                data.setImageId(imageId);
                data.setFilename(image.getFilename());
                data.setFilePath(image.getFilePath());
                data.setWidth(image.getWidth());
                data.setHeight(image.getHeight());
                
                // YoloLabelData 已包含处理完成的 YOLO 格式 labels
                // 保存到 rawLabels 用于直接生成标注文件（避免重复转换）
                if (labelData != null && labelData.getLabels() != null && !labelData.getLabels().isEmpty()) {
                    data.setRawLabels(labelData.getLabels());
                } else {
                    data.setRawLabels(Collections.emptyList());
                }
                
                result.add(data);
            }
            
            log.info("成功组装 {} 张图像的标注数据", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("查询图像和标注数据失败", e);
            throw new RuntimeException("查询数据失败", e);
        }
    }
    
    /**
     * 根据配置条件查询完整图像信息
     */
    private List<Image> queryImages(YoloDatasetConfig config) {
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();

        // 批次ID筛选
        if (config.getBatchIds() != null && !config.getBatchIds().isEmpty()) {
            wrapper.in(Image::getBatchId, config.getBatchIds());
        }
        
        // 标签ID筛选（通过 biz_annotation 表关联）
        if (config.getTagIds() != null && !config.getTagIds().isEmpty()) {
            // 将 tagIds 转换为逗号分隔的字符串，用于 SQL IN 子句
            String tagIdsStr = config.getTagIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            wrapper.inSql(Image::getImageId, 
                "SELECT DISTINCT image_id FROM biz_annotation WHERE tag_id IN (" + tagIdsStr + ")");
        }
        
        // 执行查询，返回完整图像对象（不再只选择 image_id）
        return imageMapper.selectList(wrapper);
    }
    
    /**
     * 分块查询 anno 服务获取 YOLO 标注数据
     * 避免一次性查询大量图像导致超时或内存溢出
     */
    private Map<Long, YoloLabelData> queryLabelsInBatches(List<Long> imageIds) {
        Map<Long, YoloLabelData> labelDataMap = new HashMap<>();
        
        int total = imageIds.size();
        int batches = (total + BATCH_SIZE - 1) / BATCH_SIZE;
        
        log.info("开始分块查询标注数据: 总数={}, 分批数={}, 每批大小={}", total, batches, BATCH_SIZE);
        
        for (int i = 0; i < batches; i++) {
            int fromIndex = i * BATCH_SIZE;
            int toIndex = Math.min(fromIndex + BATCH_SIZE, total);
            List<Long> batchIds = imageIds.subList(fromIndex, toIndex);
            
            try {
                log.debug("查询第 {}/{} 批，图像数: {}", i + 1, batches, batchIds.size());
                
                // 调用 anno Feign 接口
                Result<List<YoloLabelData>> result = yoloLabelClient.queryByImageIds(batchIds);
                
                if (result.getCode() == 200 && result.getData() != null) {
                    List<YoloLabelData> batchLabels = result.getData();
                    
                    // 将结果放入 Map
                    for (YoloLabelData labelData : batchLabels) {
                        labelDataMap.put(labelData.getImageId(), labelData);
                    }
                    
                    log.debug("第 {}/{} 批查询成功，获取 {} 张图像的标注", 
                        i + 1, batches, batchLabels.size());
                } else {
                    log.warn("第 {}/{} 批查询失败: {}", i + 1, batches, result.getMessage());
                }
                
            } catch (Exception e) {
                log.error("第 {}/{} 批查询异常", i + 1, batches, e);
                // 继续处理下一批，不中断整个流程
            }
        }
        
        log.info("分块查询完成，共获取 {} 张图像的标注数据", labelDataMap.size());
        return labelDataMap;
    }
    
    @Override
    public DatasetSplit splitDataset(List<ImageAnnotationData> dataList, YoloDatasetConfig config) {
        // 如果需要打乱
        if (Boolean.TRUE.equals(config.getShuffle())) {
            Collections.shuffle(dataList);
        }
        
        int total = dataList.size();
        int trainSize = (int) (total * config.getTrainRatio());
        int valSize = (int) (total * config.getValRatio());
        
        DatasetSplit split = new DatasetSplit();
        split.setTrainSet(new ArrayList<>(dataList.subList(0, trainSize)));
        split.setValSet(new ArrayList<>(dataList.subList(trainSize, Math.min(trainSize + valSize, total))));
        
        if (trainSize + valSize < total) {
            split.setTestSet(new ArrayList<>(dataList.subList(trainSize + valSize, total)));
        } else {
            split.setTestSet(new ArrayList<>());
        }
        
        return split;
    }
    
    @Override
    public String generateConfigFile(YoloDatasetConfig config, Map<String, Integer> classDistribution, String outputDir) {
        String yamlPath = outputDir + "/data.yaml";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(yamlPath))) {
            writer.println("# YOLO Dataset Configuration");
            writer.println("# Generated by JNet Platform");
            writer.println("# Date: " + new Date());
            writer.println();
            
            // 路径配置
            writer.println("path: " + outputDir);
            writer.println("train: train/images");
            writer.println("val: val/images");
            if (!classDistribution.isEmpty()) {
                writer.println("test: test/images");
            }
            writer.println();
            
            // 类别配置
            writer.println("nc: " + classDistribution.size());
            writer.println("names:");
            
            int idx = 0;
            for (String className : classDistribution.keySet()) {
                writer.println("  " + idx + ": \"" + className + "\"");
                idx++;
            }
            
        } catch (IOException e) {
            throw new RuntimeException("生成data.yaml失败", e);
        }
        
        return yamlPath;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private Map<String, Integer> calculateClassDistribution(List<ImageAnnotationData> dataList) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (var data : dataList) {
            @SuppressWarnings("unchecked")
            List<String> labels = (List<String>) data.getRawLabels();
            if (labels != null) {
                for (String yoloLabel : labels) {
                    try {
                        // YOLO 格式: class_id x_center y_center width height
                        String[] parts = yoloLabel.trim().split("\\s+");
                        if (parts.length >= 1) {
                            int classId = Integer.parseInt(parts[0]);
                            String className = String.valueOf(classId);
                            distribution.merge(className, 1, Integer::sum);
                        }
                    } catch (Exception e) {
                        log.warn("解析 YOLO 标注失败: {}", yoloLabel);
                    }
                }
            }
        }
        
        return distribution;
    }
    
    private void applyClassMapping(List<ImageAnnotationData> dataList, Map<String, String> mapping) {
        for (var data : dataList) {
            @SuppressWarnings("unchecked")
            List<String> labels = (List<String>) data.getRawLabels();
            if (labels != null && !labels.isEmpty()) {
                List<String> updatedLabels = new ArrayList<>();
                for (String yoloLabel : labels) {
                    try {
                        String[] parts = yoloLabel.trim().split("\\s+");
                        if (parts.length >= 1) {
                            int oldClassId = Integer.parseInt(parts[0]);
                            String oldClassName = String.valueOf(oldClassId);
                            
                            if (mapping.containsKey(oldClassName)) {
                                // 替换类别 ID
                                String newClassName = mapping.get(oldClassName);
                                int newClassId = Integer.parseInt(newClassName);
                                parts[0] = String.valueOf(newClassId);
                                updatedLabels.add(String.join(" ", parts));
                            } else {
                                updatedLabels.add(yoloLabel);
                            }
                        } else {
                            updatedLabels.add(yoloLabel);
                        }
                    } catch (Exception e) {
                        log.warn("应用类别映射失败: {}", yoloLabel);
                        updatedLabels.add(yoloLabel);
                    }
                }
                data.setRawLabels(updatedLabels);
            }
        }
    }
    
    private Map<String, Integer> recalculateClassDistribution(List<ImageAnnotationData> dataList) {
        return calculateClassDistribution(dataList);
    }
    
    private String createDatasetDirectory(YoloDatasetConfig config, String taskId) {
        // 使用配置类获取 YOLO 数据集目录
        String baseDir = storagePathConfig.getYoloDatasetDir(taskId);
        Path path = Paths.get(baseDir);
        
        try {
            Files.createDirectories(path.resolve("train/images"));
            Files.createDirectories(path.resolve("train/labels"));
            Files.createDirectories(path.resolve("val/images"));
            Files.createDirectories(path.resolve("val/labels"));
            Files.createDirectories(path.resolve("test/images"));
            Files.createDirectories(path.resolve("test/labels"));
            log.info("创建 YOLO 数据集目录: {}", baseDir);
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败: " + baseDir, e);
        }
        
        return baseDir;
    }
    
    private void generateYoloLabels(List<ImageAnnotationData> dataList, String outputDir, 
                                    TaskExecutionContext context) {
        Path dir = Paths.get(outputDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败", e);
        }
        
        int processed = 0;
        int total = dataList.size();
        
        for (var data : dataList) {
            if (context.isCancelled()) {
                throw new RuntimeException("任务已取消");
            }
            
            String txtFileName = data.getFilename().replaceAll("\\.[^.]+$", "") + ".txt";
            Path txtPath = dir.resolve(txtFileName);
            
            // 优先使用 rawLabels 中保存的原始 YOLO labels，无需再次转换
            @SuppressWarnings("unchecked")
            List<String> labels = (List<String>) data.getRawLabels();
            String content;
            
            if (labels != null && !labels.isEmpty()) {
                // 直接使用 anno 服务返回的 YOLO 格式 labels
                content = String.join("\n", labels);
            } else {
                // 降级方案：如果没有 rawLabels，使用空字符串
                content = "";
                log.warn("图像 {} 没有标注数据（rawLabels为空）", data.getImageId());
            }
            
            try {
                Files.writeString(txtPath, content);
            } catch (IOException e) {
                context.log(TaskExecutionContext.LogLevel.ERROR, 
                    "生成标注文件失败: " + data.getFilename());
            }
            
            processed++;
            if (processed % 100 == 0) {
                context.log(TaskExecutionContext.LogLevel.INFO, 
                    String.format("已处理 %d/%d 张图像", processed, total));
            }
        }
    }
    
    private void copyImagesToDataset(DatasetSplit split, String datasetDir, TaskExecutionContext context) {
        log.info("开始复制图像文件到数据集目录");
        
        try {
            // 复制训练集图像
            copyImageSet(split.getTrainSet(), datasetDir + "/train/images", context);
            
            // 复制验证集图像
            copyImageSet(split.getValSet(), datasetDir + "/val/images", context);
            
            // 复制测试集图像
            if (!split.getTestSet().isEmpty()) {
                copyImageSet(split.getTestSet(), datasetDir + "/test/images", context);
            }
            
            log.info("图像文件复制完成");
            
        } catch (Exception e) {
            log.error("复制图像文件失败", e);
            throw new RuntimeException("复制图像文件失败", e);
        }
    }
    
    /**
     * 复制图像集合到指定目录
     */
    private void copyImageSet(List<ImageAnnotationData> dataSet, String targetDir, TaskExecutionContext context) {
        Path targetPath = Paths.get(targetDir);
        
        try {
            Files.createDirectories(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("创建目录失败: " + targetDir, e);
        }
        
        int copied = 0;
        int total = dataSet.size();
        
        for (ImageAnnotationData data : dataSet) {
            if (context.isCancelled()) {
                throw new RuntimeException("任务已取消");
            }
            
            try {
                // 【关键修复】将相对路径转换为绝对路径
                String absoluteFilePath = storagePathConfig.toAbsolutePath(data.getFilePath());
                Path sourcePath = Paths.get(absoluteFilePath);
                Path destPath = targetPath.resolve(data.getFilename());
                
                // 检查源文件是否存在
                if (!Files.exists(sourcePath)) {
                    context.log(TaskExecutionContext.LogLevel.ERROR, 
                        "源文件不存在: " + absoluteFilePath);
                    continue;
                }
                
                // 复制文件
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                
                copied++;
                if (copied % 50 == 0) {
                    context.log(TaskExecutionContext.LogLevel.INFO, 
                        String.format("已复制 %d/%d 张图像", copied, total));
                }
                
            } catch (IOException e) {
                context.log(TaskExecutionContext.LogLevel.ERROR, 
                    "复制图像失败: " + data.getFilename() + ", 错误: " + e.getMessage());
                // 继续处理下一个，不中断整个流程
            }
        }
        
        log.info("图像复制完成: {}/{}", copied, total);
    }
    
    private String generateDataYaml(YoloDatasetConfig config, Map<String, Integer> classDistribution, 
                                    String outputDir) {
        return generateConfigFile(config, classDistribution, outputDir);
    }
    
    private String compressDataset(String datasetDir, Boolean shouldCompress, String taskId) {
        // 如果不压缩，直接返回目录路径
        if (!Boolean.TRUE.equals(shouldCompress)) {
            log.info("跳过压缩: dir={}", datasetDir);
            return datasetDir;
        }
        
        log.info("开始压缩数据集: dir={}", datasetDir);
        
        // 使用配置类获取 zip 文件路径
        String zipPath = storagePathConfig.getYoloDatasetZipPath(taskId);
        
        try {
            // 默认使用zip压缩
            zipWithJavaUtil(datasetDir, zipPath);
            
            log.info("数据集压缩完成: {}", zipPath);
            return zipPath;
            
        } catch (Exception e) {
            log.error("压缩数据集失败", e);
            throw new RuntimeException("压缩数据集失败", e);
        }
    }
    
    /**
     * 使用Java内置ZipOutputStream压缩
     */
    private void zipWithJavaUtil(String sourceDir, String zipPath) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        
        try (FileOutputStream fos = new FileOutputStream(zipPath);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {
            
            Files.walk(sourcePath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String zipEntryName = sourcePath.relativize(path).toString();
                        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(zipEntryName);
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        log.error("压缩文件失败: {}", path, e);
                    }
                });
        }
    }
}

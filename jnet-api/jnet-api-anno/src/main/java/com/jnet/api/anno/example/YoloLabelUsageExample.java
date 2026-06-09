package com.jnet.api.anno.example;

import com.jnet.api.anno.dto.YoloLabelData;
import com.jnet.api.anno.dto.YoloLabelQueryRequest;
import com.jnet.api.anno.feign.YoloLabelFeignClient;
import com.jnet.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * YoloLabelFeignClient 使用示例
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoloLabelUsageExample {
    
    private final YoloLabelFeignClient yoloLabelClient;
    
    /**
     * 示例 1: 按项目ID查询标注数据
     */
    public void example1_QueryByProjectId() {
        log.info("=== 示例 1: 按项目ID查询 ===");
        
        // 构建请求
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .projectId(1L)
            .build();
        
        // 调用 Feign 接口
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        // 处理结果
        if (result.getCode() == 200 && result.getData() != null) {
            List<YoloLabelData> labels = result.getData();
            log.info("查询成功，共 {} 张图像", labels.size());
            
            for (YoloLabelData labelData : labels) {
                log.info("图像ID: {}, 标注数: {}", 
                    labelData.getImageId(), 
                    labelData.getLabelCount());
                
                // 打印标注详情
                for (String label : labelData.getLabels()) {
                    log.info("  - {}", label);
                }
            }
        } else {
            log.error("查询失败: {}", result.getMessage());
        }
    }
    
    /**
     * 示例 2: 按批次ID集合查询
     */
    public void example2_QueryByBatchIds() {
        log.info("=== 示例 2: 按批次ID查询 ===");
        
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .batchIds(Arrays.asList(1001L, 1002L, 1003L))
            .build();
        
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        if (result.getCode() == 200) {
            log.info("查询到 {} 张图像的标注", result.getData().size());
        }
    }
    
    /**
     * 示例 3: 按标签ID筛选
     */
    public void example3_QueryByTagIds() {
        log.info("=== 示例 3: 按标签ID筛选 ===");
        
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .tagIds(Arrays.asList(501L, 502L))
            .build();
        
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        if (result.getCode() == 200) {
            log.info("查询到 {} 张图像的标注", result.getData().size());
        }
    }
    
    /**
     * 示例 4: 按图像ID集合查询（快捷方式）
     */
    public void example4_QueryByImageIds() {
        log.info("=== 示例 4: 按图像ID查询（快捷方式）===");
        
        List<Long> imageIds = Arrays.asList(2001L, 2002L, 2003L);
        
        Result<List<YoloLabelData>> result = yoloLabelClient.queryByImageIds(imageIds);
        
        if (result.getCode() == 200) {
            log.info("查询到 {} 张图像的标注", result.getData().size());
        }
    }
    
    /**
     * 示例 5: 组合条件查询
     */
    public void example5_CombinedQuery() {
        log.info("=== 示例 5: 组合条件查询 ===");
        
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .projectId(1L)
            .batchIds(Arrays.asList(1001L))
            .tagIds(Arrays.asList(501L))
            .build();
        
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        if (result.getCode() == 200) {
            log.info("查询到 {} 张图像的标注", result.getData().size());
        }
    }
    
    /**
     * 示例 6: 保存为 YOLO 格式文件
     */
    public void example6_SaveToFile() {
        log.info("=== 示例 6: 保存为YOLO格式文件 ===");
        
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .imageIds(Arrays.asList(2001L, 2002L))
            .build();
        
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        if (result.getCode() == 200 && result.getData() != null) {
            for (YoloLabelData labelData : result.getData()) {
                String fileName = "labels_" + labelData.getImageId() + ".txt";
                saveLabelsToFile(fileName, labelData.getLabels());
                log.info("已保存文件: {}", fileName);
            }
        }
    }
    
    /**
     * 示例 7: 处理服务不可用的情况（降级测试）
     */
    public void example7_HandleFallback() {
        log.info("=== 示例 7: 处理服务降级 ===");
        
        YoloLabelQueryRequest request = YoloLabelQueryRequest.builder()
            .imageIds(Arrays.asList(2001L))
            .build();
        
        // 即使 anno 服务不可用，也不会抛出异常
        Result<List<YoloLabelData>> result = yoloLabelClient.queryYoloLabels(request);
        
        // 会返回空列表
        if (result.getCode() == 200 && result.getData().isEmpty()) {
            log.warn("未获取到标注数据，可能触发了降级保护");
            // 可以继续执行其他逻辑，不会中断主流程
        }
    }
    
    /**
     * 示例 8: 批量处理大量图像（分批查询）
     */
    public void example8_BatchProcessing() {
        log.info("=== 示例 8: 批量处理大量图像 ===");
        
        // 假设有1000张图像
        List<Long> allImageIds = generateImageIds(1, 1000);
        
        // 分批查询，每批100张
        int batchSize = 100;
        for (int i = 0; i < allImageIds.size(); i += batchSize) {
            List<Long> batchIds = allImageIds.subList(i, Math.min(i + batchSize, allImageIds.size()));
            
            Result<List<YoloLabelData>> result = yoloLabelClient.queryByImageIds(batchIds);
            
            if (result.getCode() == 200) {
                log.info("批次 {}-{} 处理完成，共 {} 张图像", 
                    i, i + batchIds.size() - 1, result.getData().size());
                
                // 处理这批数据...
                processBatch(result.getData());
            }
        }
    }
    
    /**
     * 辅助方法：保存标注到文件
     */
    private void saveLabelsToFile(String fileName, List<String> labels) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (String label : labels) {
                writer.println(label);
            }
        } catch (IOException e) {
            log.error("保存文件失败: {}", fileName, e);
        }
    }
    
    /**
     * 辅助方法：生成图像ID列表（仅用于示例）
     */
    private List<Long> generateImageIds(int start, int end) {
        return java.util.stream.LongStream.rangeClosed(start, end)
            .boxed()
            .toList();
    }
    
    /**
     * 辅助方法：处理一批数据（仅用于示例）
     */
    private void processBatch(List<YoloLabelData> labels) {
        // 实际业务逻辑
        log.info("处理批次数据，共 {} 张图像", labels.size());
    }
}

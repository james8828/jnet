package com.jnet.biz.algorithm.yolo;

import com.jnet.biz.algorithm.config.AlgorithmConfig;
import com.jnet.biz.enums.AlgorithmType;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * YOLO数据集构建配置
 * 实现AlgorithmConfig接口，确保类型安全和统一验证
 * 
 * @author JNet Team
 * @since 2024-05-11
 */
@Data
public class YoloDatasetConfig implements AlgorithmConfig {
    // 基本信息
    private Long projectId;
    private List<Long> batchIds;  // 修改为复数，与 RequestDTO 保持一致
    private List<Long> tagIds;    // 新增：标签ID列表
    private String taskName;
    private String description;   // 新增：任务描述
    
    // 数据筛选
    private List<Long> imageIds;
    private String lifecycleStatusFilter;
    private List<String> annotationTypes;
    private Integer minAnnotationCount;
    private Integer maxAnnotationCount;
    private Integer minImageSize;  // 新增：图像最小尺寸
    private Integer maxImageSize;  // 新增：图像最大尺寸
    
    // 数据集配置
    private Double trainRatio = 0.7;   // 修改为 Double，与 RequestDTO 保持一致
    private Double valRatio = 0.2;     // 修改为 Double，与 RequestDTO 保持一致
    private Double testRatio = 0.1;    // 修改为 Double，与 RequestDTO 保持一致
    private Map<String, String> classMapping;
    private Boolean shuffle = true;
    
    // 输出配置
    private String outputFormat = "yolov8";
    private Boolean includeImages = true;
    private Boolean compress = false;       // 新增：是否压缩（替代 compressFormat）
    private Integer compressQuality;        // 新增：压缩质量
    private String extraConfig;             // 新增：额外配置
    
    /**
     * 获取算法类型
     */
    @Override
    public String getAlgorithmType() {
        return AlgorithmType.YOLO.getCode();
    }
    
    /**
     * 获取配置类的Class对象
     */
    @Override
    public Class<? extends AlgorithmConfig> getConfigClass() {
        return YoloDatasetConfig.class;
    }
    
    /**
     * 验证配置参数
     * 在解析配置后自动调用
     */
    @Override
    public void validate() {
        if (projectId == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }
        
        double totalRatio = trainRatio + valRatio + testRatio;
        if (Math.abs(totalRatio - 1.0) > 0.01) {
            throw new IllegalArgumentException(
                "训练/验证/测试集比例之和必须等于1，当前: " + totalRatio);
        }
        
        if (outputFormat == null || 
            (!"yolov5".equals(outputFormat) && !"yolov8".equals(outputFormat))) {
            throw new IllegalArgumentException("输出格式必须是yolov5或yolov8");
        }
        
        if (trainRatio < 0 || valRatio < 0 || testRatio < 0) {
            throw new IllegalArgumentException("比例值不能为负数");
        }
        
        // 验证压缩质量范围
        if (Boolean.TRUE.equals(compress) && compressQuality != null) {
            if (compressQuality < 1 || compressQuality > 100) {
                throw new IllegalArgumentException("压缩质量必须在1-100之间");
            }
        }
        
        // 验证图像尺寸
        if (minImageSize != null && minImageSize <= 0) {
            throw new IllegalArgumentException("图像最小尺寸必须大于0");
        }
        if (maxImageSize != null && maxImageSize <= 0) {
            throw new IllegalArgumentException("图像最大尺寸必须大于0");
        }
        if (minImageSize != null && maxImageSize != null && minImageSize > maxImageSize) {
            throw new IllegalArgumentException("图像最小尺寸不能大于最大尺寸");
        }
    }
}

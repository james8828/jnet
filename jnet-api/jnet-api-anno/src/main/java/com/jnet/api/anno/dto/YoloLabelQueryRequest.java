package com.jnet.api.anno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * YOLO标注查询请求DTO
 * 用于从 anno 服务查询图像的 YOLO 格式标注数据
 * 
 * @author JNet Team
 * @since 2024-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "YOLO标注查询请求")
public class YoloLabelQueryRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 项目ID（可选）
     */
    @Schema(description = "项目ID", example = "1")
    private Long projectId;
    
    /**
     * 批次ID集合（可选）
     */
    @Schema(description = "批次ID集合", example = "[1001, 1002, 1003]")
    private List<Long> batchIds;
    
    /**
     * 标签ID集合（可选）
     */
    @Schema(description = "标签ID集合", example = "[501, 502]")
    private List<Long> tagIds;
    
    /**
     * 图像ID集合（可选，优先级最高）
     * 如果指定了图像ID集合，则忽略其他筛选条件
     */
    @Schema(description = "图像ID集合", example = "[2001, 2002, 2003]")
    private List<Long> imageIds;
}

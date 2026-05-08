package com.jnet.biz.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量重新解析结果统计
 *
 * @author JNet Team
 */
@Data
public class ReparseResult {
    
    /**
     * 总数量
     */
    private int totalCount;
    
    /**
     * 成功数量
     */
    private int successCount;
    
    /**
     * 失败数量
     */
    private int failedCount;
    
    /**
     * 跳过数量
     */
    private int skippedCount;
    
    /**
     * 错误消息列表
     */
    private List<String> errorMessages = new ArrayList<>();
}

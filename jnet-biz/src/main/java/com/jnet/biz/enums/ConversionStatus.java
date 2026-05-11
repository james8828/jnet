package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图像转换状态枚举
 *
 * @author JNet Team
 * @since 2024-05-09
 */
@Getter
@AllArgsConstructor
public enum ConversionStatus {
    
    /**
     * 无需转换（WSI 格式）
     */
    NONE("NONE", "无需转换"),
    
    /**
     * 待转换（JPG/PNG 上传完成但未转换）
     */
    PENDING("PENDING", "待转换"),
    
    /**
     * 转换完成
     */
    COMPLETED("COMPLETED", "转换完成"),
    
    /**
     * 转换失败
     */
    FAILED("FAILED", "转换失败");
    
    private final String code;
    private final String description;
}

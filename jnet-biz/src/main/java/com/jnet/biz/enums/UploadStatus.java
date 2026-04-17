package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传状态枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum UploadStatus {

    /**
     * 待上传
     */
    PENDING("pending", "待上传"),

    /**
     * 上传中
     */
    UPLOADING("uploading", "上传中"),

    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),

    /**
     * 失败
     */
    FAILED("failed", "失败");

    /**
     * 状态码
     */
    private final String code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static UploadStatus fromCode(String code) {
        for (UploadStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的上传状态: " + code);
    }
}

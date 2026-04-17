package com.jnet.biz.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 图像格式枚举
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Getter
@AllArgsConstructor
public enum ImageFormat {

    /**
     * SVS格式（Aperio）
     */
    SVS("SVS", "Aperio SVS"),

    /**
     * NDPI格式（Hamamatsu）
     */
    NDPI("NDPI", "Hamamatsu NDPI"),

    /**
     * JPG格式
     */
    JPG("JPG", "JPEG图片"),

    /**
     * PNG格式
     */
    PNG("PNG", "PNG图片"),

    /**
     * TIFF格式
     */
    TIFF("TIFF", "TIFF图片"),

    /**
     * MRXS格式（3DHistech）
     */
    MRXS("MRXS", "3DHistech MRXS");

    /**
     * 格式代码
     */
    private final String code;

    /**
     * 格式描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static ImageFormat fromCode(String code) {
        for (ImageFormat format : values()) {
            if (format.getCode().equalsIgnoreCase(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("未知的图像格式: " + code);
    }
}

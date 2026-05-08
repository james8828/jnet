package com.jnet.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenSlide TIFF 转换配置类
 *
 * @author JNet Team
 * @since 2024-05-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "jnet.openslide.converter")
public class OpenSlideConverterProperties {

    /**
     * Python 解释器路径
     * 例如: E:\\conda\\envs\\wt_env\\python.exe
     */
    private String pythonPath = "python";

    /**
     * 转换脚本路径
     * 例如: /python/tools/convert_to_openslide_tiff.py
     */
    private String scriptPath = "/python/tools/convert_to_openslide_tiff.py";

    /**
     * 默认压缩质量 (0.0 - 1.0)
     */
    private float defaultQuality = 0.9f;

    /**
     * 支持的输入格式
     */
    private String[] supportedFormats = {"jpg", "jpeg", "png"};
}

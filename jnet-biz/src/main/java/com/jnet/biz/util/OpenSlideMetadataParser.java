package com.jnet.biz.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnet.biz.config.StoragePathConfig;
import com.jnet.biz.entity.Image;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openslide.OpenSlide;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSlide 元数据解析工具类
 * <p>
 * 提供统一的 WSI 图像元数据解析和缩略图生成功能
 *
 * @author JNet Team
 * @since 2024-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSlideMetadataParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final StoragePathConfig storageConfig;

    /**
     * 解析并设置图像元数据（使用 OpenSlide）
     *
     * @param image    图像实体
     * @param filePath 文件路径
     * @throws IOException 解析失败时抛出异常
     */
    public void parseAndSetMetadata(Image image, String filePath) throws IOException {
        try (OpenSlide slide = new OpenSlide(new File(filePath))) {
            // 获取基本属性
            long width = slide.getLevel0Width();
            long height = slide.getLevel0Height();
            int levelCount = slide.getLevelCount();

            image.setWidth((int) width);
            image.setHeight((int) height);
            image.setLevels(levelCount);

            // 获取所有属性
            Map<String, String> properties = slide.getProperties();

            // 调试：输出所有属性
            if (log.isDebugEnabled()) {
                log.debug("=== OpenSlide 属性列表 ===");
                properties.forEach((key, value) -> log.debug("{}: {}", key, value));
            }

            // 获取 MPP (Microns Per Pixel)
            String mppXStr = properties.get(OpenSlide.PROPERTY_NAME_MPP_X);
            String mppYStr = properties.get(OpenSlide.PROPERTY_NAME_MPP_Y);

            if (mppXStr != null) {
                try {
                    image.setMppX(Double.parseDouble(mppXStr));
                    log.debug("MPP X: {}", mppXStr);
                } catch (NumberFormatException e) {
                    log.warn("无法解析 MPP X: {}", mppXStr);
                }
            }

            if (mppYStr != null) {
                try {
                    image.setMppY(Double.parseDouble(mppYStr));
                    log.debug("MPP Y: {}", mppYStr);
                } catch (NumberFormatException e) {
                    log.warn("无法解析 MPP Y: {}", mppYStr);
                }
            }

            // 获取放大倍数
            String magStr = properties.get(OpenSlide.PROPERTY_NAME_OBJECTIVE_POWER);
            if (magStr != null) {
                try {
                    image.setMagnification(Integer.parseInt(magStr));
                    log.debug("放大倍数: {}", magStr);
                } catch (NumberFormatException e) {
                    log.warn("无法解析放大倍数: {}", magStr);
                }
            }

            // 构建元数据 JSON
            Map<String, Object> metadata = buildMetadataMap(slide, properties, levelCount);
            image.setMetadata(objectMapper.writeValueAsString(metadata));

            // 生成缩略图
            generateAndSaveThumbnail(image, slide);

            log.info("WSI 元数据解析成功: {}x{}, {} levels, MPP: {}x{}, 放大倍数: {}",
                    width, height, levelCount,
                    image.getMppX(), image.getMppY(),
                    image.getMagnification());

        } catch (Exception e) {
            log.error("OpenSlide 解析失败: {}", filePath, e);
            throw new IOException("OpenSlide 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建元数据 Map
     */
    private static Map<String, Object> buildMetadataMap(OpenSlide slide, Map<String, String> properties, int levelCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("vendor", properties.get(OpenSlide.PROPERTY_NAME_VENDOR));
        metadata.put("quickhash1", properties.get(OpenSlide.PROPERTY_NAME_QUICKHASH1));
        metadata.put("levelCount", levelCount);
        metadata.put("properties", properties); // 保存所有属性

        // 添加各层级信息
        List<Map<String, Object>> levels = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            Map<String, Object> levelInfo = new HashMap<>();
            long levelWidth = slide.getLevelWidth(i);
            long levelHeight = slide.getLevelHeight(i);
            levelInfo.put("level", i);
            levelInfo.put("width", levelWidth);
            levelInfo.put("height", levelHeight);
            levelInfo.put("downsample", slide.getLevelDownsample(i));
            levels.add(levelInfo);
        }
        metadata.put("levels", levels);

        return metadata;
    }

    /**
     * 生成并保存缩略图
     *
     * @param image 图像实体
     * @param slide OpenSlide 实例
     */
    public void generateAndSaveThumbnail(Image image, OpenSlide slide) {
        try {
            // 生成缩略图（最大边长 512px）
            BufferedImage thumbnail = slide.createThumbnailImage(512);

            // 获取缩略图目录（按 imageId 分目录）
            String thumbnailDir = storageConfig.getThumbnailDirByImageId(image.getImageId());
            File dir = new File(thumbnailDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    log.debug("创建缩略图目录: {}", thumbnailDir);
                }
            }

            String thumbnailFilePath = storageConfig.getThumbnailFilePath(image.getImageId());
            File thumbnailFile = new File(thumbnailFilePath);

            javax.imageio.ImageIO.write(thumbnail, "JPEG", thumbnailFile);

            // 设置缩略图 URL（存储完整文件路径）
            image.setThumbnailUrl(thumbnailFilePath);

            log.debug("缩略图生成成功: imageId={}, path={}", image.getImageId(), thumbnailFile.getAbsolutePath());

        } catch (Exception e) {
            log.warn("生成缩略图失败: imageId={}", image.getImageId(), e);
            // 缩略图生成失败不影响主流程
        }
    }
}

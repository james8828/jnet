package com.jnet.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据存储路径配置
 * <p>
 * 集中管理所有文件存储相关的路径配置
 *
 * @author JNet Team
 * @since 2024-05-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-pool.storage")
public class StoragePathConfig {

    /**
     * 根存储路径
     * 默认: E:/doc/jnet/imageStore
     */
    private String rootPath = "E:/doc/jnet/imageStore";

    /**
     * 临时文件存储路径
     * 默认: E:/doc/jnet/imageStore/temp
     */
    private String tempPath = "E:/doc/jnet/imageStore/temp";

    /**
     * 缩略图存储路径
     * 默认: E:/doc/jnet/imageStore/thumbnails
     */
    private String thumbnailPath = "E:/doc/jnet/imageStore/thumbnails";

    /**
     * TIFF 转换文件存储路径（仅用于 JPG/PNG 转换后的 TIFF）
     * 默认: E:/doc/jnet/imageStore/converted-tiff
     */
    private String convertedTiffPath = "E:/doc/jnet/imageStore/converted-tiff";

    /**
     * 瓦片缓存路径（用于阅片加速）
     * 默认: E:/doc/jnet/imageStore/tile-cache
     */
    private String tileCachePath = "E:/doc/jnet/imageStore/tile-cache";

    /**
     * 获取完整的缩略图目录路径
     *
     * @return 缩略图根目录绝对路径
     */
    public String getThumbnailDir() {
        return thumbnailPath;
    }

    /**
     * 获取指定图像的缩略图目录路径
     * <p>
     * 路径结构: {thumbnailPath}/{imageId}/
     * 使用 imageId 作为子目录，避免单目录文件过多
     *
     * @param imageId 图像ID
     * @return 缩略图目录绝对路径
     */
    public String getThumbnailDirByImageId(Long imageId) {
        return thumbnailPath + "/" + imageId;
    }

    /**
     * 获取缩略图文件的完整路径
     * <p>
     * 路径结构: {thumbnailPath}/{imageId}/thumbnail.jpg
     * 使用 imageId 作为子目录，便于管理和清理
     *
     * @param imageId 图像ID
     * @return 缩略图文件绝对路径
     */
    public String getThumbnailFilePath(Long imageId) {
        return thumbnailPath + "/" + imageId + "/thumbnail.jpg";
    }

    /**
     * 获取缩略图的访问 URL
     * <p>
     * URL 结构: /thumbnails/{imageId}/thumbnail.jpg
     *
     * @param imageId 图像ID
     * @return 缩略图相对 URL
     */
    public String getThumbnailUrl(Long imageId) {
        return "/thumbnails/" + imageId + "/thumbnail.jpg";
    }

    /**
     * 构建项目批次目录路径
     *
     * @param projectCode 项目编码
     * @param batchCode   批次编码
     * @return 批次目录绝对路径
     */
    public String getBatchDir(String projectCode, String batchCode) {
        return rootPath + "/" + projectCode + "/" + batchCode;
    }

    /**
     * 构建图像文件完整路径
     *
     * @param projectCode 项目编码
     * @param batchCode   批次编码
     * @param filename    文件名
     * @return 图像文件绝对路径
     */
    public String getImageFilePath(String projectCode, String batchCode, String filename) {
        return getBatchDir(projectCode, batchCode) + "/" + filename;
    }

    /**
     * 获取转换后 TIFF 文件的存储路径
     * <p>
     * 路径结构: {rootPath}/{projectCode}/{batchCode}/tiff/{imageId}/{baseName}.tif
     * 将转换后的TIFF文件存储在对应批次的tiff子目录下
     *
     * @param originalFilename 原始文件名
     * @param projectCode    项目编码
     * @param batchCode      批次编码
     * @return 转换后 TIFF 文件路径
     */
    public String getConvertedTiffPath(String originalFilename, String projectCode, String batchCode) {
        // 提取基础文件名（不含扩展名）
        String baseName = extractBaseName(originalFilename);
        
        // 路径结构: {rootPath}/{projectCode}/{batchCode}/tiff/{baseName}.tif
        // 将转换后的TIFF文件存储在对应批次的tiff子目录下，便于按批次管理
        return rootPath + "/" + projectCode + "/" + batchCode + "/tiff/" + baseName + ".tif";
    }

    /**
     * 构建瓦片缓存路径
     *
     * @param imageId 图像ID
     * @param zoom    缩放级别
     * @param x       X坐标
     * @param y       Y坐标
     * @return 瓦片文件路径
     */
    public String getTileCachePath(Long imageId, int zoom, int x, int y) {
        return String.format("%s/%d/%d/%d-%d-%d.jpg", 
                tileCachePath, imageId, zoom, zoom, x, y);
    }

    /**
     * 判断是否为 WSI 格式（无需转换）
     *
     * @param filename 文件名
     * @return true-WSI格式, false-需要转换
     */
    public static boolean isWsiFormat(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".svs") || 
               lowerName.endsWith(".ndpi") || 
               lowerName.endsWith(".tif") || 
               lowerName.endsWith(".tiff");
    }

    /**
     * 判断是否为需要转换的格式
     *
     * @param filename 文件名
     * @return true-需要转换, false-不需要
     */
    public static boolean needsConversion(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".jpg") || 
               lowerName.endsWith(".jpeg") || 
               lowerName.endsWith(".png");
    }

    /**
     * 提取基础文件名（不含扩展名）
     *
     * @param filename 文件名
     * @return 基础文件名
     */
    private String extractBaseName(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }
}

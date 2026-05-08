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
     * 获取完整的缩略图目录路径
     *
     * @return 缩略图目录绝对路径
     */
    public String getThumbnailDir() {
        return thumbnailPath;
    }

    /**
     * 获取缩略图文件的完整路径
     *
     * @param imageId 图像ID
     * @return 缩略图文件绝对路径
     */
    public String getThumbnailFilePath(Long imageId) {
        return thumbnailPath + "/" + imageId + ".jpg";
    }

    /**
     * 获取缩略图的访问 URL
     *
     * @param imageId 图像ID
     * @return 缩略图相对 URL
     */
    public String getThumbnailUrl(Long imageId) {
        return "/thumbnails/" + imageId + ".jpg";
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
}

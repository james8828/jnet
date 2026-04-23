package com.jnet.biz.service;

import com.jnet.biz.dto.TileQueryDTO;
import com.jnet.biz.vo.ImageMetadataVO;
import org.springframework.core.io.Resource;

/**
 * 图像瓦片 Service 接口
 *
 * @author JNet Team
 * @since 2024-04-16
 */
public interface IImageTileService {

    /**
     * 获取图像元数据
     *
     * @param imageId 图像ID
     * @return 元数据信息
     */
    ImageMetadataVO getImageMetadata(Long imageId);

    /**
     * 获取缩略图
     *
     * @param imageId 图像ID
     * @param maxSize 最大尺寸（宽或高）
     * @return 缩略图资源
     */
    Resource getThumbnail(Long imageId, Integer maxSize);

    /**
     * 获取指定Tile
     *
     * @param query Tile查询参数
     * @return Tile图像资源
     */
    Resource getTile(TileQueryDTO query);

    /**
     * 根据OpenLayers zoom级别获取Tile（后端负责zoom到level的转换）
     *
     * @param imageId 图像ID
     * @param zoom OpenLayers zoom级别
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标
     * @return Tile图像资源
     */
    Resource getTileByZoom(Long imageId, Integer zoom, Integer x, Integer y);

    /**
     * 获取金字塔层级信息
     *
     * @param imageId 图像ID
     * @return 层级信息JSON
     */
    String getLevelInfo(Long imageId);
}

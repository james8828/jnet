package com.jnet.image.tile.statistics;

import java.util.List;

/**
 * 瓦片统计计算器
 */
public class TileStatisticsCalculator {

    /**
     * 计算单个图像的瓦片数量
     * @param width 图像宽度
     * @param height 图像高度
     * @param tileSize 瓦片大小
     * @return 瓦片总数
     */
    public static int calculateTilesForImage(int width, int height, int tileSize) {
        int tilesX = (int) Math.ceil((double) width / tileSize);
        int tilesY = (int) Math.ceil((double) height / tileSize);
        return tilesX * tilesY;
    }

    /**
     * 计算单个图像的瓦片数量（默认512瓦片大小）
     * @param width 图像宽度
     * @param height 图像高度
     * @return 瓦片总数
     */
    public static int calculateTilesForImage(int width, int height) {
        return calculateTilesForImage(width, height, 512);
    }

    /**
     * 计算Zoomify格式的总瓦片数量（包括所有层级）
     * @param width 图像宽度
     * @param height 图像高度
     * @param tileSize 瓦片大小
     * @return 所有层级的瓦片总数
     */
    public static int calculateZoomifyTilesForImage(int width, int height, int tileSize) {
        int totalTiles = 0;
        int currentWidth = width;
        int currentHeight = height;

        while (currentWidth > tileSize || currentHeight > tileSize) {
            int tilesX = (int) Math.ceil((double) currentWidth / tileSize);
            int tilesY = (int) Math.ceil((double) currentHeight / tileSize);
            totalTiles += tilesX * tilesY;

            currentWidth = (int) Math.ceil((double) currentWidth / 2);
            currentHeight = (int) Math.ceil((double) currentHeight / 2);
        }

        // 最后一层
        int tilesX = (int) Math.ceil((double) currentWidth / tileSize);
        int tilesY = (int) Math.ceil((double) currentHeight / tileSize);
        totalTiles += tilesX * tilesY;

        return totalTiles;
    }

    /**
     * 批量计算图像瓦片总数
     * @param imageInfoList 图像信息列表
     * @param useZoomify 是否使用Zoomify计算方式
     * @return 瓦片总数
     */
    public static long calculateTotalTiles(List<ImageInfo> imageInfoList, boolean useZoomify) {
        if (imageInfoList == null || imageInfoList.isEmpty()) {
            return 0;
        }

        long totalTiles = 0;
        for (ImageInfo imageInfo : imageInfoList) {
            int tiles;
            if (useZoomify) {
                tiles = calculateZoomifyTilesForImage(
                        imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getTileSize());
            } else {
                tiles = calculateTilesForImage(
                        imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getTileSize());
            }
            totalTiles += tiles;
        }

        return totalTiles;
    }

    /**
     * 批量计算图像瓦片总数（默认简单计算）
     * @param imageInfoList 图像信息列表
     * @return 瓦片总数
     */
    public static long calculateTotalTiles(List<ImageInfo> imageInfoList) {
        return calculateTotalTiles(imageInfoList, false);
    }

    /**
     * 计算瓦片处理速率（每秒处理的瓦片数）
     * @param totalTiles 总瓦片数
     * @param totalTimeInSeconds 总处理时间（秒）
     * @return 瓦片处理速率（tiles/second）
     */
    public static double calculateTileProcessingRate(long totalTiles, double totalTimeInSeconds) {
        if (totalTimeInSeconds <= 0) {
            throw new IllegalArgumentException("处理时间必须大于0");
        }
        return (double) totalTiles / totalTimeInSeconds;
    }

    /**
     * 统计结果类
     */
    public static class TileStatisticsResult {
        private long totalTiles;           // 总瓦片数
        private double totalTimeSeconds;   // 总处理时间（秒）
        private double tileProcessingRate; // 瓦片处理速率（tiles/second）
        private int imageCount;            // 图像数量

        public TileStatisticsResult(long totalTiles, double totalTimeSeconds, int imageCount) {
            this.totalTiles = totalTiles;
            this.totalTimeSeconds = totalTimeSeconds;
            this.imageCount = imageCount;
            this.tileProcessingRate = calculateTileProcessingRate(totalTiles, totalTimeSeconds);
        }

        // Getters
        public long getTotalTiles() { return totalTiles; }
        public double getTotalTimeSeconds() { return totalTimeSeconds; }
        public double getTileProcessingRate() { return tileProcessingRate; }
        public int getImageCount() { return imageCount; }

        // 转换为其他时间单位的处理速率
        public double getTilesPerMinute() { return tileProcessingRate * 60; }
        public double getTilesPerHour() { return tileProcessingRate * 3600; }

        @Override
        public String toString() {
            return String.format(
                    "图像数量: %d\n" +
                            "总瓦片数: %,d\n" +
                            "总处理时间: %.2f 秒\n" +
                            "瓦片处理速率: %.2f tiles/秒 (%.2f tiles/分钟)\n",
                    imageCount, totalTiles, totalTimeSeconds,
                    tileProcessingRate, getTilesPerMinute()
            );
        }
    }

    /**
     * 计算完整的瓦片统计数据
     * @param imageInfoList 图像信息列表
     * @param totalTimeInSeconds 总处理时间（秒）
     * @param useZoomify 是否使用Zoomify计算方式
     * @return 统计结果
     */
    public static TileStatisticsResult calculateStatistics(
            List<ImageInfo> imageInfoList, double totalTimeInSeconds, boolean useZoomify) {

        long totalTiles = calculateTotalTiles(imageInfoList, useZoomify);
        return new TileStatisticsResult(totalTiles, totalTimeInSeconds, imageInfoList.size());
    }

    /**
     * 计算完整的瓦片统计数据（默认简单计算）
     * @param imageInfoList 图像信息列表
     * @param totalTimeInSeconds 总处理时间（秒）
     * @return 统计结果
     */
    public static TileStatisticsResult calculateStatistics(
            List<ImageInfo> imageInfoList, double totalTimeInSeconds) {
        return calculateStatistics(imageInfoList, totalTimeInSeconds, false);
    }
}



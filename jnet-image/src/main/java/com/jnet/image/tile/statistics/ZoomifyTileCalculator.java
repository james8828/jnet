package com.jnet.image.tile.statistics;

import java.util.List;

/**
 * Zoomify瓦片统计计算器
 */
public class ZoomifyTileCalculator {

    // 标准图像尺寸
    public static final int STANDARD_WIDTH = 80000;
    public static final int STANDARD_HEIGHT = 80000;
    public static final int DEFAULT_TILE_SIZE = 512;

    /**
     * 计算Zoomify格式的总瓦片数量（包括所有层级）
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
     * 计算标准图像(80000x80000)的Zoomify瓦片数量
     */
    public static int calculateStandardImageTiles() {
        return calculateZoomifyTilesForImage(STANDARD_WIDTH, STANDARD_HEIGHT, DEFAULT_TILE_SIZE);
    }

    /**
     * 批量计算图像Zoomify瓦片总数
     */
    public static long calculateTotalTiles(List<ImageInfo> imageInfoList) {
        if (imageInfoList == null || imageInfoList.isEmpty()) {
            return 0;
        }

        long totalTiles = 0;
        for (ImageInfo imageInfo : imageInfoList) {
            int tiles = calculateZoomifyTilesForImage(
                    imageInfo.getWidth(), imageInfo.getHeight(), imageInfo.getTileSize());
            totalTiles += tiles;
        }

        return totalTiles;
    }

    /**
     * 计算瓦片处理速率
     */
    public static double calculateTileProcessingRate(long totalTiles, double totalTimeInSeconds) {
        if (totalTimeInSeconds <= 0) {
            throw new IllegalArgumentException("处理时间必须大于0");
        }
        return (double) totalTiles / totalTimeInSeconds;
    }

    /**
     * 计算处理标准图像所需时间
     */
    public static double calculateTimeForStandardImage(double tileProcessingRate) {
        int standardTiles = calculateStandardImageTiles();
        return (double) standardTiles / tileProcessingRate;
    }

    /**
     * Zoomify瓦片统计结果类
     */
    public static class ZoomifyTileStatistics {
        private long totalTiles;
        private double totalTimeSeconds;
        private double tileProcessingRate;
        private int imageCount;
        private int standardImageTiles;
        private double timeForStandardImage;

        public ZoomifyTileStatistics(long totalTiles, double totalTimeSeconds, int imageCount) {
            this.totalTiles = totalTiles;
            this.totalTimeSeconds = totalTimeSeconds;
            this.imageCount = imageCount;
            this.tileProcessingRate = calculateTileProcessingRate(totalTiles, totalTimeSeconds);
            this.standardImageTiles = calculateStandardImageTiles();
            this.timeForStandardImage = calculateTimeForStandardImage(this.tileProcessingRate);
        }

        // Getters
        public long getTotalTiles() { return totalTiles; }
        public double getTotalTimeSeconds() { return totalTimeSeconds; }
        public double getTileProcessingRate() { return tileProcessingRate; }
        public int getImageCount() { return imageCount; }
        public int getStandardImageTiles() { return standardImageTiles; }
        public double getTimeForStandardImage() { return timeForStandardImage; }

        public double getTilesPerMinute() { return tileProcessingRate * 60; }
        public double getTilesPerHour() { return tileProcessingRate * 3600; }
        public double getImagesPerHour() {
            return 3600.0 / timeForStandardImage;
        }
    }

    /**
     * 计算完整的Zoomify瓦片统计数据
     */
    public static ZoomifyTileStatistics calculateStatistics(List<ImageInfo> imageInfoList,
                                                            double totalTimeInSeconds) {
        long totalTiles = calculateTotalTiles(imageInfoList);
        return new ZoomifyTileStatistics(totalTiles, totalTimeInSeconds, imageInfoList.size());
    }
}


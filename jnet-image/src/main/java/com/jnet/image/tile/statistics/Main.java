package com.jnet.image.tile.statistics;

import java.util.List;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        // 创建示例图像数据
        List<ImageInfo> imageInfoList = new ArrayList<>();
        imageInfoList.add(new ImageInfo(80000, 80000)); // 80000x80000
        imageInfoList.add(new ImageInfo(40000, 40000)); // 40000x30000
        imageInfoList.add(new ImageInfo(20000, 20000)); // 20000x15000
        imageInfoList.add(new ImageInfo(10000, 10000));  // 10000x8000
        imageInfoList.add(new ImageInfo(5000, 5000));   // 5000x4000
        imageInfoList.add(new ImageInfo(2500, 2500));   // 5000x4000
        imageInfoList.add(new ImageInfo(1250, 1250));   // 1000x800
        imageInfoList.add(new ImageInfo(625, 625));     // 600x400
        imageInfoList.add(new ImageInfo(313, 313));     // 300x200

        // 假设处理这批数据总共耗时300秒
        double totalTimeInSeconds = 300.0;

        // 计算简单瓦片统计
        TileStatisticsCalculator.TileStatisticsResult simpleResult =
                TileStatisticsCalculator.calculateStatistics(imageInfoList, totalTimeInSeconds, false);

        System.out.println("=== 简单瓦片统计 ===");
        System.out.println(simpleResult);

        // 计算Zoomify瓦片统计
        TileStatisticsCalculator.TileStatisticsResult zoomifyResult =
                TileStatisticsCalculator.calculateStatistics(imageInfoList, totalTimeInSeconds, true);

        System.out.println("=== Zoomify瓦片统计 ===");
        System.out.println(zoomifyResult);

        // 详细信息展示
        System.out.println("=== 详细瓦片计算 ===");
        for (int i = 0; i < imageInfoList.size(); i++) {
            ImageInfo image = imageInfoList.get(i);
            int simpleTiles = TileStatisticsCalculator.calculateTilesForImage(
                    image.getWidth(), image.getHeight());
            int zoomifyTiles = TileStatisticsCalculator.calculateZoomifyTilesForImage(
                    image.getWidth(), image.getHeight(), image.getTileSize());

            System.out.printf("图像%d: %dx%d -> 简单:%d tiles, Zoomify:%d tiles%n",
                    image.getName(), image.getWidth(), image.getHeight(), simpleTiles, zoomifyTiles);
        }
    }
}



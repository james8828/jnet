package com.jnet.image.tile.statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.text.DecimalFormat;

/**
 * Markdown格式Zoomify瓦片测试报告生成器（包含标准图像对比）
 */
public class StandardImageTestReportGenerator {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("#,###.00");
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("#,###.00");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成包含标准图像处理能力的Markdown测试报告
     */
    public static String generateTestReport(List<ImageInfo> imageInfoList,
                                            double totalTimeInSeconds,
                                            String testName,
                                            String testDescription) {

        StringBuilder report = new StringBuilder();

        // 报告头部
        report.append("# 📊 Zoomify瓦片处理性能测试报告\n\n");
        report.append("| 项目 | 内容 |\n");
        report.append("|------|------|\n");
        report.append("| 📋 测试名称 | ").append(testName).append(" |\n");
        report.append("| 📝 测试描述 | ").append(testDescription).append(" |\n");
        report.append("| 🕐 生成时间 | ").append(LocalDateTime.now().format(DATE_FORMAT)).append(" |\n\n");

        // 测试数据概览
        report.append("## 📈 测试数据概览\n\n");
        report.append("| 指标 | 数值 |\n");
        report.append("|------|------|\n");
        report.append("| 🖼️ 图像总数 | ").append(imageInfoList.size()).append(" 张 |\n");
        report.append("| ⏱️ 总处理时间 | ").append(TIME_FORMAT.format(totalTimeInSeconds)).append(" 秒 |\n");

        if (totalTimeInSeconds >= 60) {
            report.append("| 🕓 处理时间 | ").append(TIME_FORMAT.format(totalTimeInSeconds/60)).append(" 分钟 |\n");
        }
        if (totalTimeInSeconds >= 3600) {
            report.append("| 🕝 处理时间 | ").append(TIME_FORMAT.format(totalTimeInSeconds/3600)).append(" 小时 |\n");
        }
        report.append("\n");

        // 图像详细信息和瓦片计算
        report.append("## 📋 图像及瓦片详细信息\n\n");
        report.append("| 序号| 名称 | 图像尺寸 | Zoomify瓦片数 | 备注 |\n");
        report.append("|------|----------|----------|---------------|------|\n");

        long totalZoomifyTiles = 0;
        for (int i = 0; i < imageInfoList.size(); i++) {
            ImageInfo image = imageInfoList.get(i);
            int zoomifyTiles = ZoomifyTileCalculator.calculateZoomifyTilesForImage(
                    image.getWidth(), image.getHeight(), image.getTileSize());
            totalZoomifyTiles += zoomifyTiles;

            String note = "";
            if (image.getWidth() >= 50000 && image.getHeight() >= 50000) {
                note = "🔷 超大图像";
            } else if (image.getWidth() >= 20000 && image.getHeight() >= 20000) {
                note = "🟢 大图像";
            } else if (image.getWidth() >= 5000 && image.getHeight() >= 5000) {
                note = "🟡 中图像";
            } else {
                note = "⚪ 小图像";
            }

            report.append(String.format("| %02d | %s | %s | %s | %s |\n",
                    i+1,image.getName(), image.toString(), NUMBER_FORMAT.format(zoomifyTiles), note));
        }
        report.append("\n");

        // 总体统计结果
        ZoomifyTileCalculator.ZoomifyTileStatistics result =
                ZoomifyTileCalculator.calculateStatistics(imageInfoList, totalTimeInSeconds);

        report.append("## 📊 Zoomify瓦片处理统计\n\n");
        report.append("| 指标 | 数值 | 说明 |\n");
        report.append("|------|------|------|\n");
        report.append("| 🧩 总瓦片数 | ").append(NUMBER_FORMAT.format(result.getTotalTiles()))
                .append(" tiles | 所有图像生成的瓦片总数 |\n");
        report.append("| ⚡ 处理速率 | ").append(RATE_FORMAT.format(result.getTileProcessingRate()))
                .append(" tiles/秒 | 每秒处理的瓦片数量 |\n");
        report.append("| 🚀 处理速率 | ").append(RATE_FORMAT.format(result.getTilesPerMinute()))
                .append(" tiles/分钟 | 每分钟处理的瓦片数量 |\n");
        report.append("| 🛰️ 处理速率 | ").append(RATE_FORMAT.format(result.getTilesPerHour()))
                .append(" tiles/小时 | 每小时处理的瓦片数量 |\n");
        report.append("| 📏 平均每张图像 | ").append(NUMBER_FORMAT.format(
                        result.getTotalTiles() / result.getImageCount()))
                .append(" tiles | 单张图像平均瓦片数 |\n\n");

        // 标准图像处理能力分析
        report.append("## 🎯 标准图像处理能力分析\n\n");
        report.append("### 📋 标准图像规格\n\n");
        report.append("- **标准图像尺寸**: 80,000 × 80,000 像素\n");
        report.append("- **瓦片大小**: 512 × 512 像素\n");
        report.append("- **标准图像总瓦片数**: ").append(NUMBER_FORMAT.format(result.getStandardImageTiles())).append(" tiles\n\n");

        report.append("### ⚡ 处理能力评估\n\n");
        report.append("| 指标 | 数值 | 说明 |\n");
        report.append("|------|------|------|\n");
        report.append("| 🕐 标准图像处理时间 | ").append(TIME_FORMAT.format(result.getTimeForStandardImage()))
                .append(" 秒 | 处理一张标准图像所需时间 |\n");
        report.append("| ⏱️ 标准图像处理时间 | ").append(TIME_FORMAT.format(result.getTimeForStandardImage()/60))
                .append(" 分钟 | 处理一张标准图像所需时间 |\n");
        report.append("| 🚀 每小时处理图像数 | ").append(RATE_FORMAT.format(result.getImagesPerHour()))
                .append(" 张 | 每小时可处理的标准图像数量 |\n");
        report.append("| 📅 每天处理图像数 | ").append(RATE_FORMAT.format(result.getImagesPerHour() * 24))
                .append(" 张 | 按24小时计算的处理能力 |\n\n");

        // 性能评级
        report.append("## 🏆 性能评级\n\n");
        report.append(getPerformanceRating(result.getTileProcessingRate(), result.getImagesPerHour()));
        report.append("\n");

        // 处理效率分析
        report.append("## 📉 处理效率分析\n\n");
        appendEfficiencyAnalysis(report, result);
        report.append("\n");

        // 建议和结论
        report.append("## 📝 结论与建议\n\n");
        report.append(concludeReport(result));

        return report.toString();
    }

    /**
     * 获取性能评级（带Emoji）
     */
    private static String getPerformanceRating(double tilesPerSecond, double imagesPerHour) {
        StringBuilder rating = new StringBuilder();

        // 瓦片处理速率评级
        rating.append("### 🧩 瓦片处理能力评级\n\n");
        if (tilesPerSecond >= 1000) {
            rating.append("🌟🌟🌟🌟🌟 **极优性能** - 每秒处理超过1,000个瓦片 🚀\n\n");
        } else if (tilesPerSecond >= 500) {
            rating.append("🌟🌟🌟🌟☆ **优秀性能** - 每秒处理500-1,000个瓦片 ✅\n\n");
        } else if (tilesPerSecond >= 100) {
            rating.append("🌟🌟🌟☆☆ **良好性能** - 每秒处理100-500个瓦片 👍\n\n");
        } else if (tilesPerSecond >= 50) {
            rating.append("🌟🌟☆☆☆ **一般性能** - 每秒处理50-100个瓦片 ⚠️\n\n");
        } else {
            rating.append("🌟☆☆☆☆ **较低性能** - 每秒处理少于50个瓦片 ❗\n\n");
        }

        // 标准图像处理能力评级
        rating.append("### 🎯 标准图像处理能力评级\n\n");
        if (imagesPerHour >= 100) {
            rating.append("🏆🏆🏆🏆🏆 **卓越处理能力** - 每小时处理超过100张标准图像 🌟\n\n");
        } else if (imagesPerHour >= 50) {
            rating.append("🏆🏆🏆🏆☆ **优秀处理能力** - 每小时处理50-100张标准图像 ✅\n\n");
        } else if (imagesPerHour >= 20) {
            rating.append("🏆🏆🏆☆☆ **良好处理能力** - 每小时处理20-50张标准图像 👍\n\n");
        } else if (imagesPerHour >= 5) {
            rating.append("🏆🏆☆☆☆ **一般处理能力** - 每小时处理5-20张标准图像 ⚠️\n\n");
        } else if (imagesPerHour >= 1) {
            rating.append("🏆☆☆☆☆ **较低处理能力** - 每小时处理1-5张标准图像 ⏳\n\n");
        } else {
            rating.append("☆☆☆☆☆ **处理能力有限** - 每小时处理少于1张标准图像 🐢\n\n");
        }

        return rating.toString();
    }

    /**
     * 添加效率分析
     */
    private static void appendEfficiencyAnalysis(StringBuilder report,
                                                 ZoomifyTileCalculator.ZoomifyTileStatistics result) {
        double rate = result.getTileProcessingRate();
        double imagesPerHour = result.getImagesPerHour();

        report.append("### 📊 综合效率评估\n\n");

        // 处理能力分析
        report.append("#### 🚀 瓦片处理能力\n");
        if (rate >= 500) {
            report.append("- **高效处理**: 系统能够快速处理大量瓦片数据 ✅\n");
        } else if (rate >= 100) {
            report.append("- **良好处理**: 系统处理速度满足一般需求 👍\n");
        } else {
            report.append("- **处理能力有限**: 建议优化处理流程 ⚠️\n");
        }
        report.append("\n");

        // 标准图像处理能力分析
        report.append("#### 🎯 标准图像处理能力\n");
        if (imagesPerHour >= 50) {
            report.append("- **高效标准图像处理**: 每小时可处理50张以上标准图像 🚀\n");
        } else if (imagesPerHour >= 20) {
            report.append("- **良好标准图像处理**: 每小时可处理20张以上标准图像 ✅\n");
        } else if (imagesPerHour >= 5) {
            report.append("- **一般标准图像处理**: 每小时可处理5张以上标准图像 👍\n");
        } else {
            report.append("- **标准图像处理能力有限**: 建议优化处理流程 ⚠️\n");
        }
        report.append("\n");

        // 时间效率分析
        report.append("#### ⏱️ 时间效率\n");
        if (result.getTotalTimeSeconds() < 60) {
            report.append("- **快速完成**: 处理时间在1分钟以内 ⚡\n");
        } else if (result.getTotalTimeSeconds() < 300) {
            report.append("- **合理时间**: 处理时间在5分钟以内 ✅\n");
        } else {
            report.append("- **处理时间较长**: 建议考虑并行处理优化 ⚠️\n");
        }
        report.append("\n");
    }

    /**
     * 生成结论和建议
     */
    private static String concludeReport(ZoomifyTileCalculator.ZoomifyTileStatistics result) {
        StringBuilder conclusion = new StringBuilder();

        conclusion.append("### 📋 测试总结\n\n");
        conclusion.append("本次测试成功处理了 **").append(result.getImageCount()).append("** 张图像，");
        conclusion.append("总共生成了 **").append(NUMBER_FORMAT.format(result.getTotalTiles())).append("** 个Zoomify瓦片，");
        conclusion.append("处理耗时 **").append(TIME_FORMAT.format(result.getTotalTimeSeconds())).append("** 秒。\n\n");

        conclusion.append("基于当前性能，系统处理标准图像(80,000×80,000)的能力为:\n");
        conclusion.append("- **单张标准图像处理时间**: ").append(TIME_FORMAT.format(result.getTimeForStandardImage())).append(" 秒 (")
                .append(TIME_FORMAT.format(result.getTimeForStandardImage()/60)).append(" 分钟)\n");
        conclusion.append("- **每小时处理能力**: ").append(RATE_FORMAT.format(result.getImagesPerHour())).append(" 张标准图像\n");
        conclusion.append("- **每天处理能力**: ").append(RATE_FORMAT.format(result.getImagesPerHour() * 24)).append(" 张标准图像\n\n");

        double rate = result.getTileProcessingRate();
        double imagesPerHour = result.getImagesPerHour();

        conclusion.append("### 📊 性能评估\n\n");
        if (rate > 300 && imagesPerHour > 30) {
            conclusion.append("🟢 **卓越性能**: 系统表现出色，处理能力强大，能够胜任大规模图像处理任务\n\n");
        } else if (rate > 150 && imagesPerHour > 15) {
            conclusion.append("🔵 **优秀性能**: 系统性能良好，能满足大部分处理需求\n\n");
        } else if (rate > 50 && imagesPerHour > 5) {
            conclusion.append("🟡 **良好性能**: 系统性能一般，适合中小规模处理任务\n\n");
        } else {
            conclusion.append("🔴 **性能待提升**: 系统性能有待提升，建议进行优化\n\n");
        }

        conclusion.append("### 💡 优化建议\n\n");
        if (rate < 50 || imagesPerHour < 5) {
            conclusion.append("1. 🧵 **多线程优化**: 考虑使用多线程并行处理以提升效率\n");
            conclusion.append("2. ⚙️ **算法优化**: 优化瓦片生成算法，减少计算复杂度\n");
            conclusion.append("3. 💾 **IO优化**: 检查磁盘IO性能，确保读写速度\n");
            conclusion.append("4. 🧠 **内存优化**: 优化内存使用，避免频繁GC\n\n");
        } else if (rate < 200 || imagesPerHour < 20) {
            conclusion.append("1. ⚡ **性能调优**: 可以进一步优化以提升处理能力\n");
            conclusion.append("2. 📚 **技术升级**: 考虑使用更高效的图像处理库\n");
            conclusion.append("3. 🛠️ **参数调优**: 调整处理参数以获得更好性能\n\n");
        } else {
            conclusion.append("1. ✅ **保持现状**: 当前性能表现优秀，可作为生产环境基准\n");
            conclusion.append("2. 📊 **持续监控**: 建议定期监控性能指标，确保稳定性\n");
            conclusion.append("3. 🚀 **扩展应用**: 可考虑应用到更大规模的处理任务中\n\n");
        }

        return conclusion.toString();
    }
}


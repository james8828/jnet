package com.jnet.image.tile.statistics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StandardImageTileTest {
    public static void main(String[] args) {
        // 创建测试数据
        List<ImageInfo> testData = createTestData();

        // 模拟处理时间（秒）
        double processingTime = 9140; // 187.65秒处理时间

        // 生成测试报告
        String report = StandardImageTestReportGenerator.generateTestReport(
                testData,
                processingTime,
                "Zoomify瓦片处理性能测试",
                "测试大尺寸图像的Zoomify瓦片生成和处理性能，对比标准图像处理能力"
        );

        // 输出报告
        System.out.println(report);

        // 保存到文件
        saveReportToFile(report, "standard_image_tile_test_report.md");
    }

    /**
     * 解析CSV文件为ImageInfo列表
     * @param file CSV文件对象
     * @return ImageInfo列表
     */
    private static List<ImageInfo> parseCsvToImageInfo(File file) {
        List<ImageInfo> images = new ArrayList<>();

        try {
            // 使用Java 8 Stream API读取和解析CSV文件
            java.nio.file.Files.lines(file.toPath())
                    .skip(1) // 跳过标题行（如果有）
                    .forEach(line -> {
                        try {
                            String[] parts = line.split(",");
                            if (parts.length >= 2) {
                                // 假设CSV格式为 width,height 或其他包含宽度和高度的格式
                                int width = Integer.parseInt(parts[1].trim());
                                int height = Integer.parseInt(parts[2].trim());
                                String name = parts[0].trim();
                                images.add(new ImageInfo(name,width, height));
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("无法解析行数据: " + line);
                        }
                    });
        } catch (Exception e) {
            System.err.println("读取CSV文件失败: " + e.getMessage());
        }

        return images;
    }


    /**
     * 创建测试数据
     */
    private static List<ImageInfo> createTestData() {

        File imageDataFile = new File("E:\\image_dev_tb_image.csv");
        //imageDataFile 文件为csv，将此文件解析为List<ImageInfo>
        List<ImageInfo> images = parseCsvToImageInfo(imageDataFile);






        /*List<ImageInfo> images = new ArrayList<>();

        // 添加各种尺寸的测试图像
        images.add(new ImageInfo(80000, 80000)); // 标准图像
        images.add(new ImageInfo(40000, 30000)); // 大图像
        images.add(new ImageInfo(20000, 15000)); // 中等图像
        images.add(new ImageInfo(10000, 8000));  // 中等图像
        images.add(new ImageInfo(5000, 4000));   // 小图像
        images.add(new ImageInfo(2000, 1500));   // 小图像*/

        return images;
    }

    /**
     * 保存报告到文件
     */
    private static void saveReportToFile(String report, String filename) {
        try {
            java.nio.file.Files.write(
                    java.nio.file.Paths.get(filename),
                    report.getBytes("UTF-8")
            );
            System.out.println("✅ 报告已保存到: " + filename);
        } catch (Exception e) {
            System.err.println("❌ 保存报告失败: " + e.getMessage());
        }
    }
}


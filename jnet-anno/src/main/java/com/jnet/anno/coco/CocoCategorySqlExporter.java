package com.jnet.anno.coco;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * COCO类别SQL导出器
 * 将COCO数据集的类别数据转换为biz_tag表的SQL INSERT语句
 */
public class CocoCategorySqlExporter {

    /**
     * 导出类别数据为SQL文件
     *
     * @param categories COCO类别列表
     * @param outputFilePath 输出文件路径
     * @throws IOException 文件写入异常
     */
    public static void exportToSql(List<CocoCategory> categories, String outputFilePath) throws IOException {
        if (categories == null || categories.isEmpty()) {
            System.out.println("警告: 类别数据为空，跳过SQL导出");
            return;
        }

        // 为所有类别生成唯一颜色
        Map<Long, String> colorMap = generateUniqueColors(categories);

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath, StandardCharsets.UTF_8))) {
            // 写入文件头注释
            writer.println("-- ============================================");
            writer.println("-- COCO数据集类别导入SQL");
            writer.println("-- 目标表: biz_tag");
            writer.println("-- 生成时间: " + new java.util.Date());
            writer.println("-- 类别总数: " + categories.size());
            writer.println("-- ============================================");
            writer.println();

            // 按supercategory分组，便于查看
            writer.println("-- 清空现有数据（可选，根据需要取消注释）");
            writer.println("-- DELETE FROM biz_tag WHERE code LIKE 'coco_%';");
            writer.println();

            int index = 1;
            for (CocoCategory category : categories) {
                // 生成标签编码：coco_{category_id}
                String code = "coco_" + category.getId();
                
                // 从颜色映射中获取唯一颜色
                String colorCode = colorMap.get(category.getId());
                
                // 生成排序号（使用category ID作为排序）
                Integer sortOrder = category.getId().intValue();
                
                // 转义单引号
                String name = escapeSqlString(category.getName());
                String supercategory = category.getSupercategory() != null ? 
                    escapeSqlString(category.getSupercategory()) : null;
                
                // 生成INSERT语句
                writer.println(String.format(
                    "INSERT INTO biz_tag (tag_id, name, code, category, parent_id, color_code, sort_order, is_system, create_time, update_time) " +
                    "VALUES (%d, '%s', '%s', %s, NULL, '%s', %d, FALSE, NOW(), NOW());",
                    category.getId(),
                    name,
                    code,
                    supercategory != null ? "'" + supercategory + "'" : "NULL",
                    colorCode,
                    sortOrder
                ));
                
                index++;
            }

            writer.println();
            writer.println("-- ============================================");
            writer.println("-- 导入完成，共 " + categories.size() + " 条记录");
            writer.println("-- ============================================");

            System.out.println("✓ SQL文件已生成: " + outputFilePath);
            System.out.println("  类别数量: " + categories.size());
        }
    }

    /**
     * 为所有类别生成唯一的颜色代码
     * 使用 HSL 色彩空间均匀分布颜色，确保视觉上区分明显且不重复
     *
     * @param categories 类别列表
     * @return 类别ID到颜色代码的映射
     */
    private static Map<Long, String> generateUniqueColors(List<CocoCategory> categories) {
        Map<Long, String> colorMap = new HashMap<>();
        int totalCategories = categories.size();
        
        // 预定义一些常用且对比度高的颜色（前20个）
        String[] presetColors = {
            "#FF0000", // 红色
            "#00FF00", // 绿色
            "#0000FF", // 蓝色
            "#FFFF00", // 黄色
            "#FF00FF", // 紫色
            "#00FFFF", // 青色
            "#FFA500", // 橙色
            "#800080", // 紫罗兰
            "#008000", // 深绿
            "#800000", // 深红
            "#000080", // 深蓝
            "#FFC0CB", // 粉色
            "#A52A2A", // 棕色
            "#808080", // 灰色
            "#FFD700", // 金色
            "#C0C0C0", // 银色
            "#FF6347", // 番茄红
            "#40E0D0", // 绿松石
            "#EE82EE", // 紫罗兰色
            "#F5DEB3"  // 小麦色
        };
        
        for (int i = 0; i < totalCategories; i++) {
            CocoCategory category = categories.get(i);
            String color;
            
            if (i < presetColors.length) {
                // 使用前20个预定义颜色
                color = presetColors[i];
            } else {
                // 超过20个后，使用 HSL 算法生成新颜色
                // 色相(Hue): 均匀分布在 0-360 度
                // 饱和度(Saturation): 固定 70% 保证颜色鲜艳
                // 亮度(Lightness): 固定 50% 保证颜色不太暗也不太亮
                double hue = (i * 137.508) % 360; // 黄金角度分布，避免相邻颜色相似
                color = hslToHex(hue, 70, 50);
            }
            
            colorMap.put(category.getId(), color);
        }
        
        return colorMap;
    }
    
    /**
     * 将 HSL 颜色转换为十六进制格式
     *
     * @param h 色相 (0-360)
     * @param s 饱和度 (0-100)
     * @param l 亮度 (0-100)
     * @return 十六进制颜色代码 (如 #FF0000)
     */
    private static String hslToHex(double h, double s, double l) {
        s /= 100.0;
        l /= 100.0;
        
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h / 60) % 2 - 1));
        double m = l - c / 2;
        
        double r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        int red = (int) Math.round((r + m) * 255);
        int green = (int) Math.round((g + m) * 255);
        int blue = (int) Math.round((b + m) * 255);
        
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    /**
     * 转义SQL字符串中的特殊字符
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private static String escapeSqlString(String str) {
        if (str == null) {
            return "";
        }
        // 转义单引号：' -> ''
        return str.replace("'", "''");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("COCO类别SQL导出工具");
        System.out.println("==================");
        System.out.println("此工具需要与CocoDatasetParser配合使用");
        System.out.println("请在CocoDatasetParser中调用exportCategoriesToSql方法");
    }
}

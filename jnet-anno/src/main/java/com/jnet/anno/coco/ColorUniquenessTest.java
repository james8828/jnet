package com.jnet.anno.coco;

import java.util.*;

/**
 * 颜色唯一性测试工具
 */
public class ColorUniquenessTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   COCO类别颜色唯一性测试");
        System.out.println("========================================\n");

        // 模拟80个COCO类别
        List<CocoCategory> categories = new ArrayList<>();
        for (long i = 1; i <= 80; i++) {
            CocoCategory category = new CocoCategory();
            category.setId(i);
            category.setName("Category_" + i);
            category.setSupercategory("test");
            categories.add(category);
        }

        // 生成颜色映射
        Map<Long, String> colorMap = generateUniqueColors(categories);

        // 检查是否有重复颜色
        Set<String> uniqueColors = new HashSet<>(colorMap.values());
        
        System.out.println("类别总数: " + categories.size());
        System.out.println("生成的颜色数: " + colorMap.size());
        System.out.println("唯一颜色数: " + uniqueColors.size());
        System.out.println();

        if (uniqueColors.size() == colorMap.size()) {
            System.out.println("✓ 所有颜色都是唯一的，没有重复！");
        } else {
            System.out.println("✗ 发现重复颜色！");
            
            // 找出重复的颜色
            Map<String, List<Long>> colorToIds = new HashMap<>();
            for (Map.Entry<Long, String> entry : colorMap.entrySet()) {
                colorToIds.computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                         .add(entry.getKey());
            }
            
            for (Map.Entry<String, List<Long>> entry : colorToIds.entrySet()) {
                if (entry.getValue().size() > 1) {
                    System.out.println("  颜色 " + entry.getKey() + " 被重复使用: " + entry.getValue());
                }
            }
        }

        System.out.println("\n前20个类别的颜色分配:");
        System.out.println("----------------------------------------");
        for (int i = 0; i < Math.min(20, categories.size()); i++) {
            CocoCategory cat = categories.get(i);
            String color = colorMap.get(cat.getId());
            System.out.printf("ID: %2d | 颜色: %s | ████\n", cat.getId(), color);
        }

        System.out.println("\n后10个类别的颜色分配（HSL生成）:");
        System.out.println("----------------------------------------");
        for (int i = Math.max(0, categories.size() - 10); i < categories.size(); i++) {
            CocoCategory cat = categories.get(i);
            String color = colorMap.get(cat.getId());
            System.out.printf("ID: %2d | 颜色: %s | ████\n", cat.getId(), color);
        }

        System.out.println("\n========================================");
    }

    /**
     * 为所有类别生成唯一的颜色代码
     */
    private static Map<Long, String> generateUniqueColors(List<CocoCategory> categories) {
        Map<Long, String> colorMap = new HashMap<>();
        int totalCategories = categories.size();
        
        String[] presetColors = {
            "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF",
            "#00FFFF", "#FFA500", "#800080", "#008000", "#800000",
            "#000080", "#FFC0CB", "#A52A2A", "#808080", "#FFD700",
            "#C0C0C0", "#FF6347", "#40E0D0", "#EE82EE", "#F5DEB3"
        };
        
        for (int i = 0; i < totalCategories; i++) {
            CocoCategory category = categories.get(i);
            String color;
            
            if (i < presetColors.length) {
                color = presetColors[i];
            } else {
                double hue = (i * 137.508) % 360;
                color = hslToHex(hue, 70, 50);
            }
            
            colorMap.put(category.getId(), color);
        }
        
        return colorMap;
    }
    
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
}

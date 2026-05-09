package com.jnet.anno.coco;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * COCO数据集解析器
 * 负责解析instances_val2017.json文件并将数据存储到CocoDataStorage中
 */
public class CocoDatasetParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        String jsonFilePath = "E:\\doc\\jnet\\data-set\\coco\\annotations_trainval2017\\annotations\\instances_train2017.json";

        if (args != null && args.length > 0) {
            jsonFilePath = args[0];
        }

        System.out.println("========================================");
        System.out.println("   COCO数据集解析程序");
        System.out.println("========================================");
        System.out.println("文件路径: " + jsonFilePath);
        System.out.println("开始时间: " + new Date());
        System.out.println();

        try {
            CocoDataStorage storage = new CocoDataStorage();

            System.out.println("【步骤1】正在解析JSON文件...");
            CocoDataset dataset = parseJsonFile(jsonFilePath);

            if (dataset == null) {
                System.err.println("错误: 解析失败，数据集为空");
                return;
            }
            System.out.println("✓ JSON文件解析成功\n");

            System.out.println("【步骤2】正在提取并存储数据...");

            List<CocoAnnotation> annotations = dataset.getAnnotations();
            List<CocoCategory> categories = dataset.getCategories();
            List<CocoImage> images = dataset.getImages();

            // 转换所有标注的几何字段
            System.out.println("  正在转换 bbox 和 segmentation 为 Geometry 对象...");
            convertAnnotationGeometries(annotations);
            System.out.println("  ✓ 几何数据转换完成\n");

            storage.storeAnnotations(annotations);
            storage.storeCategories(categories);
            storage.storeImages(images);

            System.out.println("✓ 数据存储完成\n");

            System.out.println("【步骤3】数据统计信息");
            storage.printStatistics();

            System.out.println("【步骤4】示例数据展示");
            displaySampleData(storage);

            System.out.println("【步骤5】数据验证");
            validateData(storage);

            System.out.println("【步骤6】数据已准备好进行下一步处理");
            System.out.println("可以通过storage对象访问所有数据:");
            System.out.println("  - storage.getAllAnnotations(): 获取所有标注");
            System.out.println("  - storage.getAllCategories(): 获取所有类别");
            System.out.println("  - storage.getAllImages(): 获取所有图片");
            System.out.println("  - storage.getAnnotationsByImageId(imageId): 按图片ID获取标注");
            System.out.println("  - storage.getAnnotationsByCategoryId(categoryId): 按类别ID获取标注");
            System.out.println();

            // 【可选步骤7】导出图片数据为 SQL INSERT 语句
            System.out.println("【步骤7】是否导出图片数据为 SQL？");
//            String exportSql = System.getenv("EXPORT_COCO_SQL");
            String exportSql = "true";
            if ("true".equalsIgnoreCase(exportSql)) {
                String sqlFilePath = "E:\\doc\\jnet\\data-set\\coco\\sql\\coco_train_images_import.sql";
                Long batchId = 4L; // 需要根据实际情况设置批次ID
                
                System.out.println("正在导出图片数据到 SQL 文件...");
                System.out.println("  批次ID: " + batchId);
                System.out.println("  输出文件: " + sqlFilePath);
                
                CocoImageSqlExporter.exportToSql(storage.getAllImages(), batchId, sqlFilePath);
                System.out.println("✓ SQL 导出完成\n");
            } else {
                System.out.println("  跳过 SQL 导出（设置环境变量 EXPORT_COCO_SQL=true 以启用）");
            }
            System.out.println();

            // 【可选步骤8】导出类别数据为 SQL INSERT 语句
            System.out.println("【步骤8】导出类别数据为 SQL");
            String categoriesSqlFilePath = "E:\\doc\\jnet\\data-set\\coco\\sql\\coco_train_categories_import.sql";
            
            System.out.println("正在导出类别数据到 SQL 文件...");
            System.out.println("  输出文件: " + categoriesSqlFilePath);
            
            CocoCategorySqlExporter.exportToSql(storage.getAllCategories(), categoriesSqlFilePath);
            System.out.println("✓ 类别 SQL 导出完成\n");

            // 【可选步骤9】导出标注数据为 SQL INSERT 语句
            System.out.println("【步骤9】导出标注数据为 SQL");
            String annotationsSqlFilePath = "E:\\doc\\jnet\\data-set\\coco\\sql\\coco_train_annotations_import.sql";
            Long slideId = 1L; // 需要根据实际情况设置切片ID
            Long projectId = 3L; // 可选
            Long batchIdForAnnotations = 3L; // 需要与图片导入时使用的批次ID一致
            
            System.out.println("正在导出标注数据到 SQL 文件...");
            System.out.println("  Slide ID: " + slideId);
            System.out.println("  Batch ID: " + batchIdForAnnotations);
            System.out.println("  输出文件: " + annotationsSqlFilePath);
            
            CocoAnnotationSqlExporter.exportToSql(storage, slideId, projectId, batchIdForAnnotations, annotationsSqlFilePath);
            System.out.println("✓ 标注 SQL 导出完成\n");

            System.out.println("完成时间: " + new Date());
            System.out.println("========================================");
            System.out.println("   解析完成！可以进行下一步数据处理");
            System.out.println("========================================");

        } catch (IOException e) {
            System.err.println("解析文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("处理数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析JSON文件
     *
     * @param filePath JSON文件路径
     * @return COCO数据集对象
     * @throws IOException 文件读取异常
     */
    private static CocoDataset parseJsonFile(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        if (!file.isFile()) {
            throw new IOException("路径不是有效的文件: " + filePath);
        }

        long fileSize = file.length();
        System.out.println("  文件大小: " + formatFileSize(fileSize));

        System.out.println("  正在读取和解析JSON...");
        long startTime = System.currentTimeMillis();

        CocoDataset dataset = objectMapper.readValue(file, CocoDataset.class);

        long endTime = System.currentTimeMillis();
        System.out.println("  解析耗时: " + (endTime - startTime) + " ms");

        return dataset;
    }

    /**
     * 显示示例数据
     *
     * @param storage 数据存储对象
     */
    private static void displaySampleData(CocoDataStorage storage) {
        System.out.println("\n--- 前5个标注示例 ---");
        List<CocoAnnotation> annotations = storage.getAllAnnotations();
        if (annotations != null && !annotations.isEmpty()) {
            annotations.stream().filter(annotation -> annotation.getImageId() == 581615)
                .limit(5)
                .forEach(annotation -> {
                    System.out.println("  标注ID: " + annotation.getId());
                    System.out.println("    图片ID: " + annotation.getImageId());
                    System.out.println("    类别ID: " + annotation.getCategoryId());
                    System.out.println("    类别名称: " + storage.getCategoryName(annotation.getCategoryId()));
                    
                    // 显示原始 bbox
                    if (annotation.getBboxRaw() != null) {
                        System.out.println("    原始边界框: " + formatBBox(annotation.getBboxRaw()));
                    }
                    
                    // 显示转换后的 Geometry
                    if (annotation.getBbox() != null) {
                        System.out.println("    Bbox Geometry: " + CocoGeometryConverter.toWKT(annotation.getBbox()));
                        System.out.println("    Bbox 面积: " + annotation.getBbox().getArea());
                    }
                    
                    System.out.println("    面积: " + annotation.getArea());
                    System.out.println("    IsCrowd: " + annotation.getIscrowd());
                    
                    // 显示 segmentation
                    if (annotation.getSegmentationRaw() != null && !annotation.getSegmentationRaw().isEmpty()) {
                        System.out.println("    原始分割多边形数: " + annotation.getSegmentationRaw().size());
                    }
                    
                    if (annotation.getSegmentation() != null) {
                        //打印原始 分割多边形
                        System.out.println("    原始分割多边形: " + annotation.getSegmentationRaw());
                        System.out.println("    Segmentation Geometry: " + 
                            CocoGeometryConverter.toWKT(annotation.getSegmentation()));
                        System.out.println("    Segmentation 类型: " + 
                            annotation.getSegmentation().getGeometryType());
                    }
                    System.out.println();
                });
        } else {
            System.out.println("  无标注数据");
        }

        System.out.println("\n--- 前5个图片示例 ---");
        List<CocoImage> images = storage.getAllImages();
        if (images != null && !images.isEmpty()) {
            images.stream()
                .limit(5)
                .forEach(image -> {
                    System.out.println("  图片ID: " + image.getId());
                    System.out.println("    文件名: " + image.getFileName());
                    System.out.println("    尺寸: " + image.getWidth() + " x " + image.getHeight());
                    System.out.println("    许可证: " + image.getLicense());
                    System.out.println("    COCO URL: " + image.getCocoUrl());

                    List<CocoAnnotation> imageAnnotations = storage.getAnnotationsByImageId(image.getId());
                    System.out.println("    标注数量: " + imageAnnotations.size());
                    System.out.println();
                });
        } else {
            System.out.println("  无图片数据");
        }

        System.out.println("\n--- 所有类别 ---");
        List<CocoCategory> categories = storage.getAllCategories();
        if (categories != null && !categories.isEmpty()) {
            categories.forEach(category -> {
                System.out.println("  ID: " + category.getId() +
                                 ", 名称: " + category.getName() +
                                 ", 父类: " + category.getSupercategory());
            });
        } else {
            System.out.println("  无类别数据");
        }
        System.out.println();
    }

    /**
     * 验证数据完整性和一致性
     *
     * @param storage 数据存储对象
     */
    private static void validateData(CocoDataStorage storage) {
        List<CocoAnnotation> annotations = storage.getAllAnnotations();
        List<CocoImage> images = storage.getAllImages();
        List<CocoCategory> categories = storage.getAllCategories();

        System.out.println("\n--- 数据验证结果 ---");

        boolean isValid = true;

        if (annotations == null || annotations.isEmpty()) {
            System.out.println("  ✗ 标注数据为空");
            isValid = false;
        } else {
            System.out.println("  ✓ 标注数据: " + annotations.size() + " 条");
        }

        if (images == null || images.isEmpty()) {
            System.out.println("  ✗ 图片数据为空");
            isValid = false;
        } else {
            System.out.println("  ✓ 图片数据: " + images.size() + " 条");
        }

        if (categories == null || categories.isEmpty()) {
            System.out.println("  ✗ 类别数据为空");
            isValid = false;
        } else {
            System.out.println("  ✓ 类别数据: " + categories.size() + " 条");
        }

        if (annotations != null && !annotations.isEmpty()) {
            long invalidAnnotations = annotations.stream()
                .filter(a -> a.getImageId() == null || a.getCategoryId() == null)
                .count();

            if (invalidAnnotations > 0) {
                System.out.println("  ✗ 存在 " + invalidAnnotations + " 条无效标注（缺少image_id或category_id）");
                isValid = false;
            } else {
                System.out.println("  ✓ 所有标注数据格式有效");
            }

            Set<Long> imageIdsInAnnotations = annotations.stream()
                .map(CocoAnnotation::getImageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            Set<Long> imageIdsInImages = images.stream()
                .map(CocoImage::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            Set<Long> orphanedAnnotations = new HashSet<>(imageIdsInAnnotations);
            orphanedAnnotations.removeAll(imageIdsInImages);

            if (!orphanedAnnotations.isEmpty()) {
                System.out.println("  ⚠ 警告: 存在 " + orphanedAnnotations.size() +
                                 " 个标注引用了不存在的图片ID");
            } else {
                System.out.println("  ✓ 所有标注的图片ID引用有效");
            }

            Set<Long> categoryIdsInAnnotations = annotations.stream()
                .map(CocoAnnotation::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            Set<Long> categoryIdsInCategories = categories.stream()
                .map(CocoCategory::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            Set<Long> orphanedCategories = new HashSet<>(categoryIdsInAnnotations);
            orphanedCategories.removeAll(categoryIdsInCategories);

            if (!orphanedCategories.isEmpty()) {
                System.out.println("  ⚠ 警告: 存在 " + orphanedCategories.size() +
                                 " 个标注引用了不存在的类别ID");
            } else {
                System.out.println("  ✓ 所有标注的类别ID引用有效");
            }

            long bboxIssues = annotations.stream()
                .filter(a -> a.getBboxRaw() == null || a.getBboxRaw().size() != 4)
                .count();

            if (bboxIssues > 0) {
                System.out.println("  ⚠ 警告: 存在 " + bboxIssues + " 个标注的边界框格式不正确");
            } else {
                System.out.println("  ✓ 所有标注的边界框格式正确");
            }

            // 验证 Geometry 转换
            long invalidBboxGeometry = annotations.stream()
                .filter(a -> a.getBboxRaw() != null && a.getBbox() == null)
                .count();
            
            if (invalidBboxGeometry > 0) {
                System.out.println("  ⚠ 警告: 存在 " + invalidBboxGeometry + " 个标注的 bbox 转换失败");
            } else {
                System.out.println("  ✓ 所有 bbox 成功转换为 Geometry");
            }

            long invalidSegmentationGeometry = annotations.stream()
                .filter(a -> a.getSegmentationRaw() != null && !a.getSegmentationRaw().isEmpty() 
                    && a.getIscrowd() != null && a.getIscrowd() == 0 && a.getSegmentation() == null)
                .count();
            
            if (invalidSegmentationGeometry > 0) {
                System.out.println("  ⚠ 警告: 存在 " + invalidSegmentationGeometry + " 个标注的 segmentation 转换失败");
            } else {
                System.out.println("  ✓ 所有 segmentation 成功转换为 Geometry");
            }
        }

        if (isValid) {
            System.out.println("\n  ✓ 数据验证通过！");
        } else {
            System.out.println("\n  ✗ 数据验证发现问题，请检查上述错误");
        }
        System.out.println();
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 格式化边界框字符串
     *
     * @param bbox 边界框列表 [x, y, width, height]
     * @return 格式化后的边界框字符串
     */
    private static String formatBBox(List<Double> bbox) {
        if (bbox == null || bbox.isEmpty()) {
            return "null";
        }
        if (bbox.size() != 4) {
            return bbox.toString();
        }
        return String.format("[x=%.2f, y=%.2f, w=%.2f, h=%.2f]",
                           bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
    }

    /**
     * 转换所有标注的几何字段
     *
     * @param annotations 标注列表
     */
    private static void convertAnnotationGeometries(List<CocoAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return;
        }

        int successCount = 0;
        int bboxSuccessCount = 0;
        int segmentationSuccessCount = 0;
        int skippedCount = 0;
        int rleSkippedCount = 0;

        for (CocoAnnotation annotation : annotations) {
            try {
                annotation.convertAllGeometries();
                
                boolean bboxConverted = annotation.getBbox() != null;
                boolean segmentationConverted = annotation.getSegmentation() != null;
                
                // 检查是否是 RLE 格式
                boolean isRLE = (annotation.getIscrowd() != null && annotation.getIscrowd() == 1) ||
                               (annotation.getSegmentationRaw() != null && annotation.getSegmentationRaw().isEmpty());
                
                if (bboxConverted) {
                    bboxSuccessCount++;
                }
                if (segmentationConverted) {
                    segmentationSuccessCount++;
                }
                if (bboxConverted || segmentationConverted) {
                    successCount++;
                } else {
                    skippedCount++;
                    if (isRLE) {
                        rleSkippedCount++;
                    }
                }
            } catch (Exception e) {
                System.err.println("警告: 标注 ID " + annotation.getId() + " 几何转换失败: " + e.getMessage());
            }
        }

        System.out.println("  几何转换统计:");
        System.out.println("    - 总标注数: " + annotations.size());
        System.out.println("    - 成功转换: " + successCount);
        System.out.println("    - Bbox 转换成功: " + bboxSuccessCount);
        System.out.println("    - Segmentation 转换成功: " + segmentationSuccessCount);
        if (rleSkippedCount > 0) {
            System.out.println("    - 跳过（RLE格式不支持）: " + rleSkippedCount);
        }
        if (skippedCount > rleSkippedCount) {
            System.out.println("    - 跳过（无几何数据）: " + (skippedCount - rleSkippedCount));
        }
    }
}

package com.jnet.anno.coco;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * COCO 图片数据 SQL 导出器
 * 将解析后的 CocoImage 数据转换为 biz_image 表的 INSERT 语句
 * 
 * @author jnet
 * @version 1.0
 * @since 2026/5/7
 */
public class CocoImageSqlExporter {

    /**
     * 导出所有图片为 INSERT 语句
     *
     * @param images   图片列表
     * @param batchId  批次ID（需要手动指定）
     * @param filePath 输出文件路径
     */
    public static void exportToSql(List<CocoImage> images, Long batchId, String filePath) {
        if (images == null || images.isEmpty()) {
            System.out.println("警告: 没有图片数据可导出");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // 写入文件头注释
            writer.println("-- ============================================");
            writer.println("-- COCO 数据集图片导入脚本");
            writer.println("-- 生成时间: " + java.time.LocalDateTime.now());
            writer.println("-- 总记录数: " + images.size());
            writer.println("-- 批次ID: " + batchId);
            writer.println("-- ============================================");
            writer.println();

            // 开始事务
            writer.println("BEGIN;");
            writer.println();

            int count = 0;
            int successCount = 0;
            int skipCount = 0;

            for (CocoImage image : images) {
                try {
                    String sql = generateInsertStatement(image, batchId);
                    if (sql != null) {
                        writer.println(sql);
                        successCount++;
                    } else {
                        skipCount++;
                    }
                    count++;
                    
                    // 每 1000 条输出一次进度
                    if (count % 1000 == 0) {
                        System.out.println("已处理: " + count + " / " + images.size() + " 条记录");
                    }
                } catch (Exception e) {
                    System.err.println("生成第 " + count + " 条记录的 SQL 失败: " + e.getMessage());
                    writer.println("-- 错误: 处理图片 ID " + image.getId() + " 时出错: " + e.getMessage());
                }
            }

            writer.println();
            writer.println("-- 提交事务");
            writer.println("COMMIT;");
            writer.println();
            writer.println("-- 统计信息");
            writer.println("-- 总记录数: " + images.size());
            writer.println("-- 成功生成: " + successCount);
            writer.println("-- 跳过记录: " + skipCount);

            System.out.println("\nSQL 文件生成完成！");
            System.out.println("文件路径: " + filePath);
            System.out.println("总记录数: " + images.size());
            System.out.println("成功生成: " + successCount);
            System.out.println("跳过记录: " + skipCount);

        } catch (IOException e) {
            System.err.println("写入 SQL 文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 生成单条 INSERT 语句
     *
     * @param image   图片对象
     * @param batchId 批次ID
     * @return INSERT SQL 语句
     */
    private static String generateInsertStatement(CocoImage image, Long batchId) {
        if (image == null) {
            return null;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO biz_image (");
        sql.append("    image_id, batch_id, filename, file_path, ");
//        sql.append("    width, height, coco_url, flickr_url, ");
        sql.append("    create_time, update_time, del_flag");
        sql.append(") VALUES (");

        // image_id - 使用 COCO 的图片ID
        sql.append(image.getId()).append(", ");

        // batch_id - 使用传入的批次ID
        sql.append(batchId).append(", ");

        // filename - 文件名
        sql.append(escapeString(image.getFileName())).append(", ");

        // file_path - 文件路径
        String filePath = "E:\\doc\\jnet\\imageStore\\coco-instance\\instances_train2017\\"+image.getFileName();
        sql.append(escapeString(filePath)).append(", ");

        /*// width
        sql.append(image.getWidth() != null ? image.getWidth() : "NULL").append(", ");

        // height
        sql.append(image.getHeight() != null ? image.getHeight() : "NULL").append(", ");

        // coco_url
        sql.append(escapeString(image.getCocoUrl())).append(", ");

        // flickr_url
        sql.append(escapeString(image.getFlickrUrl())).append(", ");*/

        // create_time - 使用当前时间
        sql.append("NOW(), ");

        // update_time - 使用当前时间
        sql.append("NOW(), ");

        // del_flag - 默认 false
        sql.append("FALSE");

        sql.append(");");

        // 添加注释
        if (image.getFileName() != null) {
            sql.append(" -- ").append(image.getFileName());
        }

        return sql.toString();
    }

    /**
     * 转义字符串，防止 SQL 注入
     *
     * @param value 原始字符串
     * @return 转义后的字符串（带引号）
     */
    private static String escapeString(String value) {
        if (value == null) {
            return "NULL";
        }
        
        // 转义单引号
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }

    /**
     * 生成批量插入语句（更高效）
     *
     * @param images   图片列表
     * @param batchId  批次ID
     * @param filePath 输出文件路径
     * @param batchSize 每批插入的记录数
     */
    public static void exportBatchInsert(List<CocoImage> images, Long batchId, String filePath, int batchSize) {
        if (images == null || images.isEmpty()) {
            System.out.println("警告: 没有图片数据可导出");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // 写入文件头
            writer.println("-- ============================================");
            writer.println("-- COCO 数据集图片批量导入脚本");
            writer.println("-- 生成时间: " + java.time.LocalDateTime.now());
            writer.println("-- 总记录数: " + images.size());
            writer.println("-- 批次ID: " + batchId);
            writer.println("-- 批量大小: " + batchSize);
            writer.println("-- ============================================");
            writer.println();

            writer.println("BEGIN;");
            writer.println();

            int totalRecords = images.size();
            int processed = 0;

            while (processed < totalRecords) {
                int endIndex = Math.min(processed + batchSize, totalRecords);
                List<CocoImage> batch = images.subList(processed, endIndex);

                writer.println("-- 批次: " + (processed / batchSize + 1));
                writer.println("INSERT INTO biz_image (");
                writer.println("    image_id, batch_id, filename, file_path, ");
                writer.println("    width, height, coco_url, flickr_url, ");
                writer.println("    create_time, update_time, del_flag");
                writer.println(") VALUES");

                for (int i = 0; i < batch.size(); i++) {
                    CocoImage image = batch.get(i);
                    writer.print("    (");
                    writer.print(image.getId() + ", ");
                    writer.print(batchId + ", ");
                    writer.print(escapeString(image.getFileName()) + ", ");
                    
                    String filePathValue = image.getCocoUrl() != null ? image.getCocoUrl() : null;
                    writer.print(escapeString(filePathValue) + ", ");
                    writer.print((image.getWidth() != null ? image.getWidth() : "NULL") + ", ");
                    writer.print((image.getHeight() != null ? image.getHeight() : "NULL") + ", ");
                    writer.print(escapeString(image.getCocoUrl()) + ", ");
                    writer.print(escapeString(image.getFlickrUrl()) + ", ");
                    writer.print("NOW(), ");
                    writer.print("NOW(), ");
                    writer.print("FALSE");
                    writer.print(")");

                    if (i < batch.size() - 1) {
                        writer.print(",");
                    }
                    writer.println();
                }

                writer.println(";");
                writer.println();

                processed = endIndex;
                System.out.println("已处理: " + processed + " / " + totalRecords + " 条记录");
            }

            writer.println("COMMIT;");

            System.out.println("\n批量 SQL 文件生成完成！");
            System.out.println("文件路径: " + filePath);

        } catch (IOException e) {
            System.err.println("写入 SQL 文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

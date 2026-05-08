package com.jnet.anno.coco;

import com.jnet.anno.coco.CocoAnnotation;
import com.jnet.anno.coco.CocoDataStorage;
import org.locationtech.jts.geom.Geometry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * COCO标注SQL导出器
 * 将COCO数据集的标注数据转换为biz_annotation表的SQL INSERT语句
 */
public class CocoAnnotationSqlExporter {

    /**
     * 导出标注数据为SQL文件
     *
     * @param storage COCO数据存储对象
     * @param slideId 关联的切片ID（需要根据实际情况设置）
     * @param projectId 所属项目ID（可选）
     * @param batchId 所属批次ID（可选）
     * @param outputFilePath 输出文件路径
     * @throws IOException 文件写入异常
     */
    public static void exportToSql(CocoDataStorage storage, Long slideId, 
                                   Long projectId, Long batchId, 
                                   String outputFilePath) throws IOException {
        List<CocoAnnotation> annotations = storage.getAllAnnotations();
        
        if (annotations == null || annotations.isEmpty()) {
            System.out.println("警告: 标注数据为空，跳过SQL导出");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath, StandardCharsets.UTF_8))) {
            // 写入文件头注释
            writer.println("-- ============================================");
            writer.println("-- COCO数据集标注导入SQL");
            writer.println("-- 目标表: biz_annotation");
            writer.println("-- 生成时间: " + new java.util.Date());
            writer.println("-- 标注总数: " + annotations.size());
            writer.println("-- Slide ID: " + slideId);
            if (projectId != null) {
                writer.println("-- Project ID: " + projectId);
            }
            if (batchId != null) {
                writer.println("-- Batch ID: " + batchId);
            }
            writer.println("-- ============================================");
            writer.println();

            // 清空现有数据（可选）
            writer.println("-- 清空现有数据（可选，根据需要取消注释）");
            writer.println("-- DELETE FROM biz_annotation WHERE slide_id = " + slideId + ";");
            writer.println();

            int successCount = 0;
            int skipCount = 0;

            for (CocoAnnotation annotation : annotations) {
                try {
                    String sql = generateInsertSql(annotation, slideId, projectId, batchId, storage);
                    if (sql != null) {
                        writer.println(sql);
                        successCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    System.err.println("警告: 标注 ID " + annotation.getId() + " 生成SQL失败: " + e.getMessage());
                    skipCount++;
                }
            }

            writer.println();
            writer.println("-- ============================================");
            writer.println("-- 导入完成");
            writer.println("-- 成功: " + successCount + " 条");
            writer.println("-- 跳过: " + skipCount + " 条");
            writer.println("-- ============================================");

            System.out.println("✓ SQL文件已生成: " + outputFilePath);
            System.out.println("  成功: " + successCount + " 条");
            System.out.println("  跳过: " + skipCount + " 条");
        }
    }

    /**
     * 生成单条标注的INSERT语句
     *
     * @param annotation COCO标注对象
     * @param slideId 切片ID
     * @param projectId 项目ID
     * @param batchId 批次ID
     * @param storage 数据存储对象
     * @return SQL INSERT语句，如果无法生成则返回null
     */
    private static String generateInsertSql(CocoAnnotation annotation, Long slideId,
                                           Long projectId, Long batchId,
                                           CocoDataStorage storage) {
        Long annotationId = annotation.getId() ;
        // 获取image_id对应的实际image记录
        Long imageId = annotation.getImageId();
        if (imageId == null) {
            return null; // 跳过没有image_id的标注
        }

        // 获取category_id对应的tag_id
        Long categoryId = annotation.getCategoryId();
        if (categoryId == null) {
            return null; // 跳过没有category_id的标注
        }
        
        // COCO category_id 映射到 biz_tag 的 tag_id
        // 假设我们使用相同的ID（coco_1 -> tag_id=1）
        Long tagId = categoryId;

        // 获取几何数据
        Geometry bboxGeom = annotation.getBbox();
        Geometry segmentationGeom = annotation.getSegmentation();

        // 确定geom_type和主几何字段
        String geomType;
        String geomWKT = null;
        String bboxWKT = null;
        Double area = null;
        Double perimeter = null;
        Double centroidX = null;
        Double centroidY = null;

        // 优先使用segmentation作为主几何
        if (segmentationGeom != null && !segmentationGeom.isEmpty()) {
            geomType = segmentationGeom.getGeometryType().toUpperCase();
            geomWKT = geometryToWKT(segmentationGeom);
            
            // 计算面积和周长
            area = segmentationGeom.getArea();
            perimeter = segmentationGeom.getLength();
            
            // 计算质心
            centroidX = segmentationGeom.getCentroid().getX();
            centroidY = segmentationGeom.getCentroid().getY();
        } else if (bboxGeom != null && !bboxGeom.isEmpty()) {
            // 如果没有segmentation，使用bbox
            geomType = bboxGeom.getGeometryType().toUpperCase();
            geomWKT = geometryToWKT(bboxGeom);
            
            area = bboxGeom.getArea();
            perimeter = bboxGeom.getLength();
            
            centroidX = bboxGeom.getCentroid().getX();
            centroidY = bboxGeom.getCentroid().getY();
        } else {
            // 没有几何数据，跳过
            return null;
        }

        // 生成bbox的WKT（如果与主几何不同）
        if (bboxGeom != null && !bboxGeom.isEmpty()) {
            bboxWKT = geometryToWKT(bboxGeom);
        }

        // 转义字符串
        String description = escapeSqlString("COCO annotation ID: " + annotation.getId());

        // 构建INSERT语句
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO biz_annotation (");
        sql.append("annotation_id,slide_id, image_id, project_id, batch_id, tag_id, ");
        sql.append("geom_type, geom, bbox, ");
        sql.append("area, perimeter, centroid_x, centroid_y, ");
        sql.append("confidence, creation_source, review_status, is_active, ");
        sql.append("create_time, update_time, description");
        sql.append(") VALUES (");
        sql.append(annotationId).append(", ");
        sql.append(imageId).append(", ");
        sql.append(imageId).append(", ");
        sql.append(projectId != null ? projectId : "NULL").append(", ");
        sql.append(batchId != null ? batchId : "NULL").append(", ");
        sql.append(tagId).append(", ");
        sql.append("'").append(geomType).append("', ");
        sql.append("ST_GeomFromText('").append(geomWKT).append("'), ");
        sql.append(bboxWKT != null ? "ST_GeomFromText('" + bboxWKT + "')" : "NULL").append(", ");
        sql.append(area != null ? area : "NULL").append(", ");
        sql.append(perimeter != null ? perimeter : "NULL").append(", ");
        sql.append(centroidX != null ? centroidX : "NULL").append(", ");
        sql.append(centroidY != null ? centroidY : "NULL").append(", ");
        sql.append("NULL, "); // confidence - COCO没有此字段
        sql.append("'MANUAL_DRAWING', ");
        sql.append("'PENDING', ");
        sql.append("TRUE, ");
        sql.append("NOW(), ");
        sql.append("NOW(), ");
        sql.append("'").append(description).append("'");
        sql.append(");");

        return sql.toString();
    }

    /**
     * 将JTS Geometry转换为WKT字符串
     *
     * @param geometry JTS几何对象
     * @return WKT字符串
     */
    private static String geometryToWKT(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return "";
        }
        return geometry.toText();
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
        // 转义反斜杠：\ -> \\
        return str.replace("\\", "\\\\").replace("'", "''");
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        System.out.println("COCO标注SQL导出工具");
        System.out.println("==================");
        System.out.println("此工具需要与CocoDatasetParser配合使用");
        System.out.println("请在CocoDatasetParser中调用exportAnnotationsToSql方法");
    }
}

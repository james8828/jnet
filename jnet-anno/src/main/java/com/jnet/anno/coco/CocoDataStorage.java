package com.jnet.anno.coco;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * COCO数据存储管理器
 * 用于存储和管理解析后的annotations、categories和images数据
 */
public class CocoDataStorage {
    
    // 存储所有标注数据
    private final List<CocoAnnotation> annotations = new ArrayList<>();
    
    // 存储所有类别数据
    private final List<CocoCategory> categories = new ArrayList<>();
    
    // 存储所有图片数据
    private final List<CocoImage> images = new ArrayList<>();
    
    // 按图片ID分组的标注索引
    private final Map<Long, List<CocoAnnotation>> annotationsByImageMap = new ConcurrentHashMap<>();
    
    // 按类别ID分组的标注索引
    private final Map<Long, List<CocoAnnotation>> annotationsByCategoryMap = new ConcurrentHashMap<>();
    
    // 类别ID到类别对象的映射
    private final Map<Long, CocoCategory> categoryMap = new HashMap<>();
    
    // 图片ID到图片对象的映射
    private final Map<Long, CocoImage> imageMap = new HashMap<>();
    
    /**
     * 存储标注数据
     */
    public void storeAnnotations(List<CocoAnnotation> annotations) {
        if (annotations != null && !annotations.isEmpty()) {
            this.annotations.clear();
            this.annotations.addAll(annotations);
            buildAnnotationIndexes();
            System.out.println("已存储 " + annotations.size() + " 个标注");
        }
    }
    
    /**
     * 存储类别数据
     */
    public void storeCategories(List<CocoCategory> categories) {
        if (categories != null && !categories.isEmpty()) {
            this.categories.clear();
            this.categories.addAll(categories);
            buildCategoryMap();
            System.out.println("已存储 " + categories.size() + " 个类别");
        }
    }
    
    /**
     * 存储图片数据
     */
    public void storeImages(List<CocoImage> images) {
        if (images != null && !images.isEmpty()) {
            this.images.clear();
            this.images.addAll(images);
            buildImageMap();
            System.out.println("已存储 " + images.size() + " 张图片");
        }
    }
    
    /**
     * 批量存储所有数据
     */
    public void storeAll(List<CocoAnnotation> annotations, 
                        List<CocoCategory> categories, 
                        List<CocoImage> images) {
        storeAnnotations(annotations);
        storeCategories(categories);
        storeImages(images);
    }
    
    /**
     * 构建标注索引
     */
    private void buildAnnotationIndexes() {
        annotationsByImageMap.clear();
        annotationsByCategoryMap.clear();
        
        for (CocoAnnotation annotation : annotations) {
            // 按图片ID分组
            annotationsByImageMap.computeIfAbsent(annotation.getImageId(), k -> new ArrayList<>())
                                .add(annotation);
            
            // 按类别ID分组
            annotationsByCategoryMap.computeIfAbsent(annotation.getCategoryId(), k -> new ArrayList<>())
                                   .add(annotation);
        }
        
        System.out.println("标注索引构建完成");
        System.out.println("  - 按图片分组: " + annotationsByImageMap.size() + " 组");
        System.out.println("  - 按类别分组: " + annotationsByCategoryMap.size() + " 组");
    }
    
    /**
     * 构建类别映射
     */
    private void buildCategoryMap() {
        categoryMap.clear();
        for (CocoCategory category : categories) {
            categoryMap.put(category.getId(), category);
        }
    }
    
    /**
     * 构建图片映射
     */
    private void buildImageMap() {
        imageMap.clear();
        for (CocoImage image : images) {
            imageMap.put(image.getId(), image);
        }
    }
    
    /**
     * 获取所有标注
     */
    public List<CocoAnnotation> getAllAnnotations() {
        return Collections.unmodifiableList(annotations);
    }
    
    /**
     * 获取所有类别
     */
    public List<CocoCategory> getAllCategories() {
        return Collections.unmodifiableList(categories);
    }
    
    /**
     * 获取所有图片
     */
    public List<CocoImage> getAllImages() {
        return Collections.unmodifiableList(images);
    }
    
    /**
     * 根据图片ID获取标注
     */
    public List<CocoAnnotation> getAnnotationsByImageId(Long imageId) {
        return annotationsByImageMap.getOrDefault(imageId, Collections.emptyList());
    }
    
    /**
     * 根据类别ID获取标注
     */
    public List<CocoAnnotation> getAnnotationsByCategoryId(Long categoryId) {
        return annotationsByCategoryMap.getOrDefault(categoryId, Collections.emptyList());
    }
    
    /**
     * 根据类别ID获取类别对象
     */
    public CocoCategory getCategoryById(Long categoryId) {
        return categoryMap.get(categoryId);
    }
    
    /**
     * 根据图片ID获取图片对象
     */
    public CocoImage getImageById(Long imageId) {
        return imageMap.get(imageId);
    }
    
    /**
     * 获取类别名称
     */
    public String getCategoryName(Long categoryId) {
        CocoCategory category = categoryMap.get(categoryId);
        return category != null ? category.getName() : "Unknown";
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_annotations", annotations.size());
        stats.put("total_categories", categories.size());
        stats.put("total_images", images.size());
        stats.put("images_with_annotations", annotationsByImageMap.size());
        
        // 每个类别的标注数量
        Map<String, Long> annotationsPerCategory = new HashMap<>();
        for (Map.Entry<Long, List<CocoAnnotation>> entry : annotationsByCategoryMap.entrySet()) {
            String categoryName = getCategoryName(entry.getKey());
            annotationsPerCategory.put(categoryName, (long) entry.getValue().size());
        }
        stats.put("annotations_per_category", annotationsPerCategory);
        
        return stats;
    }
    
    /**
     * 打印统计信息
     */
    public void printStatistics() {
        System.out.println("\n========== COCO数据集统计信息 ==========");
        System.out.println("标注总数: " + annotations.size());
        System.out.println("类别总数: " + categories.size());
        System.out.println("图片总数: " + images.size());
        System.out.println("有标注的图片数: " + annotationsByImageMap.size());
        
        System.out.println("\n各类别标注数量:");
        Map<String, Long> annotationsPerCategory = new TreeMap<>();
        for (Map.Entry<Long, List<CocoAnnotation>> entry : annotationsByCategoryMap.entrySet()) {
            String categoryName = getCategoryName(entry.getKey());
            annotationsPerCategory.put(categoryName, (long) entry.getValue().size());
        }
        
        annotationsPerCategory.forEach((name, count) -> 
            System.out.println(String.format("  %-20s: %d", name, count))
        );
        
        System.out.println("========================================\n");
    }
}

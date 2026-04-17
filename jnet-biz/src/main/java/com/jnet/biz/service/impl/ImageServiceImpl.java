package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.dto.BatchSelectImagesDTO;
import com.jnet.biz.dto.ImageQueryDTO;
import com.jnet.biz.entity.Batch;
import com.jnet.biz.entity.Image;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.biz.service.IBatchService;
import com.jnet.biz.service.IImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 图像资产 Service 实现类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image> implements IImageService {

    private final IBatchService batchService;

    @Override
    public Page<Image> searchImages(ImageQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<Image> page = query.toPage();
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        
        // 所属批次ID筛选
        if (query.getBatchId() != null) {
            wrapper.eq(Image::getBatchId, query.getBatchId());
        }
        
        // 生命周期状态筛选
        if (query.getLifecycleStatus() != null) {
            wrapper.eq(Image::getLifecycleStatus, query.getLifecycleStatus().name());
        }
        
        // 病理报告号模糊查询
        if (StringUtils.hasText(query.getPathologyId())) {
            wrapper.like(Image::getPathologyId, query.getPathologyId());
        }
        
        // 患者ID模糊查询
        if (StringUtils.hasText(query.getPatientId())) {
            wrapper.like(Image::getPatientId, query.getPatientId());
        }
        
        // 图像格式筛选
        if (StringUtils.hasText(query.getFormat())) {
            wrapper.eq(Image::getFormat, query.getFormat());
        }
        
        // 标注进度范围筛选
        if (query.getMinAnnotationProgress() != null) {
            wrapper.ge(Image::getAnnotationProgress, query.getMinAnnotationProgress());
        }
        if (query.getMaxAnnotationProgress() != null) {
            wrapper.le(Image::getAnnotationProgress, query.getMaxAnnotationProgress());
        }
        
        // TODO: 如果需要按标签筛选，需要关联查询 biz_image_tag_rel
        // 这里简化处理，实际应该使用自定义SQL
        
        // 排序处理
        if (StringUtils.hasText(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                wrapper.orderByAsc(getOrderColumn(query.getOrderBy()));
            } else {
                wrapper.orderByDesc(getOrderColumn(query.getOrderBy()));
            }
        } else {
            wrapper.orderByDesc(Image::getCreateTime);
        }
        
        return this.page(page, wrapper);
    }

    /**
     * 获取排序字段（防止SQL注入）
     */
    private SFunction<Image, ?> getOrderColumn(String orderBy) {
        return switch (orderBy.toLowerCase()) {
            case "pathology_id" -> Image::getPathologyId;
            case "patient_id" -> Image::getPatientId;
            case "lifecycle_status" -> Image::getLifecycleStatus;
            case "annotation_progress" -> Image::getAnnotationProgress;
            case "create_time" -> Image::getCreateTime;
            default -> Image::getCreateTime; // 默认按创建时间排序
        };
    }

    @Override
    public boolean updateLifecycleStatus(Long imageId, String status) {
        Image image = this.getById(imageId);
        if (image == null) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, 
                    "图像不存在: " + imageId);
        }
        
        image.setLifecycleStatus(status);
        return this.updateById(image);
    }

    @Override
    public boolean batchUpdateAnnotationProgress(List<Long> imageIds, Integer progress) {
        if (imageIds == null || imageIds.isEmpty()) {
            return false;
        }
        
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Image::getImageId, imageIds);
        
        Image updateEntity = new Image();
        updateEntity.setAnnotationProgress(progress);
        
        return this.update(updateEntity, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSelectImages(BatchSelectImagesDTO dto) {
        if (dto.getImageIds() == null || dto.getImageIds().isEmpty()) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "图像ID列表不能为空");
        }

        // 1. 验证目标批次
        Batch targetBatch = batchService.getById(dto.getTargetBatchId());
        if (targetBatch == null) {
            throw new BizException(BizErrorCode.BATCH_NOT_FOUND, 
                    "目标批次不存在: " + dto.getTargetBatchId());
        }

        // 2. 查询所有图像
        List<Image> images = this.listByIds(dto.getImageIds());
        if (images.size() != dto.getImageIds().size()) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, "部分图像不存在");
        }

        // 3. 执行移动或复制操作
        String operationType = dto.getOperationType() != null ? dto.getOperationType() : "COPY";
        int successCount = 0;

        for (Image image : images) {
            try {
                if ("MOVE".equalsIgnoreCase(operationType)) {
                    // 移动操作：更新批次ID和文件路径
                    moveImageToBatch(image, targetBatch);
                } else {
                    // 复制操作：创建新记录并复制文件
                    copyImageToBatch(image, targetBatch);
                }
                successCount++;
            } catch (Exception e) {
                log.error("处理图像失败: imageId={}", image.getImageId(), e);
                // 继续处理下一个，不中断整个流程
            }
        }

        log.info("批量选择完成: total={}, success={}, operation={}", 
                dto.getImageIds().size(), successCount, operationType);

        return successCount > 0;
    }

    /**
     * 移动图像到目标批次
     */
    private void moveImageToBatch(Image image, Batch targetBatch) throws IOException {
        // 1. 构建新的文件路径：E:\doc\jnet\imageStore\{projectCode}\{batchCode}\{filename}
        String projectCode = getProjectCodeByBatchId(targetBatch.getProjectId());
        String newDir = String.format("E:/doc/jnet/imageStore/%s/%s", 
                projectCode, targetBatch.getBatchCode());
        File dir = new File(newDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String newFilePath = newDir + "/" + image.getFilename();
        File oldFile = new File(image.getFilePath());
        File newFile = new File(newFilePath);

        // 2. 移动文件
        if (oldFile.exists()) {
            Files.move(Paths.get(image.getFilePath()), Paths.get(newFilePath), 
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // 3. 更新数据库记录
        image.setBatchId(targetBatch.getBatchId());
        image.setFilePath(newFilePath);
        this.updateById(image);

        // 4. 更新批次统计（TODO: 需要在IBatchService中添加此方法）
        // batchService.updateBatchImageCount(targetBatch.getBatchId());
    }

    /**
     * 复制图像到目标批次
     */
    private void copyImageToBatch(Image sourceImage, Batch targetBatch) throws IOException {
        // 1. 构建新的文件路径
        String projectCode = getProjectCodeByBatchId(targetBatch.getProjectId());
        String newDir = String.format("E:/doc/jnet/imageStore/%s/%s", 
                projectCode, targetBatch.getBatchCode());
        File dir = new File(newDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String newFilePath = newDir + "/" + sourceImage.getFilename();
        File sourceFile = new File(sourceImage.getFilePath());
        File targetFile = new File(newFilePath);

        // 2. 复制文件
        if (sourceFile.exists()) {
            Files.copy(Paths.get(sourceImage.getFilePath()), Paths.get(newFilePath), 
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // 3. 创建新的图像记录
        Image newImage = new Image();
        newImage.setBatchId(targetBatch.getBatchId());
        newImage.setFilename(sourceImage.getFilename());
        newImage.setFilePath(newFilePath);
        newImage.setPathologyId(sourceImage.getPathologyId());
        newImage.setPatientId(sourceImage.getPatientId());
        newImage.setFormat(sourceImage.getFormat());
        newImage.setLifecycleStatus(sourceImage.getLifecycleStatus());
        newImage.setAnnotationProgress(sourceImage.getAnnotationProgress());
        newImage.setWidth(sourceImage.getWidth());
        newImage.setHeight(sourceImage.getHeight());
        newImage.setMppX(sourceImage.getMppX());
        newImage.setMppY(sourceImage.getMppY());
        newImage.setMagnification(sourceImage.getMagnification());
        newImage.setFileSize(sourceImage.getFileSize());
        newImage.setScannerInfo(sourceImage.getScannerInfo());
        newImage.setMetadata(sourceImage.getMetadata());
        newImage.setThumbnailUrl(sourceImage.getThumbnailUrl());
        newImage.setCreateTime(java.time.LocalDateTime.now());
        newImage.setUpdateTime(java.time.LocalDateTime.now());
        newImage.setDelFlag(false);

        this.save(newImage);

        // 4. 更新批次统计（TODO: 需要在IBatchService中添加此方法）
        // batchService.updateBatchImageCount(targetBatch.getBatchId());
    }

    /**
     * 根据批次ID获取项目编码
     */
    private String getProjectCodeByBatchId(Long projectId) {
        // TODO: 实际应该从数据库查询项目表获取projectCode
        // 这里简化处理，返回默认值
        return "PROJECT_" + projectId;
    }
}

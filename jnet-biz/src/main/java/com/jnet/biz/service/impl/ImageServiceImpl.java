package com.jnet.biz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.biz.dto.BatchSelectImagesDTO;
import com.jnet.biz.dto.CopyImageDTO;
import com.jnet.biz.dto.ImageQueryDTO;
import com.jnet.biz.dto.ReparseResult;
import com.jnet.biz.entity.Batch;
import com.jnet.biz.entity.Image;
import com.jnet.biz.entity.Project;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.biz.service.IBatchService;
import com.jnet.biz.service.IImageService;
import com.jnet.biz.config.StoragePathConfig;
import com.jnet.biz.util.OpenSlideMetadataParser;
import com.jnet.biz.util.OpenSlideTiffConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final OpenSlideTiffConverter tiffConverter;
    private final OpenSlideMetadataParser metadataParser;
    private final StoragePathConfig storagePathConfig;
    private final com.jnet.biz.service.IProjectService projectService;

    @Override
    public Page<Image> searchImages(ImageQueryDTO query) {
        // 验证分页参数
        query.validate();
        
        Page<Image> page = query.toPage();
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        
        // 所属批次ID筛选
        if (query.getBatchId() != null) {
            wrapper.eq(Image::getBatchId, query.getBatchId());
        }else if (query.getProjectId() != null){
            wrapper.inSql(Image::getBatchId, "select batch_id from biz_batch where project_id = " + query.getProjectId());
        }
        
        // 生命周期状态筛选
        if (query.getLifecycleStatus() != null) {
            wrapper.eq(Image::getLifecycleStatus, query.getLifecycleStatus());
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
            wrapper.orderByDesc(Image::getImageId);
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
        
        // 手动设置更新时间和更新人
        image.setUpdateTime(java.time.LocalDateTime.now());
        image.setUpdateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyImages(CopyImageDTO dto) {
        if (dto.getImageIds() == null || dto.getImageIds().isEmpty()) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "图像ID列表不能为空");
        }

        if (dto.getTargetBatchId() == null) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "目标文件夹ID不能为空");
        }

        // 1. 验证目标批次（文件夹）
        Batch targetBatch = batchService.getById(dto.getTargetBatchId());
        if (targetBatch == null) {
            throw new BizException(BizErrorCode.BATCH_NOT_FOUND, 
                    "目标文件夹不存在: " + dto.getTargetBatchId());
        }

        // 2. 查询所有要复制的图像
        List<Image> sourceImages = this.listByIds(dto.getImageIds());
        if (sourceImages.size() != dto.getImageIds().size()) {
            throw new BizException(BizErrorCode.IMAGE_NOT_FOUND, "部分源图像不存在");
        }

        // 3. 逐个复制图像
        int successCount = 0;
        for (Image sourceImage : sourceImages) {
            try {
                copyImageToBatch(sourceImage, targetBatch);
                successCount++;
                log.info("图像复制成功: sourceImageId={}, targetBatchId={}", 
                        sourceImage.getImageId(), dto.getTargetBatchId());
            } catch (Exception e) {
                log.error("图像复制失败: sourceImageId={}, filename={}", 
                        sourceImage.getImageId(), sourceImage.getFilename(), e);
                // 继续处理下一个，不中断整个流程
            }
        }

        log.info("批量复制完成: total={}, success={}", 
                dto.getImageIds().size(), successCount);

        return successCount > 0;
    }

    /**
     * 移动图像到目标批次
     */
    private void moveImageToBatch(Image image, Batch targetBatch) throws IOException {
        // 1. 构建新的文件路径：{rootPath}/{projectCode}/{batchCode}/{filename}
        String projectCode = getProjectCodeById(targetBatch.getProjectId());
        String newDir = storagePathConfig.getBatchDir(projectCode, targetBatch.getBatchCode());
        File dir = new File(newDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    
        String newFilePath = storagePathConfig.getImageFilePath(projectCode, targetBatch.getBatchCode(), image.getFilename());
            
        // 【改造】将相对路径转换为绝对路径进行文件操作
        String oldRelativePath = image.getOriginalFilePath() != null 
                ? image.getOriginalFilePath() 
                : image.getFilePath();
        String oldFilePath = storagePathConfig.toAbsolutePath(oldRelativePath);
            
        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);
    
        // 2. 移动文件
        if (oldFile.exists()) {
            Files.move(Paths.get(oldFilePath), Paths.get(newFilePath), 
                    StandardCopyOption.REPLACE_EXISTING);
        }
    
        // 3. 更新数据库记录（存储相对路径）
        image.setBatchId(targetBatch.getBatchId());
        String newRelativePath = storagePathConfig.toRelativePath(newFilePath);
        image.setFilePath(newRelativePath);  // 兼容旧字段
        image.setOriginalFilePath(newRelativePath);  // 【新增】
        this.updateById(image);
    
        // 4. 更新批次统计（TODO: 需要在 IBatchService 中添加此方法）
        // batchService.updateBatchImageCount(targetBatch.getBatchId());
    }

    /**
     * 复制图像到目标批次
     */
    private void copyImageToBatch(Image sourceImage, Batch targetBatch) throws IOException {
        // 1. 构建新的文件路径
        String projectCode = getProjectCodeById(targetBatch.getProjectId());
        String newDir = storagePathConfig.getBatchDir(projectCode, targetBatch.getBatchCode());
        File dir = new File(newDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 2. 复制原始文件
        String originalSourceRelativePath = sourceImage.getOriginalFilePath() != null 
                ? sourceImage.getOriginalFilePath() 
                : sourceImage.getFilePath();  // 兼容旧数据
        String originalSourcePath = storagePathConfig.toAbsolutePath(originalSourceRelativePath);
        
        String newOriginalPath = storagePathConfig.getImageFilePath(
                projectCode, targetBatch.getBatchCode(), sourceImage.getFilename());
        
        File sourceFile = new File(originalSourcePath);
        if (sourceFile.exists()) {
            Files.copy(Paths.get(originalSourcePath), Paths.get(newOriginalPath), 
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("复制原始文件: {} -> {}", originalSourcePath, newOriginalPath);
        }

        // 3. 【关键改造】如果有转换文件，也复制转换文件
        String newConvertedPath = null;
        if (sourceImage.getConvertedTiffPath() != null && !sourceImage.getConvertedTiffPath().isEmpty()) {
            String convertedSourcePath = storagePathConfig.toAbsolutePath(sourceImage.getConvertedTiffPath());
            
            File convertedSourceFile = new File(convertedSourcePath);
            if (convertedSourceFile.exists()) {
                // 生成新的转换文件路径
                String newConvertedAbsolutePath = storagePathConfig.getConvertedTiffPath(
                        sourceImage.getFilename(), projectCode, targetBatch.getBatchCode());
                
                // 确保目录存在
                File newConvertedDir = new File(newConvertedAbsolutePath).getParentFile();
                if (!newConvertedDir.exists()) {
                    newConvertedDir.mkdirs();
                }
                
                // 复制转换文件
                Files.copy(Paths.get(convertedSourcePath), 
                          Paths.get(newConvertedAbsolutePath), 
                          StandardCopyOption.REPLACE_EXISTING);
                
                // 存储相对路径
                newConvertedPath = storagePathConfig.toRelativePath(newConvertedAbsolutePath);
                log.info("复制转换文件: {} -> {}", convertedSourcePath, newConvertedAbsolutePath);
            }
        }

        // 4. 创建新的图像记录
        Image newImage = new Image();
        newImage.setBatchId(targetBatch.getBatchId());
        newImage.setFilename(sourceImage.getFilename());
        newImage.setOriginalFilename(sourceImage.getOriginalFilename());
        
        // 【改造】存储相对路径
        String newOriginalRelativePath = storagePathConfig.toRelativePath(newOriginalPath);
        newImage.setFilePath(newOriginalRelativePath);  // 兼容旧字段
        newImage.setOriginalFilePath(newOriginalRelativePath);  // 【新增】
        newImage.setConvertedTiffPath(newConvertedPath);  // 【新增】相对路径
        
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
        newImage.setRequiresConversion(sourceImage.getRequiresConversion());  // 【新增】
        newImage.setConversionStatus(sourceImage.getConversionStatus());  // 【新增】
        newImage.setCreateTime(java.time.LocalDateTime.now());
        newImage.setUpdateTime(java.time.LocalDateTime.now());
        newImage.setCreateBy(1L); // TODO: 从 SecurityContext 获取当前用户 ID
        newImage.setUpdateBy(1L); // TODO: 从 SecurityContext 获取当前用户 ID
        newImage.setDelFlag(false);

        this.save(newImage);

        // 5. 更新批次统计（TODO: 需要在 IBatchService 中添加此方法）
//         batchService.updateBatchImageCount(targetBatch.getBatchId());
    }

    /**
     * 根据项目ID获取项目编码
     */
    private String getProjectCodeById(Long projectId) {
        // 从数据库查询项目表获取真实的 projectCode
        Project project = projectService.getById(projectId);
        if (project != null && project.getCode() != null) {
            return project.getCode();
        }
        // 降级处理：如果查询失败，使用默认格式
        log.warn("无法获取项目编码，使用默认格式: projectId={}", projectId);
        return "project_" + projectId;
    }

    @Override
//    @Transactional(rollbackFor = Exception.class)
    public ReparseResult batchReparseMetadata(List<Long> imageIds, Long projectId, Long batchId, boolean forceReparse) {
        ReparseResult result = new ReparseResult();
        
        // 1. 确定要解析的图像列表
        List<Image> imagesToReparse;
        if (imageIds != null && !imageIds.isEmpty()) {
            // 手动选择图像
            imagesToReparse = this.listByIds(imageIds);
            log.info("手动选择 {} 个图像进行重新解析", imagesToReparse.size());
        } else if (batchId != null) {
            // 按批次解析所有图像
            LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Image::getBatchId, batchId);
            imagesToReparse = this.list(wrapper);
            log.info("批次 {} 下共找到 {} 个图像", batchId, imagesToReparse.size());
        } else if (projectId != null) {
            // 按项目解析所有图像
            LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
            wrapper.inSql(Image::getBatchId, 
                "SELECT batch_id FROM biz_batch WHERE project_id = " + projectId);
            imagesToReparse = this.list(wrapper);
            log.info("项目 {} 下共找到 {} 个图像", projectId, imagesToReparse.size());
        } else {
            throw new BizException(BizErrorCode.PARAM_ERROR, "必须提供图像ID列表、项目ID或批次ID");
        }
        
        result.setTotalCount(imagesToReparse.size());
        
        if (imagesToReparse.isEmpty()) {
            log.warn("没有找到需要解析的图像");
            return result;
        }
        
        // 2. 逐个处理图像
        for (Image image : imagesToReparse) {
            try {
                // 检查是否需要跳过
                if (!forceReparse && hasValidMetadata(image)) {
                    log.debug("跳过已有元数据的图像: imageId={}, filename={}", 
                        image.getImageId(), image.getFilename());
                    result.setSkippedCount(result.getSkippedCount() + 1);
                    continue;
                }
                
                // 执行重新解析
                reparseSingleImage(image);
                result.setSuccessCount(result.getSuccessCount() + 1);
                log.info("图像解析成功: imageId={}, filename={}", 
                    image.getImageId(), image.getFilename());
                
            } catch (Exception e) {
                log.error("图像解析失败: imageId={}, filename={}", 
                    image.getImageId(), image.getFilename(), e);
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrorMessages().add(
                    String.format("图像 %s 解析失败: %s", image.getFilename(), e.getMessage()));
            }
        }
        
        log.info("批量重新解析完成: total={}, success={}, failed={}, skipped={}",
            result.getTotalCount(), result.getSuccessCount(), 
            result.getFailedCount(), result.getSkippedCount());
        
        return result;
    }
    
    /**
     * 检查图像是否已有有效的元数据
     */
    private boolean hasValidMetadata(Image image) {
        // 如果已有宽度、高度和层级信息，认为已有元数据
        return image.getWidth() != null && image.getWidth() > 0
            && image.getHeight() != null && image.getHeight() > 0
            && image.getLevels() != null && image.getLevels() > 0;
    }
    
    /**
     * 重新解析单个图像
     */
    private void reparseSingleImage(Image image) throws IOException {
        String filePath = image.getFilePath();
        if (!StringUtils.hasText(filePath)) {
            throw new IOException("文件路径为空");
        }
        
        // 【修复】将相对路径转换为绝对路径
        String absolutePath = storagePathConfig.toAbsolutePath(filePath);
        File imageFile = new File(absolutePath);
        if (!imageFile.exists()) {
            throw new IOException("文件不存在: " + absolutePath);
        }
        
        log.info("开始解析图像: imageId={}, file={}", image.getImageId(), absolutePath);
        
        // 获取批次和项目信息
        Batch batch = batchService.getById(image.getBatchId());
        if (batch == null) {
            throw new IOException("批次不存在: batchId=" + image.getBatchId());
        }
        String projectCode = getProjectCodeById(batch.getProjectId());
        String batchCode = batch.getBatchCode();
        
        // 1. 如果是 JPG/PNG，先转换为 OpenSlide 兼容的 TIFF
        File processedFile = tiffConverter.ensureOpenSlideCompatible(
                image.getImageId(), absolutePath, projectCode, batchCode);
        
        // 2. 使用 OpenSlide 解析元数据
        parseAndSetMetadata(image, processedFile.getAbsolutePath());
        
        // 3. 更新数据库
        this.updateById(image);
        
        log.info("图像解析完成: imageId={}, width={}, height={}, levels={}",
            image.getImageId(), image.getWidth(), image.getHeight(), image.getLevels());
    }
    
    /**
     * 解析并设置图像元数据（使用OpenSlide）
     */
    private void parseAndSetMetadata(Image image, String filePath) throws IOException {
        metadataParser.parseAndSetMetadata(image, filePath);
    }
}

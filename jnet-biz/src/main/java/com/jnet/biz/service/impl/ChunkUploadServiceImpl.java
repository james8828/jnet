package com.jnet.biz.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jnet.biz.dto.ChunkUploadDTO;
import com.jnet.biz.dto.ChunkUploadInitDTO;
import com.jnet.biz.entity.Batch;
import com.jnet.biz.entity.Image;
import com.jnet.biz.enums.ImageFormat;
import com.jnet.biz.enums.LifecycleStatus;
import com.jnet.biz.exception.BizErrorCode;
import com.jnet.biz.exception.BizException;
import com.jnet.biz.mapper.ImageMapper;
import com.jnet.biz.service.IBatchService;
import com.jnet.biz.service.IChunkUploadService;
import com.jnet.biz.vo.ChunkUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openslide.OpenSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 图像分片上传 Service 实现类
 *
 * @author JNet Team
 * @since 2024-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements IChunkUploadService {

    private final ImageMapper imageMapper;
    private final IBatchService batchService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${data-pool.storage.temp-path:E:/doc/jnet/imageStore/temp}")
    private String tempPath;

    @Value("${data-pool.storage.root-path:E:/doc/jnet/imageStore}")
    private String rootPath;

    /**
     * Redis Key前缀
     */
    private static final String UPLOAD_PREFIX = "upload:chunk:";
    private static final String UPLOAD_EXPIRE_KEY = "upload:expire:";
    private static final long EXPIRE_TIME = 24 * 3600; // 24小时过期

    @Override
    public ChunkUploadVO initUpload(ChunkUploadInitDTO initDTO) {
        log.info("初始化分片上传: filename={}, size={}, batchId={}", 
                initDTO.getFilename(), initDTO.getFileSize(), initDTO.getBatchId());

        // 1. 验证批次是否存在
        Batch batch = batchService.getById(initDTO.getBatchId());
        if (batch == null) {
            throw new BizException(BizErrorCode.BATCH_NOT_FOUND, 
                    "批次不存在: " + initDTO.getBatchId());
        }

        // 2. 获取文件MD5（由前端计算后传入）
        String fileMd5 = initDTO.getFileMd5();
        if (fileMd5 == null || fileMd5.isEmpty()) {
            // 如果前端未传入，则使用伪MD5（不推荐）
            log.warn("前端未传入fileMd5，使用伪MD5（可能影响秒传准确性）");
            fileMd5 = generatePseudoMd5(initDTO);
        }

        // 3. 检查是否秒传
        if (checkFileExists(fileMd5)) {
            log.info("文件已存在，支持秒传: {}", fileMd5);
            Image existingImage = findImageByFilePath(fileMd5, initDTO.getFilename());
            return ChunkUploadVO.builder()
                    .fileMd5(fileMd5)
                    .exists(true)
                    .imageId(existingImage != null ? existingImage.getImageId() : null)
                    .uploadedChunks(new ArrayList<>())
                    .message("文件已存在，秒传成功")
                    .build();
        }

        // 4. 生成分片ID
        String uploadId = generateUploadId(fileMd5);
        String redisKey = UPLOAD_PREFIX + uploadId;

        // 5. 检查是否有未完成的上传（支持断点续传）
        @SuppressWarnings("unchecked")
        List<Integer> uploadedChunks = (List<Integer>) redisTemplate.opsForValue().get(redisKey);
        
        boolean hasUnfinishedUpload = uploadedChunks != null && !uploadedChunks.isEmpty();
        
        if (hasUnfinishedUpload) {
            log.info("检测到未完成的上传，支持断点续传: uploadId={}, 已完成{}/{}分片",
                    uploadId, uploadedChunks.size(), initDTO.getTotalChunks());
        } else {
            // 6. 创建新的上传任务
            String chunkDir = getChunkDir(uploadId);
            File dir = new File(chunkDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 初始化Redis记录
            uploadedChunks = new ArrayList<>();
            redisTemplate.opsForValue().set(redisKey, uploadedChunks, EXPIRE_TIME, TimeUnit.SECONDS);
            
            // 保存元数据
            String metaKey = UPLOAD_EXPIRE_KEY + uploadId;
            redisTemplate.opsForValue().set(metaKey, initDTO, EXPIRE_TIME, TimeUnit.SECONDS);
            
            log.info("上传初始化成功: uploadId={}, chunkDir={}", uploadId, chunkDir);
        }

        return ChunkUploadVO.builder()
                .fileMd5(fileMd5)
                .exists(false)
                .uploadedChunks(uploadedChunks)
                .uploadId(uploadId)
                .tempPath(getChunkDir(uploadId))
                .message(hasUnfinishedUpload ? "继续上传" : "初始化成功")
                .build();
    }

    @Override
    public Boolean uploadChunk(ChunkUploadDTO uploadDTO) {
        // 1. 生成分片ID
        String uploadId = generateUploadId(uploadDTO.getFileMd5());
        String redisKey = UPLOAD_PREFIX + uploadId;

        // 2. 检查上传是否有效
        if (!redisTemplate.hasKey(redisKey)) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "上传已过期，请重新初始化");
        }

        // 3. 保存分片文件
        String chunkPath = getChunkPath(uploadId, uploadDTO.getChunkIndex());
        try {
            File chunkFile = new File(chunkPath);
            
            // 确保父目录存在
            File parentDir = chunkFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            if (uploadDTO.getFile() != null && !uploadDTO.getFile().isEmpty()) {
                uploadDTO.getFile().transferTo(chunkFile);
            } else {
                throw new BizException(BizErrorCode.PARAM_ERROR, "分片文件不能为空");
            }
        } catch (IOException e) {
            log.error("保存分片失败: {}", chunkPath, e);
            throw new BizException(BizErrorCode.SYSTEM_ERROR, "保存分片失败: " + e.getMessage());
        }

        // 4. 更新Redis记录
        @SuppressWarnings("unchecked")
        List<Integer> uploadedChunks = (List<Integer>) redisTemplate.opsForValue().get(redisKey);
        if (uploadedChunks == null) {
            uploadedChunks = new ArrayList<>();
        }
        
        boolean isNewChunk = !uploadedChunks.contains(uploadDTO.getChunkIndex());
        if (isNewChunk) {
            uploadedChunks.add(uploadDTO.getChunkIndex());
            redisTemplate.opsForValue().set(redisKey, uploadedChunks, EXPIRE_TIME, TimeUnit.SECONDS);
        }

        // 5. 获取元数据以计算进度
        String metaKey = UPLOAD_EXPIRE_KEY + uploadId;
        ChunkUploadInitDTO initDTO = (ChunkUploadInitDTO) redisTemplate.opsForValue().get(metaKey);
        int totalChunks = initDTO != null ? initDTO.getTotalChunks() : 0;
        
        // 6. 输出进度日志（每10个分片或最后一个分片输出一次）
        if (isNewChunk && (uploadedChunks.size() % 10 == 0 || uploadedChunks.size() == totalChunks)) {
            double progress = totalChunks > 0 ? (uploadedChunks.size() * 100.0 / totalChunks) : 0;
            log.info(String.format("分片上传进度: %d/%d (%.1f%%), 当前分片: %d", 
                    uploadedChunks.size(), totalChunks, progress, uploadDTO.getChunkIndex()));
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeChunks(String fileMd5, Long batchId, String filename, String pathologyId, String patientId) {
        log.info("合并分片: fileMd5={}, batchId={}, filename={}", fileMd5, batchId, filename);

        // 1. 生成分片ID
        String uploadId = generateUploadId(fileMd5);
        String redisKey = UPLOAD_PREFIX + uploadId;

        // 2. 获取已上传的分片列表
        @SuppressWarnings("unchecked")
        List<Integer> uploadedChunks = (List<Integer>) redisTemplate.opsForValue().get(redisKey);
        
        // 3. 获取元数据
        String metaKey = UPLOAD_EXPIRE_KEY + uploadId;
        ChunkUploadInitDTO initDTO = (ChunkUploadInitDTO) redisTemplate.opsForValue().get(metaKey);
        if (initDTO == null) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "上传元数据已过期");
        }
        
        int totalChunks = initDTO.getTotalChunks();
        
        // 4. 如果Redis中没有记录或记录不完整，扫描磁盘文件重建分片列表
        if (uploadedChunks == null || uploadedChunks.isEmpty()) {
            log.warn("Redis中无分片记录，尝试从磁盘扫描: uploadId={}", uploadId);
            
            // 检查分片目录是否存在
            String chunkDir = getChunkDir(uploadId);
            File dir = new File(chunkDir);
            if (!dir.exists()) {
                log.error("分片目录不存在: {}", chunkDir);
                throw new BizException(BizErrorCode.PARAM_ERROR, 
                        "分片目录不存在，请重新上传。可能原因：1)上传未开始 2)分片已被清理 3)Redis记录丢失");
            }
            
            uploadedChunks = scanDiskForChunks(uploadId, totalChunks);
            
            if (uploadedChunks.isEmpty()) {
                log.error("分片目录为空: {}, 请重新上传", chunkDir);
                throw new BizException(BizErrorCode.PARAM_ERROR, 
                        "分片目录为空，请重新上传。可能原因：1)上传中断 2)分片被手动删除 3)磁盘空间不足");
            }
            
            // 恢复Redis记录
            redisTemplate.opsForValue().set(redisKey, uploadedChunks, EXPIRE_TIME, TimeUnit.SECONDS);
            log.info("从磁盘恢复分片记录: {}/{}", uploadedChunks.size(), totalChunks);
        }
        
        log.info("开始合并: fileMd5={}, 总分片={}, 已上传={}", fileMd5, totalChunks, uploadedChunks.size());
        
        // 5. 检查是否有缺失的分片
        List<Integer> missingChunks = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!uploadedChunks.contains(i)) {
                missingChunks.add(i);
            }
        }
        
        if (!missingChunks.isEmpty()) {
            log.error("分片不完整! 总分片={}, 已上传={}, 缺失分片={}", 
                    totalChunks, uploadedChunks.size(), missingChunks);
            throw new BizException(BizErrorCode.PARAM_ERROR, 
                    String.format("分片不完整，缺少分片: %s", missingChunks));
        }

        // 4. 创建目标目录：E:\doc\jnet\imageStore\{projectCode}\{batchCode}
        Batch batch = batchService.getById(batchId);
        if (batch == null) {
            throw new BizException(BizErrorCode.BATCH_NOT_FOUND, "批次不存在");
        }

        String projectCode = getProjectCodeByBatchId(batchId);
        String targetDir = String.format("%s/%s/%s", rootPath, projectCode, batch.getBatchCode());
        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 5. 合并文件
        String targetPath = targetDir + "/" + filename;
        File targetFile = new File(targetPath);
        
        try (RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {
            for (int i = 0; i < initDTO.getTotalChunks(); i++) {
                if (!uploadedChunks.contains(i)) {
                    throw new BizException(BizErrorCode.PARAM_ERROR, 
                            "分片不完整，缺少分片: " + i);
                }
                
                String chunkPath = getChunkPath(uploadId, i);
                File chunkFile = new File(chunkPath);
                
                try (FileInputStream fis = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        raf.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            log.error("合并分片失败", e);
            throw new BizException(BizErrorCode.SYSTEM_ERROR, "合并分片失败: " + e.getMessage());
        }

        // 6. 创建图像记录
        Image image = new Image();
        image.setBatchId(batchId);
        image.setFilename(filename);  // 存储用文件名
        image.setOriginalFilename(filename);  // 原始文件名（用户上传时的名称）
        image.setFilePath(targetPath);
        image.setPathologyId(pathologyId);
        image.setPatientId(patientId);
        image.setFormat(detectImageFormat(filename));
        image.setLifecycleStatus(LifecycleStatus.RAW.getCode()); // 使用枚举的code
        image.setAnnotationProgress(0);
        image.setFileSize(targetFile.length());
        
        // 7. 异步解析WSI元数据（不阻塞上传流程）
        try {
            parseAndSetMetadata(image, targetPath, filename);
        } catch (Exception e) {
            log.warn("解析图像元数据失败: {}, 将稍后异步处理", filename, e);
            // 元数据解析失败不影响上传，可以后续异步处理
        }
        
        image.setCreateTime(LocalDateTime.now());
        image.setUpdateTime(LocalDateTime.now());
        image.setCreateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        image.setUpdateBy(1L); // TODO: 从SecurityContext获取当前用户ID
        image.setDelFlag(false);

        imageMapper.insert(image);

        // 8. 清理临时文件和Redis
        cleanupUpload(uploadId);

        // 9. 更新批次统计（TODO: 需要在IBatchService中添加此方法）
//         batchService.updateBatchImageCount(batchId);

        log.info("分片合并成功: imageId={}, path={}", image.getImageId(), targetPath);

        return image.getImageId();
    }

    @Override
    public void cancelUpload(String fileMd5) {
        String uploadId = generateUploadId(fileMd5);
        cleanupUpload(uploadId);
        log.info("取消上传: {}", uploadId);
    }

    @Override
    public Boolean checkFileExists(String fileMd5) {
        // 检查数据库中是否已有相同MD5的文件
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getFilePath, fileMd5) // 假设filePath存储MD5用于去重
               .or()
               .apply("metadata->>'fileMd5' = {0}", fileMd5); // 或者从metadata中查询
        
        return imageMapper.selectCount(wrapper) > 0;
    }

    /**
     * 生成伪MD5（仅当前端未传入时使用）
     */
    private String generatePseudoMd5(ChunkUploadInitDTO initDTO) {
        return org.apache.commons.codec.digest.DigestUtils.md5Hex(
                initDTO.getFilename() + "_" + initDTO.getFileSize());
    }

    /**
     * 生成上传ID
     */
    private String generateUploadId(String fileMd5) {
        return "upload_" + fileMd5;
    }

    /**
     * 获取分片目录
     */
    private String getChunkDir(String uploadId) {
        return tempPath + "/" + uploadId;
    }

    /**
     * 获取分片路径
     */
    private String getChunkPath(String uploadId, int chunkIndex) {
        return getChunkDir(uploadId) + "/chunk_" + chunkIndex;
    }

    /**
     * 清理上传临时文件
     */
    private void cleanupUpload(String uploadId) {
        // 删除临时目录
        String chunkDir = getChunkDir(uploadId);
        File dir = new File(chunkDir);
        if (dir.exists()) {
            deleteDirectory(dir);
        }

        // 删除Redis记录
        String redisKey = UPLOAD_PREFIX + uploadId;
        String metaKey = UPLOAD_EXPIRE_KEY + uploadId;
        redisTemplate.delete(redisKey);
        redisTemplate.delete(metaKey);
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * 检测图像格式
     */
    private String detectImageFormat(String filename) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
        return switch (ext) {
            case "SVS" -> ImageFormat.SVS.name();
            case "NDPI" -> ImageFormat.NDPI.name();
            case "JPG", "JPEG" -> ImageFormat.JPG.name();
            case "PNG" -> ImageFormat.PNG.name();
            case "TIFF", "TIF" -> ImageFormat.TIFF.name();
            default -> "UNKNOWN";
        };
    }

    /**
     * 根据批次ID获取项目编码
     */
    private String getProjectCodeByBatchId(Long batchId) {
        // 通过batchService查询batch，再关联project
        Batch batch = batchService.getById(batchId);
        if (batch != null && batch.getProjectId() != null) {
            // TODO: 这里需要关联查询项目表获取projectCode
            // 简化处理：使用projectId作为projectCode
            return "project_" + batch.getProjectId();
        }
        return "default_project";
    }

    /**
     * 根据文件路径查找已存在的图像记录
     */
    private Image findImageByFilePath(String fileMd5, String filename) {
        // 简化实现：根据文件名和MD5查找
        // 实际应该有更精确的匹配逻辑
        LambdaQueryWrapper<Image> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Image::getFilename, filename)
               .eq(Image::getDelFlag, false)
               .orderByDesc(Image::getCreateTime)
               .last("LIMIT 1");
        return imageMapper.selectOne(wrapper);
    }

    /**
     * 解析并设置图像元数据（使用OpenSlide）
     */
    private void parseAndSetMetadata(Image image, String filePath, String filename) {
        try {
            // 只对WSI格式进行元数据解析
            String format = image.getFormat();
            if (!"SVS".equals(format) && !"NDPI".equals(format) && !"TIFF".equals(format)) {
                log.debug("非WSI格式，跳过元数据解析: {}", format);
                return;
            }
    
            log.info("开始解析WSI元数据: {}", filePath);
                
            try (OpenSlide slide = new OpenSlide(new File(filePath))) {
                // 获取基本属性
                long width = slide.getLevel0Width();
                long height = slide.getLevel0Height();
                int levelCount = slide.getLevelCount();
                    
                image.setWidth((int) width);
                image.setHeight((int) height);
                image.setLevels(levelCount);
                    
                // 获取所有属性
                Map<String, String> properties = slide.getProperties();
                    
                // 调试：输出所有属性
                log.debug("=== OpenSlide 属性列表 ===");
                properties.forEach((key, value) -> {
                    log.debug("{}: {}", key, value);
                });
                    
                // 获取分辨率 (mpp = microns per pixel)
                String mppXStr = properties.get(OpenSlide.PROPERTY_NAME_MPP_X);
                String mppYStr = properties.get(OpenSlide.PROPERTY_NAME_MPP_Y);
                    
                if (mppXStr != null) {
                    try {
                        image.setMppX(Double.parseDouble(mppXStr));
                        log.debug("MPP X: {}", mppXStr);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析MPP X: {}", mppXStr);
                    }
                }
                if (mppYStr != null) {
                    try {
                        image.setMppY(Double.parseDouble(mppYStr));
                        log.debug("MPP Y: {}", mppYStr);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析MPP Y: {}", mppYStr);
                    }
                }
                    
                // 获取放大倍数
                String magStr = properties.get(OpenSlide.PROPERTY_NAME_OBJECTIVE_POWER);
                if (magStr != null) {
                    try {
                        image.setMagnification(Integer.parseInt(magStr));
                        log.debug("放大倍数: {}", magStr);
                    } catch (NumberFormatException e) {
                        log.warn("无法解析放大倍数: {}", magStr);
                    }
                }
                    
                // 构建元数据JSON
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("vendor", properties.get(OpenSlide.PROPERTY_NAME_VENDOR));
                metadata.put("quickhash1", properties.get(OpenSlide.PROPERTY_NAME_QUICKHASH1));
                metadata.put("levelCount", levelCount);
                metadata.put("properties", properties); // 保存所有属性
                    
                // 添加各层级信息
                List<Map<String, Object>> levels = new ArrayList<>();
                for (int i = 0; i < levelCount; i++) {
                    Map<String, Object> levelInfo = new HashMap<>();
                    long levelWidth = slide.getLevelWidth(i);
                    long levelHeight = slide.getLevelHeight(i);
                    levelInfo.put("level", i);
                    levelInfo.put("width", levelWidth);
                    levelInfo.put("height", levelHeight);
                    levelInfo.put("downsample", slide.getLevelDownsample(i));
                    levels.add(levelInfo);
                }
                metadata.put("levels", levels);
                    
                image.setMetadata(objectMapper.writeValueAsString(metadata));
                    
                log.info("WSI元数据解析成功: {}x{}, {} levels, MPP: {}x{}, 放大倍数: {}",
                        width, height, levelCount, 
                        image.getMppX(), image.getMppY(), 
                        image.getMagnification());
            }
                
        } catch (Exception e) {
            log.error("解析WSI元数据失败: {}", filePath, e);
            // 不抛出异常，允许上传继续
            log.warn("元数据解析失败，将使用默认值");
        }
    }
    
    /**
     * 扫描磁盘上的分片文件，重建分片列表
     * @param uploadId 上传ID
     * @param totalChunks 总分片数
     * @return 已上传的分片索引列表
     */
    private List<Integer> scanDiskForChunks(String uploadId, int totalChunks) {
        List<Integer> foundChunks = new ArrayList<>();
        String chunkDir = getChunkDir(uploadId);
        File dir = new File(chunkDir);
        
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("分片目录不存在: {}", chunkDir);
            return foundChunks;
        }
        
        // 扫描所有chunk_*文件
        File[] chunkFiles = dir.listFiles((d, name) -> name.startsWith("chunk_"));
        if (chunkFiles == null || chunkFiles.length == 0) {
            log.warn("分片目录为空: {}", chunkDir);
            return foundChunks;
        }
        
        log.info("扫描到 {} 个分片文件", chunkFiles.length);
        
        for (File chunkFile : chunkFiles) {
            try {
                // 从文件名提取分片索引：chunk_0 -> 0
                String fileName = chunkFile.getName();
                String indexStr = fileName.substring("chunk_".length());
                int index = Integer.parseInt(indexStr);
                
                // 验证分片索引有效且文件大小>0
                if (index >= 0 && index < totalChunks && chunkFile.length() > 0) {
                    foundChunks.add(index);
                    log.debug("找到分片: index={}, size={} bytes", index, chunkFile.length());
                } else {
                    log.warn("无效分片文件: {}, size={}", fileName, chunkFile.length());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析分片索引: {}", chunkFile.getName());
            }
        }
        
        // 排序
        foundChunks.sort(Integer::compareTo);
        
        log.info("磁盘扫描完成: 找到 {}/{} 个分片", foundChunks.size(), totalChunks);
        return foundChunks;
    }
}

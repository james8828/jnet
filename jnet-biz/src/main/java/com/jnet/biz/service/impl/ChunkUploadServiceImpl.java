package com.jnet.biz.service.impl;

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
import java.util.List;
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
        log.debug("上传分片: fileMd5={}, chunkIndex={}/{}", 
                uploadDTO.getFileMd5(), uploadDTO.getChunkIndex(), uploadDTO.getTotalChunks());

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
        
        if (!uploadedChunks.contains(uploadDTO.getChunkIndex())) {
            uploadedChunks.add(uploadDTO.getChunkIndex());
            redisTemplate.opsForValue().set(redisKey, uploadedChunks, EXPIRE_TIME, TimeUnit.SECONDS);
        }

        log.debug("分片上传成功: {}/{}", uploadedChunks.size(), uploadDTO.getTotalChunks());

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
        if (uploadedChunks == null || uploadedChunks.isEmpty()) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "没有可合并的分片");
        }

        // 3. 获取元数据
        String metaKey = UPLOAD_EXPIRE_KEY + uploadId;
        ChunkUploadInitDTO initDTO = (ChunkUploadInitDTO) redisTemplate.opsForValue().get(metaKey);
        if (initDTO == null) {
            throw new BizException(BizErrorCode.PARAM_ERROR, "上传元数据已过期");
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
        image.setFilename(filename);
        image.setFilePath(targetPath);
        image.setPathologyId(pathologyId);
        image.setPatientId(patientId);
        image.setFormat(detectImageFormat(filename));
        image.setLifecycleStatus(LifecycleStatus.RAW.name());
        image.setAnnotationProgress(0);
        image.setFileSize(targetFile.length());
        image.setCreateTime(LocalDateTime.now());
        image.setUpdateTime(LocalDateTime.now());
        image.setDelFlag(false);

        imageMapper.insert(image);

        // 7. 清理临时文件和Redis
        cleanupUpload(uploadId);

        // 8. 更新批次统计（TODO: 需要在IBatchService中添加此方法）
        // batchService.updateBatchImageCount(batchId);

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
        // 这里需要关联查询项目表，简化处理直接返回默认值
        // 实际应该通过batchService查询batch，再关联project
        return "DEFAULT_PROJECT";
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
}

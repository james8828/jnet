package com.jnet.biz.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;

/**
 * 上传临时文件清理任务
 * 
 * @author JNet Team
 * @since 2024-04-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCleanupTask {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String UPLOAD_PREFIX = "upload:chunk:";
    private static final String TEMP_PATH = "E:/doc/jnet/imageStore/temp";

    /**
     * 每天凌晨3点执行清理过期临时文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredUploads() {
        log.info("开始清理过期的上传临时文件...");
        
        int cleanedCount = 0;
        long totalSizeCleaned = 0;
        File tempDir = new File(TEMP_PATH);
        
        if (!tempDir.exists()) {
            log.warn("临时目录不存在: {}", TEMP_PATH);
            return;
        }
        
        // 遍历所有临时上传目录
        File[] uploadDirs = tempDir.listFiles(File::isDirectory);
        if (uploadDirs == null || uploadDirs.length == 0) {
            log.info("没有需要清理的临时目录");
            return;
        }
        
        for (File uploadDir : uploadDirs) {
            String uploadId = uploadDir.getName();
            String redisKey = UPLOAD_PREFIX + uploadId;
            
            // 检查Redis中是否存在该上传任务
            Boolean exists = redisTemplate.hasKey(redisKey);
            
            if (Boolean.FALSE.equals(exists)) {
                // Redis中不存在，说明已过期或已完成，删除临时文件
                long dirSize = calculateDirSize(uploadDir);
                if (deleteDirectory(uploadDir)) {
                    cleanedCount++;
                    totalSizeCleaned += dirSize;
                    log.debug("清理临时目录: {}, 大小: {} MB", 
                            uploadDir.getAbsolutePath(), dirSize / 1024 / 1024);
                }
            }
        }
        
        log.info("清理完成，共删除 {} 个过期临时目录，释放空间: {} MB", 
                cleanedCount, totalSizeCleaned / 1024 / 1024);
    }

    /**
     * 每小时检查一次孤儿记录（Redis存在但文件不存在的情况）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupOrphanRecords() {
        log.debug("检查孤儿上传记录...");
        
        Set<String> keys = redisTemplate.keys(UPLOAD_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        int orphanCount = 0;
        for (String key : keys) {
            String uploadId = key.replace(UPLOAD_PREFIX, "");
            File uploadDir = new File(TEMP_PATH + "/" + uploadId);
            
            if (!uploadDir.exists()) {
                // 文件目录不存在，但Redis中存在，删除Redis记录
                redisTemplate.delete(key);
                orphanCount++;
                log.debug("清理孤儿记录: {}", key);
            }
        }
        
        if (orphanCount > 0) {
            log.info("清理了 {} 条孤儿上传记录", orphanCount);
        }
    }

    /**
     * 计算目录大小
     */
    private long calculateDirSize(File directory) {
        long size = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += calculateDirSize(file);
                    }
                }
            }
        }
        return size;
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }
}

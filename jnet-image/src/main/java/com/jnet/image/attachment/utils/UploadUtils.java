package com.jnet.image.attachment.utils;

import com.jnet.image.constant.ImageConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 分块上传工具类
 */
@Slf4j
public class UploadUtils {

    private static final Integer COMPLETE = 1;

    /**
     * 存储分块信息的线程安全映射
     */
    private static final Map<String, UploadState> UPLOAD_STATE_MAP = new ConcurrentHashMap<>();

    /**
     * 初始化锁，用于保护某些关键操作
     */
    private static final ReentrantReadWriteLock initLock = new ReentrantReadWriteLock();

    /**
     * 向分块映射中添加或更新数据
     *
     * @param key   映射的键
     * @param value 映射的值
     */
    public static void putUploadStateValue(String key, UploadState value) {
        // 使用写锁保护写操作
        initLock.writeLock().lock();
        try {
            UPLOAD_STATE_MAP.put(key, value);
        } finally {
            initLock.writeLock().unlock();
        }
    }

    /**
     * 删除指定的块值
     *
     * @param key 要删除的块的键
     */
    public static void deleteUploadStateValue(String key) {
        // 使用写锁保护写操作
        initLock.writeLock().lock();
        try {
            UPLOAD_STATE_MAP.remove(key);
        } finally {
            initLock.writeLock().unlock();
        }
    }

    /**
     * 从分块映射中获取数据
     *
     * @param key 映射的键
     * @return 映射的值
     */
    public static UploadState getUploadStateValue(String key) {
        // 使用读锁保护读操作
        initLock.readLock().lock();
        try {
            return UPLOAD_STATE_MAP.get(key);
        } finally {
            initLock.readLock().unlock();
        }
    }

    /**
     * 检查上传状态值是否存在
     *
     * @param key 键值
     * @return 如果上传状态值存在，则返回true；否则返回false
     */
    public static boolean containsUploadStateValue(String key) {
        // 使用读锁保护读操作
        initLock.readLock().lock();
        try {
            return UPLOAD_STATE_MAP.containsKey(key);
        } finally {
            initLock.readLock().unlock();
        }
    }

    /**
     * 初始化分块上传状态
     *
     * @param key
     * @param chunkTotal
     */
    public static void initUploadState(String key, int chunkTotal) {
        if (!containsUploadStateValue(key))
            putUploadStateValue(key, new UploadState(chunkTotal));
    }

    /**
     * 判断文件所有分块是否已上传
     *
     * @param key
     * @return
     */
    public static boolean isUploaded(String key) {
        boolean flag = false;
        if (containsUploadStateValue(key)) {
            AtomicReference<int[]> status = getUploadStateValue(key).getStatus();
            int[] statusArray = status.get();
            log.info("文件：[{}];上传进度：[{}]", key, Arrays.stream(statusArray).sum() + "/" + statusArray.length);
            if (statusArray.length == Arrays.stream(statusArray).sum()) {
                flag = true;
            }
        }
        return flag;
    }

    /**
     * 更新上传状态
     * 该方法用于更新给定键的上传状态，通过设置对应分块的上传状态为完成
     *
     * @param key        上传状态的键，用于标识不同的上传任务
     * @param chunkIndex 分块编号，表示当前上传的分块
     *                   <p>
     *                   注意：此方法假定存在名为initLock的锁，以及getUploadStateValue和COMPLETE的定义，
     *                   这些定义和锁的细节在代码片段中未显示
     */
    public static void updateUploadState(String key, int chunkIndex) {
        initLock.writeLock().lock();
        try {
            UploadState value = getUploadStateValue(key);
            value.getStatus().get()[chunkIndex] = COMPLETE;
        } finally {
            initLock.writeLock().unlock();
        }
    }

    public static void updateUploadState(String key, String md5, int chunkIndex) {
        initLock.writeLock().lock();
        try {
            UploadState value = getUploadStateValue(key);
            value.getStatus().get()[chunkIndex] = COMPLETE;
            value.getMd5s().get()[chunkIndex] = md5;
        } finally {
            initLock.writeLock().unlock();
        }
    }

    /**
     * 验证给定的MD5值是否与给定字节数组的MD5值匹配
     *
     * @param md5   要验证的MD5值
     * @param bytes 用于计算MD5的字节数组
     * @return 如果md5与给定字节数组的MD5匹配，则为true，否则为false
     * @throws NoSuchAlgorithmException 如果MD5算法不可用
     */
    public static boolean checkMd5(String md5, byte[] bytes) {
        try {
            boolean result = false;
            String md5_ = computeMD5(bytes);
            log.debug("生成md5:[{}]；输入md5:[{}]" ,md5_,md5);
            if (md5_.equals(md5)) {
                result = true;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 计算给定文件的MD5值
     *
     * @param file 要计算MD5的文件
     * @return 文件的MD5值
     * @throws NoSuchAlgorithmException 如果MD5算法不可用
     * @throws IOException              如果读取文件时发生错误
     */
    public static String computeMD5(File file) throws NoSuchAlgorithmException, IOException {
        byte[] buffer = new byte[ImageConstant.COMPUTE_MD5_BYTES];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(buffer, 0, ImageConstant.COMPUTE_MD5_BYTES);
        }
        return computeMD5(buffer);
    }

    /**
     * 计算给定字节数组的MD5值
     *
     * @param bytes 要计算MD5的字节数组
     * @return 字节数组的MD5值
     * @throws NoSuchAlgorithmException 如果MD5算法不可用
     */
    public static String computeMD5(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        if (bytes.length > 0) {
            if (bytes.length > ImageConstant.COMPUTE_MD5_BYTES) {
                md.update(bytes, 0, ImageConstant.COMPUTE_MD5_BYTES);
            } else {
                md.update(bytes);
            }
            byte[] digest = md.digest();
            //return Base64.getEncoder().encodeToString(digest);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            /*StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();*/
        } else {
            throw new RuntimeException("computeMD5 bytes is empty");
        }
    }


    public static void validateChunks(String uploadDir) throws NoSuchAlgorithmException, IOException {
        File directory = new File(uploadDir);
        //File[] chunks = directory.listFiles((dir, name) -> name.startsWith("chunk_"));
        File[] chunks = directory.listFiles();
        if (chunks != null) {
            for (File chunk : chunks) {
                String md5 = computeMD5(chunk);
                System.out.println("Chunk " + chunk.getName() + " MD5: " + md5);
            }
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String uploadDir = "D:\\work\\dict\\jnet\\imageStore";
        validateChunks(uploadDir);
    }
}

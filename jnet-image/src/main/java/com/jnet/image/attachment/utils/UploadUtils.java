package com.jnet.image.attachment.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

import static com.jnet.image.attachment.utils.FileUtils.generateFileName;


/**
 * 分块上传工具类
 */
public class UploadUtils {

    private static final Integer COMPLETE = -1;

    private static final Map<String, Value> CHUNK_MAP = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(UploadUtils.class);

    /**
     * 内部类记录分块上传文件信息
     */
    private static class Value {
        String name;
        AtomicIntegerArray status;

        Value(int n) {
            this.name = generateFileName();
            this.status = new AtomicIntegerArray(n);
        }
    }



    /**
     * 判断文件所有分块是否已上传
     * @param key
     * @return
     */
    public static boolean isUploaded(String key) {
        if (isExist(key)) {
            AtomicIntegerArray status = CHUNK_MAP.get(key).status;
            log.info("文件：[{}];上传进度：[{}]",key,status);
            int length = status.length();
            for (int i=0;i<length;i++) {
                if (status.get(i)> COMPLETE) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 判断文件是否有分块已上传
     * @param key
     * @return
     */
    private static boolean isExist(String key) {
        return CHUNK_MAP.containsKey(key);
    }

    /**
     * 为文件添加上传分块记录
     * @param key
     * @param chunk
     */
    public static void addChunk(String key, int chunk) {
        CHUNK_MAP.get(key).status.set(chunk,COMPLETE);
    }

    /**
     * 从map中删除键为key的键值对
     * @param key
     */
    public static void removeKey(String key) {
        if (isExist(key)) {
            CHUNK_MAP.remove(key);
        }
    }

    /**
     * 获取随机生成的文件名
     * @param key
     * @param chunks
     * @return
     */
    public static String getFileName(String key, int chunks) {
        if (!isExist(key)) {
            synchronized (UploadUtils.class) {
                if (!isExist(key)) {
                    CHUNK_MAP.put(key, new Value(chunks));
                }
            }
        }
        return CHUNK_MAP.get(key).name;
    }
}

package com.jnet.image.attachment.utils;


import java.io.*;
import java.util.UUID;

/**
 * 文件操作工具类
 */
public class FileUtils {

    /**
     * 写入文件
     * @param target
     * @param src
     * @throws IOException
     */
    public static void write(String target, InputStream src) throws IOException {
        OutputStream os = new FileOutputStream(target);
        byte[] buf = new byte[1024];
        int len;
        while (-1 != (len = src.read(buf))) {
            os.write(buf,0,len);
        }
        os.flush();
        os.close();
    }

    /**
     * 分块写入文件
     * @param target
     * @param targetSize
     * @param src
     * @param srcSize
     * @param chunkTotal
     * @param chunkIndex
     * @throws IOException
     */
    public static void writeWithBlok(String target, Long targetSize, InputStream src, Long srcSize, Integer chunkTotal, Integer chunkIndex) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(target,"rw");
        //randomAccessFile.setLength(targetSize);
        /*if (chunk == chunks - 1) {
            randomAccessFile.seek(chunk * targetSize);
        } else {
            randomAccessFile.seek(chunk * targetSize);
        }*/
        randomAccessFile.seek(chunkIndex * targetSize);
        byte[] buf = new byte[1024];
        int len;
        while (-1 != (len = src.read(buf))) {
            randomAccessFile.write(buf,0,len);
        }
        randomAccessFile.close();
    }

    public static void writeWithBlok(String target, Long chunkSize, byte[] fileBytes, Integer chunkTotal, Integer chunkIndex) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(target,"rw");
        randomAccessFile.seek(chunkIndex * chunkSize);
        randomAccessFile.write(fileBytes);
        randomAccessFile.close();
    }

    public static boolean checkMd5AndWrite(String target, Long targetSize, InputStream src, Long srcSize, Integer chunks, Integer chunk) throws IOException {
        boolean flag = false;
        writeWithBlok(target, targetSize, src, srcSize, chunks, chunk);
        return flag;
    }



    /**
     * 生成随机文件名
     * @return
     */
    public static String generateFileName() {
        return UUID.randomUUID().toString();
    }

    private static final int CHUNK_SIZE = 30 * 1024 * 1024; // 10 MB

    public static void splitFile(String filePath, String outputDir) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead;
        int chunkIndex = 0;

        while ((bytesRead = fis.read(buffer)) != -1) {
            String chunkFilePath = outputDir + "/chunk_" + chunkIndex;
            try (FileOutputStream fos = new FileOutputStream(chunkFilePath)) {
                fos.write(buffer, 0, bytesRead);
            }
            chunkIndex++;
        }

        fis.close();
    }

    public static void main(String[] args) {
        try {
            String filePath = "D:\\work\\dict\\jnet\\imageStore\\JP2K-33003-1.svs";
            String outputDir = "D:\\work\\dict\\jnet\\imageStore\\";
            splitFile(filePath, outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

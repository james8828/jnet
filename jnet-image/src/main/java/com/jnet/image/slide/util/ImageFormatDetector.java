package com.jnet.image.slide.util;

import java.io.*;
import java.util.Arrays;

/**
 * 图像格式识别工具类
 */
public class ImageFormatDetector {

    // 定义各种图像格式的签名
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87_SIGNATURE = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] GIF89_SIGNATURE = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] BMP_SIGNATURE = {0x42, 0x4D};
    private static final byte[] TIFF_LE_SIGNATURE = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BE_SIGNATURE = {0x4D, 0x4D, 0x00, 0x2A};

    /**
     * 图像格式枚举
     */
    public enum ImageFormat {
        JPEG, PNG, GIF, BMP, TIFF, UNKNOWN
    }

    /**
     * 从文件路径识别图像格式
     *
     * @param filePath 文件路径
     * @return 图像格式
     */
    public static ImageFormat detectFormat(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return detectFormat(fis);
        }
    }

    /**
     * 从文件对象识别图像格式
     *
     * @param file 文件对象
     * @return 图像格式
     */
    public static ImageFormat detectFormat(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return detectFormat(fis);
        }
    }

    /**
     * 从输入流识别图像格式
     *
     * @param inputStream 输入流
     * @return 图像格式
     */
    public static ImageFormat detectFormat(InputStream inputStream) throws IOException {
        // 读取文件头部字节，最多读取16个字节
        byte[] header = new byte[16];
        int bytesRead = inputStream.read(header);

        if (bytesRead < 2) {
            return ImageFormat.UNKNOWN;
        }

        // 检查各种格式
        if (startsWith(header, JPEG_SIGNATURE)) {
            return ImageFormat.JPEG;
        } else if (startsWith(header, PNG_SIGNATURE)) {
            return ImageFormat.PNG;
        } else if (startsWith(header, GIF87_SIGNATURE) || startsWith(header, GIF89_SIGNATURE)) {
            return ImageFormat.GIF;
        } else if (startsWith(header, BMP_SIGNATURE)) {
            return ImageFormat.BMP;
        } else if (startsWith(header, TIFF_LE_SIGNATURE) || startsWith(header, TIFF_BE_SIGNATURE)) {
            return ImageFormat.TIFF;
        }

        return ImageFormat.UNKNOWN;
    }

    /**
     * 检查字节数组是否以指定签名开始
     *
     * @param data 数据数组
     * @param signature 签名数组
     * @return 是否匹配
     */
    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(data, signature.length), signature);
    }

    /**
     * 获取格式的描述信息
     *
     * @param format 图像格式
     * @return 描述信息
     */
    public static String getFormatDescription(ImageFormat format) {
        switch (format) {
            case JPEG:
                return "JPEG (Joint Photographic Experts Group)";
            case PNG:
                return "PNG (Portable Network Graphics)";
            case GIF:
                return "GIF (Graphics Interchange Format)";
            case BMP:
                return "BMP (Bitmap)";
            case TIFF:
                return "TIFF (Tagged Image File Format)";
            default:
                return "Unknown format";
        }
    }

    /**
     * 主方法 - 测试程序
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ImageFormatDetector <image_file>");
            return;
        }

        String filePath = args[0];

        try {
            ImageFormat format = detectFormat(filePath);
            System.out.println("File: " + filePath);
            System.out.println("Detected format: " + format);
            System.out.println("Description: " + getFormatDescription(format));
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}


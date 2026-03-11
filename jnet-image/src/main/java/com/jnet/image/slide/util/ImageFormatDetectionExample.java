package com.jnet.image.slide.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// 使用示例
public class ImageFormatDetectionExample {
    public static void main(String[] args) {
        try {
            // 方法1: 通过文件路径检测
            ImageFormatDetector.ImageFormat format1 = ImageFormatDetector.detectFormat("E:\\123596483163.tif");
            System.out.println("Format: " + format1);
           /* // 方法2: 通过File对象检测
            File imageFile = new File("example.png");
            ImageFormatDetector.ImageFormat format2 = ImageFormatDetector.detectFormat(imageFile);
            System.out.println("Format: " + format2);

            // 方法3: 通过InputStream检测
            try (FileInputStream fis = new FileInputStream("example.gif")) {
                ImageFormatDetector.ImageFormat format3 = ImageFormatDetector.detectFormat(fis);
                System.out.println("Format: " + format3);
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

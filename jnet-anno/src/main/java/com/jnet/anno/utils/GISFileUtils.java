package com.jnet.anno.utils;

import com.jnet.anno.common.exception.FileException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author: Magic1412
 * @description: 文件处理
 * @date: 2021/1/11 13:32
 */
public class GISFileUtils {


    /**
     * @description: 存储文件
     * @param file 文件
     * @param fileStorageLocation 存储目录
     * @return
     */
    public static String storeFile(MultipartFile file ,Path fileStorageLocation) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (fileName.contains("..")) {
                throw new FileException("Sorry! Filename contains invalid path sequence " + fileName);
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHMMss_SSSSSS_");
            String targetFileName = formatter.format(new Date()) + fileName;
            Path targetLocation = fileStorageLocation.resolve(targetFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetFileName;
        } catch (IOException ex) {
            throw new FileException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }


    /**
     * @description: 加载文件
     * @param fileName 文件名
     * @param fileStorageLocation 存储目录
     * @return
     */
    public static Resource loadFile(String fileName ,Path fileStorageLocation) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileException("File not found. filename: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileException("File not found. filename: " + fileName, ex);
        }
    }


    /**
     * @description: 读取json文件内容
     * @param path 路径
     * @return
     */
    public static String readFileContent(String path) throws IOException {
        String jsonStr = "";
        try {
            File jsonFile = new File(path);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"GB2312");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

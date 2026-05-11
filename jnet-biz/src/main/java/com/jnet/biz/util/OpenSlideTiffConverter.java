package com.jnet.biz.util;

import com.jnet.biz.config.OpenSlideConverterProperties;
import com.jnet.biz.config.StoragePathConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OpenSlide 兼容的 TIFF 图像格式转换工具类
 *
 * <p>通过调用 Python 脚本将 JPG/PNG 转换为包含金字塔结构的 BigTIFF，
 * 确保生成的文件可被 OpenSlide 库直接识别和读取。
 *
 * @author JNet Team
 * @since 2024-05-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenSlideTiffConverter {

    private final OpenSlideConverterProperties converterProperties;
    private final StoragePathConfig storagePathConfig;

    /**
     * 智能处理：根据文件类型决定是否需要转换
     *
     * @param imageId     图像 ID
     * @param inputPath   输入文件路径
     * @param projectCode 项目编码
     * @param batchCode   批次编码
     * @return 可用于 OpenSlide 读取的文件路径
     * @throws IOException 处理失败时抛出异常
     */
    public File ensureOpenSlideCompatible(Long imageId, String inputPath, String projectCode, String batchCode) throws IOException {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("文件不存在: " + inputPath);
        }

        String filename = inputFile.getName();

        // 【关键分支1】WSI 格式：直接返回，无需转换
        if (StoragePathConfig.isWsiFormat(filename)) {
            log.debug("WSI 格式文件，无需转换: {}", inputPath);
            return inputFile;
        }

        // 【关键分支2】普通图片：需要转换
        if (StoragePathConfig.needsConversion(filename)) {
            log.info("检测到普通图片格式 [{}]，开始转换为 OpenSlide 兼容 TIFF...", filename);

            // 生成转换文件路径（批次目录下的tiff子目录）
            String convertedPath = storagePathConfig.getConvertedTiffPath(filename, projectCode, batchCode);
            File convertedFile = new File(convertedPath);

            // 幂等性检查：如果已存在转换文件，直接返回
            if (convertedFile.exists() && convertedFile.length() > 0) {
                log.info("转换文件已存在，跳过转换: {}", convertedPath);
                return convertedFile;
            }

            // 执行转换
            return convertToOpenSlideTiff(inputPath, convertedPath);
        }

        throw new IllegalArgumentException("不支持的文件格式: " + filename);
    }

    /**
     * 将 JPG/PNG 文件转换为 OpenSlide 兼容的 TIFF 格式
     *
     * @param inputPath 输入文件路径（JPG/PNG）
     * @return 转换后的 TIFF 文件
     * @throws IOException 转换失败时抛出异常
     */
    public File convertToOpenSlideTiff(String inputPath) throws IOException {
        String outputPath = inputPath.replaceAll("\\.(?i)(jpg|jpeg|png)$", ".tif");
        return convertToOpenSlideTiff(new File(inputPath), new File(outputPath));
    }

    /**
     * 将 JPG/PNG 文件转换为 OpenSlide 兼容的 TIFF 格式
     *
     * @param inputPath  输入文件路径（JPG/PNG）
     * @param outputPath 输出文件路径（TIFF）
     * @return 转换后的 TIFF 文件
     * @throws IOException 转换失败时抛出异常
     */
    public File convertToOpenSlideTiff(String inputPath, String outputPath) throws IOException {
        return convertToOpenSlideTiff(new File(inputPath), new File(outputPath));
    }

    /**
     * 将 JPG/PNG 文件转换为 OpenSlide 兼容的 TIFF 格式
     *
     * @param inputFile  输入文件（JPG/PNG）
     * @param outputFile 输出文件（TIFF）
     * @return 转换后的 TIFF 文件
     * @throws IOException 转换失败时抛出异常
     */
    public File convertToOpenSlideTiff(File inputFile, File outputFile) throws IOException {
        // 验证输入文件
        validateInputFile(inputFile);

        // 验证输出路径
        validateOutputFile(outputFile);

        log.info("开始调用 Python 脚本转换图像为 OpenSlide 兼容格式: {} -> {}",
                inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

        try {
            // 获取配置的路径
            String pythonPath = converterProperties.getPythonPath();
            String scriptPath = System.getProperty("user.dir") + converterProperties.getScriptPath();

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath,
                    scriptPath,
                    inputFile.getAbsolutePath(),
                    outputFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true); // 合并错误流

            // 执行进程
            Process process = pb.start();

            // 读取输出日志
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Python Output: {}", line);
                }
            }

            // 等待完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Python 转换脚本执行失败, Exit Code: " + exitCode);
            }

            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("转换后的文件不存在或为空");
            }

            log.info("OpenSlide 兼容图像转换成功: {} ({} KB)",
                    outputFile.getName(),
                    outputFile.length() / 1024);

            return outputFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("转换过程被中断", e);
        } catch (IOException e) {
            log.error("OpenSlide 兼容图像转换失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量转换目录下的所有 JPG/PNG 文件为 OpenSlide 兼容的 TIFF
     *
     * @param inputDir  输入目录路径
     * @param outputDir 输出目录路径
     * @return 转换成功的文件数量
     * @throws IOException 转换失败时抛出异常
     */
    public int batchConvertToOpenSlideTiff(String inputDir, String outputDir) throws IOException {
        Path inputPath = Paths.get(inputDir);
        Path outputPath = Paths.get(outputDir);

        // 验证输入目录
        if (!Files.exists(inputPath) || !Files.isDirectory(inputPath)) {
            throw new IllegalArgumentException("输入目录不存在: " + inputDir);
        }

        // 创建输出目录
        Files.createDirectories(outputPath);

        int successCount = 0;
        int failCount = 0;

        // 遍历输入目录
        try (var stream = Files.list(inputPath)) {
            var files = stream.filter(Files::isRegularFile)
                    .filter(p -> isSupportedFormat(p.toString()))
                    .toList();

            log.info("找到 {} 个待转换的图像文件", files.size());

            for (Path inputFile : files) {
                try {
                    // 生成输出文件名（保持原名，扩展名改为 .tif）
                    String originalName = inputFile.getFileName().toString();
                    String outputName = originalName.replaceAll("\\.(?i)(jpg|jpeg|png)$", ".tif");
                    Path outputFile = outputPath.resolve(outputName);

                    // 执行转换
                    convertToOpenSlideTiff(inputFile.toFile(), outputFile.toFile());
                    successCount++;

                } catch (Exception e) {
                    log.error("转换失败: {}", inputFile.getFileName(), e);
                    failCount++;
                }
            }
        }

        log.info("批量转换完成: 成功={}, 失败={}", successCount, failCount);

        if (failCount > 0) {
            throw new IOException(String.format("批量转换部分失败: 成功=%d, 失败=%d", successCount, failCount));
        }

        return successCount;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 验证输入文件
     */
    private void validateInputFile(File inputFile) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + (inputFile != null ? inputFile.getPath() : "null"));
        }

        if (!inputFile.isFile()) {
            throw new IllegalArgumentException("输入路径不是文件: " + inputFile.getPath());
        }

        if (!isSupportedFormat(inputFile.getName())) {
            String formats = String.join(", ", converterProperties.getSupportedFormats());
            throw new IllegalArgumentException("不支持的输入格式: " + inputFile.getName()
                    + "，支持的格式: " + formats);
        }
    }

    /**
     * 验证输出文件
     */
    private static void validateOutputFile(File outputFile) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("输出文件不能为 null");
        }

        // 创建父目录
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            boolean created = outputFile.getParentFile().mkdirs();
            if (!created) {
                throw new IOException("无法创建输出目录: " + outputFile.getParent());
            }
        }
    }


    /**
     * 检查文件格式是否支持
     */
    private boolean isSupportedFormat(String fileName) {
        if (fileName == null) {
            return false;
        }

        String lowerName = fileName.toLowerCase();
        for (String format : converterProperties.getSupportedFormats()) {
            if (lowerName.endsWith("." + format)) {
                return true;
            }
        }
        return false;
    }


}
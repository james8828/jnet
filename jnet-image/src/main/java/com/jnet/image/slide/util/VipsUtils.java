package com.jnet.image.slide.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 普通图片转tiff格式，供openslide使用
 */
@Slf4j
public class VipsUtils {
    /**
     * jpeg图片转tiff
     * <p>
     * 静态方法是属于类的而不属于对象的。同样的，synchronized修饰的静态方法锁定的是这个类的所有对象。
     *
     * @param source
     * @param target
     * @throws IOException
     * @throws InterruptedException
     */
    public static synchronized boolean convertToPyramidalTIFF(String source, String target) throws IOException, InterruptedException {
        String vipsExecutable = "";
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            vipsExecutable = "D:\\d\\vips\\vips\\vips-dev-w64-web-8.13.0\\vips-dev-8.13\\bin\\vips.exe ";
        } else {
            vipsExecutable = "vips ";
        }

        // jpeg -Q 95
        String compression = "lzw";
        String tileSize = "256";

        String command = vipsExecutable;
        if (source.contains(" ")) {
            command = command + " tiffsave \"" + source + "\"  \"" + target + "\" --bigtiff " +
                    "--tile "
                    + "--tile-width " + tileSize + " --tile-height " + tileSize + " --pyramid --compression " + compression;
        } else {
            command = command + " tiffsave " + source + " " + target + " --bigtiff " +
                    "--tile "
                    + "--tile-width " + tileSize + " --tile-height " + tileSize + " --pyramid --compression " + compression;
        }

        log.info("OpenSlide未能识别图片转tiff command: {}", command);
        Process exec = Runtime.getRuntime().exec(command);
        log.info("exec command执行完成");
        if (exec.waitFor() == 0) {
            return true;
        }
        return false;
    }
}

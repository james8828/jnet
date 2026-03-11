package com.jnet.image.slide.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.google.common.cache.Cache;
import com.jnet.image.slide.domain.Slide;
import com.jnet.image.slide.service.OpenSlideService;
import com.jnet.image.slide.service.SlideService;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openslide.AssociatedImage;
import org.openslide.OpenSlide;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;


/**
 * @author mugw
 * @version 1.0
 * @description 图像解析服务
 * @date 2024/2/3 17:27:10
 */
@Slf4j
@Service
public class OpenSlideServiceImpl implements OpenSlideService {

    private static Snowflake snowflake = IdUtil.getSnowflake();

    private final String imgType = "jpg";

    private final Integer imgSize = 256;
    @Resource
    private SlideService slideService;
    @Resource
    private Cache<String, OpenSlide> cache;



    /**
     * 总层数小于2为不可用
     */
    private final int MIN_LEVEL_COUNT = 2;

    /**
     * 获取缩略图
     * @param slideId
     * @param response
     * @throws Exception
     */
    @Override
    public void getThumbnailImage(Long slideId, HttpServletResponse response) throws Exception {
        Slide slide = slideService.getById(slideId);
        File slideFile = new File(slide.getSlidePath());
        OpenSlide os = new OpenSlide(slideFile);
        BufferedImage thumbnailImage = os.createThumbnailImage(imgSize);
        // 返回图片
        response.setContentType("image/jpeg");
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(thumbnailImage, "jpg", out);
        out.flush();
        out.close();
    }

    /**
     * 获取关联图像 label、macro
     * label 图像标签图，通常为图像的标注信息（如医院 Logo、文字描述等）
     * macro 宏图（宏观图像），展示整个组织切片的概览图
     * @param slideId
     * @param key
     * @param response
     * @throws Exception
     */
    @Override
    public void getAssociatedImage(Long slideId, String key, HttpServletResponse response) throws Exception {
        Slide slide = slideService.getById(slideId);
        File slideFile = new File(slide.getSlidePath());
        OpenSlide os = new OpenSlide(slideFile);
        Map<String, AssociatedImage> associatedImages = os.getAssociatedImages();
        if (associatedImages.containsKey(key)){
            AssociatedImage associatedImage = associatedImages.get(key);
            BufferedImage bufferedImage = associatedImage.toBufferedImage();
            // 返回图片
            response.setContentType("image/jpeg");
            ServletOutputStream out = response.getOutputStream();
            ImageIO.write(bufferedImage, "jpg", out);
            out.flush();
            out.close();
        }
    }

    /**
     * 解析tile
     * @param titleGroup
     * @param location
     * @param response
     * @throws Exception
     */
    @Override
    public void getSlideTile(Long slideId, String titleGroup, String location, HttpServletResponse response) throws Exception {
        if (StringUtils.isNotEmpty(location)) {
            Slide slide = slideService.getById(slideId);
            String path = slide.getSlidePath();
            File slideFile = new File(slide.getSlidePath());
            OpenSlide os = new OpenSlide(slideFile);
            cache.put(path, os);
            String[] l = StringUtils.split(location, "-");
            //x或y坐标切片数量
            int tile_count = (int) Math.pow(2, (Integer.parseInt(l[0]) - 1));
            int x = Integer.parseInt(l[1]);
            int y = Integer.parseInt(l[2]);
            log.info("x:[{}],y:[{}],downSample:[{}]", x, y, tile_count);
            log.debug("Level0Height:[{}],Level0Width:[{}],LevelCount:[{}],Properties:[{}],LevelDownsample:[{}]",
                    os.getLevel0Height(), os.getLevel0Width(), os.getLevelCount(), os.getProperties(), os.getLevelDownsample(0));
            ServletOutputStream outputStream = null;
            File temp = File.createTempFile(imgSize + "-" + x + "-" + y + "-" + tile_count, ".jpg");
            try {
                response.setCharacterEncoding("utf-8");
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                outputStream = response.getOutputStream();
                long height = os.getLevel0Height();
                long width = os.getLevel0Width();
                double ds;
                Long max_dimension = Math.max(height, width);
                ds = max_dimension / (imgSize * tile_count * 1.0);
                if (ds < 1.0) {
                    ds = 1.0;
                }
                BufferedImage result = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = result.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, imgSize, imgSize);
                os.paintRegion(g, 0, 0, x * imgSize, y * imgSize, imgSize, imgSize, ds);
                g.dispose();
                ImageIO.write(result, "jpg", temp);
                outputStream.write(FileUtils.readFileToByteArray(temp));
            } catch (Exception e) {
                log.error("加载：[{}]异常", location);
                e.printStackTrace();
                IOUtils.close(outputStream);
            } finally {
                temp.deleteOnExit();
                //os.dispose();
            }
        }
    }
}

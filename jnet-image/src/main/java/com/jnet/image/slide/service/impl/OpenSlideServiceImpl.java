package com.jnet.image.slide.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.cache.Cache;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.constant.ImageConstant;
import com.jnet.image.event.UploadCompleteEvent;
import com.jnet.image.slide.domain.Image;
import com.jnet.image.slide.mapper.ImageMapper;
import com.jnet.image.slide.service.OpenSlideService;
import com.jnet.image.slide.util.VipsUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openslide.AssociatedImage;
import org.openslide.OpenSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    private Cache<String, OpenSlide> cache;
    @Value("${file.path}")
    private String localFilePath;

    @Resource
    private ImageMapper imageMapper;

    /**
     * 总层数小于2为不可用
     */
    private final int MIN_LEVEL_COUNT = 2;

    /**
     * 解析tile
     * @param imageType
     * @param imageId
     * @param gamma
     * @param firmUpNum
     * @param titleGroup
     * @param location
     * @param response
     * @throws Exception
     */
    @Override
    public void processImage2Tile(Integer imageType, Long imageId, Integer gamma, Integer firmUpNum, String titleGroup, String location, HttpServletResponse response) throws Exception {
        if (StringUtils.isNotEmpty(location)) {
            /*Image image = imageMapper.selectById(imageId);
            String path = image.getImageUrl();*/
            String path = "D:\\work\\dict\\jnet\\imageStore\\CMU-1.svs";
            //level-x-y.jpg
            String[] l = StringUtils.split(location, "-");
            int downSample = (int) Math.pow(2, (Integer.parseInt(l[0]) - 1));
            int x = Integer.parseInt(l[1]);
            int y = Integer.parseInt(l[2]);
            File slide = new File(path);
            OpenSlide os = cache.getIfPresent(path);
            if (os == null) {
                os = new OpenSlide(slide);
                cache.put(path, os);
            }
            log.info("x:[{}],y:[{}],downSample:[{}]", x, y, downSample);
            log.debug("Level0Height:[{}],Level0Width:[{}],LevelCount:[{}],Properties:[{}],LevelDownsample:[{}]",
                    os.getLevel0Height(), os.getLevel0Width(), os.getLevelCount(), os.getProperties(), os.getLevelDownsample(0));
            ServletOutputStream outputStream = null;
            File temp = File.createTempFile(imgSize + "-" + x + "-" + y + "-" + downSample, ".jpg");
            try {
                response.setCharacterEncoding("utf-8");
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                outputStream = response.getOutputStream();
                long height = os.getLevel0Height();
                long width = os.getLevel0Width();

                double ds;

                if (width > height) {
                    ds = width / (imgSize * downSample * 1.0);
                } else {
                    ds = height / (imgSize * downSample * 1.0);
                }

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

    @Async
    @Order(1)
    @EventListener
    @Override
    public void processImage(UploadCompleteEvent event) throws Exception {
        Attachment attachment = event.getAttachment();
        String filePath = attachment.getAttachmentPath();
        File file = new File(filePath);
        String thumbPath = createOpenSlidePath(file.getParent(), ImageConstant.THUMB_PATH);
        String labelPath = createOpenSlidePath(file.getParent(), ImageConstant.LABEL_PATH);
        String marcoPath = createOpenSlidePath(file.getParent(), ImageConstant.MARCO_PATH);
        QueryWrapper<Image> queryWrapper = Wrappers.query();
        queryWrapper.eq("md5", attachment.getAttachmentMd5());
        Image image = imageMapper.selectOne(queryWrapper);
        if (image == null) {
            image = Image.builder()
                    .md5(attachment.getAttachmentMd5())
                    .imagePath(filePath)
                    .imageUrl(filePath)
                    .thumbUrl(thumbPath)
                    .labelUrl(labelPath)
                    .macroUrl(marcoPath)
                    .format(attachment.getAttachmentExt())
                    .processFlag(ImageConstant.IMAGE_PROCESS_PARSING)
                    .status(ImageConstant.IMAGE_STATUS_UNABLE).build();
        }
        //if (checkFileMd5(attachment.getAttachmentMd5(), file)) {
        if (true) {
            OpenSlide os = isOpenSlide(file);
            if (os == null) {
                log.info("********************Vips原始切片转换，切片地址：[{}]********************", filePath);
                String destPath = filePath.substring(0, filePath.indexOf(".")) + ".tif";
                // 把不能识别的图片转换成可以识别的tif
                boolean flag = VipsUtils.convertToPyramidalTIFF(filePath, destPath);
                log.info("********************Vips原始切片转换完成，原地址：[{}]，转换后地址：[{}]********************", filePath, destPath);
                image.setImageUrl(destPath);
                if (flag) {
                    os = new OpenSlide(new File(destPath));
                }
            }
            processThumb(os, image);
            imageMapper.insert(image);
        } else {
            log.info("文件[{}] checkFileMd5 未通过。", attachment.getAttachmentMd5());
        }

    }

    /**
     * 是否openslide可解析文件
     * @param file
     * @return
     */
    private OpenSlide isOpenSlide(File file) {
        OpenSlide os = null;
        try {
            if (file.exists() && file.isFile()) {
                log.info("********************开始验证原始切片是否可以被OpenSlide解析，切片地址：[{}]*****************", file.getPath());
                os = new OpenSlide(file);
                log.info("********************OpenSlide解析原始切片验证通过，切片地址：[{}]********************", file.getPath());
            }
        } catch (IOException e) {
            if (os != null) {
                os.dispose();
            }
            log.warn("********************OpenSlide解析原始切片验证失败：[{}]，切片地址：[{}]********************", e.getMessage(), file.getPath());
        }
        return os;
    }

    /**
     * 创建缩略图文件夹
     *
     * @param filePath
     * @param type
     * @return
     */
    private String createOpenSlidePath(String filePath, String type) {
        //String path = filePath + File.separator + type;
        String path = filePath + type;
        //创建文件夹
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    private boolean checkFileMd5(String md5, File fileSrc) {
        boolean flag = false;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileSrc);
            String tempMd5 = DigestUtils.md5DigestAsHex(inputStream);
            log.info("检查文件md5是否一致，传入md5：[{}]，文件生成md5：[{}]", md5, tempMd5);
            if (md5.equals(tempMd5)) {
                flag = true;
            }
        } catch (IOException e) {
            log.error("文件md5效验异常：[{}]", e.getMessage());
        } finally {
            try {
                IOUtils.close(inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return flag;
    }

    private Image processThumb(OpenSlide os, Image image) {
        try {
            String thumbPath = image.getThumbUrl();
            String labelPath = image.getLabelUrl();
            String marcoPath = image.getMacroUrl();

            // 生成缩略图
            createThumbnailImage(os, thumbPath, imgSize);
            // 解析图像属性
            Map<String, AssociatedImage> map = os.getAssociatedImages();

            if (map.containsKey("label")) {
                createImage(map, labelPath, "label");
            }
            if (map.containsKey("macro")) {
                createImage(map, marcoPath, "macro");
            }
            // 把缩略图、整个图片的长和宽存入image
            image = updateThumbWidthHeightTileCountImage(os, image);
            if (image.getFormat().equals(ImageConstant.SVS) || image.getFormat().equals(ImageConstant.NDPI)) {
                writeResolution(os, image);
            }

            // 总层数小于2为不可用 不可用原因共三种，2解析失败（不能获得缩略图）
            if (image.getLevelCount() < MIN_LEVEL_COUNT) {
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
                image.setStatus(ImageConstant.IMAGE_STATUS_UNABLE);
            } else {
                // 可用
                image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_SUCCESS);
                image.setStatus(ImageConstant.IMAGE_STATUS_ENABLE);
            }
        } catch (Exception e) {
            log.error("slide文件解析异常：[{}]", e.getMessage());
            image.setProcessFlag(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
        } finally {
            if (os != null) {
                os.dispose();
                log.info("原始切片openslide对象已关闭，原始切片信息：[{}]", image);
            }
        }
        return image;
    }

    /**
     * 根据物理地址,获取到病理图片的resolutionX,resolutionY,sourceLens并存入image
     *
     * @param os
     * @param image
     * @return
     * @throws IOException
     */
    private void writeResolution(OpenSlide os, Image image) {
        String mppX = "";
        String mppY = "";
        Integer sourceLens = 0;

        Map<String, String> properties = os.getProperties();
        // log.info("writeRseolution --> image id: {} path: {} properties: {}", image.getImageId(), image.getImagePath(), properties);

        // 获取原始图像参数
        if (properties.containsKey("openslide.mpp-x")) {
            mppX = properties.get("openslide.mpp-x");
        }

        if (properties.containsKey("openslide.mpp-y")) {
            mppY = properties.get("openslide.mpp-y");
        }

        if (properties.containsKey("openslide.objective-power")) {
            // SVS
            sourceLens = Integer.valueOf(properties.get("openslide.objective-power"));
        } else if (properties.containsKey("hamamatsu.SourceLens")) {
            // NDPI
            sourceLens = Integer.valueOf(properties.get("hamamatsu.SourceLens"));
        }

        // 特殊处理生仝算法组导出的图像
        if (mppX == "" && mppY == "" && sourceLens == 0 && properties.containsKey("tiff.ImageDescription")) {
            String imageDescription = properties.get("tiff.ImageDescription");
            if (imageDescription != null) {
                JSONObject jsonObject = JSON.parseObject(imageDescription);
                if (jsonObject.containsKey("openslide.mpp-x")) {
                    mppX = jsonObject.get("openslide.mpp-x").toString();
                }
                if (jsonObject.containsKey("openslide.mpp-y")) {
                    mppY = jsonObject.get("openslide.mpp-y").toString();
                }
                if (jsonObject.containsKey("openslide.objective-power")) {
                    sourceLens = Integer.valueOf(jsonObject.get("openslide.objective-power").toString());
                }
            }
        }

        image.setResolutionX(mppX);
        image.setResolutionY(mppY);
        image.setSourceLens(sourceLens);

    }

    /**
     * 把缩略图、整个图片的长和宽存入image
     *
     * @param os
     * @param image
     * @return
     */
    private Image updateThumbWidthHeightTileCountImage(OpenSlide os, Image image) {
        int levelCount = os.getLevelCount();
        // 更新levelCount
        image.setLevelCount(levelCount);
        String tileCountList = "";
        // 遍历存原生的每层切片个数
        for (int k = 0; k < levelCount; k++) {
            long l = os.getLevel0Width() / os.getLevelWidth(k) * os.getLevel0Height() / os.getLevelHeight(k);
            tileCountList += l;
            if (k != levelCount - 1) {
                tileCountList += ',';
            }
        }
        image.setTileCountList(tileCountList);
        image.setWidth(String.valueOf(os.getLevel0Width()));
        image.setHeight(String.valueOf(os.getLevel0Height()));

        double multiple = os.getLevel0Width() > os.getLevel0Height() ? 1024.0 / os.getLevel0Width() : 1024.0 / os.getLevel0Height();
        image.setMultiple(String.valueOf(multiple));
        return image;
    }

    /**
     * 生成本地缩略图
     *
     * @param os
     * @param path
     * @param size
     * @throws IOException
     */
    private void createThumbnailImage(OpenSlide os, String path, Integer size) throws IOException {
        log.debug("开始生成缩略图：size: {}, path: {}", size, path);
        // 开始生成缩略图
        BufferedImage th = os.createThumbnailImage(size);
        String resultName = path + File.separator + "0." + imgType;
        log.info("已生成缩略图文件 createThumbnailImage path : [{}]", resultName);
        // 缩略图实际保存到本地
        ImageIO.write(th, imgType, new File(resultName));
    }

    /**
     * 生成 label、macro
     *
     * @param map
     * @param path
     * @param type
     * @throws IOException
     */
    private void createImage(Map<String, AssociatedImage> map, String path, String type) throws IOException {
        log.debug("开始生{}图: path: {}", type, path);
        String resultName = path + File.separator + "0." + imgType;
        AssociatedImage associatedImage = map.get(type);
        BufferedImage ar = associatedImage.toBufferedImage();
        BufferedImage result = new BufferedImage(ar.getWidth(), ar.getHeight(), 1);
        Graphics2D g = result.createGraphics();
        g.drawImage(ar, 0, 0, null);
        ImageIO.write(result, imgType, new File(resultName));
        log.info("已生成{}图, path : [{}]",type, resultName);
    }
}

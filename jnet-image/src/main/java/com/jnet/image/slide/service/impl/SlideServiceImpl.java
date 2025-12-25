package com.jnet.image.slide.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.core.utils.SecurityUtils;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.constant.ImageConstant;
import com.jnet.image.event.UploadCompleteEvent;
import com.jnet.image.slide.domain.Slide;
import com.jnet.image.slide.service.SlideService;
import com.jnet.image.slide.mapper.SlideMapper;
import lombok.extern.slf4j.Slf4j;
import org.openslide.OpenSlide;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SlideServiceImpl extends ServiceImpl<SlideMapper, Slide> implements SlideService {

    private final int MIN_LEVEL_COUNT = 2;
    private final String PYRAMIDAL_TIFF_SUFFIX = ".tif";


    /**
     * 创建定时任务，处理解析失败图像
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void processSlideParseFail() {
        List<Slide> slides = baseMapper.selectList(Wrappers.<Slide>lambdaQuery()
                .eq(Slide::getStatus, ImageConstant.IMAGE_PROCESS_PARSE_FAIL)
                .eq(Slide::getDelFlag, false));
        for (Slide slide : slides) {
            log.info("处理解析失败的切片信息：[{}]", slide);
            extractSlideMetadata(slide);
            if (slide.getStatus() == ImageConstant.IMAGE_PROCESS_PARSE_FAIL) {
                String filePath = slide.getSlidePath();
                String outputPath = filePath + PYRAMIDAL_TIFF_SUFFIX;
                boolean flag = convertToPyramidalTIFF(filePath, outputPath);
                if (flag) {
                    slide.setStatus(ImageConstant.IMAGE_PROCESS_PARSING);
                    //修改切片状态为解析中
                    baseMapper.updateById(slide);
                    slide.setSlidePath(outputPath);
                    extractSlideMetadata(slide);
                }
            }
            baseMapper.updateById(slide);
        }
    }

    /**
     * 处理上传失败的切片
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void processSlideUploadFail() {
        log.info("开始处理上传失败的图片");
        List<Slide> slides = baseMapper.selectList(Wrappers.<Slide>lambdaQuery()
                .eq(Slide::getStatus, ImageConstant.IMAGE_PROCESS_UPLOADING)
                .lt(Slide::getUpdateTime, DateUtil.offsetHour(DateUtil.date(), -2))
                .eq(Slide::getDelFlag, false));
        log.info("待处理的图片数量：[{}]", slides.size());
        //上传两小时内未处理的图片
        for (Slide slide : slides) {
            slide.setStatus(ImageConstant.IMAGE_PROCESS_UPLOAD_FAIL);
        }
        updateBatchById(slides);
    }

    private boolean convertToPyramidalTIFF(String inputPath, String outputPath) {
        try {
            // 使用 VIPS 命令或 JNI 调用生成金字塔结构的 TIFF
            ProcessBuilder pb = new ProcessBuilder("vips", "tiffsave", inputPath, outputPath,
                    "--compression=jpeg", "--tile", "--pyramid", "--tile-width=256", "--tile-height=256");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理上传完成事件
     *
     * @param event
     */
    @Async
    @EventListener
    public void handleSlideUploadComplete(UploadCompleteEvent event) {
        Attachment attachment = event.getAttachment();
        if (attachment != null && attachment.getBizId() != null) {
            Long slideId = attachment.getBizId();
            Slide slide = baseMapper.selectById(slideId);
            if (slide == null){
                slide = Slide.builder().slideId(attachment.getBizId()).build();
            }
            slide.setFormat(attachment.getAttachmentExt());
            slide.setSlideName(attachment.getAttachmentName());
            slide.setSlidePath(attachment.getAttachmentPath());
            slide.setSize(attachment.getAttachmentSize());
            slide.setUpdateTime(new Date());
            slide.setUpdateBy(SecurityUtils.getUserId());
            slide.setStatus(ImageConstant.IMAGE_PROCESS_PARSING);
            //修改切片状态为解析中
            baseMapper.updateById(slide);
            //提取切片元数据
            extractSlideMetadata(slide);
            baseMapper.updateById(slide);
        }
    }

    /**
     * 提取切片元数据
     *
     * @param slide
     */
    private void extractSlideMetadata(Slide slide) {
        File slideFile = new File(slide.getSlidePath());
        OpenSlide os = null;
        try {
            os = new OpenSlide(slideFile);
            int levelCount = os.getLevelCount();
            slide.setLevelCount(levelCount);
            String tileCountList = "";
            // 遍历存原生的每层切片个数
            for (int k = 0; k < levelCount; k++) {
                long l = os.getLevel0Width() / os.getLevelWidth(k) * os.getLevel0Height() / os.getLevelHeight(k);
                tileCountList += l;
                if (k != levelCount - 1) {
                    tileCountList += ',';
                }
            }
            slide.setTileCountList(tileCountList);
            slide.setWidth(String.valueOf(os.getLevel0Width()));
            slide.setHeight(String.valueOf(os.getLevel0Height()));

            double multiple = os.getLevel0Width() > os.getLevel0Height() ? 1024.0 / os.getLevel0Width() : 1024.0 / os.getLevel0Height();
            slide.setMultiple(String.valueOf(multiple));

            String mppX = "";
            String mppY = "";
            Integer sourceLens = 0;

            Map<String, String> properties = os.getProperties();

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

            slide.setResolutionX(mppX);
            slide.setResolutionY(mppY);
            slide.setSourceLens(sourceLens);

            // 总层数小于2为不可用 不可用原因共三种，2解析失败（不能获得缩略图）
            if (slide.getLevelCount() < MIN_LEVEL_COUNT) {
                slide.setStatus(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
            } else {
                slide.setStatus(ImageConstant.IMAGE_PROCESS_PARSE_SUCCESS);
            }

        } catch (Exception e) {
            log.error("切片信息：[{}]，OpenSlide 元数据解析失败：{}", slide, e.getMessage());
            slide.setStatus(ImageConstant.IMAGE_PROCESS_PARSE_FAIL);
        } finally {
            if (os != null) {
                os.dispose();
                log.info("切片信息：[{}]，切片openslide对象已关闭", slide);
            }
        }
    }
}





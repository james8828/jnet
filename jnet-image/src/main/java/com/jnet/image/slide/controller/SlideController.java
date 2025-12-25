package com.jnet.image.slide.controller;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.common.core.utils.SecurityUtils;
import com.jnet.image.slide.domain.Slide;
import com.jnet.image.slide.service.OpenSlideService;
import com.jnet.image.slide.service.SlideService;
import com.jnet.image.vo.SlidePageQueryVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/29 10:51:31
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/slide")
public class SlideController {

    @Resource
    private OpenSlideService openSlideService;
    @Resource
    private SlideService slideService;

    private static Snowflake snowflake = IdUtil.getSnowflake();

    /**
     * 添加切片
     *
     * @return
     */
    @PostMapping(value = "/add")
    public R<String> add() {
        Long slideId = snowflake.nextId();
        slideService.save(Slide.builder().slideId(slideId)
                .createBy(SecurityUtils.getUserId())
                .updateBy(SecurityUtils.getUserId()).build());
        return R.success(String.valueOf(slideId));
    }

    @DeleteMapping(value = "/delete/{slideId}")
    public R<Boolean> delete(@PathVariable("slideId") Long slideId) {
        return R.success(slideService.removeById(slideId));
    }

    /**
     * 获取切片列表
     *
     * @param req
     * @return
     */
    @PostMapping(value = "/page")
    public R<Page<Slide>> page(@RequestBody SlidePageQueryVo req) {
        Page<Slide> page = Page.of(req.getCurrent(), req.getSize());
        slideService.page(page, Wrappers.<Slide>lambdaQuery()
                .eq(Slide::getDelFlag, false)
                .like(req.getSlideName() != null, Slide::getSlideName, req.getSlideName())
                .eq(req.getStatus() != null, Slide::getStatus, req.getStatus()));
        return R.success(page);
    }

    /**
     * 查询切片详情
     *
     * @param slideId
     * @return
     */
    @GetMapping("/getSlide/{slideId}")
    public R<Slide> getSlideById(@PathVariable("slideId") Long slideId) {
        return R.success(slideService.getById(slideId));
    }


    /**
     * 获取缩略图
     *
     * @param slideId
     * @param response
     * @throws Exception
     */
    @GetMapping(value = "/getThumbnailImage/{slideId}", produces = MediaType.IMAGE_JPEG_VALUE)
    public void getThumbnailImage(@PathVariable("slideId") Long slideId, HttpServletResponse response) throws Exception {
        openSlideService.getThumbnailImage(slideId, response);
    }


    @GetMapping(value = "/tile/{slideId}/{titleGroup}/{location}.jpg", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public void getSlideTile(@PathVariable("slideId") Long slideId,
                              @PathVariable("titleGroup") String titleGroup,
                              @PathVariable("location") String location,
                              HttpServletResponse response) throws Exception {
        openSlideService.getSlideTile( slideId, titleGroup, location, response);
    }
}

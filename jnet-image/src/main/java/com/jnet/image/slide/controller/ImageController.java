package com.jnet.image.slide.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.jnet.api.R;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.event.UploadCompleteEvent;
import com.jnet.image.slide.domain.Image;
import com.jnet.image.slide.service.ImageService;
import com.jnet.image.slide.service.OpenSlideService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
@RequestMapping("/image")
public class ImageController {

    @Resource
    private ImageService imageService;

    @PostMapping("/pageImages")
    public R<Page> pageImages(@RequestBody PageDTO page) throws Exception {
        imageService.page(page);
        return R.success(page);
    }

}

package com.jnet.image.slide.controller;

import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.event.UploadCompleteEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    @Autowired
    private ApplicationEventPublisher publisher;

    @GetMapping("/test")
    public void test() throws Exception {
        Attachment attachment = Attachment.builder().attachmentExt("ndpi").attachmentMd5("md5").attachmentPath("C:\\Users\\86186\\Desktop\\sms\\836849-1_-_2018-12-07_19.15.48.ndpi").build();
        publisher.publishEvent(new UploadCompleteEvent(attachment));
    }
}

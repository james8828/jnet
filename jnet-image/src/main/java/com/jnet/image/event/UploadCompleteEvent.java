package com.jnet.image.event;

import com.jnet.image.attachment.domain.Attachment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/3 17:27:10
 */
@Getter
public class UploadCompleteEvent extends ApplicationEvent {

    private Attachment attachment;
    public UploadCompleteEvent(Attachment attachment) {
        super(attachment);
        this.attachment = attachment;
    }
}

package com.jnet.image.event;

import com.jnet.image.attachment.domain.Attachment;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/3 17:27:10
 */
public class UploadCompleteEvent extends ApplicationEvent {

    private final Attachment attachment;

    public UploadCompleteEvent(Object source, Attachment attachment) {
        super(source);
        if (attachment == null) {
            throw new IllegalArgumentException("Attachment cannot be null");
        }
        // 如果 Attachment 是可变对象，建议进行深拷贝
        this.attachment = copyAttachment(attachment);
    }

    private Attachment copyAttachment(Attachment original) {
        return original.clone(); // 使用clone方法复制对象
    }

    public Attachment getAttachment() {
        return attachment;
    }


}


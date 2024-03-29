package com.jnet.image.attachment.service;

import com.jnet.image.attachment.domain.Attachment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 86186
* @description 针对表【ta_attachment(附件表)】的数据库操作Service
* @createDate 2023-09-08 15:46:51
*/
public interface AttachmentService extends IService<Attachment> {

    Attachment uploadChunk(String name,
                           String md5,
                           Long size,
                           Integer chunks,
                           Integer chunk,
                           MultipartFile attachmentFile)throws Exception;
    Attachment saveAttachment(String md5,String name,MultipartFile attachmentFile)throws Exception;
    Attachment saveAttachment(MultipartFile attachmentFile)throws Exception;

    void foo();

}

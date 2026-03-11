package com.jnet.image.attachment.service;

import com.jnet.api.R;
import com.jnet.image.attachment.domain.Attachment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.image.attachment.vo.ChunkUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 86186
* @description 针对表【ta_attachment(附件表)】的数据库操作Service
* @createDate 2023-09-08 15:46:51
*/
public interface AttachmentService extends IService<Attachment> {

    R<Boolean> uploadChunk(ChunkUploadVO chunkVO) throws  Exception;
    R<Attachment> completeMultipartUpload(ChunkUploadVO chunkVO);
    /*Attachment uploadChunk(String name,
                           String uploadId,
                           String md5,
                           Long chunkSize,
                           Integer chunkTotal,
                           Integer chunkIndex,
                           MultipartFile attachmentFile)throws Exception;*/
    Attachment saveAttachment(String md5,String name,MultipartFile attachmentFile)throws Exception;
    R initiateMultipartUpload() throws Exception;
}

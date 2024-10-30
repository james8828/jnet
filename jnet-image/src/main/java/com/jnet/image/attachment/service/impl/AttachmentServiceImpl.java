package com.jnet.image.attachment.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.jnet.api.R;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.attachment.mapper.AttachmentMapper;
import com.jnet.image.attachment.service.AttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.image.attachment.utils.FileUtils;
import com.jnet.image.attachment.utils.UploadState;
import com.jnet.image.attachment.utils.UploadUtils;
import com.jnet.image.constant.ImageConstant;
import com.jnet.image.event.UploadCompleteEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
* @author james
* @description
* @createDate 2023-09-08 15:46:51
*/
@Slf4j
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment>
    implements AttachmentService{

    @Value("${filePath}")
    public String ATTACHMENT_PATH;
    private static final Snowflake snowflake = IdUtil.getSnowflake();

    @Resource
    private ApplicationEventPublisher publisher;

    @Override
    public Attachment uploadChunk(String name,String uploadId, String md5, Long chunkSize, Integer chunkTotal, Integer chunkIndex, MultipartFile file) throws Exception {
        Long userId = 1L;
        Attachment attachment = null;
        //验证分片MD5
        InputStream inputStream = file.getInputStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);
        if (!UploadUtils.checkMd5(md5, bytes)) {
            throw new RuntimeException("md5校验失败");
        }
        log.info("上传文件分片信息,name:[{}];uploadId:[{}];md5:[{}];chunkSize:[{}];chunkTotal:[{}];fileSize:[{}];chunkIndex:[{}];", name, uploadId, md5, chunkSize, chunkTotal, file.getSize(), chunkIndex);
        //创建文件
        File persistFile = null;
        if (!UploadUtils.containsUploadStateValue(uploadId)) {
            persistFile = new File(ATTACHMENT_PATH + name);
        }
        //初始化分片状态
        UploadUtils.initUploadState(uploadId, chunkTotal);
        //写入文件
        FileUtils.writeWithBlok(ATTACHMENT_PATH + name, chunkSize, bytes, chunkTotal, chunkIndex);
        //更新分片状态
        UploadUtils.updateUploadState(uploadId,md5, chunkIndex);
        log.debug("文件分片上传完成,uploadId:[{}];chunkIndex:[{}];", uploadId, chunkIndex);
        //判断是否已上传完成
        if (UploadUtils.isUploaded(uploadId)) {
            //分片上传状态
            UploadState state = UploadUtils.getUploadStateValue(uploadId);
            //获取分片文件MD5
            String[] md5s = state.getMd5s().get();
            String attachmentCode = snowflake.nextIdStr();
            String attachmentExt = name.substring(name.lastIndexOf(".") + 1);
            attachment = Attachment.builder().attachmentName(name).attachmentCode(attachmentCode).attachmentExt(attachmentExt)
                    .attachmentPath(ATTACHMENT_PATH + name).attachmentSize(persistFile == null ? 0 : persistFile.length()).attachmentMd5(StringUtils.join(md5s, ImageConstant.MD5_SEPARATOR)).createBy(userId).updateBy(userId).build();
            getBaseMapper().insert(attachment);
            //删除分片状态
            UploadUtils.deleteUploadStateValue(uploadId);
            publisher.publishEvent(new UploadCompleteEvent(attachment));
            log.info("文件上传完成,attachmentCode:[{}];", attachmentCode);
        }
        return attachment;
    }

    @Override
    public Attachment saveAttachment(String md5, String name, MultipartFile attachmentFile) throws Exception {
        Long userId = 1L;
        InputStream inputStream = attachmentFile.getInputStream();
        byte[] bytes = IOUtils.toByteArray(inputStream);
        if(!UploadUtils.checkMd5(md5, bytes)){
            throw new RuntimeException("md5校验失败");
        }
        String attachmentCode = snowflake.nextIdStr();
        long attachmentSize = attachmentFile.getSize();
        String attachmentName = name;
        if (StringUtils.isEmpty(attachmentName)){
            attachmentName = attachmentFile.getOriginalFilename();
        }
        String attachmentExt = attachmentName.substring(attachmentName.lastIndexOf(".") + 1);
        //创建文件夹
        File dir = new File(ATTACHMENT_PATH+File.separator+attachmentCode);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        File file = FileUtil.file(ATTACHMENT_PATH+File.separator+attachmentCode,attachmentName);
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(out);
        }
        Attachment attachment = Attachment.builder().attachmentName(attachmentName).attachmentCode(attachmentCode).attachmentExt(attachmentExt)
                .attachmentPath(file.getPath()).attachmentSize(attachmentSize).attachmentMd5(md5).createBy(userId).updateBy(userId).build();
        getBaseMapper().insert(attachment);
        publisher.publishEvent(new UploadCompleteEvent(attachment));
        return attachment;
    }

    @Transactional
    @Override
    public Attachment saveAttachment(MultipartFile attachmentFile)throws Exception{
        Long userId = 1L;
        InputStream inputStream = attachmentFile.getInputStream();
        String attachmentCode = snowflake.nextIdStr();
        long attachmentSize = attachmentFile.getSize();
        String attachmentName = attachmentFile.getOriginalFilename();
        String  attachmentExt = attachmentName.substring(attachmentName.lastIndexOf(".") + 1);
        //创建文件夹
        File dir = new File(ATTACHMENT_PATH+File.separator+attachmentCode);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        File file = FileUtil.file(ATTACHMENT_PATH+File.separator+attachmentCode,attachmentName);
        FileOutputStream out = new FileOutputStream(file);
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            out.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(out);
        }
        Attachment attachment = Attachment.builder().attachmentName(attachmentName).attachmentCode(attachmentCode).attachmentExt(attachmentExt)
                .attachmentPath(file.getPath()).attachmentSize(attachmentSize).createBy(userId).updateBy(userId).build();
        getBaseMapper().insert(attachment);
        publisher.publishEvent(new UploadCompleteEvent(attachment));
        return attachment;
    }

    /**
     * 初始化分片上传任务
     * @return
     * @throws Exception
     */
    @Override
    public R initiateMultipartUpload() throws Exception {
        return R.success(snowflake.nextIdStr());
    }


}





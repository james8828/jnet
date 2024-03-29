package com.jnet.image.attachment.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jnet.common.biz.thread.Pooled;
import com.jnet.common.biz.thread.TaskToolExecutor;
import com.jnet.common.biz.thread.Worker;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.attachment.mapper.AttachmentMapper;
import com.jnet.image.attachment.service.AttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.image.attachment.utils.FileUtils;
import com.jnet.image.event.UploadCompleteEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static com.jnet.image.attachment.utils.UploadUtils.*;

/**
* @author 86186
* @description 针对表【ta_attachment(附件表)】的数据库操作Service实现
* @createDate 2023-09-08 15:46:51
*/
@Slf4j
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment>
    implements AttachmentService{

    @Value("${filePath}")
    public String ATTACHMENT_PATH;
    private static final Snowflake snowflake = IdUtil.getSnowflake();

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public Attachment uploadChunk(String name, String md5, Long size, Integer chunks, Integer chunk, MultipartFile file) throws Exception {
        if (!checkMd5(md5)){
            return null;
        }
        Long userId = 1L;
        //查询文件是否存在
        String fileName = getFileName(md5, chunks);
        log.info("上传文件信息,fileName:[{}];size:[{}];chunks:[{}];fileSize:[{}];chunk:[{}];",fileName,size,chunks,file.getSize(),chunk);
        FileUtils.writeWithBlok(ATTACHMENT_PATH + fileName, size, file.getInputStream(), file.getSize(), chunks, chunk);
        addChunk(md5,chunk);
        if (isUploaded(md5)) {
            removeKey(md5);
            String attachmentCode = snowflake.nextIdStr();
            String  attachmentExt = name.substring(name.lastIndexOf(".") + 1);
            File f = new File(ATTACHMENT_PATH + fileName);
            Attachment attachment = Attachment.builder().attachmentName(name).attachmentCode(attachmentCode).attachmentExt(attachmentExt)
                    .attachmentPath(ATTACHMENT_PATH + fileName).attachmentSize(f.length()).attachmentMd5(md5).createBy(userId).updateBy(userId).build();
            getBaseMapper().insert(attachment);
            publisher.publishEvent(new UploadCompleteEvent(attachment));
        }
        return null;
    }

    /**
     * 检查Md5判断文件是否已上传
     * @param md5
     * @return
     */
    public boolean checkMd5(String md5) {
        List<Attachment> attachmentList = getBaseMapper().selectList(Wrappers.query(Attachment.builder().attachmentMd5(md5).build()));
        return CollectionUtils.isEmpty(attachmentList);
    }

    @Override
    public Attachment saveAttachment(String md5, String name, MultipartFile attachmentFile) throws Exception {
        if (!checkMd5(md5)){
            return null;
        }
        Long userId = 1L;
        InputStream inputStream = attachmentFile.getInputStream();
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
            byte[] bytes = IOUtils.toByteArray(inputStream);
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

    @Resource
    private TaskToolExecutor msgExecutor;

    @Override
    public void foo() {
        Worker<Object> worker = new Worker<>();
        worker.setPoolOverAct(Pooled.PoolOverAct.BLOCK);
        Runnable command = () -> {
            try {
                log.info("start:[{}]","foo");
                Thread.sleep(5000);
                log.info("end:[{}]","foo");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        /*Callable<String> callable = () -> {
            Thread.sleep(5000);
            return "5555";
        };
        FutureTask<String> futureTask = new FutureTask<>(callable);
        worker.setCommand(futureTask);*/
        worker.setCommand(command);
        msgExecutor.execute(worker);
        /*try {
            String s = futureTask.get();
            log.info("{}",s);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }*/
    }

}





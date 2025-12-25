package com.jnet.image.attachment.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.jnet.api.R;
import com.jnet.common.core.utils.SecurityUtils;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.attachment.mapper.AttachmentMapper;
import com.jnet.image.attachment.service.AttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.image.attachment.vo.ChunkUploadVO;
import com.jnet.image.event.UploadCompleteEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author james
 * @description
 * @createDate 2023-09-08 15:46:51
 */
@Slf4j
@Service
public class AttachmentServiceImpl extends ServiceImpl<AttachmentMapper, Attachment>
        implements AttachmentService {

    @Value("${filePath}")
    public String ATTACHMENT_PATH;
    private static final Snowflake snowflake = IdUtil.getSnowflake();

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Resource
    private ApplicationEventPublisher publisher;

    @Override
    public R<Boolean> uploadChunk(ChunkUploadVO vo) throws IOException {
        Long uploadId = vo.getUploadId();
        int chunkIndex = vo.getChunkIndex();
        String expectedMd5 = vo.getChunkMd5();

        // 参数合法性检查
        if (uploadId == null || uploadId <= 0) {
            log.warn("非法 uploadId: {}", uploadId);
            return R.fail("非法 uploadId");
        }

        if (expectedMd5 == null || expectedMd5.isEmpty()) {
            log.warn("MD5 校验值为空");
            return R.fail("MD5 校验值为空");
        }

        // 构建临时目录
        File tempDir = new File(uploadDir + File.separator + "temp" + File.separator + uploadId);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            log.error("无法创建临时目录: {}", tempDir.getAbsolutePath());
            return R.fail("无法创建临时目录");
        }

        File targetFile = new File(tempDir, "part-" + chunkIndex);

        // 判断是否已存在并跳过上传
        if (targetFile.exists()) {
            try {
                String existingMd5 = calculateFileMd5(targetFile);
                if (existingMd5.equals(expectedMd5)) {
                    log.info("uploadId: {}, 分片 {} 已存在且 MD5 校验通过，跳过上传", uploadId, chunkIndex);
                    return R.success(true);
                } else {
                    log.warn("uploadId: {}, 分片 {} MD5 校验失败，重新上传", uploadId, chunkIndex);
                    FileUtils.forceDelete(targetFile);
                }
            } catch (IOException e) {
                log.error("uploadId: {}, 分片 {} MD5 校验失败", uploadId, chunkIndex, e);
                return R.fail("MD5 校验失败");
            }
        }else{
            //创建文件
            FileUtils.forceMkdir(targetFile.getParentFile());
        }

        // 写入分片文件
        try {
//            byte[] chunkBytes = vo.getChunk().getBytes();
//            FileUtils.writeByteArrayToFile(targetFile, chunkBytes);
            vo.getChunk().transferTo(targetFile);
        } catch (IOException e) {
            log.error("uploadId: {}, 分片 {} 写入失败", uploadId, chunkIndex, e);
            return R.fail("分片写入失败");
        }

        // 二次 MD5 校验
        try {
            String actualMd5 = calculateFileMd5(targetFile);
            if (!actualMd5.equals(expectedMd5)) {
                log.warn("uploadId: {}, 分片 {} 写入后 MD5 校验失败，预期: {}, 实际: {}", uploadId, chunkIndex, expectedMd5, actualMd5);
                FileUtils.forceDelete(targetFile);
                return R.fail("MD5 校验失败，请重新上传");
            }
        } catch (IOException e) {
            log.error("uploadId: {}, 分片 {} MD5 校验失败", uploadId, chunkIndex, e);
            FileUtils.forceDelete(targetFile);
            return R.fail("MD5 校验失败");
        }

        log.info("uploadId: {}, 分片 {} 上传成功，MD5 校验通过", uploadId, chunkIndex);
        return R.success(true);
    }


    /**
     * 计算文件的 MD5
     *
     * @param file
     * @return
     * @throws IOException
     */
    private String calculateFileMd5(File file) throws IOException {
        byte[] headTail = getHeadTailBytes(file);
        return DigestUtil.md5Hex(headTail);
    }

    /**
     * 从文件中取出头尾各 128 字节，拼接为新 byte[]
     *
     * @param file 文件对象
     * @return 拼接后的 byte[]
     */
    private byte[] getHeadTailBytes(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File must be a valid file");
        }
        Long fileSize = file.length();
        int bufferSize = (int) Math.min(fileSize, 256);
        byte[] buffer = new byte[bufferSize];
        byte[] fileBytes = FileUtils.readFileToByteArray( file);
        if (fileSize >= 256) {
            System.arraycopy(fileBytes, 0, buffer, 0, 128);
            System.arraycopy(fileBytes, fileSize.intValue() - 128, buffer, 128, 128);
        } else {
            buffer = fileBytes;
        }
        return buffer;
    }

    public static void main(String[] args) throws Exception{
        File tempDir = new File("D:\\home\\upload\\temp\\1939566310020055040");
        File[] files = tempDir.listFiles();
        for (File file : files) {
            System.out.println(file.getName());
            int partFileSize = (int) file.length();
            byte[] partBytes = new byte[partFileSize];
            log.info("partFileSize: {}", partFileSize);
//            log.info("partBytes: {}", partBytes);
        }
        File targetFile = new File("D:\\home\\upload\\files\\20230519172600195_1473.svs");
        log.info("targetFile: {},exist:{}", targetFile.getAbsolutePath(),targetFile.exists());
        try (FileChannel outChannel = FileChannel.open(targetFile.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {            for (File file : files)  {
                int partFileSize = (int) file.length();
                byte[] partBytes = new byte[partFileSize];
                long i = Long.valueOf(file.getAbsolutePath().substring((int)file.getAbsolutePath().length() - 1, (int)file.getAbsolutePath().length()));
                long offset = i * 5242880;
                outChannel.position(offset);
                outChannel.write(ByteBuffer.wrap(partBytes));
//                    inChannel.read(ByteBuffer.wrap(partBytes));

                    /*try (FileChannel inChannel = FileChannel.open(partFile.toPath())) {
                        log.info("合并分片: {}, index:{} ", partFile.getAbsolutePath(), i);
                        ByteBuffer buffer = ByteBuffer.allocate(8192);
                        while (inChannel.read(buffer) != -1) {
                            buffer.flip();
                            long offset = (long) i * chunkSize + buffer.position();
                            log.info("合并分片: {}, 偏移量: {}", partFile.getAbsolutePath(), offset);
                            outChannel.position(offset);
                            outChannel.write(buffer);
                        }
                    }*/
            }
        }


    }

    @Override
    public R<Attachment> completeMultipartUpload(ChunkUploadVO vo) {
        Long uploadId = vo.getUploadId();
        int chunkTotal = vo.getChunkTotal();
        long chunkSize = vo.getChunkSize();
        String fileName = vo.getName();

        File tempDir = new File(uploadDir + File.separator + "temp" + File.separator + uploadId);
        if (!tempDir.exists()) {
            log.warn("临时目录不存在: {}", tempDir.getAbsolutePath());
            return R.fail("临时目录不存在");
        }
        File targetFile = new File(uploadDir + File.separator + "files" + File.separator + fileName);
        targetFile.getParentFile().mkdirs(); // 确保目标目录存在
        try {
            targetFile.createNewFile();
            // 使用 NIO 的 FileChannel 进行高效的文件合并
            try (FileChannel outChannel = FileChannel.open(targetFile.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                for (int i = 0; i < chunkTotal; i++) {
                    File partFile = new File(tempDir, "part-" + i);
                    if (!partFile.exists()) {
                        log.warn("分片文件不存在: {}", partFile.getAbsolutePath());
                        return R.fail("分片文件缺失");
                    }

                    try (FileChannel inChannel = FileChannel.open(partFile.toPath())) {
                        log.info("合并分片: {}, index:{} ", partFile.getAbsolutePath(), i);
                        long offset = (long) i * chunkSize;
                        outChannel.position(offset);
                        ByteBuffer buffer = ByteBuffer.allocate(64*1024);
                        int bytesRead = inChannel.read(buffer);
                        while (bytesRead != -1) {

                            buffer.flip(); // 切换为读模式
                            while (buffer.hasRemaining()) {
                                outChannel.write(buffer);
                            }
                            offset += buffer.position();
                            outChannel.position(offset);
                            buffer.clear(); // 清空准备下一次读取
                            bytesRead = inChannel.read(buffer);
                        }
                    }
                }
            }

            // 清理临时目录
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                log.warn("清理临时目录失败: {}", tempDir.getAbsolutePath(), e);
            }

            // 构建附件对象并保存到数据库
            Attachment attachment = new Attachment();
            attachment.setBizId(uploadId);
            attachment.setAttachmentName(fileName);
            attachment.setAttachmentCode(snowflake.nextIdStr());
            attachment.setAttachmentPath(targetFile.getAbsolutePath());
            attachment.setAttachmentSize(targetFile.length());
            attachment.setAttachmentExt(fileName.substring(fileName.lastIndexOf('.') + 1));
            baseMapper.insert(attachment);
            publisher.publishEvent(new UploadCompleteEvent(this,attachment));
            return R.success(attachment);
        } catch (Exception e) {
            log.error("合并分片文件失败", e);
            // 删除部分写入的目标文件，避免残留
            if (targetFile.exists()) {
                targetFile.delete();
            }
            return R.fail("合并分片文件失败");
        }
    }


    /*@Override
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
*/
    public Attachment saveAttachment(String md5, String name, MultipartFile attachmentFile) throws Exception {
        Long userId = SecurityUtils.getUserId(); // 替换为实际获取用户ID的方式
        try (InputStream inputStream = attachmentFile.getInputStream()) {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            String fileMd5 = DigestUtil.md5Hex(bytes);
            if (md5.equals(fileMd5)) {
                throw new RuntimeException("md5校验失败");
            }

            String attachmentCode = snowflake.nextIdStr();
            long attachmentSize = attachmentFile.getSize();
            String attachmentName = StringUtils.isEmpty(name) ? attachmentFile.getOriginalFilename() : name;

            if (attachmentName == null || attachmentName.isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            String attachmentExt = "";
            int dotIndex = attachmentName.lastIndexOf(".");
            if (dotIndex >= 0 && dotIndex < attachmentName.length() - 1) {
                attachmentExt = attachmentName.substring(dotIndex + 1);
            }

            // 创建文件夹
            Path dirPath = Paths.get(ATTACHMENT_PATH, attachmentCode);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(attachmentName);
            try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                out.write(bytes);
            } catch (IOException e) {
                log.error("文件写入失败: {}", filePath, e); // 假设已定义 log
                throw new IOException("文件写入失败", e);
            }

            Attachment attachment = Attachment.builder()
                    .attachmentName(attachmentName)
                    .attachmentCode(attachmentCode)
                    .attachmentExt(attachmentExt)
                    .attachmentPath(filePath.toString())
                    .attachmentSize(attachmentSize)
                    .attachmentMd5(md5)
                    .createBy(userId)
                    .updateBy(userId)
                    .build();

            getBaseMapper().insert(attachment);
            publisher.publishEvent(new UploadCompleteEvent(this,attachment));
            return attachment;
        } catch (IOException e) {
            log.error("文件处理过程中发生异常", e);
            throw e;
        } catch (Exception e) {
            log.error("未知异常", e);
            throw new Exception("上传过程中发生异常", e);
        }
    }


    /**
     * 初始化分片上传任务
     *
     * @return
     * @throws Exception
     */
    @Override
    public R initiateMultipartUpload() throws Exception {
        return R.success(snowflake.nextIdStr());
    }


}





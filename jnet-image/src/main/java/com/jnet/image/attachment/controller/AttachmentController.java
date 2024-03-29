package com.jnet.image.attachment.controller;

import com.jnet.api.R;
import com.jnet.common.biz.thread.TaskToolExecutor;
import com.jnet.image.attachment.domain.Attachment;
import com.jnet.image.attachment.service.AttachmentService;
import com.jnet.image.attachment.utils.BeanCopyUtil;
import com.jnet.image.attachment.vo.AttachmentVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2023/9/8 15:50:50
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/attachment")
public class AttachmentController {

    @Resource
    private AttachmentService attachmentService;

    @PostMapping("/verify")
    public List<String> verify(@RequestBody Map params) throws Exception {
        log.info("params:[{}]",params);
        List<String> array = new ArrayList<>();
        array.add("1");
        array.add("2");
        return array;
    }

    @PostMapping("/uploadChunk")
    public R uploadChunk(@RequestParam("name") String name,
                            @RequestParam("md5") String md5,
                            @RequestParam("size") Long size,
                            @RequestParam("chunks") Integer chunks,
                            @RequestParam("chunk") Integer chunk,
                            @RequestParam("file") MultipartFile file) throws Exception {
        if (chunks != null && chunks != 0) {
            attachmentService.uploadChunk(name, md5,size,chunks,chunk,file);
        } else {
            upload(name,md5,file);
        }
        return R.success();
    }

    @Operation(summary = "附件管理-附件上传")
    @PostMapping("/upload")
    public R<Attachment> upload(String name,
                                String md5,
                                MultipartFile file) throws Exception {
        return R.success(attachmentService.saveAttachment(name,md5,file));
    }

    @Operation(summary =  "附件管理-附件下载")
    @GetMapping("/download")
    public void download(AttachmentVO in, HttpServletResponse response) throws Exception {
        Attachment attachment = Attachment.builder().build();
        BeanCopyUtil.copy(in,attachment);
        attachment = attachmentService.getOne(Wrappers.query(attachment));
        String attachmentPath = attachment.getAttachmentPath();
        File file = new File(attachmentPath);
        response.setContentType("multipart/form-data");
        response.setHeader("Content-Disposition", "attachment;fileName="+
                URLEncoder.encode(attachment.getAttachmentName(), "utf-8"));
        OutputStream out = response.getOutputStream();
        byte[] bytes = FileUtils.readFileToByteArray(file);
        out.write(bytes);
    }

    @Operation(summary =  "附件管理-删除附件")
    @DeleteMapping("/delete")
    public R delete(@NotNull(message = "附件编码！") @RequestParam("attachmentCode")  String attachmentCode) throws Exception {
        Attachment attachment = Attachment.builder().attachmentCode(attachmentCode).build();
        attachment = attachmentService.getOne(Wrappers.query(attachment));
        String attachmentPath = attachment.getAttachmentPath();
        File file = new File(attachmentPath);
        if (file.exists()){
            file.delete();
        }
        attachmentService.removeById(attachment.getAttachmentId());
        return R.success();
    }

    @Operation(summary =  "附件管理-附件分页查询")
    @GetMapping("/page")
    public R<Page<Attachment>> page(
            @NotNull(message = "分页参数为空！") @RequestParam("pageNum")  Integer pageNum,
            @NotNull(message = "分页参数为空！") @RequestParam("pageSize") Integer pageSize)
            throws Exception {
        Page<Attachment> page = new Page<>(pageNum, pageSize);
        attachmentService.page(page);
        return R.success(page);
    }

    @Operation(summary =  "附件管理-附件查询")
    @GetMapping("/query")
    public R<List<Attachment>> query(AttachmentVO in)throws Exception {
        Attachment attachment = Attachment.builder().build();
        BeanCopyUtil.copy(in,attachment);
        return R.success(attachmentService.list(Wrappers.query(attachment)));
    }

    @Operation(summary =  "附件管理-附件查询")
    @PostMapping("/queryByAttachmentCodes")
    public R<List<Attachment>> query(@NotNull(message = "附件编码集合！") @RequestBody List<String> attachmentCodes)throws Exception {
        QueryWrapper<Attachment> queryWrapper = Wrappers.query();
        queryWrapper.in("attachment_code",attachmentCodes);
        return R.success(attachmentService.list(queryWrapper));
    }
    @Resource
    private TaskToolExecutor taskToolExecutor;
    @PostMapping("/foo")
    public R query()throws Exception {
        attachmentService.foo();
        return R.success(taskToolExecutor.monitor());
    }
}

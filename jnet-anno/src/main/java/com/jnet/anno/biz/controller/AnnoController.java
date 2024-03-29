package com.jnet.anno.biz.controller;

import com.jnet.anno.biz.domain.Anno;
import com.jnet.anno.biz.service.AnnoService;
import com.jnet.anno.common.model.ApiResult;
import com.jnet.anno.utils.ResultUtils;
import com.jnet.api.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/1 16:44:17
 */
@CrossOrigin
@RestController
@RequestMapping("/anno")
public class AnnoController {
    @Resource
    private AnnoService annoService;

    @PostMapping("/addAnno")
    public R addAnno(@RequestBody Anno anno){
        return R.success(annoService.save(anno));
    }

    @PostMapping("/queryAnno")
    public ApiResult<List<Anno>> queryAnno(@RequestBody Anno anno){
        List<Anno> annoList = annoService.list();
        return ResultUtils.Success(annoList);
    }

    @PostMapping("/getAnno")
    public ApiResult<List<Anno>> getAnno(@RequestBody Anno anno){
        List<Anno> annoList = annoService.list();
        return ResultUtils.Success(annoList.get(0));
    }

    @PostMapping("/uploadAnnoFile")
    public R uploadAnnoFile(MultipartFile file)throws Exception{
        annoService.uploadAnnoFile(file);
        return R.success();
    }

    @GetMapping("/upload")
    public R upload()throws Exception{
        annoService.uploadAnnoFile(null);
        return R.success();
    }
}

package com.jnet.anno.biz.service;

import com.jnet.anno.biz.domain.Anno;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 86186
* @description 针对表【anno】的数据库操作Service
* @createDate 2024-02-01 16:36:51
*/
public interface AnnoService extends IService<Anno> {

    void uploadAnnoFile(MultipartFile file)throws Exception;

}

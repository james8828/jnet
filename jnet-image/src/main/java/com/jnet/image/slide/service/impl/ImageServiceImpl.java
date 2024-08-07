package com.jnet.image.slide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.image.slide.domain.Image;
import com.jnet.image.slide.mapper.ImageMapper;
import com.jnet.image.slide.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/8/6 16:41:43
 */
@Service
@Slf4j
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image> implements ImageService {

}

package com.jnet.image.slide.service;

import jakarta.servlet.http.HttpServletResponse;

public interface OpenSlideService {

    /**
     * 获取切片缩略图
     * @param slideId
     * @param response
     * @throws Exception
     */
    void getThumbnailImage(Long slideId, HttpServletResponse response)throws Exception;

    /**
     * 获取关联图片
     * @param slideId
     * @param key
     * @param response
     * @throws Exception
     */
    void getAssociatedImage(Long slideId, String key, HttpServletResponse response)throws Exception;

    /**
     * 处理图片
     * @param titleGroup
     * @param location
     * @param response
     * @throws Exception
     */
    void getSlideTile(Long slideId, String titleGroup, String location, HttpServletResponse response)throws Exception;

}


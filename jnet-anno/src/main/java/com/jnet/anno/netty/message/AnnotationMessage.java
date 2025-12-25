package com.jnet.anno.netty.message;

import lombok.Data;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description 轮廓数据websocket报文消息
 * @date 2025/5/22 13:24:31
 */
@Data
public class AnnotationMessage {

    private String type;

    private Long slideId;

    private String annotation_type;

    private AnnotationFeature data;

    private List<AnnotationFeature> dataList;


}

package com.jnet.anno.utils.annotation;

import cn.hutool.core.date.DateUtil;
import com.jnet.anno.vo.StructureTagPageVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationJsonIdGenerator {

    private static final String AI = "ai";
    private static final String SD = "sd";
    private static final String CL = "cl";
    private static final String LABEL_NAME = "labelname";
    private static final String MEASURE_NAME = "measure_name";

    public static int RandomNumbers() {
        return (int) (Math.random() * 90 + 10);
    }

    public static String getAiId() {
        return AI + LABEL_NAME + DateUtil.current() + RandomNumbers();
    }

    public static String getSdId() {
        return SD  + DateUtil.current() + RandomNumbers();
    }

    public static String getSdId(String categoryName) {
        return SD + categoryName + DateUtil.current() + RandomNumbers();
    }

    public static String getSdId(StructureTagPageVo tag) {
        if (tag == null){
            return getSdId();
        }else{
            return getSdId(tag.getStructureTagName());
        }
    }

    public static String getClId() {
        return CL + MEASURE_NAME + DateUtil.current() + RandomNumbers();
    }

}

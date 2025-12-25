package com.jnet.image.attachment.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2023/9/7 09:51:13
 */
@Slf4j
public class BeanCopyUtil {

    /**
     * 删除对象属性中的空字符串
     *
     * @param obj
     */
    public static void cleanSource(Object obj) {
        Field[] fields = ReflectUtil.getFields(obj.getClass());
        for (Field field : fields) {
            try {
                Object val = ReflectUtil.getFieldValue(obj,field);
                if (val!=null){
                    if ("".equals(val)||((val instanceof Integer||val instanceof Long)&&(Long) val==0)) {
                        field.set(obj, null);
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("删除对象属性中的空字符串异常:{}", e.getMessage());
            }
        }
    }

    public static void copy(Object source, Object target) {
        cleanSource(source);
        BeanUtil.copyProperties(source, target);
    }

}

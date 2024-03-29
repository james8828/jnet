package com.jnet.anno.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @name: BaseUtil
 * @description: 基础公共工具
 **/
public class BaseUtil {
    /**
     * 日志管理
     */
    private static final Logger logger = LoggerFactory.getLogger(BaseUtil.class);

    /**
     * list集合是否不为空
     * @param con 目标集合
     * @return 返回结果
     */
    public static boolean listNotNull(Collection<?> con){
        return con != null && !con.isEmpty();
    }

    /**
     * list集合是否为空
     * @param con 目标集合
     * @return 返回结果
     */
    public static boolean listIsNull(Collection<?> con){
        return !listNotNull(con);
    }

    /**
     * 数组是否不为空
     * @param array 目标数组
     * @return 返回结果
     */
    public static boolean arrayNotNull(Object[] array){
        if(array != null){
            return array.length != 0;
        }
        return false;
    }

    /**
     * 数组是为空
     * @param array 目标数组
     * @return 返回结果
     */
    public static boolean arrayIsNull(Object[] array){
        return !arrayNotNull(array);
    }

    /**
     * 对象是否不为空
     * @param obj 目标对象
     * @return 返回结果
     */
    public static boolean objectNotNull(Object obj){
        return obj != null;
    }

    /**
     * 对象是否为空
     * @param obj 目标对象
     * @return 返回结果
     */
    public static boolean objectIsNull(Object obj){
        return !objectNotNull(obj);
    }

    /**
     * map集合是否不为空
     * @param map 目标集合
     * @return 返回结果
     */
    public static boolean mapNotNull(Map<?, ?> map){
        return map!=null && !map.isEmpty();
    }

    /**
     * map集合是否为空
     * @param map 目标集合
     * @return 返回结果
     */
    public static boolean mapIsNull(Map<?, ?> map){
        return !mapNotNull(map);
    }

    /**
     * 字符串是否为空
     * @param str 目标字符串
     * @return 返回结果
     */
    public static boolean stringNotNull(String str){
        if(str != null && str.length() >= 0 && str != ""){
            return true;
        }
        return false;
    }

    /**
     * 字符串是否不为空
     * @param str 目标字符串
     * @return 返回结果
     */
    public static boolean stringIsNull(String str){
        return !stringNotNull(str);
    }

    /**
     * 去除list中的重复元素
     * @param cons 目标字符串list集合
     * @return 没有重复的list
     */
    public static List<String> removeDuplicate(List<String> cons){
        if(BaseUtil.listNotNull(cons)){
            List<String> newtList = new ArrayList<String>();
            for (String str : cons) {
                if(!newtList.contains(str) && stringNotNull(str)){
                    newtList.add(str);
                }
            }
            return newtList;
        }else{
            return cons;
        }
    }

    /**
     * 随机生成指定位数的整数
     * @param num 要生成随机数的个数
     * @return 返回随机生成的数字
     */
    public static String randomNum(int num) {
        Random random = new Random();
        String numStr = "";
        for (int i = 0; i < num; i++) {
            numStr += random.nextInt(10);
        }
        return numStr;
    }

    /**
     * 获取文件后缀
     * @param fileName
     * @return 返回文件扩展名
     */
    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
    }


    /**
     * 去掉文件后缀
     * @param fileName
     * @return
     */
    public static String trimFileSuffix(String fileName) {
        return fileName.substring(0,fileName.lastIndexOf("."));
    }

    /**
     * 移除字符串中重复的元素（String以逗号分隔）
     * @param str 字符串，如112,121,112
     * @param operation 分隔符
     * @return 去重后的字符串，112,121
     */
    public static String removeDuplicate(String str, String operation){
        StringBuffer sb = new StringBuffer();
        if(stringIsNull(str)){
            try {
                //字符串按分隔符分隔存入字符串数组
                String[] strArr = str.split(operation);
                List<String> arrList = removeDuplicate(Arrays.asList(strArr));
                for (String str2 : arrList) {
                    sb.append(str2).append(operation);
                }
                if(sb.length()>0){
                    sb.delete(sb.length()-operation.length(), sb.length());
                }
            } catch (Exception e) {
                logger.error("message", e);
                return str;
            }
        }
        return sb.toString();
    }
}

package com.jnet.anno.constant;

/**
 * @author mugw
 * @version 1.0
 * @description 标注业务常量
 * @date 2025/5/14 13:44:14
 */
public class Constant {



    /**
     * image 图像切片状态：0-上传中；1-上传失败；2-解析中；3-解析失败；4-解析成功
     */
    public static final String IMAGE_STATUS_ENABLE = "4";
    public static final String IMAGE_PROCESS_PARSING = "2";
    public static final String IMAGE_PROCESS_PARSE_FAIL = "3";
    public static final String IMAGE_PROCESS_UPLOAD_FAIL = "1";
    public static final String IMAGE_PROCESS_UPLOADING = "0";
    public static final Integer IMAGE_SOURCE_UPLOAD = 1;
    public static final Integer IMAGE_SOURCE_SERVER = 2;

    /**
     * 图像名称解析状态
     */
    public static final Integer IMAGE_NAME_PARSE_FAIL = 0;
    public static final Integer IMAGE_NAME_PARSE_SUCC = 1;

    public static final Double IMAGE_RESOLUTION = 0.262;
    public static final Double IMAGE_RESOLUTION_SQUARE = 0.262*0.262;

    /**
     * 项目成员角色常量
     */
    public static final int ROLE_ADMIN = 1;    // 机构管理员
    public static final int ROLE_OWNER = 2;    // 项目负责人
    public static final int ROLE_MEMBER = 3;   // 项目参与用户
    public static final int ROLE_OTHER = 4;    // 其他用户

    /**
     * 轮廓数据常量
     */
    //标注类型(AI表示AI算出的标注，Draw表示前端绘制的标注，Measure表示测量)
    public static final String ANNO_TYPE_DRAW = "Draw";
    public static final String ANNO_TYPE_AI = "AI";
    public static final String ANNO_TYPE_MEASURE = "Measure";

    //轮廓操作
    public static final String ANNO_ACTION_ADD = "add";
    public static final String ANNO_ACTION_UPDATE = "update";
    public static final String ANNO_ACTION_DELETE = "delete";
    //操作(UNION:相交,DIFFERENCE:相差,修改-UPDATE,删除-DELETE,添加-INSERT)
    public static final String ANNO_OPERATION_UNION = "UNION";
    public static final String ANNO_OPERATION_DIFFERENCE = "DIFFERENCE";
    public static final String ANNO_OPERATION_ADD = "INSERT";
    public static final String ANNO_OPERATION_UPDATE = "UPDATE";
    public static final String ANNO_OPERATION_DELETE = "DELETE";

    //UNDO_REDO_STACK_SIZE
    public static final int UNDO_REDO_STACK_SIZE = 100;


    /**
     * 上传下载限制
     */
    public static final double UPLOAD_FILE_LIMIT = 300;
    public static final double DOWN_FILE_LIMIT = 300;

    /**
     * Viewer
     */
    public static final Double MICRON = 0.26;

    public static final String STRUCTURE_RO = "RO";
    public static final String STRUCTURE_ROA = "ROA";
    public static final String STRUCTURE_ROE = "ROE";

}

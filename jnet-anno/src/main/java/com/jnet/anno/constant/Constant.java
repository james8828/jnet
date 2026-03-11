package com.jnet.anno.constant;

/**
 * 标注业务常量
 * @author mugw
 * @version 1.0
 * @since 2025/5/14 13:44:14
 */
public class Constant {



    public static final Double IMAGE_RESOLUTION = 0.262;
    public static final Double IMAGE_RESOLUTION_SQUARE = 0.262*0.262;

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

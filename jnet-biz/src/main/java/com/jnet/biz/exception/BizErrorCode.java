package com.jnet.biz.exception;

import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * @author JNet Team

 */
@Getter
public enum BizErrorCode {

    // ==================== 通用错误 (10xxx) ====================
    SUCCESS(10000, "操作成功"),
    PARAM_ERROR(10001, "参数错误"),
    VALIDATION_ERROR(10002, "数据校验失败"),
    
    // ==================== 项目相关错误 (20xxx) ====================
    PROJECT_NOT_FOUND(20001, "项目不存在"),
    PROJECT_CODE_EXISTS(20002, "项目编码已存在"),
    PROJECT_STATUS_INVALID(20003, "项目状态无效"),
    PROJECT_HAS_BATCHES(20004, "项目下存在批次，无法删除"),
    PROJECT_PRIVACY_LEVEL_INVALID(20005, "隐私级别无效"),
    
    // ==================== 批次相关错误 (21xxx) ====================
    BATCH_NOT_FOUND(21001, "批次不存在"),
    BATCH_CODE_EXISTS(21002, "批次编号已存在"),
    BATCH_UPLOAD_STATUS_INVALID(21003, "上传状态无效"),
    BATCH_HAS_IMAGES(21004, "批次下存在图像，无法删除"),
    
    // ==================== 图像相关错误 (22xxx) ====================
    IMAGE_NOT_FOUND(22001, "图像不存在"),
    IMAGE_FORMAT_UNSUPPORTED(22002, "不支持的图像格式"),
    IMAGE_LIFECYCLE_STATUS_INVALID(22003, "图像生命周期状态无效"),
    IMAGE_PATH_INVALID(22004, "图像路径无效"),
    IMAGE_METADATA_PARSE_ERROR(22005, "图像元数据解析失败"),
    IMAGE_ANNOTATION_PROGRESS_INVALID(22006, "标注进度无效（0-100）"),
    
    // ==================== 标签相关错误 (23xxx) ====================
    TAG_NOT_FOUND(23001, "标签不存在"),
    TAG_CODE_EXISTS(23002, "标签编码已存在"),
    TAG_PARENT_NOT_FOUND(23003, "父标签不存在"),
    TAG_CYCLE_REFERENCE(23004, "标签存在循环引用"),
    TAG_SOURCE_INVALID(23005, "标签来源无效"),
    
    // ==================== 任务相关错误 (24xxx) ====================
    TASK_NOT_FOUND(24001, "任务不存在"),
    TASK_TYPE_INVALID(24002, "任务类型无效"),
    TASK_STATUS_INVALID(24003, "任务状态无效"),
    TASK_ALREADY_RUNNING(24004, "任务已在运行中"),
    TASK_CANCEL_FAILED(24005, "任务取消失败"),
    TASK_TIMEOUT(24006, "任务执行超时"),
    
    // ==================== 模型相关错误 (25xxx) ====================
    MODEL_NOT_FOUND(25001, "模型不存在"),
    MODEL_VERSION_EXISTS(25002, "模型版本已存在"),
    MODEL_WEIGHTS_NOT_FOUND(25003, "模型权重文件不存在"),
    MODEL_TRAINING_FAILED(25004, "模型训练失败"),
    MODEL_METRICS_INVALID(25005, "模型性能指标无效"),
    
    // ==================== 预测相关错误 (26xxx) ====================
    PREDICTION_NOT_FOUND(26001, "预测结果不存在"),
    PREDICTION_REVIEW_STATUS_INVALID(26002, "预测质检状态无效"),
    PREDICTION_INFERENCE_FAILED(26003, "AI推理失败"),
    PREDICTION_GEOJSON_PARSE_ERROR(26004, "GeoJSON解析失败"),
    
    // ==================== 标注相关错误 (27xxx) ====================
    ANNOTATION_NOT_FOUND(27001, "标注不存在"),
    ANNOTATION_TYPE_INVALID(27002, "标注类型无效"),
    ANNOTATION_GEOMETRY_INVALID(27003, "标注几何数据无效"),
    ANNOTATION_REVIEW_STATUS_INVALID(27004, "标注审核状态无效"),
    ANNOTATION_CREATION_SOURCE_INVALID(27005, "标注创建来源无效"),
    ANNOTATION_LOD_LEVEL_INVALID(27006, "LOD层级无效（0-5）"),
    
    // ==================== 权限相关错误 (28xxx) ====================
    PERMISSION_DENIED(28001, "权限不足"),
    TOKEN_EXPIRED(28002, "令牌已过期"),
    TOKEN_INVALID(28003, "令牌无效"),
    USER_NOT_FOUND(28004, "用户不存在"),
    USER_DISABLED(28005, "用户已被禁用"),
    
    // ==================== 系统错误 (29xxx) ====================
    SYSTEM_ERROR(29001, "系统内部错误"),
    DATABASE_ERROR(29002, "数据库操作失败"),
    FILE_UPLOAD_FAILED(29003, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(29004, "文件下载失败"),
    EXTERNAL_SERVICE_ERROR(29005, "外部服务调用失败");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    BizErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

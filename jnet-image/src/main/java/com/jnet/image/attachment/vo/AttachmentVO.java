package com.jnet.image.attachment.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
* 附件表
* @TableName ta_attachment
*/
@Data
@Builder
public class AttachmentVO implements Serializable {

    /**
    * 主键id
    */
    @NotNull(message="[主键id]不能为空")
    private Long attachmentId;
    /**
    * 附件名称
    */

    private String attachmentName;
    /**
    * 附件编号
    */

    private String attachmentCode;
    /**
     * 附件大小
     */

    private Long attachmentSize;
    /**
    * 附件位置
    */

    private String attachmentPath;
    /**
    * MD5值
    */

    private String attachmentMd5;
    /**
    * 文件后缀名
    */

    private String attachmentExt;
    /**
    * 创建者
    */
    @NotNull(message="[创建者]不能为空")

    private Long createBy;
    /**
    * 创建时间
    */
    @NotNull(message="[创建时间]不能为空")
    private Date createTime;
    /**
    * 更新者
    */
    @NotNull(message="[更新者]不能为空")
    private Long updateBy;
    /**
    * 更新时间
    */
    @NotNull(message="[更新时间]不能为空")
    private Date updateTime;

}

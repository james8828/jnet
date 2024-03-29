package com.jnet.image.attachment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 附件表
 * @TableName ta_attachment
 */
@TableName(value ="img_attachment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attachment implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
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
    private Long createBy;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新者
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Attachment other = (Attachment) that;
        return (this.getAttachmentId() == null ? other.getAttachmentId() == null : this.getAttachmentId().equals(other.getAttachmentId()))
            && (this.getAttachmentName() == null ? other.getAttachmentName() == null : this.getAttachmentName().equals(other.getAttachmentName()))
            && (this.getAttachmentCode() == null ? other.getAttachmentCode() == null : this.getAttachmentCode().equals(other.getAttachmentCode()))
            && (this.getAttachmentPath() == null ? other.getAttachmentPath() == null : this.getAttachmentPath().equals(other.getAttachmentPath()))
            && (this.getAttachmentMd5() == null ? other.getAttachmentMd5() == null : this.getAttachmentMd5().equals(other.getAttachmentMd5()))
            && (this.getAttachmentExt() == null ? other.getAttachmentExt() == null : this.getAttachmentExt().equals(other.getAttachmentExt()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getAttachmentId() == null) ? 0 : getAttachmentId().hashCode());
        result = prime * result + ((getAttachmentName() == null) ? 0 : getAttachmentName().hashCode());
        result = prime * result + ((getAttachmentCode() == null) ? 0 : getAttachmentCode().hashCode());
        result = prime * result + ((getAttachmentPath() == null) ? 0 : getAttachmentPath().hashCode());
        result = prime * result + ((getAttachmentMd5() == null) ? 0 : getAttachmentMd5().hashCode());
        result = prime * result + ((getAttachmentExt() == null) ? 0 : getAttachmentExt().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", attachmentId=").append(attachmentId);
        sb.append(", attachmentName=").append(attachmentName);
        sb.append(", attachmentCode=").append(attachmentCode);
        sb.append(", attachmentPath=").append(attachmentPath);
        sb.append(", attachmentMd5=").append(attachmentMd5);
        sb.append(", attachmentExt=").append(attachmentExt);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
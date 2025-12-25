package com.jnet.image.slide.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

/**
 * 切片信息
 * @TableName t_slide
 */
@TableName(value ="t_slide")
@Data
@Builder
public class Slide implements Serializable {
    /**
     * 图像ID
     */
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long slideId;

    /**
     * 无扩展名文件名称
     */
    private String slideName;

    /**
     * 图像的绝对路径
     */
    private String slidePath;

    /**
     * 缩略图URL地址
     */
    private String thumbUrl;

    /**
     * macro图片URL地址
     */
    private String macroUrl;

    /**
     * label图片RUL地址
     */
    private String labelUrl;

    /**
     * 1024缩略图路径（用于缓存、标注缩略图时需要）
     */
    private String cacheUrl;

    /**
     * 原图缩到cache图的倍数
     */
    private String multiple;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 宽度
     */
    private String width;

    /**
     * 高度
     */
    private String height;

    /**
     * 深度
     */
    private String depth;

    /**
     * 大小
     */
    private Long size;

    /**
     * 全局大小
     */
    private String globalSize;

    /**
     * 分辨率
     */
    private String resolvingPower;

    /**
     * 每层的切片个数
     */
    private String tileCountList;

    /**
     * 总层数（小于2则失败）
     */
    private Integer levelCount;

    /**
     * x轴分辨率
     */
    private String resolutionX;

    /**
     * y轴分辨率
     */
    private String resolutionY;

    /**
     * 原放大倍数
     */
    private Integer sourceLens;

    /**
     * 处理状态，1-上传中;2-解析中;3-可用;4-解析失败;5上传失败
     */
    private Integer status;

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

    /**
     * 删除标志（false代表存在 true代表删除）
     */
    private Boolean delFlag;

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
        Slide other = (Slide) that;
        return (this.getSlideId() == null ? other.getSlideId() == null : this.getSlideId().equals(other.getSlideId()))
            && (this.getSlideName() == null ? other.getSlideName() == null : this.getSlideName().equals(other.getSlideName()))
            && (this.getSlidePath() == null ? other.getSlidePath() == null : this.getSlidePath().equals(other.getSlidePath()))
            && (this.getThumbUrl() == null ? other.getThumbUrl() == null : this.getThumbUrl().equals(other.getThumbUrl()))
            && (this.getMacroUrl() == null ? other.getMacroUrl() == null : this.getMacroUrl().equals(other.getMacroUrl()))
            && (this.getLabelUrl() == null ? other.getLabelUrl() == null : this.getLabelUrl().equals(other.getLabelUrl()))
            && (this.getCacheUrl() == null ? other.getCacheUrl() == null : this.getCacheUrl().equals(other.getCacheUrl()))
            && (this.getMultiple() == null ? other.getMultiple() == null : this.getMultiple().equals(other.getMultiple()))
            && (this.getFormat() == null ? other.getFormat() == null : this.getFormat().equals(other.getFormat()))
            && (this.getWidth() == null ? other.getWidth() == null : this.getWidth().equals(other.getWidth()))
            && (this.getHeight() == null ? other.getHeight() == null : this.getHeight().equals(other.getHeight()))
            && (this.getDepth() == null ? other.getDepth() == null : this.getDepth().equals(other.getDepth()))
            && (this.getSize() == null ? other.getSize() == null : this.getSize().equals(other.getSize()))
            && (this.getGlobalSize() == null ? other.getGlobalSize() == null : this.getGlobalSize().equals(other.getGlobalSize()))
            && (this.getResolvingPower() == null ? other.getResolvingPower() == null : this.getResolvingPower().equals(other.getResolvingPower()))
            && (this.getTileCountList() == null ? other.getTileCountList() == null : this.getTileCountList().equals(other.getTileCountList()))
            && (this.getLevelCount() == null ? other.getLevelCount() == null : this.getLevelCount().equals(other.getLevelCount()))
            && (this.getResolutionX() == null ? other.getResolutionX() == null : this.getResolutionX().equals(other.getResolutionX()))
            && (this.getResolutionY() == null ? other.getResolutionY() == null : this.getResolutionY().equals(other.getResolutionY()))
            && (this.getSourceLens() == null ? other.getSourceLens() == null : this.getSourceLens().equals(other.getSourceLens()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreateBy() == null ? other.getCreateBy() == null : this.getCreateBy().equals(other.getCreateBy()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateBy() == null ? other.getUpdateBy() == null : this.getUpdateBy().equals(other.getUpdateBy()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getDelFlag() == null ? other.getDelFlag() == null : this.getDelFlag().equals(other.getDelFlag()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSlideId() == null) ? 0 : getSlideId().hashCode());
        result = prime * result + ((getSlideName() == null) ? 0 : getSlideName().hashCode());
        result = prime * result + ((getSlidePath() == null) ? 0 : getSlidePath().hashCode());
        result = prime * result + ((getThumbUrl() == null) ? 0 : getThumbUrl().hashCode());
        result = prime * result + ((getMacroUrl() == null) ? 0 : getMacroUrl().hashCode());
        result = prime * result + ((getLabelUrl() == null) ? 0 : getLabelUrl().hashCode());
        result = prime * result + ((getCacheUrl() == null) ? 0 : getCacheUrl().hashCode());
        result = prime * result + ((getMultiple() == null) ? 0 : getMultiple().hashCode());
        result = prime * result + ((getFormat() == null) ? 0 : getFormat().hashCode());
        result = prime * result + ((getWidth() == null) ? 0 : getWidth().hashCode());
        result = prime * result + ((getHeight() == null) ? 0 : getHeight().hashCode());
        result = prime * result + ((getDepth() == null) ? 0 : getDepth().hashCode());
        result = prime * result + ((getSize() == null) ? 0 : getSize().hashCode());
        result = prime * result + ((getGlobalSize() == null) ? 0 : getGlobalSize().hashCode());
        result = prime * result + ((getResolvingPower() == null) ? 0 : getResolvingPower().hashCode());
        result = prime * result + ((getTileCountList() == null) ? 0 : getTileCountList().hashCode());
        result = prime * result + ((getLevelCount() == null) ? 0 : getLevelCount().hashCode());
        result = prime * result + ((getResolutionX() == null) ? 0 : getResolutionX().hashCode());
        result = prime * result + ((getResolutionY() == null) ? 0 : getResolutionY().hashCode());
        result = prime * result + ((getSourceLens() == null) ? 0 : getSourceLens().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreateBy() == null) ? 0 : getCreateBy().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateBy() == null) ? 0 : getUpdateBy().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getDelFlag() == null) ? 0 : getDelFlag().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", slideId=").append(slideId);
        sb.append(", slideName=").append(slideName);
        sb.append(", slidePath=").append(slidePath);
        sb.append(", thumbUrl=").append(thumbUrl);
        sb.append(", macroUrl=").append(macroUrl);
        sb.append(", labelUrl=").append(labelUrl);
        sb.append(", cacheUrl=").append(cacheUrl);
        sb.append(", multiple=").append(multiple);
        sb.append(", format=").append(format);
        sb.append(", width=").append(width);
        sb.append(", height=").append(height);
        sb.append(", depth=").append(depth);
        sb.append(", size=").append(size);
        sb.append(", globalSize=").append(globalSize);
        sb.append(", resolvingPower=").append(resolvingPower);
        sb.append(", tileCountList=").append(tileCountList);
        sb.append(", levelCount=").append(levelCount);
        sb.append(", resolutionX=").append(resolutionX);
        sb.append(", resolutionY=").append(resolutionY);
        sb.append(", sourceLens=").append(sourceLens);
        sb.append(", status=").append(status);
        sb.append(", createBy=").append(createBy);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateBy=").append(updateBy);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", delFlag=").append(delFlag);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
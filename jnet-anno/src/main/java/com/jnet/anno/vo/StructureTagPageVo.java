package com.jnet.anno.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class StructureTagPageVo {

    @Schema(description = "脏器编号")
    @JsonProperty("organId")
    private String organCode;
    @Schema(description = "中文结构名称")
    private String name;
    @Schema(description = "英文结构名称")
    private String nameEn;
    @Schema(description = "标签名称")
    private String structureTagName;
    @Schema(description = "颜色名称")
    private String color;
    @Schema(description = "颜色值rgb")
    private String rgb;
    @Schema(description = "颜色值HEX")
    private String hex;
    @Schema(description = "标签编号")
    @JsonProperty("number")
    private String structureId;
    @Schema(description = "图层顺序")
    private Integer orderNumber;
    @Schema(description = "创建者名字")
    private String userName;
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Long structureTagId;
}

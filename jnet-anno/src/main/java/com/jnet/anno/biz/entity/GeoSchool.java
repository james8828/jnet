package com.jnet.anno.biz.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.locationtech.jts.geom.Polygon;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author Magic1412
 * @since 2021-01-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("geo_school")
public class GeoSchool implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("fid")
    private Long fid;

    @TableField("geom")
    private Polygon geom;

    @TableField("id")
    private String id;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("addr")
    private String addr;

    @TableField("class1")
    private String class1;

    @TableField("class2")
    private String class2;

    @TableField("class3")
    private String class3;

    @TableField("structure")
    private String structure;

    @TableField("time")
    private String time;


}

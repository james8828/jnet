package com.jnet.api.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单表
 * @TableName sys_menu
 */
@TableName(value ="sys_menu")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Menu implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long menuId;            // 资源ID（主键）

    private String menuName;        // 资源名称（如：用户管理）

    private String path;            // 路由地址（如：/user/list）

    private String component;       // 前端组件路径（如：system/user/index.vue）

    private Boolean visible;        // 是否显示在菜单栏（菜单可见性）

    private Boolean enabled;        // 是否启用该资源

    private String perms;           // 权限标识（如：user:list, user:create）

    private String icon;            // 图标（菜单用）

    private Integer type;           // 资源类型（0=目录，1=菜单，2=按钮，3=接口）

    private Long parentId;          // 父级资源ID（用于构建树形结构）

    private String remark;          // 备注说明

    private Integer orderNum;       // 显示顺序

    private Date createTime;

    private Date updateTime;

    private Long createBy;

    private Long updateBy;

    private Boolean delFlag;

    @TableField(exist = false)
    private List<Menu> children;
}
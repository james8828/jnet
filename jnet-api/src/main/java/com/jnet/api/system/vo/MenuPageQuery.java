package com.jnet.api.system.vo;

import com.jnet.api.vo.PageQueryVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/19 14:00:15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuPageQuery extends PageQueryVO {

    /**
     * 菜单名
     */
    private String menuName;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单显隐状态
     */
    private Boolean visible;

    /**
     * 菜单状态（0正常 1停用）
     */
    private Boolean enabled;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 备注
     */
    private String remark;

    private String type;

    private Long parentId;

    private List<Long> roleIds;

}

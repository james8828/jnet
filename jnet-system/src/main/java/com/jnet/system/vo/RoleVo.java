package com.jnet.system.vo;

import lombok.Data;

import java.util.Set;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/6/18 17:41:41
 */
@Data
public class RoleVo {

    private Long roleId;


    private String roleName;

    /**
     * 角色权限字符串
     */
    private String roleKey;

    /**
     * 角色状态
     */
    private Boolean enabled;

    /**
     * del_flag
     */
    private Boolean delFlag;

    /**
     * 备注
     */
    private String remark;

    private Set<Long> menus;
}

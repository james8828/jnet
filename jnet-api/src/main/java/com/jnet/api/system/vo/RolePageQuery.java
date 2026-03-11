package com.jnet.api.system.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.system.domain.Role;
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
public class RolePageQuery {

    private String roleName;

    /**
     * 角色权限字符串
     */
    private String roleKey;

    /**
     * 角色状态
     */
    private Boolean enabled;

    private Long currentPage;

    private Integer pageSize;
}

package com.jnet.system.service;

import com.jnet.api.system.domain.Menu;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.api.system.domain.Role;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author 86186
* @description 针对表【sys_menu(菜单表)】的数据库操作Service
* @createDate 2024-07-19 11:30:48
*/
public interface MenuService extends IService<Menu> {
    Map<Long, Set<Menu>> mapMenuByRoleId(List<Long> roleIds) throws Exception;

    List<Menu> queryMenuByRoleId(List<Long> roleIds) throws Exception;

    Map<Long, Set<Role>> listRoleByMenuId(List<Long> menuIds) throws Exception;
}

package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.RoleMenu;
import com.jnet.system.mapper.RoleMenuMapper;
import com.jnet.system.service.MenuService;
import com.jnet.system.mapper.MenuMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author 86186
* @description 针对表【sys_menu(菜单表)】的数据库操作Service实现
* @createDate 2024-07-19 11:30:48
*/
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu>
    implements MenuService{

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private RoleServiceImpl roleService;

    @Override
    public Map<Long, Set<Menu>> mapMenuByRoleId(List<Long> roleIds) throws Exception {
        Map<Long, Set<Menu>> result = new HashMap<>();
        LambdaQueryWrapper<RoleMenu> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(RoleMenu::getRoleId, roleIds);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(roleMenus)){
            List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
            List<Menu> menus = listByIds(menuIds);
            Map<Long, Menu> menuMap = menus.stream().collect(Collectors.toMap(Menu::getMenuId, menu -> menu));
            Map<Long, List<RoleMenu>> temp =roleMenus.stream().collect(Collectors.groupingBy(RoleMenu::getRoleId));
            temp.forEach((k,v)->{
                if (CollectionUtils.isNotEmpty(v)){
                    Set<Menu> rs = v.stream().map(x-> menuMap.get(x.getRoleId())).collect(Collectors.toSet());
                    result.put(k, rs);
                }
            });
        }
        return result;
    }

    @Override
    public List<Menu> queryMenuByRoleId(List<Long> roleIds) throws Exception {
        LambdaQueryWrapper<RoleMenu> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(RoleMenu::getRoleId, roleIds);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(roleMenus)){
            List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
            List<Menu> menus = listByIds(menuIds);
            return menus;
        }
        return Arrays.asList();
    }

    @Override
    public Map<Long, Set<Role>> listRoleByMenuId(List<Long> menuIds) throws Exception {

        Map<Long, Set<Role>> result = new HashMap<>();
        if (CollectionUtils.isNotEmpty(menuIds)){
            LambdaQueryWrapper<RoleMenu> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.in(RoleMenu::getMenuId, menuIds);
            List<RoleMenu> roleMenus = roleMenuMapper.selectList(queryWrapper);
            if (CollectionUtils.isNotEmpty(roleMenus)){
                List<Long> roleIds = roleMenus.stream().map(RoleMenu::getRoleId).collect(Collectors.toList());
                List<Role> roles = roleService.listByIds(roleIds);
                Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getRoleId, role -> role));
                Map<Long, List<RoleMenu>> temp =roleMenus.stream().collect(Collectors.groupingBy(RoleMenu::getMenuId));
                temp.forEach((k,v)->{
                    if (CollectionUtils.isNotEmpty(v)){
                        Set<Role> rs = v.stream().map(x-> roleMap.get(x.getRoleId())).collect(Collectors.toSet());
                        result.put(k, rs);
                    }
                });
            }
        }
        return result;
    }


}





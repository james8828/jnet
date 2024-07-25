package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.RoleMenu;
import com.jnet.system.mapper.RoleMenuMapper;
import com.jnet.system.service.MenuService;
import com.jnet.system.mapper.MenuMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public Map<Long, Set<Menu>> listMenuByRoleId(List<Long> roleIds) throws Exception {
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
}





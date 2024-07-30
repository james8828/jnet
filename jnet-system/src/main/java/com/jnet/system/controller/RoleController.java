package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.system.service.MenuService;
import com.jnet.system.service.RoleService;
import com.jnet.api.system.vo.RoleQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/22 15:45:31
 */
@Slf4j
@RestController()
@RequestMapping("/v1/role")
public class RoleController {

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;

    @PostMapping("/addOrUpdateRole")
    public R addOrUpdateRole(@RequestBody Role params) throws Exception{
        return roleService.addOrUpdateRole(params);
    }

    @DeleteMapping("/deleteRoleById")
    public R deleteRoleById(@RequestParam("id") Long roleId) throws Exception{
        boolean result = roleService.updateById(Role.builder().roleId(roleId).delFlag(true).build());
        return R.success(result);
    }

    @GetMapping("/getRoleById")
    public R getRoleById(@RequestParam("id") Long  roleId) throws Exception{
        Role role = roleService.getById(roleId);
        Map<Long, Set<Menu>> menuMap = menuService.mapMenuByRoleId(Arrays.asList(roleId));
        role.setMenus(menuMap.get(roleId));
        return R.success(role);
    }

    @PostMapping("/pageRole")
    public R<Page<Role>> pageRole(@RequestBody RoleQuery<Role> query) throws Exception{
        LambdaQueryWrapper<Role> queryWrapper = Wrappers.lambdaQuery(query.getRole());
        if (CollectionUtils.isNotEmpty(query.getRoleIds())){
            queryWrapper.exists("select menu_id from role_menu where role_menu.role_id = sys_role.role_id and role_menu.role_id in (?)",query.getRoleIds());
        }
        Page<Role> page = roleService.page(query,queryWrapper);
        List<Role> roles = page.getRecords();
        List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
        page.convert(role -> {
            try {
                Map<Long, Set<Menu>> menuMap =  menuService.mapMenuByRoleId(roleIds);
                role.setMenus(menuMap.get(role.getRoleId()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return role;
        });
        return R.success(page);
    }

    @PostMapping("/queryRole")
    public R<List<Role>> queryRole(@RequestBody RoleQuery<Role> query) throws Exception{
        LambdaQueryWrapper<Role> queryWrapper = Wrappers.lambdaQuery(query.getRole());
        List<Role> roles = roleService.list(queryWrapper);
        List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
        Map<Long, Set<Menu>> menuMap =  menuService.mapMenuByRoleId(roleIds);
        roles.forEach(role -> role.setMenus(menuMap.get(role.getRoleId())));
        return R.success(roles);
    }
}

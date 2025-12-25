package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.system.constants.SystemConstants;
import com.jnet.system.service.MenuService;
import com.jnet.system.service.RoleService;
import com.jnet.api.system.vo.RolePageQuery;
import com.jnet.system.vo.RoleVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@RequestMapping(SystemConstants.VERSION + "/role")
public class RoleController {

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;

    @PostMapping("/addOrUpdateRole")
    public R addOrUpdateRole(@RequestBody RoleVo params) throws Exception{
        return roleService.addOrUpdateRole(params);
    }

    @DeleteMapping("/delete/{roleId}")
    public R deleteRoleById(@PathVariable("roleId") Long roleId) throws Exception{
        boolean result = roleService.updateById(Role.builder().roleId(roleId).delFlag(true).build());
        return R.success(result);
    }

    @GetMapping("/getRole/{roleId}")
    public R getRoleById(@PathVariable("roleId") Long roleId) throws Exception{
        Role role = roleService.getById(roleId);
        Map<Long, Set<Menu>> menuMap = menuService.mapMenuByRoleId(Arrays.asList(roleId));
        role.setMenus(menuMap.get(roleId));
        return R.success(role);
    }

    @PostMapping("/pageRole")
    public R<Page<Role>> pageRole(@RequestBody RolePageQuery query) throws Exception{
        Page<Role> page = Page.of(query.getCurrentPage(), query.getPageSize());
        roleService.page(page,Wrappers.<Role>lambdaQuery()
                .like(StringUtils.isNotEmpty(query.getRoleName()), Role::getRoleName, query.getRoleName())
                .like(StringUtils.isNotEmpty(query.getRoleKey()), Role::getRoleKey, query.getRoleKey())
                .eq(query.getEnabled()!=null,Role::getEnabled, query.getEnabled()));
        List<Role> roles = page.getRecords();
        List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
        Map<Long, Set<Menu>> menuMap =  menuService.mapMenuByRoleId(roleIds);
        page.convert(role -> {
            role.setMenus(menuMap.get(role.getRoleId()));
            return role;
        });
        return R.success(page);
    }

    @PostMapping("/queryRole")
    public R<List<Role>> queryRole(@RequestBody RolePageQuery query) throws Exception{
        List<Role> roles = roleService.list(Wrappers.<Role>lambdaQuery()
                .like(StringUtils.isNotEmpty(query.getRoleName()), Role::getRoleName, query.getRoleName())
                .like(StringUtils.isNotEmpty(query.getRoleKey()), Role::getRoleKey, query.getRoleKey())
                .eq(query.getEnabled()!=null,Role::getEnabled, query.getEnabled()));
        List<Long> roleIds = roles.stream().map(Role::getRoleId).collect(Collectors.toList());
        Map<Long, Set<Menu>> menuMap =  menuService.mapMenuByRoleId(roleIds);
        roles.forEach(role -> role.setMenus(menuMap.get(role.getRoleId())));
        return R.success(roles);
    }
}

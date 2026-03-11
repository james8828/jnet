package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.feign.Oauth2ServiceClient;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.system.constants.SystemConstants;
import com.jnet.system.service.MenuService;
import com.jnet.api.system.vo.MenuPageQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/22 16:37:07
 */
@Slf4j
@RestController()
@RequestMapping(SystemConstants.VERSION + "/menu")
public class MenuController {

    @Resource
    private MenuService menuService;
    @Resource
    private Oauth2ServiceClient oauth2ServiceClient;

    @PostMapping("/addOrUpdateMenu")
    public R<Boolean> addOrUpdateMenu(@RequestBody Menu params) throws Exception {
        return R.success(menuService.saveOrUpdate(params));
    }

    @DeleteMapping("/{menuId}")
    public R<Boolean> deleteMenuById(@PathVariable("menuId") Long menuId) throws Exception {
        boolean result = menuService.updateById(Menu.builder().menuId(menuId).delFlag(true).build());
        return R.success(result);
    }

    @GetMapping("/{menuId}")
    public R<Menu> getMenuById(@PathVariable("menuId") Long menuId) throws Exception {
        Menu menu = menuService.getById(menuId);
        return R.success(menu);
    }

    @PostMapping("/pageMenu")
    public R<Page<Menu>> pageMenu(@RequestBody MenuPageQuery query) throws Exception {
        Page<Menu> page = Page.of(query.getCurrent(), query.getSize());
        menuService.page(page, Wrappers.<Menu>lambdaQuery()
                .like(StringUtils.isNotEmpty(query.getMenuName()), Menu::getMenuName, query.getMenuName())
                .eq(query.getEnabled()!=null,Menu::getEnabled, query.getEnabled())
                .eq(query.getVisible()!=null,Menu::getVisible, query.getVisible())
                .eq(query.getType()!=null,Menu::getType, query.getType())
                .eq(query.getParentId()!=null,Menu::getParentId, query.getParentId())
                .like(StringUtils.isNotEmpty(query.getPath()),Menu::getPath,query.getPath())
                .like(StringUtils.isNotEmpty(query.getComponent()),Menu::getComponent,query.getComponent()));
        return R.success(page);
    }

    @PostMapping("/queryMenu")
    public R<List<Menu>> queryMenu(@RequestBody MenuPageQuery query) throws Exception {
        List<Menu> menus = menuService.list(Wrappers.<Menu>lambdaQuery());
        List<Menu> resp = buildMenuTree(menus);
        return R.success(resp);
    }

    public List<Menu> buildMenuTree(List<Menu> menus) {
        Map<Long, Menu> menuMap = menus.stream()
                .collect(Collectors.toMap(Menu::getMenuId, m -> m));

        List<Menu> rootMenus = new ArrayList<>();
        menus.forEach(menu -> {
            if (menu.getParentId() == null || menu.getParentId() == 0L) {
                rootMenus.add(menu);
            } else {
                Menu parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        });

        return rootMenus;
    }

    @PostMapping("/listRoleByMenuId")
    public R<Set<Role>> listRoleByMenuId(@RequestBody Long menuId) throws Exception {
        List<Long> menuIds = Arrays.asList(menuId);
        Map<Long, Set<Role>> map = menuService.listRoleByMenuId(menuIds);
        return R.success(map.get(menuId));
    }

    @PostMapping("/queryRolesByMenuId")
    public R<List<Menu>> queryMenuByRoleId(@RequestBody List<Long> roleIds) throws Exception {
        return R.success(menuService.queryMenuByRoleId(roleIds));

    }

}

package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.system.domain.Menu;
import com.jnet.system.service.MenuService;
import com.jnet.api.system.vo.MenuQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;


/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/22 16:37:07
 */
@Slf4j
@RestController()
@RequestMapping("/v1/menu")
public class MenuController {
    
    @Resource
    private MenuService menuService;

    @PostMapping("/addOrUpdateMenu")
    public R addOrUpdateMenu(@RequestBody Menu params) throws Exception{
        return R.success(menuService.saveOrUpdate(params));
    }

    @DeleteMapping("/deleteMenuById")
    public R deleteMenuById(@RequestParam("id") Long menuId) throws Exception{
        boolean result = menuService.updateById(Menu.builder().menuId(menuId).delFlag(true).build());
        return R.success(result);
    }

    @GetMapping("/getMenuById")
    public R getMenuById(@RequestParam("id") Long  menuId) throws Exception{
        Menu menu = menuService.getById(menuId);
        return R.success(menu);
    }

    @PostMapping("/pageMenu")
    public R<Page<Menu>> pageMenu(@RequestBody MenuQuery<Menu> query) throws Exception{
        LambdaQueryWrapper<Menu> queryWrapper = Wrappers.lambdaQuery(query.getMenu());
        Page<Menu> page = menuService.page(query,queryWrapper);
        return R.success(page);
    }

    @PostMapping("/queryMenu")
    public R<List<Menu>> queryMenu(@RequestBody MenuQuery<Menu> query) throws Exception{
        LambdaQueryWrapper<Menu> queryWrapper = Wrappers.lambdaQuery(query.getMenu());
        List<Menu> menus = menuService.list(queryWrapper);
        return R.success(menus);
    }
}

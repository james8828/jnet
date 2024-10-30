package com.jnet.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jnet.api.R;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.User;
import com.jnet.system.service.RoleService;
import com.jnet.system.service.UserService;
import com.jnet.api.system.vo.UserQuery;
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
 * @date 2024/7/19 11:32:47
 */
@Slf4j
@RestController()
@RequestMapping("/v1/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;

    @PostMapping("/addOrUpdateUser")
    public R addOrUpdateUser(@RequestBody User params) throws Exception{
        return userService.addOrUpdateUser(params);
    }

    @DeleteMapping("/deleteUserById")
    public R deleteUser(@RequestParam("id") Long  userId) throws Exception{
        boolean result = userService.updateById(User.builder().userId(userId).delFlag(true).build());
        return R.success(result);
    }

    @GetMapping("/getUserById")
    public R getUserById(@RequestParam("id") Long  userId) throws Exception{
        User user = userService.getById(userId);
        Map<Long, Set<Role>> roleMap = roleService.listRoleByUserId(Arrays.asList(userId));
        user.setRoles(roleMap.get(userId));
        return R.success(user);
    }

    @PostMapping("/pageUser")
    public R<Page<User>> pageUser(@RequestBody UserQuery<User> query) throws Exception{
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(query.getUser());
        if (CollectionUtils.isNotEmpty(query.getRoleIds())){
            queryWrapper.exists("select role_id from user_role where user_role.user_id = sys_user.user_id and user_role.role_id in (?)",query.getRoleIds());
        }
        Page<User> page = userService.page(query,queryWrapper);
        List<User> users = page.getRecords();
        List<Long> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        Map<Long, Set<Role>> roleMap =  roleService.listRoleByUserId(userIds);
        page.convert(user -> {
            try {
                Set<Role> roles = roleMap.get(user.getUserId());
                user.setRoles(roles);
                user.setRoleNames(roles.stream().map(Role::getRoleName).collect(Collectors.joining(",")));
                user.setPassword(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return user;
        });
        return R.success(page);
    }

    @PostMapping("/queryUser")
    public R<List<User>> queryUser(@RequestBody UserQuery<User> query) throws Exception{
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(query.getUser());
        List<User> users = userService.list(queryWrapper);
        List<Long> userIds = users.stream().map(User::getUserId).collect(Collectors.toList());
        Map<Long, Set<Role>> roleMap =  roleService.listRoleByUserId(userIds);
        users.forEach(user -> user.setRoles(roleMap.get(user.getUserId())));
        return R.success(users);
    }

    @GetMapping("/loadUserByUsername")
    public User loadUserByUsername(@RequestParam("username") String username) throws Exception{
        return userService.loadUserByUsername(username);
    }
}

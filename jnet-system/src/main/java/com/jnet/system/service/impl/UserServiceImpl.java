package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.R;
import com.jnet.system.constants.SystemConstants;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.domain.UserRole;
import com.jnet.system.exception.SystemException;
import com.jnet.system.mapper.UserRoleMapper;
import com.jnet.system.service.RoleService;
import com.jnet.system.service.UserService;
import com.jnet.system.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 86186
 * @description 针对表【sys_user】的数据库操作Service实现
 * @createDate 2024-07-19 11:30:48
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private RoleService roleService;

    /**
     * @param params
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public R addOrUpdateUser(User params) throws Exception {
        params.setPassword(passwordEncoder.encode(params.getPassword()));
        boolean flag = saveOrUpdate(params);
        if (!flag) {
            log.error("{}:[{}]", SystemConstants.ADD_USER_ERROR, params);
            throw new SystemException(SystemConstants.ADD_USER_ERROR);
        }
        Set<Role> roleList = params.getRoles();
        //删除历史用户角色关系，添加新的用户角色关系
        if (CollectionUtils.isNotEmpty(roleList)) {
            LambdaQueryWrapper<UserRole> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(UserRole::getUserId, params.getUserId());
            Integer delResult = userRoleMapper.delete(queryWrapper);
            List<UserRole> newUserRoles = roleList.stream()
                    .map(role -> UserRole.builder().userId(params.getUserId()).roleId(role.getRoleId()).build())
                    .collect(Collectors.toList());
            for (UserRole userRole : newUserRoles) {
                userRoleMapper.insert(userRole);
            }
        }
        log.info("{}:[{}]", SystemConstants.ADD_USER_SUCCESS, params);
        return R.success();
    }

    @Override
    public User loadUserByUsername(String username) throws Exception {
        User user = getOne(Wrappers.<User>lambdaQuery().eq(User::getUserName, username).eq(User::getEnabled, true).eq(User::getDelFlag, false));
        if (user == null) {
            return null;
        }
        Map<Long, Set<Role>> roleMap = roleService.listRoleByUserId(Arrays.asList(user.getUserId()));
        user.setRoles(roleMap.get(user.getUserId()));
        return user;
    }
}





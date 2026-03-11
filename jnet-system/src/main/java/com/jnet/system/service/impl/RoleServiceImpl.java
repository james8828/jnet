package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.R;
import com.jnet.system.constants.SystemConstants;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.RoleMenu;
import com.jnet.api.system.domain.UserRole;
import com.jnet.system.exception.SystemException;
import com.jnet.system.mapper.RoleMenuMapper;
import com.jnet.system.mapper.UserRoleMapper;
import com.jnet.system.service.RoleMenuService;
import com.jnet.system.service.RoleService;
import com.jnet.system.mapper.RoleMapper;
import com.jnet.system.vo.RoleVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author 86186
* @description 针对表【sys_role(角色表)】的数据库操作Service实现
* @createDate 2024-07-19 11:30:48
*/
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService{

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private RoleMenuService roleMenuService;
    @Resource
    private UserRoleMapper userRoleMapper;

    @Transactional
    @Override
    public R addOrUpdateRole(RoleVo params) throws Exception {
        Role role = new Role();
        role.setRoleId(params.getRoleId());
        role.setRoleName(params.getRoleName());
        role.setRoleKey(params.getRoleKey());
        role.setEnabled(params.getEnabled());
        if (role.getRoleId()!=null){
            // todo
            role.setUpdateTime(new Date());
        }
        boolean flag = saveOrUpdate(role);
        if (!flag){
            log.error("{}:[{}]",SystemConstants.ADD_ROLE_ERROR,params);
            throw new SystemException(SystemConstants.ADD_ROLE_ERROR);
        }
        Set<Long> menuIds = params.getMenus();
        //删除历史角色菜单关系，添加新的角色菜单关系
        if (CollectionUtils.isNotEmpty(menuIds)){
            Integer delResult = roleMenuMapper.delete(Wrappers.<RoleMenu>lambdaQuery()
                    .eq(RoleMenu::getRoleId, params.getRoleId()));
            List<RoleMenu> roleMenus = menuIds.stream()
                    .map(id-> RoleMenu.builder().roleId(role.getRoleId()).menuId(id).build() )
                    .collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenus);
        }
        log.info("{}:[{}]",SystemConstants.ADD_ROLE_SUCCESS,params);
        return R.success();
    }

    @Override
    public Map<Long, Set<Role>> listRoleByUserId(List<Long> userIds) throws Exception {
        Map<Long, Set<Role>> userRoleMap = new HashMap<>();
        LambdaQueryWrapper<UserRole> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(UserRole::getUserId, userIds);
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(userRoles)){
            List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            List<Role> roles = listByIds(roleIds);
            // 使用Stream API将List转换为Map，其中key是Role的id，value是Role对象本身
            Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getRoleId, role -> role));
            Map<Long, List<UserRole>> temp =userRoles.stream().collect(Collectors.groupingBy(UserRole::getUserId));
            temp.forEach((k,v)->{
                if (CollectionUtils.isNotEmpty(v)){
                    Set<Role> rs = v.stream().map(x-> roleMap.get(x.getRoleId())).collect(Collectors.toSet());
                    userRoleMap.put(k, rs);
                }
            });
        }
        return userRoleMap;
    }
}





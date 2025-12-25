package com.jnet.system.service;

import com.jnet.api.R;
import com.jnet.api.system.domain.Role;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.system.vo.RoleVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author 86186
* @description 针对表【sys_role(角色表)】的数据库操作Service
* @createDate 2024-07-19 11:30:48
*/
public interface RoleService extends IService<Role> {

    R addOrUpdateRole(RoleVo params)throws Exception;
    Map<Long, Set<Role>> listRoleByUserId(List<Long> userIds)throws Exception;
}

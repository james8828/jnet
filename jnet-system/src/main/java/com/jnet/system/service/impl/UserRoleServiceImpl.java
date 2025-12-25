package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.api.system.domain.UserRole;
import com.jnet.system.service.UserRoleService;
import com.jnet.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

/**
* @author 86186
* @description 针对表【sys_user_role】的数据库操作Service实现
* @createDate 2024-07-19 11:30:48
*/
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>
    implements UserRoleService{

}





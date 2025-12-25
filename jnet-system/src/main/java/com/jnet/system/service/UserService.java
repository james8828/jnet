package com.jnet.system.service;

import com.jnet.api.R;
import com.jnet.api.system.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

/**
* @author 86186
* @description 针对表【sys_user】的数据库操作Service
* @createDate 2024-07-19 11:30:48
*/
public interface UserService extends IService<User> {
     R addOrUpdateUser(User params)throws Exception;

     User loadUserByUsername(@RequestParam("username") String username) throws Exception;
}

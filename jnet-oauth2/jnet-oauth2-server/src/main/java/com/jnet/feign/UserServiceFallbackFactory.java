package com.jnet.feign;

import com.jnet.api.R;
import com.jnet.api.system.auth.UserDetailsCustom;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.vo.UserQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * userService降级工场
 *
 * @author james
 * @date 2024/07/18
 */
@Slf4j
public class UserServiceFallbackFactory implements FallbackFactory<UserService> {

    @Override
    public UserService create(Throwable throwable) {
        return new UserService() {
            @Override
            public R<List<User>> queryUser(@RequestBody UserQuery<User> query){
                log.error("通过用户名查询用户异常:{}", query.getUserName(), throwable);
                return R.fail("通过用户名查询用户异常:" + query.getUserName());
            }
            @Override
            public UserDetailsCustom loadUserByUsername(@RequestParam("username") String username) {
                log.error("通过用户名查询用户异常:{}", username, throwable);
                throw new UsernameNotFoundException("通过用户名查询用户异常:" + username);
            }
        };
    }
}

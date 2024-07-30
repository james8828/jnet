package com.jnet.gateway.resource.server.feign;

import com.jnet.api.R;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.vo.UserQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * userService降级工场
 *
 * @author james
 * @date 2024/07/18
 */
@Slf4j
public class SystemServiceFallbackFactory implements FallbackFactory<SystemService> {

    @Override
    public SystemService create(Throwable throwable) {
        return new SystemService() {
            @Override
            public R<List<User>> queryUser(@RequestBody UserQuery<User> query){
                log.error("通过用户名查询用户异常:{}", query.getUserName(), throwable);
                return R.fail("通过用户名查询用户异常:" + query.getUserName());
            }
            @Override
            public User loadUserByUsername(@RequestParam("username") String username) {
                log.error("通过用户名查询用户异常:{}", username, throwable);
                throw new UsernameNotFoundException("通过用户名查询用户异常:" + username);
            }

            public R<Set<Role>> queryRolesByMenuId(@RequestBody Long menuId) throws Exception{
                log.error("使用菜单id查询角色失败",throwable);
                return R.fail("使用菜单id查询角色失败");
            }

            @Override
            public R<List<Menu>> queryMenuByRoleId(List<Long> roleIds) throws Exception {
                log.error("使用角色id查询菜单失败",throwable);
                return R.fail("使用角色id查询菜单失败");
            }
        };
    }
}

package com.jnet.gateway.resource.server.feign;

import com.jnet.api.system.domain.User;
import com.jnet.common.core.security.bean.GrantedAuthorityCustom;
import com.jnet.common.core.security.bean.UserDetailsCustom;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/26 16:33:25
 */
@Service
public class UserServiceCustom implements UserDetailsService {

    @Resource
    private UserService userService;

    @Override
    public UserDetailsCustom loadUserByUsername(String username) {
        User user = userService.loadUserByUsername(username);
        if (user == null) {
            return null;
        }
        UserDetailsCustom userDetailsCustom = UserDetailsCustom.builder()
                .accountNonExpired(false)
                .accountNonLocked(false)
                .authorities(user.getRoles().stream().map(role -> GrantedAuthorityCustom.builder().authority(role.getRoleKey()).build()).collect(Collectors.toSet()))
                .credentialsNonExpired(false)
                .enabled(user.getEnabled())
                .password(user.getPassword())
                .username(user.getUserName())
                .build();
        return userDetailsCustom;
    }
}

package com.jnet.oauth2.server.feign;


import com.jnet.api.system.domain.User;
import com.jnet.api.feign.SystemServiceClient;
import com.jnet.common.core.security.bean.GrantedAuthorityCustom;
import com.jnet.common.core.security.bean.UserDetailsCustom;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/26 16:33:25
 */
@Slf4j
@Service
public class UserServiceCustom implements UserDetailsService {

    @Resource
    private SystemServiceClient systemService;

    @Override
    public UserDetailsCustom loadUserByUsername(String username) {
        User user = null;
        try {
            user = systemService.loadUserByUsername(username);
        } catch (Exception e) {
            if (log.isDebugEnabled()){
                e.printStackTrace();
            }
            log.error("loadUserByUsername error,[{}]", e);
            throw new UsernameNotFoundException(e.getMessage());
        }
        if (user == null||user.getEnabled()==null) {
            return null;
        }
        UserDetailsCustom userDetailsCustom = UserDetailsCustom.builder()
                .accountNonExpired(false)
                .accountNonLocked(false)
                .credentialsNonExpired(false)
                .enabled(user.getEnabled())
                .password(user.getPassword())
                .username(user.getUserName())
                .userId(user.getUserId())
                .build();
        if (CollectionUtils.isNotEmpty(user.getRoles())){
            userDetailsCustom.setAuthorities(user.getRoles().stream().map(role -> GrantedAuthorityCustom.builder().authority(role.getRoleKey()).build()).collect(Collectors.toSet()));
        }
        return userDetailsCustom;
    }
}

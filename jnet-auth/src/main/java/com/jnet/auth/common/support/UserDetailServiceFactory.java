package com.jnet.auth.common.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jnet.auth.constant.SecurityConstants;
import com.jnet.auth.exception.CustomOAuth2AuthenticationException;
import com.jnet.auth.util.AuthUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户service工厂
 *
 * @author zlt
 * @version 1.0
 * @date 2021/7/24
 * <p>

 */
@Slf4j
@Service
public class UserDetailServiceFactory {
    private static final String ERROR_MSG = "Cannot find the implementation class for the account type {}";

    @Resource
    private List<CustomUserDetailsService> userDetailsServices;

    public CustomUserDetailsService getService(Authentication authentication) {
        String accountType = AuthUtils.getAccountType(authentication);
        return this.getService(accountType);
    }

    public CustomUserDetailsService getService(String accountType) {
        if (StrUtil.isEmpty(accountType)) {
            accountType = SecurityConstants.DEF_ACCOUNT_TYPE;
        }
        log.info("UserDetailServiceFactory.getService:{}", accountType);
        if (CollUtil.isNotEmpty(userDetailsServices)) {
            for (CustomUserDetailsService userService : userDetailsServices) {
                if (userService.supports(accountType)) {
                    return userService;
                }
            }
        }
        throw new CustomOAuth2AuthenticationException(StrUtil.format(ERROR_MSG, accountType));
    }
}

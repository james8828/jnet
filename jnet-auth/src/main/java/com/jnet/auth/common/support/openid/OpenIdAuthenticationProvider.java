package com.jnet.auth.common.support.openid;

import com.jnet.auth.common.support.UserDetailServiceFactory;
import com.jnet.auth.common.token.OpenIdAuthenticationToken;
import com.jnet.auth.exception.CustomOAuth2AuthenticationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import com.jnet.auth.common.support.base.BaseAuthenticationProvider;

/**
 * @author: zlt
 * @date: 2023/11/18
 * <p>
 * Blog: https://zlt2000.gitee.io
 * Github: https://github.com/zlt2000
 */
@Setter
@Getter
public class OpenIdAuthenticationProvider extends BaseAuthenticationProvider {
    private UserDetailServiceFactory userDetailsServiceFactory;

    public OpenIdAuthenticationProvider(OAuth2AuthorizationService authorizationService
            , OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator
            , UserDetailServiceFactory userDetailsServiceFactory) {
        super(authorizationService, tokenGenerator);
        Assert.notNull(userDetailsServiceFactory, "userDetailsServiceFactory cannot be null");
        this.userDetailsServiceFactory = userDetailsServiceFactory;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OpenIdAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected Authentication getPrincipal(Authentication authentication) {
        OpenIdAuthenticationToken authToken = (OpenIdAuthenticationToken) authentication;
        String openId = (String) authToken.getPrincipal();
        UserDetails userDetails;
        try {
            userDetails = userDetailsServiceFactory.getService(authToken).loadUserByUserId(openId);
        } catch (AuthenticationException e) {
            throw new CustomOAuth2AuthenticationException(e.getMessage());
        }
        if (userDetails == null) {
            throw new CustomOAuth2AuthenticationException("openId错误");
        }
        return new OpenIdAuthenticationToken(userDetails, userDetails.getAuthorities());
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return OpenIdAuthenticationToken.GRANT_TYPE;
    }
}
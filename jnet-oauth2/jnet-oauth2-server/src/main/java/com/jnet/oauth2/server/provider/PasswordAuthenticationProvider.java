package com.jnet.oauth2.server.provider;

import com.jnet.common.core.security.bean.UserDetailsCustom;
import com.jnet.oauth2.server.token.PasswordAuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

/**
 *
 * @author james
 * @version 1.0
 * @date 2023/11/18
 * @description 认证核心类，用户名密码 provider
 */
@Setter
@Getter
public class PasswordAuthenticationProvider extends BaseAuthenticationProvider {
    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    public PasswordAuthenticationProvider(OAuth2AuthorizationService authorizationService
            , OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator
            , UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super(authorizationService, tokenGenerator);
        Assert.notNull(userDetailsService, "userDetailsService cannot be null");
        Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    protected Authentication getPrincipal(Authentication authentication) {
        PasswordAuthenticationToken authToken = (PasswordAuthenticationToken) authentication;
        String username = (String) authToken.getPrincipal();
        String password = authToken.getCredentials();
        UserDetailsCustom userDetails;
        try {
            userDetails = (UserDetailsCustom) this.userDetailsService.loadUserByUsername(username);

        } catch (AuthenticationException e) {
            throw new OAuth2AuthenticationException(e.getMessage());
        }
        if (userDetails == null || !this.passwordEncoder.matches(password, userDetails.getPassword())) {
            String msg = this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Access is denied");
            throw new OAuth2AuthenticationException(msg);
        }
        return new PasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
    }

    @Override
    protected AuthorizationGrantType grantType() {
        return PasswordAuthenticationToken.GRANT_TYPE;
    }
}

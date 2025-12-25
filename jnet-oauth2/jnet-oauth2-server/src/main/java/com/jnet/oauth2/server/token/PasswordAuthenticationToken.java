package com.jnet.oauth2.server.token;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Collection;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/8 15:31:30
 */
@Getter
public class PasswordAuthenticationToken extends BaseAuthenticationToken {
    public static final AuthorizationGrantType GRANT_TYPE = new  AuthorizationGrantType("password");
    private final Object principal;
    private final String credentials;

    public PasswordAuthenticationToken(String username, String password) {
        super(GRANT_TYPE);
        this.principal = username;
        this.credentials = password;
        super.setAuthenticated(true);
        super.setClientPrincipal(this);
    }

    public PasswordAuthenticationToken(UserDetails user, String password, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = user;
        this.credentials = password;
    }
}

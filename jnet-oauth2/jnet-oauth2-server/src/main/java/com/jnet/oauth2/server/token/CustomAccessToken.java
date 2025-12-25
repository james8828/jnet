package com.jnet.oauth2.server.token;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/8 15:31:30
 */
public class CustomAccessToken extends OAuth2Authorization.Token<OAuth2AccessToken> {
    public CustomAccessToken(OAuth2AccessToken token, Map metadata) {
        super(token, metadata);
    }
}

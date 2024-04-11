package com.jnet.auth.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class CustomOAuth2AuthenticationException extends OAuth2AuthenticationException {
    public CustomOAuth2AuthenticationException(String msg) {
        super(new OAuth2Error(msg), msg);
    }
}

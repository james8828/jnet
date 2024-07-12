package com.jnet.oauth2.server.config;

import com.jnet.oauth2.server.token.RedisOAuth2AuthorizationService;
import com.jnet.oauth2.server.converter.PasswordAuthenticationConverter;
import com.jnet.oauth2.server.properties.SecurityProperties;
import com.jnet.oauth2.server.provider.PasswordAuthenticationProvider;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description OAuth2组件配置
 * @date 2024/7/9 14:34:45
 */
public class OAuth2ComponentConfig {

    public static final String  ENCODING_ID = "bcrypt";

    /**
     * AuthorizationService 使用redis 存储
     * @param securityProperties
     * @param redisson
     * @return
     */
    @Bean
    public OAuth2AuthorizationService oAuth2AuthorizationService(SecurityProperties securityProperties, RedissonClient redisson) {
        return new RedisOAuth2AuthorizationService(securityProperties, redisson);
    }

    /**
     * authenticationProvider(): 添加一个用于验证 OAuth2ClientAuthenticationToken 的 AuthenticationProvider（主处理器）。
     * @param userDetailsService
     * @param authorizationService
     * @param tokenGenerator
     * @param passwordEncoder
     * @return
     */
    @Bean
    public PasswordAuthenticationProvider passwordAuthenticationProvider(UserDetailsService userDetailsService, OAuth2AuthorizationService authorizationService, OAuth2TokenGenerator tokenGenerator, PasswordEncoder passwordEncoder) {
        return new PasswordAuthenticationProvider(authorizationService, tokenGenerator, userDetailsService, passwordEncoder);
    }

    @Bean
    public AuthenticationConverter passwordAuthenticationConverter(RegisteredClientRepository registeredClientRepository) {
        return new PasswordAuthenticationConverter(registeredClientRepository);
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("ldap", new LdapShaPasswordEncoder());
        encoders.put("MD4", new Md4PasswordEncoder());
        encoders.put("MD5", new MessageDigestPasswordEncoder("MD5"));
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("pbkdf2",Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        encoders.put("SHA-1", new MessageDigestPasswordEncoder("SHA-1"));
        encoders.put("SHA-256", new MessageDigestPasswordEncoder("SHA-256"));
        encoders.put("sha256", new StandardPasswordEncoder());
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        Assert.isTrue(encoders.containsKey(ENCODING_ID), ENCODING_ID + " is not found in idToPasswordEncoder");
        DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder(ENCODING_ID, encoders);
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(encoders.get(ENCODING_ID));
        return delegatingPasswordEncoder;
    }
}

package com.jnet.system.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.util.Assert;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/22 18:06:35
 */
@Slf4j
@Configuration
public class InitConfig {

    public static final String  ENCODING_ID = "bcrypt";

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

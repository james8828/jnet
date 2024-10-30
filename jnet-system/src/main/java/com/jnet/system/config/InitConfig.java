package com.jnet.system.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.util.Assert;


import javax.sql.DataSource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
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

    @Resource
    private DataSource dataSource;
    @PostConstruct
    public void init() throws Exception {
        log.info("dataSource:{}",dataSource);
        Connection connection = dataSource.getConnection();
        /*ScriptRunner runner = new ScriptRunner(connection);
        runner.setErrorLogWriter(null);
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("db/migration/ddl.sql"));*/
        ClassPathResource resource = new ClassPathResource("db/migration/ddl.sql");
        ScriptUtils.executeSqlScript(dataSource.getConnection(), resource);
    }

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

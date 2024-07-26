/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jnet.common.core.security;

import com.jnet.common.core.constants.SecurityConstants;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.authorization.token.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.UUID;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/3 16:36:04
 */
@Slf4j
public class JwtConfiguration {
    private static final SecureRandom secureRandom = new SecureRandom("fixedSeed".getBytes());
    /**
     * OAuth2TokenGenerator 负责从所提供的 OAuth2TokenContext 中的信息生成 OAuth2Token。
     * 生成的 OAuth2Token 主要取决于在 OAuth2TokenContext 中指定的 OAuth2TokenType。
     * 例如，当 OAuth2TokenType 的 value 为：
     * code, 则生成 OAuth2AuthorizationCode。
     * access_token, 则生成 OAuth2AccessToken。
     * refresh_token, 则生成 OAuth2RefreshToken。
     * id_token, 则生成 OidcIdToken。
     * 此外，生成的 OAuth2AccessToken 的格式是不同的，取决于为 RegisteredClient 配置的 TokenSettings.getAccessTokenFormat()。
     * 如果格式是 OAuth2TokenFormat.SELF_CONTAINED（默认），
     * 那么就会生成一个 Jwt。如果格式是 OAuth2TokenFormat.REFERENCE，那么就会生成一个 "opaque" token。
     * @param jwtEncoder
     * @return
     */
    @Bean
    public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder) {
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
        //jwtGenerator.setJwtCustomizer(jwtCustomizer());
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }


    /**
     * 提供JWK源，用于JWT的编码和解码。
     * 使用提供的RSA密钥对，构建包含公钥和私钥的JWK对象。
     *
     * @param keyPair RSA密钥对，用于JWT的签名和验证。
     * @return JWK源，包含RSA密钥对的公钥和私钥信息。
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        // @formatter:off
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        // @formatter:on
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }



    /**
     * 提供基于RSA公钥的JWT解码器。
     *
     * @param keyPair RSA密钥对，仅使用公钥进行JWT解码。
     * @return JWT解码器，用于验证和解析JWT。
     */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }


    /**
     * 提供JWT编码器，使用JWK源。
     *
     * @param jwkSource JWK源，用于JWT的签名。
     * @return JWT编码器，用于生成带签名的JWT。
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * 生成RSA密钥对，用于JWT的签名和验证。
     *
     * @return RSA密钥对，包含公钥和私钥。
     */
    /*@Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048,secureRandom);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }*/

    /**
     * 创建文件命令 keytool -genkey -alias jwt -keyalg RSA -keystore jwt.jks
     * @return
     */
    @Bean
    public KeyPair keyPair() {
        try {
            ClassPathResource classPathResource = new ClassPathResource("jwt.jks");
            InputStream inputStream = classPathResource.getInputStream();
            char[] jksPassword = SecurityConstants.JKS_PASSWORD.toCharArray();
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, jksPassword);
            RSAPrivateCrtKey key = (RSAPrivateCrtKey)keyStore.getKey(SecurityConstants.ALIAS, jksPassword);
            Certificate certificate = keyStore.getCertificate(SecurityConstants.ALIAS);
            PublicKey publicKey = null;
            if (certificate != null) {
                publicKey = certificate.getPublicKey();
            } else if (key != null) {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
                publicKey = KeyFactory.getInstance(SecurityConstants.ALGORITHM).generatePublic(spec);
            }
            return new KeyPair(publicKey, key);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}

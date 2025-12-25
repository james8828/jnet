package com.jnet.oauth2.server.rest;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description 获取RSA公钥接口
 * @date 2024/7/26 11:58:49
 */
@RestController
public class KeyPairController {

    private final KeyPair keyPair;

    public KeyPairController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @GetMapping("/rsa/publicKey")
    public Map<String, Object> getKey() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey key = new RSAKey.Builder(publicKey).build();
        return new JWKSet(key).toJSONObject();
    }

}

package com.jnet.api.feign;

import com.jnet.api.R;
import com.jnet.api.system.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2025/6/16 20:24:23
 */
@Slf4j
public class Oauth2ServiceClientFallbackFactory implements FallbackFactory<Oauth2ServiceClient> {
    @Override
    public Oauth2ServiceClient create(Throwable cause) {
        return new Oauth2ServiceClient() {
            @Override
            public R<User> getJwt() {
                log.error("查询内省端点:{}", cause);
                return R.fail("查询内省端点:" + cause.getMessage());
            }
        };
    }
}

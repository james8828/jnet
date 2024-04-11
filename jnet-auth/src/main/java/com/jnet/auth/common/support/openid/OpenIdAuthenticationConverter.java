package com.jnet.auth.common.support.openid;

import com.jnet.auth.common.token.BaseAuthenticationToken;
import com.jnet.auth.common.token.OpenIdAuthenticationToken;
import com.jnet.auth.util.OAuthEndpointUtils;
import org.springframework.util.MultiValueMap;
import com.jnet.auth.common.support.base.BaseAuthenticationConverter;

import java.util.List;

/**
 * @author: zlt
 * @date: 2023/11/18
 * <p>
 * Blog: https://zlt2000.gitee.io
 * Github: https://github.com/zlt2000
 */
public class OpenIdAuthenticationConverter extends BaseAuthenticationConverter {
    private final static String PARAM_OPENID = "openId";

    @Override
    protected String supportGrantType() {
        return OpenIdAuthenticationToken.GRANT_TYPE.getValue();
    }

    @Override
    protected List<String> paramNames() {
        return List.of(PARAM_OPENID);
    }

    @Override
    protected BaseAuthenticationToken getToken(MultiValueMap<String, String> parameters) {
        String openId = OAuthEndpointUtils.getParam(parameters, PARAM_OPENID);
        return new OpenIdAuthenticationToken(openId);
    }
}

package com.jnet.auth.common.support.password;

import com.jnet.auth.common.token.BaseAuthenticationToken;
import com.jnet.auth.common.token.PasswordAuthenticationToken;
import com.jnet.auth.util.OAuthEndpointUtils;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
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
public class PasswordAuthenticationConverter extends BaseAuthenticationConverter {
    @Override
    protected String supportGrantType() {
        return PasswordAuthenticationToken.GRANT_TYPE.getValue();
    }

    @Override
    protected List<String> paramNames() {
        return List.of(OAuth2ParameterNames.USERNAME, OAuth2ParameterNames.PASSWORD);
    }

    @Override
    protected BaseAuthenticationToken getToken(MultiValueMap<String, String> parameters) {
        String username = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.PASSWORD);
        return new PasswordAuthenticationToken(username, password);
    }
}

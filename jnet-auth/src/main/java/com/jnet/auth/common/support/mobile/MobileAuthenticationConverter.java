package com.jnet.auth.common.support.mobile;

import com.jnet.auth.common.token.BaseAuthenticationToken;
import com.jnet.auth.common.token.MobileAuthenticationToken;
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
public class MobileAuthenticationConverter extends BaseAuthenticationConverter {
    private final static String PARAM_MOBILE= "mobile";

    @Override
    protected String supportGrantType() {
        return MobileAuthenticationToken.GRANT_TYPE.getValue();
    }

    @Override
    protected List<String> paramNames() {
        return List.of(PARAM_MOBILE, OAuth2ParameterNames.PASSWORD);
    }

    @Override
    protected BaseAuthenticationToken getToken(MultiValueMap<String, String> parameters) {
        String mobile = OAuthEndpointUtils.getParam(parameters, PARAM_MOBILE);
        String password = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.PASSWORD);
        return new MobileAuthenticationToken(mobile, password);
    }
}

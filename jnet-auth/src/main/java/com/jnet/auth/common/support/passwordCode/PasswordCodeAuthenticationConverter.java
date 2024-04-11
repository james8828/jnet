package com.jnet.auth.common.support.passwordCode;

import com.jnet.auth.common.token.BaseAuthenticationToken;
import com.jnet.auth.common.token.PasswordCodeAuthenticationToken;
import com.jnet.auth.util.OAuthEndpointUtils;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;
import com.jnet.auth.common.support.base.BaseAuthenticationConverter;

import java.util.List;

/**
 * @author: james
 * @date: 2023/11/18
 */
public class PasswordCodeAuthenticationConverter extends BaseAuthenticationConverter {
    private final static String PARAM_DEVICEID = "deviceId";
    private final static String PARAM_VALIDCODE = "validCode";
    @Override
    protected String supportGrantType() {
        return PasswordCodeAuthenticationToken.GRANT_TYPE.getValue();
    }

    @Override
    protected List<String> paramNames() {
        return List.of(OAuth2ParameterNames.USERNAME, OAuth2ParameterNames.PASSWORD
                , PARAM_DEVICEID, PARAM_VALIDCODE);
    }

    @Override
    protected BaseAuthenticationToken getToken(MultiValueMap<String, String> parameters) {
        String username = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.PASSWORD);
        String deviceId = OAuthEndpointUtils.getParam(parameters, PARAM_DEVICEID);
        String validCode = OAuthEndpointUtils.getParam(parameters, PARAM_VALIDCODE);
        return new PasswordCodeAuthenticationToken(username, password, deviceId, validCode);
    }
}

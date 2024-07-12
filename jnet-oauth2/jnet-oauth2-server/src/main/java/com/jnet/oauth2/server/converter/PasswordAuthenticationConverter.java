package com.jnet.oauth2.server.converter;

import com.jnet.oauth2.server.token.BaseAuthenticationToken;
import com.jnet.oauth2.server.token.PasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * @author: zlt
 * @date: 2023/11/18
 * <p>
 * Blog: https://zlt2000.gitee.io
 * Github: https://github.com/zlt2000
 */
public class PasswordAuthenticationConverter extends BaseAuthenticationConverter {

    private RegisteredClientRepository registeredClientRepository;

    public PasswordAuthenticationConverter(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }
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
        String clientId = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.CLIENT_ID);
        SecurityContextHolder.getContext().setAuthentication(new OAuth2ClientAuthenticationToken(this.registeredClientRepository.findByClientId(clientId), ClientAuthenticationMethod.NONE,null));
        String username = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.PASSWORD);
        return new PasswordAuthenticationToken(username, password);
    }
}

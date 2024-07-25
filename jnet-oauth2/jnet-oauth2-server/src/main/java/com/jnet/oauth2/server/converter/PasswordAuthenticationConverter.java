package com.jnet.oauth2.server.converter;

import com.jnet.oauth2.server.token.BaseAuthenticationToken;
import com.jnet.oauth2.server.token.PasswordAuthenticationToken;
import com.jnet.oauth2.server.util.OAuthEndpointUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.MultiValueMap;

import java.util.List;


/**
 * @author mugw
 * @version 1.0
 * @description 用户密码 AuthenticationConverter（预处理器）
 * @date 2024/7/8 15:31:30
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
        //SecurityContextHolder.getContext().setAuthentication(new OAuth2ClientAuthenticationToken(this.registeredClientRepository.findByClientId(clientId), ClientAuthenticationMethod.NONE,null));
        String username = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.USERNAME);
        String password = OAuthEndpointUtils.getParam(parameters, OAuth2ParameterNames.PASSWORD);
        PasswordAuthenticationToken passwordAuthenticationToken = new PasswordAuthenticationToken(username, password);
        passwordAuthenticationToken.setClientPrincipal(new OAuth2ClientAuthenticationToken(this.registeredClientRepository.findByClientId(clientId), ClientAuthenticationMethod.NONE,null));
        return passwordAuthenticationToken;
    }
}

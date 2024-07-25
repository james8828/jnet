package com.jnet.oauth2.server.converter;

import com.jnet.oauth2.server.token.BaseAuthenticationToken;
import com.jnet.oauth2.server.util.OAuthEndpointUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author mugw
 * @version 1.0
 * @description AuthenticationConverter（预处理器）
 * @date 2024/7/8 15:31:30
 */
public abstract class BaseAuthenticationConverter implements AuthenticationConverter {
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!this.supportGrantType().equals(grantType)) {
            return null;
        }

        MultiValueMap<String, String> parameters = OAuthEndpointUtils.getParameters(request);
        return this.getAuthentication(parameters);
    }

    public Authentication getAuthentication(MultiValueMap<String, String> parameters) {
        Set<String> requestScopes = this.getRequestScopes(parameters);

        Map<String, Object> additionalParameters = getAdditionalParameters(parameters, this.paramNames());
        BaseAuthenticationToken baseToken = this.getToken(parameters);

        if (baseToken.getClientPrincipal()==null){
            Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
            baseToken.setClientPrincipal(clientPrincipal);
        }
        baseToken.setScopes(requestScopes);
        baseToken.setAdditionalParameters(additionalParameters);
        return baseToken;
    };

    public Set<String> getRequestScopes(MultiValueMap<String, String> parameters) {
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        Set<String> requestedScopes = null;
        if (!StringUtils.isEmpty(scope)) {
            requestedScopes = new HashSet<>(
                    Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        } else {
            requestedScopes = Collections.emptySet();
        }
        return requestedScopes;
    }

    public Map<String, Object> getAdditionalParameters(MultiValueMap<String, String> parameters, List<String> paramName) {
        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                    !key.equals(OAuth2ParameterNames.SCOPE)) {
                boolean isAdd = true;
                if (!CollectionUtils.isEmpty(paramName)) {
                    for (String name : paramName) {
                        if (key.equals(name)) {
                            isAdd = false;
                        }
                    }
                }
                if (isAdd) {
                    additionalParameters.put(key, value.get(0));
                }
            }
        });
        return additionalParameters;
    }

    protected abstract String supportGrantType();

    protected abstract List<String> paramNames();

    protected abstract BaseAuthenticationToken getToken(MultiValueMap<String, String> parameters);
}

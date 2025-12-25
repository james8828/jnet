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

package com.jnet.oauth2.server.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnet.api.R;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.core.endpoint.DefaultOAuth2AccessTokenResponseMapConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/8 14:01:09
 */
public class CustomAccessTokenResponseHttpMessageConverter extends OAuth2AccessTokenResponseHttpMessageConverter {
    private Converter<OAuth2AccessTokenResponse, Map<String, Object>> accessTokenResponseParametersConverter = new DefaultOAuth2AccessTokenResponseMapConverter();
    private static final ParameterizedTypeReference<R<Map<String, Object>>> STRING_OBJECT_MAP = new ParameterizedTypeReference<>() {};

    private final ObjectMapper objectMapper;
    public CustomAccessTokenResponseHttpMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void writeInternal(OAuth2AccessTokenResponse tokenResponse, HttpOutputMessage outputMessage)
            throws HttpMessageNotWritableException {
        try {
            Map<String, Object> tokenResponseParameters = this.accessTokenResponseParametersConverter.convert(tokenResponse);
            GenericHttpMessageConverter<Object> jsonMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
            jsonMessageConverter.write(R.success(tokenResponseParameters), STRING_OBJECT_MAP.getType(), MediaType.APPLICATION_JSON, outputMessage);
        }
        catch (Exception ex) {
            throw new HttpMessageNotWritableException(
                    "An error occurred writing the OAuth 2.0 Access Token Response: " + ex.getMessage(), ex);
        }
    }
}


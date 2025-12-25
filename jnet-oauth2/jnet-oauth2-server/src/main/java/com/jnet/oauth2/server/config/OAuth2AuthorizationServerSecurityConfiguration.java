/*
 * Copyright 2021 the original author or authors.
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

package com.jnet.oauth2.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jnet.api.R;
import com.jnet.common.core.security.JwtConfiguration;
import com.jnet.common.core.security.SecurityComponentConfig;
import com.jnet.oauth2.server.authorizationManager.PermissionAuthorizationManager;
import com.jnet.oauth2.server.converter.CustomAccessTokenResponseHttpMessageConverter;
import com.jnet.oauth2.server.provider.PasswordAuthenticationProvider;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationEndpointConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2TokenIntrospectionEndpointConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author mugw
 * @version 1.0
 * @description 认证授权服务配置
 * https://springdoc.cn/spring-authorization-server/core-model-components.html#registered-client
 * @date 2024/7/8 15:31:30
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Import({JwtConfiguration.class,OAuth2ComponentConfig.class, SecurityComponentConfig.class})
public class OAuth2AuthorizationServerSecurityConfiguration {

	private AuthenticationConverter authenticationConverter;

	private PasswordAuthenticationProvider passwordAuthenticationProvider;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,AuthenticationConverter authenticationConverter,
																	  PasswordAuthenticationProvider passwordAuthenticationProvider) throws Exception {
		/**
		 * https://springdoc.cn/spring-authorization-server/configuration-model.html
		 * OAuth2AuthorizationServerConfigurer 提供以下配置选项。
		 *
		 * @Bean
		 * public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		 * 	OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();
		 * 	http.apply(authorizationServerConfigurer);
		 *
		 * 	authorizationServerConfigurer
		 * 		.registeredClientRepository(registeredClientRepository)
		 * 		.authorizationService(authorizationService)
		 * 		.authorizationConsentService(authorizationConsentService)
		 * 		.authorizationServerSettings(authorizationServerSettings)
		 * 		.tokenGenerator(tokenGenerator)
		 * 		.clientAuthentication(clientAuthentication -> { })
		 * 		.authorizationEndpoint(authorizationEndpoint -> { })
		 * 		.tokenEndpoint(tokenEndpoint -> { })
		 * 		.tokenIntrospectionEndpoint(tokenIntrospectionEndpoint -> { })
		 * 		.tokenRevocationEndpoint(tokenRevocationEndpoint -> { })
		 * 		.authorizationServerMetadataEndpoint(authorizationServerMetadataEndpoint -> { })
		 * 		.oidc(oidc -> oidc
		 * 			.providerConfigurationEndpoint(providerConfigurationEndpoint -> { })
		 * 			.userInfoEndpoint(userInfoEndpoint -> { })
		 * 			.clientRegistrationEndpoint(clientRegistrationEndpoint -> { })
		 * 		);
		 *
		 * 	return http.build();
		 * }
		 *
		 * registeredClientRepository(): RegisteredClientRepository （必需的） 用于管理新的和现有的客户。
		 * authorizationService(): 用于管理新的和现有的授权的 OAuth2AuthorizationService。
		 * authorizationConsentService(): OAuth2AuthorizationConsentService ，用于管理新的和现有的授权许可（Consent）。
		 * authorizationServerSettings(): 用于自定义OAuth2授权服务器的 configuration setting 的 AuthorizationServerSettings （必需的）。
		 * tokenGenerator(): OAuth2TokenGenerator，用于生成OAuth2授权服务器支持的令牌。
		 * clientAuthentication(): OAuth2客户端认证的configurer。
		 * authorizationEndpoint(): OAuth2授权端点 的 configurer。
		 * tokenEndpoint(): OAuth2令牌端点 的 configurer。
		 * tokenIntrospectionEndpoint(): OAuth2 Token Introspection endpoint 端点的 configurer。
		 * tokenRevocationEndpoint(): OAuth2 Token Revocation 端点 的 configurer。
		 * authorizationServerMetadataEndpoint(): OAuth2授权服务器元数据端点 的 configurer。
		 * providerConfigurationEndpoint(): OpenID Connect 1.0 Provider 配置端点 的 configurer。
		 * userInfoEndpoint(): OpenID Connect 1.0 UserInfo 端点 的 configurer。
		 * clientRegistrationEndpoint(): OpenID Connect 1.0 客户端注册端点 的 configurer。
		 *
		 * 默认情况下，OAuth2令牌端点、OAuth2令牌内省（Introspection）端点和 OAuth2令牌撤销端点 都需要客户端认证。
		 * 支持的客户端认证方法有 client_secret_basic、client_secret_post、private_key_jwt、client_secret_jwt 和 none（公共客户端）。
		 */
		this.authenticationConverter = authenticationConverter;
		this.passwordAuthenticationProvider = passwordAuthenticationProvider;
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
				//.tokenIntrospectionEndpoint(this::configureIntrospectionEndpoint)//todo 内省
				.authorizationEndpoint(this::configure);
		http.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));
        return http.build();
    }

	private void configure(OAuth2AuthorizationEndpointConfigurer authorizationEndpoint){
		authorizationEndpoint
				.authorizationRequestConverter(authenticationConverter)
				.authorizationResponseHandler(this::configureAuthorizationResponseHandler)
				.errorResponseHandler(this::configureErrorResponseHandler)
				.authenticationProvider(passwordAuthenticationProvider);
	}

	private void configureAuthorizationResponseHandler(HttpServletRequest request, HttpServletResponse response,
						   Authentication authentication)throws IOException {
		OAuth2AccessTokenAuthenticationToken accessTokenAuthentication =
				(OAuth2AccessTokenAuthenticationToken) authentication;

		OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
		OAuth2RefreshToken refreshToken = accessTokenAuthentication.getRefreshToken();
		Map<String, Object> additionalParameters = accessTokenAuthentication.getAdditionalParameters();

		OAuth2AccessTokenResponse.Builder builder =
				OAuth2AccessTokenResponse.withToken(accessToken.getTokenValue())
						.tokenType(accessToken.getTokenType())
						.scopes(accessToken.getScopes());
		if (accessToken.getIssuedAt() != null && accessToken.getExpiresAt() != null) {
			builder.expiresIn(ChronoUnit.SECONDS.between(accessToken.getIssuedAt(), accessToken.getExpiresAt()));
		}
		if (refreshToken != null) {
			builder.refreshToken(refreshToken.getTokenValue());
		}
		if (!CollectionUtils.isEmpty(additionalParameters)) {
			builder.additionalParameters(additionalParameters);
		}
		OAuth2AccessTokenResponse accessTokenResponse = builder.build();
		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
		CustomAccessTokenResponseHttpMessageConverter converter = new CustomAccessTokenResponseHttpMessageConverter(new ObjectMapper());
		converter.write(accessTokenResponse, null, httpResponse);
	}

	private void configureErrorResponseHandler(HttpServletRequest request, HttpServletResponse response,
						   AuthenticationException authException)throws IOException{
		String msg = null;
		if (StringUtils.hasText(authException.getMessage())) {
			msg = authException.getMessage();
		} else if (authException instanceof OAuth2AuthenticationException exception) {
			msg = exception.getError().getErrorCode();
		}
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		try (ServletOutputStream out = response.getOutputStream()){
			ObjectMapper objectMapper = new ObjectMapper();
			R<String> result = R.fail(msg);
			IOUtils.write(objectMapper.writeValueAsString(result), out, StandardCharsets.UTF_8);
			out.flush();
			IOUtils.close(out);
		}
	}

	/**
	 * 内省配置
	 * @param introspectionEndpoint  OAuth2TokenIntrospectionEndpointConfigurer
	 * @throws Exception
	 */
	private void configureIntrospectionEndpoint(OAuth2TokenIntrospectionEndpointConfigurer introspectionEndpoint) {
		introspectionEndpoint.introspectionResponseHandler(this::configureIntrospectionResponseHandler);
	}

	private void configureIntrospectionResponseHandler(HttpServletRequest request, HttpServletResponse response,
													   Authentication authentication){
		// 自定义响应格式（可选）
		ObjectMapper mapper = new ObjectMapper();
		log.info("内省认证成功：{}", authentication);
		/*Map<String, Object> result = new HashMap<>();
		result.put("active", true);
		result.put("username", authentication.getName());
		result.put("scopes", authentication.getScopes());
		result.put("client_id", authentication.getClientRegistrationId());
		result.put("exp", authentication.getPrincipal());*/

		/*ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
		httpResponse.write(mapper.writeValueAsBytes(authentication));*/
	}


	@Bean
	@Order(2)
	public SecurityFilterChain securityFilterChain(HttpSecurity http, PermissionAuthorizationManager customAuthorizationManager) throws Exception {
		http.csrf((csrf) -> csrf.ignoringRequestMatchers("/h2-console/**","/oauth2/token/**","/oauth2/authorize/**"))
				.headers((headers) -> headers.frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin()))
				.authorizeHttpRequests((authorize) -> {
					authorize.requestMatchers("/h2-console/**").permitAll()
							.requestMatchers("/rsa/publicKey/**").permitAll()
							.anyRequest().access(customAuthorizationManager);
				})
				.oauth2ResourceServer((oauth2) -> oauth2.jwt(withDefaults()));
		return http.build();
	}

	/**
	 * id: RegisteredClient 的唯一 ID。
	 * clientId: client id。
	 * clientIdIssuedAt: client id 签发时间。
	 * clientSecret: client secret，该值应使用 Spring Security 的 PasswordEncoder 进行编码。
	 * clientSecretExpiresAt: client secret 过期时间。
	 * clientName: 一个用于描述客户端名称。该名称可能会在某些情况下使用，例如在同意页中显示客户端名称时。
	 * clientAuthenticationMethods: 客户端可能使用的认证方法。支持的值是 client_secret_basic， client_secret_post， private_key_jwt， client_secret_jwt 和 none（ 公共客户端）。
	 * authorizationGrantTypes: 客户端可以使用的 授权许可类型。支持的值是 authorization_code、client_credentials 和 refresh_token。
	 * redirectUris: 注册的 重定向URI，客户端可以在基于重定向的流程中使用—​例如，authorization_code 授权。
	 * scopes: 客户允许请求的scope。
	 * clientSettings: 客户端的自定义设置—​例如，需要 PKCE，需要授权许可，以及其他。
	 * tokenSettings: 发给客户端的OAuth2令牌的自定义设置 - 例如，访问/刷新令牌的生存时间，重用刷新令牌，以及其他。
	 * @return
	 */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
		// 构建令牌设置对象，用于配置OAuth2令牌的各种设置
		TokenSettings tokenSettings = TokenSettings.builder()
				// 设置访问令牌格式为自包含，意味着所有信息都包含在令牌本身
				.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
				// 设置访问令牌的有效期为30分钟
				.accessTokenTimeToLive(Duration.ofMinutes(30))
				// 设置刷新令牌的有效期为60分钟
				.refreshTokenTimeToLive(Duration.ofMinutes(60))
				.build();
		RegisteredClient loginClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("login-client")
				.clientSecret("{noop}openid-connect")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.redirectUri("http://127.0.0.1:8080/login/oauth2/code/login-client")
				.redirectUri("http://127.0.0.1:8080/authorized")
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
				.build();
		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("messaging-client")
				.clientSecret("{noop}secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("message:read")
				.scope("message:write")
				.build();
		// 设置客户端密钥过期时间为当前时间加上30天
		Instant clientSecretExpiresAt = Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS);
		RegisteredClient frontClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("front-client")
				.clientSecret("{noop}front-secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
				.authorizationGrantType(new AuthorizationGrantType("password"))
				.clientSecretExpiresAt(clientSecretExpiresAt)
				.scope("message:read")
				.scope("message:write")
				.tokenSettings(tokenSettings)
				.build();
        return new InMemoryRegisteredClientRepository(loginClient, registeredClient,frontClient);
    }

	/**
	 * public final class AuthorizationServerSettings extends AbstractSettings {
	 *
	 * 	public static Builder builder() {
	 * 		return new Builder()
	 * 			.authorizationEndpoint("/oauth2/authorize")
	 * 			.tokenEndpoint("/oauth2/token")
	 * 			.tokenIntrospectionEndpoint("/oauth2/introspect")
	 * 			.tokenRevocationEndpoint("/oauth2/revoke")
	 * 			.jwkSetEndpoint("/oauth2/jwks")
	 * 			.oidcUserInfoEndpoint("/userinfo")
	 * 			.oidcClientRegistrationEndpoint("/connect/register");
	 * 	    }
	 * }
	 * @return
	 */
    @Bean
    public AuthorizationServerSettings providerSettings() {
        return AuthorizationServerSettings.builder().issuer("http://localhost:9000").build();
    }

}

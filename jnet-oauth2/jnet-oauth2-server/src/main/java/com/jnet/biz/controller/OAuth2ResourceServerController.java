/*
 * Copyright 2002-2024 the original author or authors.
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

package com.jnet.biz.controller;

import com.jnet.api.R;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class OAuth2ResourceServerController {

	@GetMapping("/jwt")
	public R index(@AuthenticationPrincipal Jwt jwt) {
		return R.success(jwt.getSubject());
	}

	/**
	 * springboot security获取当前登录用户信息
	 * @return
	 */
	@GetMapping("/auth")
	public R index() {
		//springboot security获取当前登录用户信息
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return R.success(authentication.getPrincipal());
	}

	@GetMapping("/message")
	public String message() {
		return "secret message";
	}

	@PostMapping("/message")
	public String createMessage(@RequestBody String message) {
		return String.format("Message was created. Content: %s", message);
	}

}

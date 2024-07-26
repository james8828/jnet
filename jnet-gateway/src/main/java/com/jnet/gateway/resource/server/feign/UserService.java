package com.jnet.gateway.resource.server.feign;

import com.jnet.api.R;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.vo.UserQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "SystemApp",path = "/v1/user", fallbackFactory = UserServiceFallbackFactory.class, dismiss404 = true)
public interface UserService{
    @PostMapping("/queryUser")
    R<List<User>> queryUser(@RequestBody UserQuery<User> query) throws Exception;
    @GetMapping("/loadUserByUsername")
    User loadUserByUsername(@RequestParam("username") String username) throws UsernameNotFoundException;

}

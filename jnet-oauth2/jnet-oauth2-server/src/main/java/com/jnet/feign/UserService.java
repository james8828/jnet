package com.jnet.feign;

import com.jnet.api.R;
import com.jnet.api.system.auth.UserDetailsCustom;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.vo.UserQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name = "SystemApp",path = "/v1/user", fallbackFactory = UserServiceFallbackFactory.class, dismiss404 = true)
public interface UserService extends UserDetailsService {
    @PostMapping("/queryUser")
    R<List<User>> queryUser(@RequestBody UserQuery<User> query) throws Exception;
    @GetMapping("/loadUserByUsername")
    UserDetailsCustom loadUserByUsername(@RequestParam("username") String username) throws UsernameNotFoundException;

}

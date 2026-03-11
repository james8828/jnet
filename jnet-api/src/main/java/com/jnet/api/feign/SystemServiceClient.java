package com.jnet.api.feign;

import com.jnet.api.R;
import com.jnet.api.config.FeignConfig;
import com.jnet.api.system.domain.Menu;
import com.jnet.api.system.domain.Role;
import com.jnet.api.system.domain.User;
import com.jnet.api.system.vo.UserQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Set;

@FeignClient(value = "jnet-system-service",path = "/v1", configuration = FeignConfig.class, fallbackFactory = SystemServiceClientFallbackFactory.class, dismiss404 = true)
public interface SystemServiceClient {
    @PostMapping("/user/queryUser")
    R<List<User>> queryUser(@RequestBody UserQuery<User> query) throws Exception;
    @GetMapping("/user/loadUserByUsername")
    User loadUserByUsername(@RequestParam("username") String username) throws Exception;
    @PostMapping("/menu/queryRolesByMenuId")
    R<Set<Role>> queryRolesByMenuId(@RequestBody Long menuId) throws Exception;
    @PostMapping("/menu/queryRolesByMenuId")
    R<List<Menu>> queryMenuByRoleId(@RequestBody List<Long> roleIds) throws Exception;
}

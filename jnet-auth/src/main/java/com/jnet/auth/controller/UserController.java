package com.jnet.auth.controller;

import com.jnet.api.R;
import com.jnet.auth.domain.User;
import com.jnet.auth.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/listUser")
    public R<List<User>> listUser() {
        return R.success(userService.list());
    }
}

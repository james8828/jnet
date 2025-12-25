package com.jnet.gateway.resource.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author mugw
 * @version 1.0
 * @description 测试webflux
 * @date 2024/9/20 09:33:00
 */
@RestController
@RequestMapping("/users")
public class UserController {

    /**
     * 查询用户列表
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public Flux<String> list() throws Exception{
        // 查询列表
        List<String> result = null;
        //result = new ArrayList<>();
        result.add("123");
        result.add("456");
        result.add("789");
        // 返回列表
        return Flux.fromIterable(result);
    }

    /**
     * 获得指定用户编号的用户
     *
     * @return 用户
     */
    @GetMapping("/get")
    public Mono<String> get() throws Exception{

        return Mono.just("user");
    }

    public static void main(String[] args) {
       Mono<String> mono = Mono.just("user");
       String s = mono.block();
       System.out.println(s);
    }
}

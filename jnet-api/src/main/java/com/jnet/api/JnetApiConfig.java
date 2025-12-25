package com.jnet.api;

import com.jnet.api.config.FeignClientInterceptor;
import com.jnet.api.config.FeignConfig;
import com.jnet.api.config.SwaggerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author mugw
 * @version 1.0
 * @description todo 导入配置文件
 * @date 2024/10/30 14:31:19
 */
//自动装配注解
@AutoConfiguration
//导入配置类
@Import({FeignConfig.class, SwaggerConfig.class, FeignClientInterceptor.class})
//开启feign
//@EnableFeignClients
public class JnetApiConfig {

}

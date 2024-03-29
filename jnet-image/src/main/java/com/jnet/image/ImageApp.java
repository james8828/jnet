package com.jnet.image;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jnet.common.biz.thread.TaskToolExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.openslide.OpenSlide;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@EnableDiscoveryClient
@MapperScan(basePackages = {"com.jnet.image.attachment.mapper","com.jnet.image.slide.mapper"})
@SpringBootApplication
public class ImageApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(ImageApp.class);
    }

    /**
     * 消息通知类线程池
     *
     * @return
     */
    @Bean(name = "msgExecutor", initMethod = "init", destroyMethod = "destroy")
    public TaskToolExecutor msgExecutor() {
        TaskToolExecutor msgExecutor = new TaskToolExecutor();
        msgExecutor.setName("msgExecutor");
        msgExecutor.setCoreSize(15);
        msgExecutor.setMaxSize(32);
        msgExecutor.setQueueSize(1024);
        return msgExecutor;
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer() {
        return registry -> registry.config().commonTags("application", "staitech-file");
    }

    @Bean
    public Cache<String, OpenSlide> initCache(){
        Cache<String, OpenSlide> loadingCache = CacheBuilder.newBuilder()
                //cache的初始容量
                .initialCapacity(5)
                //cache最大缓存数
                .maximumSize(10)
                //设置写缓存后n秒钟过期
                .expireAfterWrite(17, TimeUnit.SECONDS)
                //设置读写缓存后n秒钟过期,实际很少用到,类似于expireAfterWrite
                //.expireAfterAccess(17, TimeUnit.SECONDS)
                .build();
        return loadingCache;
    }
}

package com.jnet.common.core.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.LockSupport;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/6/7 13:04:07
 */
@Slf4j
public class RedissonUtil {

    private static RedissonClient redissonClient;
    private static LinkedBlockingDeque queue = new LinkedBlockingDeque();

    private static class SingletonHolder{
        private static final RedissonClient INSTANCE = createRedissonClient();
    }

    public static RedissonClient createRedissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        config.useSingleServer().setPassword("123456");
        redissonClient = Redisson.create(config);
        return redissonClient;
    }

    public static RedissonClient getInstance(){
        return SingletonHolder.INSTANCE;
    }

    public static class Task implements Runnable{
        @Override
        public void run() {
            LockSupport.park("");
            RLock lock = getInstance().getLock("lock");
            lock.lock();
            log.info("start exec Task end");
            LockSupport.parkNanos(1000000000);
            log.info("finish exec Task");
            lock.unlock();
        }
    }

    public static void test(){
        Task task = new Task();
        Thread thread = new Thread(task);
        thread.start();
    }

    public static void main(String[] args) throws Exception{
//        SingletonTopic.getInstance();
        /*for (int i = 0; i < 10; i++){
            test();
        }
        log.info("finish");*/
        queue.add(1);
        queue.offer(2);
        queue.addFirst(3);
        queue.offerFirst(4);
        Object o1 = queue.element();
        Object o2 = queue.peek();
        Object o = queue.poll();
        log.info(o.toString());
    }
}

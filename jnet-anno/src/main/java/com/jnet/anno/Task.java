package com.jnet.anno;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/2 15:16:28
 */
@Slf4j
public class Task implements Runnable{

    private CountDownLatch downLatch;
    private Integer num;

    private AtomicIntegerArray atomicIntegerArray;

    public Task(CountDownLatch downLatch, Integer num, AtomicIntegerArray atomicIntegerArray) {
        this.downLatch = downLatch;
        this.num = num;
        this.atomicIntegerArray = atomicIntegerArray;
    }

    public Task(CountDownLatch downLatch, Integer num) {
        this.downLatch = downLatch;
        this.num = num;
    }

    @Override
    public void run() {
        try {
            downLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        int resp = atomicIntegerArray.getAndSet(num,-1);
        log.info("in task atomicIntegerArray:{}",atomicIntegerArray);
        if (resp>0){
            System.out.println("task num:["+num+"];resp:["+resp+"];atomicIntegerArray length:["+atomicIntegerArray.length()+"]");
        }
    }
}

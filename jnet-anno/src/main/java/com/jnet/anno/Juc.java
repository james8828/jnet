package com.jnet.anno;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.LongStream;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/2 15:01:21
 */
@Slf4j
public class Juc {
    public static void main(String[] args){
        
        /*AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(100);
        int count = 100;
        CountDownLatch downLatch = new CountDownLatch(count);
        for (int i=0;i<count;i++){
            //atomicIntegerArray.set(i,i);
            Task task = new Task(downLatch,i,atomicIntegerArray);
            new Thread(task).start();
            log.info("AtomicIntegerArray state:[{}]",atomicIntegerArray);
            downLatch.countDown();
        }*/

    }

}

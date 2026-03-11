package com.jnet.anno.test;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class Juf {

    public static void main(String[] args){
        func(x-> Integer.parseInt(x)+1,y->y+"00");
        func1(x-> Integer.parseInt(x)+1);
        func2(x-> Integer.parseInt(x)+1,y->y+"00");
    }

    public static void func(Function<String,Integer> f1,Function<Integer,String> f2){
        Integer i = f1.compose(f2).apply(Integer.valueOf("10"));
        log.info("{}",i);
    }

    public static void func1(Function<String,Integer> f1){
        Integer i = f1.apply("10");
        log.info("{}",i);
    }
    public static void func2(Function<String,Integer> f1,Function<Integer,String> f2){
        String i = f1.andThen(f2).apply("10");
        log.info("{}",i);
    }
}

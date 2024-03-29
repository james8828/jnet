package com.jnet.common.biz.thread;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * @author mugw
 * @version 1.0
 * @description 线程池切面
 * @date 2024/2/20 10:27:22
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface Pooled {

    boolean async() default true;

    long timeout() default 500;

    String executor() default "ciToolExecutor";

    PoolOverAct poolOverAct() default PoolOverAct.REJECT;

    enum PoolOverAct {
        REJECT, BLOCK, RUN, NEW_THREAD;
    }
}


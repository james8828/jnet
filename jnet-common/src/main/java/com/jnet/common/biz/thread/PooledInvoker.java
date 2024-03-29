package com.jnet.common.biz.thread;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author mugw
 * @version 1.0
 * @description 线程池切面
 * @date 2024/2/20 11:27:38
 */
@Slf4j
@Aspect
@Component
@Order(AopOrder.POOLED_INVOKER_ORDER)
public class PooledInvoker {

    @Autowired
    private Map<String, TaskToolExecutor> executorMap;

    @Around("@annotation(com.scaffolding.example.threads.aop.Pooled) && @annotation(pooled)")
    public Object around(final ProceedingJoinPoint pjp, Pooled pooled) {
        TaskToolExecutor executor = getExecutor(pooled.executor());
        Object result = null;
        Worker<Object> worker = toWorker(pjp);
        worker.setPoolOverAct(pooled.poolOverAct());
        if (pooled.async()) {
            executor.execute(worker);
        } else {
            worker.setTimeout(pooled.timeout());
            result = executor.submit(worker);
        }

        if (null == result) {
            Class<?> returnType = ((MethodSignature) pjp.getSignature()).getMethod().getReturnType();
            if (returnType.isPrimitive()) {
                return PrimitiveUtils.getPrimitiveDefaultValue(returnType);
            }
        }

        return result;
    }

    private Worker<Object> toWorker(ProceedingJoinPoint pjp) {
        final Worker<Object> worker = new Worker<>();
        Runnable command = () -> {
            try {
                worker.setResult(pjp.proceed());
            } catch (Throwable e) {
                log.error("Error pooled execute:", e);
            }
        };
        worker.setCommand(command);
        return worker;
    }

    private TaskToolExecutor getExecutor(String poolType) {
        return executorMap.get(poolType);
    }
}


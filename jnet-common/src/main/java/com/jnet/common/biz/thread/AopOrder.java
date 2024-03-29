package com.jnet.common.biz.thread;

/**
 * @author mugw
 * @version 1.0
 * @description 切面顺序
 * @date 2024/2/20 13:20:04
 */

import org.springframework.core.Ordered;

/**
 * Aop的拦截顺序定义
 * POOLED_INVOKER_ORDER > DISTRIBUTED_LOCK_ORDER > 日志 > 事务
 *
 * @author mugw
 * @version 1.0
 * @description 切面顺序
 * @date 2024/2/20 13:20:04
 */
public class AopOrder {
    public static final int DISTRIBUTED_LOCK_ORDER = Ordered.HIGHEST_PRECEDENCE + 4;
    public static final int POOLED_INVOKER_ORDER = Ordered.HIGHEST_PRECEDENCE + 2;
}

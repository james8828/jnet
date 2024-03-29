package com.jnet.common.biz.thread;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mugw
 * @version 1.0
 * @description 任务执行结果
 * @date 2024/2/20 10:27:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    public T value;
}


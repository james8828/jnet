package com.jnet.anno;

import java.util.concurrent.RecursiveTask;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/4 11:17:35
 */
public class SumArrayTask extends RecursiveTask<Integer> {

    private static final int THRESHOLD = 10; // 阈值，当数组长度小于此值时，不再拆分任务
    private final int[] array;
    private final int start;
    private final int end;

    private SumArrayTask(int[] array,  int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        // 如果任务足够小，直接计算结果
        if (end - start <= THRESHOLD) {
            int sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // 拆分任务
            int mid = start + (end - start) / 2;
            SumArrayTask leftTask = new SumArrayTask(array, start, mid);
            SumArrayTask rightTask = new SumArrayTask(array, mid, end);

            // 递归执行任务并等待结果
            invokeAll(leftTask, rightTask);
            return leftTask.join() + rightTask.join();
        }
    }
}

package com.jnet.image.attachment.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/9/19 13:30:21
 */
@Slf4j
@Data
public class UploadState {
    private String name;
    private AtomicReference<String[]> md5s;
    private AtomicReference<int[]> status;

    UploadState(int n) {
        this.status = new AtomicReference(new int[n]);
        this.md5s = new AtomicReference(new String[n]);
    }

    UploadState(String name, int n) {
        this.name = name;
        this.status = new AtomicReference(new int[n]);
        this.md5s = new AtomicReference(new String[n]);
    }
}

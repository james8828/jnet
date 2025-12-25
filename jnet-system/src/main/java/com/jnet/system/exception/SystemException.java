package com.jnet.system.exception;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/7/19 11:50:33
 */

public class SystemException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}

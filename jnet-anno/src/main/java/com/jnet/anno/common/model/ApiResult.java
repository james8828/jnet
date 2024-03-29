package com.jnet.anno.common.model;

import com.jnet.anno.common.enums.ApiResultCode;

public class ApiResult<T> {
    private Integer code;
    private String message;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ApiResult(ApiResultCode code, String message, T data) {
        this.code = code.getCode();
        this.message = message;
        this.data = data;
    }

    public ApiResult(ApiResultCode code, String message) {
        this.code = code.getCode();
        this.message = message;
        this.data = null;
    }
}

package com.jnet.anno.common.exception;

import com.jnet.anno.common.enums.ApiResultCode;

public class ApiException extends RuntimeException {
    private ApiResultCode code;

    public ApiResultCode getCode() {
        return code;
    }

    public void setCode(ApiResultCode code) {
        this.code = code;
    }

    public ApiException(String message, ApiResultCode code) {
        super(message);
        this.code = code;
    }
}

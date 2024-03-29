package com.jnet.anno.common.enums;

public enum  ApiResultCode {
    Success(20000),
    APIVersionErr(30005),
    ParameterErr(40000),
    TokenErr(40002),
    ServerErr(50000),
    UnknowErr(50001);

    private Integer code;

    ApiResultCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}

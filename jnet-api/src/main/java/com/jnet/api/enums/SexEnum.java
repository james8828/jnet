package com.jnet.api.enums;

public enum SexEnum {
    MALE(1),
    FEMALE(2),
    UNKNOWN(3);

    private final int code;

    SexEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}

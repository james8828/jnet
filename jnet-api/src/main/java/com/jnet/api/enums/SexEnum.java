package com.jnet.api.enums;

public enum SexEnum {
    MALE(1, "男"),
    FEMALE(2, "女"),
    UNKNOWN(3, "未知");

    private final int code;
    private final String description;

    SexEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取对应的枚举实例
     *
     * @param code 枚举编码
     * @return 对应的 SexEnum 实例
     * @throws IllegalArgumentException 如果 code 不合法
     */
    public static SexEnum fromCode(int code) {
        for (SexEnum sex : values()) {
            if (sex.code == code) {
                return sex;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }

    @Override
    public String toString() {
        return "SexEnum{" +
                "code=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}


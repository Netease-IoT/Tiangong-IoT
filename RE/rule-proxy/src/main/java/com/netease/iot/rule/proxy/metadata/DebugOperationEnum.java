package com.netease.iot.rule.proxy.metadata;

public enum DebugOperationEnum {
    RUN(0),
    STOP(1);

    private final int code;

    DebugOperationEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    DebugOperationEnum getEnumByCode(int code) {
        for (DebugOperationEnum value : DebugOperationEnum.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }

        throw new RuntimeException("Invalid code when getting " + DebugOperationEnum.class.getCanonicalName());
    }
}

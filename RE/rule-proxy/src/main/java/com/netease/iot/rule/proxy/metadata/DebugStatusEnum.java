package com.netease.iot.rule.proxy.metadata;

public enum DebugStatusEnum {
    STARTING(1),
    RUNNING(2),
    STOPING(3),
    STOP(4),
    UNKINOW(5);

    private final int index;

    public int getIndex() {
        return index;
    }

    DebugStatusEnum(int index) {
        this.index = index;
    }

    public DebugStatusEnum getByIndex(int index) {
        for (DebugStatusEnum debugStatusEnum : DebugStatusEnum.values()) {
            if (debugStatusEnum.getIndex() == index) {
                return debugStatusEnum;
            }
        }
        throw new RuntimeException("invalid index");
    }
}

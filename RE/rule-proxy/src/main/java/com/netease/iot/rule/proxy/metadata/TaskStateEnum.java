package com.netease.iot.rule.proxy.metadata;

public enum TaskStateEnum {
    READY, STARTING, RUNNING, SUSPENDING, SUSPENDED, DELETING, DELETED, RESUMING, FAILED,

    SAVEPOINT_DELETED, OPERATION_FAILED, CUSTER_CRASH;

    public static TaskStateEnum getByValue(int value) {
        for (TaskStateEnum stateEnum : values()) {
            if (stateEnum.ordinal() == value) {
                return stateEnum;
            }
        }
        return null;
    }

    public boolean isSameState(int value) {
        return ordinal() == value;
    }
}

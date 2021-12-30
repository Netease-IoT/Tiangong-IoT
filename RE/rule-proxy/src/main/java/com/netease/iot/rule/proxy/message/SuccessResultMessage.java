package com.netease.iot.rule.proxy.message;


public class SuccessResultMessage extends IotBaseResultMessage {

    public SuccessResultMessage(String requestId) {
        setRequestId(requestId);
    }

    public static SuccessResultMessage cook(String requestId) {
        return new SuccessResultMessage(requestId);
    }
}

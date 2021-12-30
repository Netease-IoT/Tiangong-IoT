package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

public class CommonResultMessage extends SuccessResultMessage {

    private String status;
    private String message;

    public CommonResultMessage(String requestId, String status, String message) {
        super(requestId);
        this.status = status;
        this.message = message;
    }

    public static CommonResultMessage cook(String requestId, String status) {
        return new CommonResultMessage(requestId, status, null);
    }

    public static CommonResultMessage cook(String requestId, String status, String message) {
        return new CommonResultMessage(requestId, status, message);
    }

    @JSONField(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JSONField(name = "Message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

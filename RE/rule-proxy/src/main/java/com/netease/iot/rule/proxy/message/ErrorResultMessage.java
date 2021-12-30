package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

public class ErrorResultMessage extends IotBaseResultMessage {

    private String code;

    private String msg;

    public ErrorResultMessage(String code, String msg, String requestId) {
        this.code = code;
        this.msg = msg;
        setRequestId(requestId);
    }

    public static ErrorResultMessage cook(String code, String msg, String requestId) {
        return new ErrorResultMessage(code, msg, requestId);
    }

    @JSONField(name = "Code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JSONField(name = "Msg")
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

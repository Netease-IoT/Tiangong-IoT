package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class IotBaseResultMessage implements Serializable {

    private String requestId;

    @JSONField(name = "RequestId")
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

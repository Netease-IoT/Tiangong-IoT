package com.netease.iot.rule.proxy.model;

import java.io.Serializable;

public class UnsupportedMessage implements Serializable {

    private String messageType;

    public UnsupportedMessage(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}

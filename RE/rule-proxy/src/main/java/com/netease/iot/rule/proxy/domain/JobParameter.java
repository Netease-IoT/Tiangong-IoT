package com.netease.iot.rule.proxy.domain;

import java.io.Serializable;

public class JobParameter implements Serializable {
    private Integer rankIndex;
    private String keyName;
    private String defaultVal;
    private String valueType;
    private boolean isNeeded;
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRankIndex() {
        return rankIndex;
    }

    public void setRankIndex(Integer rankIndex) {
        this.rankIndex = rankIndex;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getDefaultVal() {
        return defaultVal;
    }

    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public boolean isNeeded() {
        return isNeeded;
    }

    public void setNeeded(boolean needed) {
        isNeeded = needed;
    }
}

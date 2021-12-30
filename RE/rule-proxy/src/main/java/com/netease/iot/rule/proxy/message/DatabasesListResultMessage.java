package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class DatabasesListResultMessage extends SuccessResultMessage {

    private List<String> dbList;

    public DatabasesListResultMessage(String requestId, List<String> dbList) {
        super(requestId);
        this.dbList = dbList;
    }

    public static DatabasesListResultMessage cook(String requestId, List<String> dbList) {
        return new DatabasesListResultMessage(requestId, dbList);
    }

    @JSONField(name = "DbList")
    public List<String> getDbList() {
        return dbList;
    }

    public void setDbList(List<String> dbList) {
        this.dbList = dbList;
    }
}

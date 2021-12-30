package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class TableListResultMessage extends SuccessResultMessage {

    private List<String> tableList;

    public TableListResultMessage(String requestId, List<String> tableList) {
        super(requestId);
        this.tableList = tableList;
    }

    public static TableListResultMessage cook(String requestId, List<String> tableList) {
        return new TableListResultMessage(requestId, tableList);
    }

    @JSONField(name = "TableList")
    public List<String> getTableList() {
        return tableList;
    }

    public void setTableList(List<String> tableList) {
        this.tableList = tableList;
    }
}

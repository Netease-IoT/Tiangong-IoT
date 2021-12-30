package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

public class EditSqlResultMessage extends SuccessResultMessage {

    private String ruleId;

    private String prepareSql;

    private String userSql;

    public EditSqlResultMessage(String requestId, String ruleId, String prepareSql, String userSql) {
        super(requestId);
        this.ruleId = ruleId;
        this.prepareSql = prepareSql;
        this.userSql = userSql;
    }

    public static EditSqlResultMessage cook(String requestId, String ruleId, String prepareSql, String userSql) {
        return new EditSqlResultMessage(requestId, ruleId, prepareSql, userSql);
    }

    @JSONField(name = "RuleId")
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @JSONField(name = "PrepareSql")
    public String getPrepareSql() {
        return prepareSql;
    }

    public void setPrepareSql(String prepareSql) {
        this.prepareSql = prepareSql;
    }

    @JSONField(name = "UserSql")
    public String getUserSql() {
        return userSql;
    }

    public void setUserSql(String userSql) {
        this.userSql = userSql;
    }
}

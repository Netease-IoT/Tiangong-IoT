package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;


public class RuleDetailResultMessage extends SuccessResultMessage {

    private String ruleId;
    private String ruleName;
    private String ruleDescription;
    private String sourceType;
    private String sourceJson;
    private String sinkType;
    private String sinkJson;
    private String prepareSql;
    private String userSql;
    private String status;
    private String createAt;
    private String updateAt;

    public RuleDetailResultMessage(String requestId) {
        super(requestId);
    }

    @JSONField(name = "RuleName")
    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    @JSONField(name = "RuleDescription")
    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    @JSONField(name = "RuleId")
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @JSONField(name = "SourceType")
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    @JSONField(name = "SourceJson")
    public String getSourceJson() {
        return sourceJson;
    }

    public void setSourceJson(String sourceJson) {
        this.sourceJson = sourceJson;
    }

    @JSONField(name = "SinkType")
    public String getSinkType() {
        return sinkType;
    }

    public void setSinkType(String sinkType) {
        this.sinkType = sinkType;
    }

    @JSONField(name = "SinkJson")
    public String getSinkJson() {
        return sinkJson;
    }

    public void setSinkJson(String sinkJson) {
        this.sinkJson = sinkJson;
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

    @JSONField(name = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JSONField(name = "CreateAt")
    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    @JSONField(name = "UpdateAt")
    public String getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(String updateAt) {
        this.updateAt = updateAt;
    }
}
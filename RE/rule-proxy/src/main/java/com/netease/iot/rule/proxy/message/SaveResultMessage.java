package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

public class SaveResultMessage extends SuccessResultMessage {

    private String ruleId;

    public SaveResultMessage(String requestId, String ruleId) {
        super(requestId);
        this.ruleId = ruleId;
    }

    public static SaveResultMessage cook(String requestId, String ruleId) {
        return new SaveResultMessage(requestId, ruleId);
    }

    @JSONField(name = "RuleId")
    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
}

package com.netease.iot.rule.proxy.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class ListRuleResultMessage extends SuccessResultMessage {

    private List<RuleDetailResultMessage> ruleList;

    private Integer count;

    public ListRuleResultMessage(String requestId, List<RuleDetailResultMessage> ruleList, int count) {
        super(requestId);
        this.ruleList = ruleList;
        this.count = count;
    }

    public static ListRuleResultMessage cook(String requestId, List<RuleDetailResultMessage> ruleList, int count) {
        return new ListRuleResultMessage(requestId, ruleList, count);
    }

    @JSONField(name = "RuleList")
    public List<RuleDetailResultMessage> getRuleList() {
        return ruleList;
    }

    public void setRuleList(List<RuleDetailResultMessage> ruleList) {
        this.ruleList = ruleList;
    }

    @JSONField(name = "Count")
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}

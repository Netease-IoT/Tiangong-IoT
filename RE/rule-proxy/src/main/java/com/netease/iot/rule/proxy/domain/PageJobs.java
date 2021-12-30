package com.netease.iot.rule.proxy.domain;

import java.util.List;


public class PageJobs {
    List<Job> jobs;
    Integer totalNum;

    public PageJobs(List<Job> jobs, Integer totalNum) {
        this.jobs = jobs;
        this.totalNum = totalNum;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public Integer getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
    }
}

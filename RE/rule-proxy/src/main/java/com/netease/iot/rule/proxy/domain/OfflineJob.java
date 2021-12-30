package com.netease.iot.rule.proxy.domain;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public class OfflineJob implements Serializable {

    private String id;
    private int type; //online or offline
    private String sql;
    private String conf;
    private String jobName;
    private String userId;
    private String editorId;
    private int status;
    private String product;
    private String createTime;
    private String editTime;
    private String deployTime;
    private int version;
    private String deployNote;
    private String startTime;

    private String cluserName; // this field has no sql field
    private String serverName; // this field has no sql field

    private List<String> jarList; // this field has not sql field;
    private boolean hasModify = false; // this field has not sql field;

    private String savePointPath;
    private String jarPath;
    private String flinkJobId;

    private Integer clusterId;
    private Integer serverId;
    private Integer versionStatus;
    private Integer operatingStatus;

    //// below for iot ///
    private String ruleDescription;
    private String sourceType;
    private String sourceJson;
    private String sinkType;
    private String sinkJson;
    private String prepareSql;

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceJson() {
        return sourceJson;
    }

    public void setSourceJson(String sourceJson) {
        this.sourceJson = sourceJson;
    }

    public String getSinkType() {
        return sinkType;
    }

    public void setSinkType(String sinkType) {
        this.sinkType = sinkType;
    }

    public String getSinkJson() {
        return sinkJson;
    }

    public void setSinkJson(String sinkJson) {
        this.sinkJson = sinkJson;
    }

    public String getPrepareSql() {
        return prepareSql;
    }

    public void setPrepareSql(String prepareSql) {
        this.prepareSql = prepareSql;
    }

    public Integer getOperatingStatus() {
        return operatingStatus;
    }

    public void setOperatingStatus(Integer operatingStatus) {
        this.operatingStatus = operatingStatus;
    }

    public String getCluserName() {
        return cluserName;
    }

    public void setCluserName(String cluserName) {
        this.cluserName = cluserName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getEditTime() {
        return editTime;
    }

    public void setEditTime(String editTime) {
        this.editTime = editTime;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getEditorId() {
        return editorId;
    }

    public void setEditorId(String editorId) {
        this.editorId = editorId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDeployNote() {
        return deployNote;
    }

    public void setDeployNote(String deployNote) {
        this.deployNote = deployNote;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getDeployTime() {
        return deployTime;
    }

    public void setDeployTime(String deployTime) {
        this.deployTime = deployTime;
    }

    public List<String> getJarList() {
        return jarList;
    }

    public void setJarList(List<String> jarList) {
        this.jarList = jarList;
    }

    public boolean isHasModify() {
        return hasModify;
    }

    public void setHasModify(boolean hasModify) {
        this.hasModify = hasModify;
    }

    public String getSavePointPath() {
        return savePointPath;
    }

    public void setSavePointPath(String savePointPath) {
        this.savePointPath = savePointPath;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getFlinkJobId() {
        return flinkJobId;
    }

    public void setFlinkJobId(String flinkJobId) {
        this.flinkJobId = flinkJobId;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public Integer getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(Integer versionStatus) {
        this.versionStatus = versionStatus;
    }

    public static Job getJobByOfflineJob(OfflineJob job1) {
        if (job1 == null) {
            return null;
        }
        Job offlineJob = new Job();
        offlineJob.setId(job1.getId());
        offlineJob.setType(job1.getType());
        offlineJob.setSql(job1.getSql());
        offlineJob.setConf(job1.getConf());
        offlineJob.setJobName(job1.getJobName());
        offlineJob.setUserId(job1.getUserId());
        offlineJob.setEditorId(job1.getEditorId());
        offlineJob.setStatus(job1.getStatus());
        offlineJob.setProduct(job1.getProduct());
        offlineJob.setCreateTime(job1.getCreateTime());
        offlineJob.setEditTime(job1.getEditTime());
        offlineJob.setDeployTime(job1.getDeployTime());
        offlineJob.setVersion(job1.getVersion());
        offlineJob.setStartTime(job1.getStartTime());
        offlineJob.setClusterName(job1.getCluserName());
        offlineJob.setJarList(job1.getJarList());
        offlineJob.setHasModify(job1.isHasModify());
        offlineJob.setSavePointPath(job1.getSavePointPath());
        offlineJob.setJarPath(job1.getJarPath());
        offlineJob.setFlinkJobId(job1.getFlinkJobId());
        offlineJob.setClusterId(job1.getClusterId());
        offlineJob.setServerId(job1.getServerId());
        offlineJob.setVersionStatus(job1.getVersionStatus());
        offlineJob.setServerName(job1.getServerName());
        return offlineJob;
    }

    public static List<Job> getJobListByOfflineJobList(List<OfflineJob> offlineJobList) {
        List<Job> list = Lists.newArrayList();
        for (OfflineJob job : offlineJobList) {
            list.add(OfflineJob.getJobByOfflineJob(job));
        }
        return list;
    }
}
package com.netease.iot.rule.proxy.domain;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public class OnlineJob implements Serializable {
    private String id;
    private int type; //online or offline
    private String deployedSql;
    private String executedSql;
    private String conf;
    private String jobName;
    private String userId;
    private String editorId;
    private int status;
    private String product;
    private String createTime;
    private String editTime;
    private String stateEditTime;
    private String deployTime;
    private int version;
    private String deployNote;
    private String startTime;

    private String clusterName; // this field has no deployedSql field
    private String serverName; // this field has no deployedSql field

    private List<String> jarList; // this field has not deployedSql field;
    private boolean hasModify = false; // this field has not deployedSql field;

    private String savePointPath;
    private String jarPath;
    private String flinkJobId;
    private String applicationId;

    private Integer clusterId;
    private Integer serverId;

    private Integer versionStatus;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
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

    public String getDeployedSql() {
        return deployedSql;
    }

    public void setDeployedSql(String deployedSql) {
        this.deployedSql = deployedSql;
    }

    public String getExecutedSql() {
        return executedSql;
    }

    public void setExecutedSql(String executedSql) {
        this.executedSql = executedSql;
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

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public static Job getJobByOnlineJob(OnlineJob onlineJob, SqlTypeEnum sqlTypeEnum) {
        if (onlineJob == null) {
            return null;
        }
        Job job = new Job();
        job.setId(onlineJob.getId());
        job.setType(onlineJob.getType());
        job.setConf(onlineJob.getConf());
        job.setJobName(onlineJob.getJobName());
        job.setUserId(onlineJob.getUserId());
        job.setEditorId(onlineJob.getEditorId());
        job.setStatus(onlineJob.getStatus());
        job.setProduct(onlineJob.getProduct());
        job.setCreateTime(onlineJob.getCreateTime());
        job.setEditTime(onlineJob.getEditTime());
        job.setDeployTime(onlineJob.getDeployTime());
        job.setVersion(onlineJob.getVersion());
        job.setStartTime(onlineJob.getStartTime());
        job.setClusterName(onlineJob.getClusterName());
        job.setJarList(onlineJob.getJarList());
        job.setHasModify(onlineJob.isHasModify());
        job.setSavePointPath(onlineJob.getSavePointPath());
        job.setJarPath(onlineJob.getJarPath());
        job.setFlinkJobId(onlineJob.getFlinkJobId());
        job.setApplicationId(onlineJob.getApplicationId());
        job.setClusterId(onlineJob.getClusterId());
        job.setServerId(onlineJob.getServerId());
        job.setVersionStatus(onlineJob.getVersionStatus());
        job.setServerName(onlineJob.getServerName());
        switch (sqlTypeEnum) {
        case DEPLOYED:
            job.setSql(onlineJob.getDeployedSql());
            break;
        case EXECUTED:
            job.setSql(onlineJob.getExecutedSql());
            break;
        }

        return job;
    }

    public static List<Job> getJobListByOnlineJobList(List<OnlineJob> onlineJobs) {
        List<Job> list = Lists.newArrayList();
        for (OnlineJob job : onlineJobs) {
            list.add(OnlineJob.getJobByOnlineJob(job, SqlTypeEnum.EXECUTED));
        }
        return list;
    }

    public String getStateEditTime() {
        return stateEditTime;
    }

    public void setStateEditTime(String stateEditTime) {
        this.stateEditTime = stateEditTime;
    }

    public Integer getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(Integer versionStatus) {
        this.versionStatus = versionStatus;
    }
}
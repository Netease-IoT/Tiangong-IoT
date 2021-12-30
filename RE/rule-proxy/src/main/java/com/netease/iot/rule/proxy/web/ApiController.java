package com.netease.iot.rule.proxy.web;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.netease.iot.rule.proxy.model.ResultMessage;
import com.netease.iot.rule.proxy.service.JobService;
import com.netease.iot.rule.proxy.service.JobService09;
import com.netease.iot.rule.proxy.util.ResultMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.Produces;

import static com.netease.iot.rule.proxy.util.Constants.JOB_VERSION_NEW;
import static com.netease.iot.rule.proxy.util.Constants.JOB_VERSION_OLD;


@Controller
@RequestMapping("/rule/v1")
public class ApiController {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobService09 jobService09;

    private final Gson gson = new Gson();

    /**
     * get jar list api
     *
     * @param product
     * @param userId
     * @return
     */
    @PostMapping("/getJarList")
    @ResponseBody
    public String getJarList(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId) {
        ResultMessage msg = jobService09.getJarList(product, userId);
        return gson.toJson(msg);
    }

    @PostMapping("/saveJarList")
    @ResponseBody
    public String saveJarList(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("type") String type,
            @RequestParam("jarList") String jarList) {
        ResultMessage msg = jobService09.saveJarList(userId, jobId, type, jarList);
        return gson.toJson(msg);
    }

    /**
     * get rule's conf key list
     *
     * @param userId
     * @param jobId
     * @return
     */
    @PostMapping("/getJobConfParameterList")
    @ResponseBody
    public String getJobConfParameterList(@RequestParam("userId") String userId, @RequestParam("jobId") String jobId) {
        ResultMessage msg = jobService09.getJobConfParameterList(userId, jobId);
        return gson.toJson(msg);
    }

    /**
     * upload jar api
     *
     * @param product
     * @param userId
     * @param jarData
     * @return
     */
    @PostMapping("/uploadJar")
    @ResponseBody
    @Produces({"text/html"})
    public String uploadJar(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("explanation") String explanation,
            @RequestParam("jarFile") MultipartFile jarData) {
        ResultMessage msg = jobService09.uploadJar(product, userId, explanation, jarData);
        return gson.toJson(msg);
    }

    /**
     * remove jar api
     *
     * @param product
     * @param userId
     * @param jarId
     * @return
     */
    @PostMapping("/removeJar")
    @ResponseBody
    public String removeJar(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("jarId") String jarId) {
        ResultMessage msg = jobService09.removeJar(product, userId, jarId);
        return gson.toJson(msg);
    }

    /**
     * get (online/ offline) Job list
     *
     * @param product
     * @param userId
     * @param type
     * @return
     */
    @PostMapping("/getJobList")
    @ResponseBody
    public String getJobList(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("type") String type,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam("pageNumber") Integer pageNumber) {
        ResultMessage msg = jobService09.getJobListByPage(product, userId, type, pageSize, pageNumber);
        return gson.toJson(msg);
    }

    /**
     * Search job by jobName
     *
     * @param product
     * @param userId
     * @param type
     * @param pageSize
     * @param pageNumber
     * @param key
     * @return
     */
    @PostMapping("/searchJobList")
    @ResponseBody
    public String searchJobList(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("type") String type,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam("pageNumber") Integer pageNumber,
            @RequestParam("searchKey") String key) {
        ResultMessage msg = jobService09.searchJobListByPage(product, userId, type, pageSize, pageNumber, key);
        return gson.toJson(msg);
    }

    /**
     * get job's detail info by jobId
     *
     * @param userId
     * @param jobId
     * @return
     */
    @PostMapping("/getJobDetail")
    @ResponseBody
    public String getJobDetail(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("type") int type) {
        ResultMessage msg = jobService09.getJobDetail(userId, jobId, type);
        return gson.toJson(msg);
    }

    /**
     * create or save a job info. (create on not exists)
     *
     * @param product
     * @param userId
     * @param sql
     * @param jobId
     * @param jobName
     * @return
     */
    @PostMapping("/createOrSaveJob")
    @ResponseBody
    public String createOrSaveJob(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("sql") String sql,
            @RequestParam("jobId") String jobId,
            @RequestParam("jobName") String jobName,
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus) {
        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.saveJob(product, userId, sql, jobId, jobName);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.createOrSaveJob(product, userId, sql, jobId, jobName, clusterId, serverId, versionStatus);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    /**
     * set job's conf that will be used when running the job
     *
     * @param userId
     * @param jobId
     * @param conf
     * @return
     */
    @PostMapping("/setJobConf")
    @ResponseBody
    public String setJobConf(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("conf") String conf,
            @RequestParam("product") String product) {
        ResultMessage msg = jobService09.setJobConf(userId, jobId, conf, product);
        return gson.toJson(msg);
    }

    /**
     * set job's monitor meta that will be used when monitoring the job
     *
     * @param userId
     * @param jobId
     * @param group
     * @param emails
     * @param alarmTypes
     * @param interval
     * @param threshold
     * @return
     */
    @PostMapping("/setJobMonitor")
    @ResponseBody
    public String setJobMonitor(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam("emails") String emails,
            @RequestParam("alarmTypes") String alarmTypes,
            @RequestParam("interval") String interval,
            @RequestParam("threshold") String threshold) {

        ResultMessage msg = jobService09.setJobMonitor(userId, jobId, group, emails, alarmTypes, interval, threshold);
        return gson.toJson(msg);
    }

    /**
     * get job's monitor meta that will be used when monitoring the job
     *
     * @param userId
     * @param jobId
     * @return
     */
    @PostMapping("/getJobMonitor")
    @ResponseBody
    public String getJobMonitor(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId) {

        ResultMessage msg = jobService09.getJobMonitor(userId, jobId);
        return gson.toJson(msg);
    }

    /**
     * set job's monitor group
     *
     * @param group
     * @param emails
     * @return
     */
    @PostMapping("/updateMonitorGroup")
    @ResponseBody
    public String updateMonitorGroup(
            @RequestParam("group") String group,
            @RequestParam("emails") String emails) {

        ResultMessage msg = jobService09.updateMonitorGroup(group, emails);
        return gson.toJson(msg);
    }

    /**
     * delete monitor group
     *
     * @param group
     * @return
     */
    @PostMapping("/deleteMonitorGroup")
    @ResponseBody
    public String deleteMonitorGroup(
            @RequestParam("group") String group) {

        ResultMessage msg = jobService09.deleteMonitorGroup(group);
        return gson.toJson(msg);
    }

    /**
     * deploy the job from offline to online
     *
     * @param userId
     * @param jobId
     * @return
     */
    @PostMapping("/deployJob")
    @ResponseBody
    public String deployJob(
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("note") String note) {
        ResultMessage msg = jobService09.deployJob(userId, jobId, note);
        return gson.toJson(msg);
    }

    /**
     * do syntax check on sql, result the error pos info and the source list
     *
     * @param product
     * @param userId
     * @param sql
     * @return
     */
    @PostMapping("/syntaxCheck")
    @ResponseBody
    public String syntaxCheck(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("sql") String sql,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("clusterId") Integer clusterId,
            @RequestParam("versionStatus") Integer versionStatus) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.syntaxCheck(product, userId, jobId, sql);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    /**
     * set job's status (start: 1, delete: 2, stop: 3)
     *
     * @param userId
     * @param jobId
     * @param status
     * @return
     */
    @PostMapping("/setJobStatus")
    @ResponseBody
    public String setJobStatus(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("status") String status,
            @RequestParam("keepState") String keepState,
            @RequestParam("versionStatus") Integer versionStatus) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.setJobStatus(product, userId, jobId, status, keepState);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.setJobStatus(product, userId, jobId, status, keepState);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    /**
     * Get job status info, see class JobStatusInfo
     *
     * @param product
     * @param userId
     * @return
     */
    @PostMapping("/getJobStatusList")
    @ResponseBody
    public String getJobStatusList(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId) {
        ResultMessage msg = jobService09.getJobStatusList(product, userId, null);
        return gson.toJson(msg);
    }

    /**
     * Delete offline job
     *
     * @param jobId
     * @param userId
     * @return
     */
    @PostMapping("/deleteOfflineJob")
    @ResponseBody
    public String deleteOfflineJob(
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId) {
        ResultMessage msg = jobService09.deleteOfflineJob(jobId, userId);
        return gson.toJson(msg);
    }

    /**
     * Batch get job
     *
     * @param jobIdList
     * @param userId
     * @param product
     * @param type
     * @return
     */
    @PostMapping("/getJobInfo")
    @ResponseBody
    public String getJobInfo(@RequestParam("jobIdList") String jobIdList,
                             @RequestParam("userId") String userId,
                             @RequestParam("product") String product,
                             @RequestParam("type") String type) {
        ResultMessage resultMessage = jobService09.batchGetJob(jobIdList, product, userId, type);
        return gson.toJson(resultMessage);
    }

    /**
     * Get debug data
     *
     * @param product
     * @param jobId
     * @param userId
     * @param tableNames
     * @return
     */
    @RequestMapping("/getDebugData")
    @ResponseBody
    public String getDebugData(@RequestParam("product") String product,
                               @RequestParam("jobId") String jobId,
                               @RequestParam("userId") String userId,
                               @RequestParam("tableNames") String tableNames) {
        ResultMessage resultMessage = jobService09.getDebugData(product, jobId, userId, tableNames);
        return gson.toJson(resultMessage);
    }

    /**
     * get the flink web url
     *
     * @param jobId
     * @param userId
     */
    @PostMapping("/getFlinkWebUrl")
    @ResponseBody
    public String getFlinkWebUrl(@RequestParam("jobId") String jobId,
                                 @RequestParam("userId") String userId,
                                 @RequestParam("versionStatus") Integer versionStatus) {
        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.getFlinkWebUrl(jobId,  userId);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.getFlinkWebUrl(jobId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    @VisibleForTesting
    public boolean setJobService(JobService09 jobService09) {
        this.jobService09 = jobService09;
        return true;
    }

    @PostMapping("/getSqlModel")
    @ResponseBody
    public String getSqlModel(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("version") String version) {
        ResultMessage msg = jobService09.getSqlModel(version);
        return gson.toJson(msg);
    }

    @PostMapping("/fetchData")
    @ResponseBody
    public String getPrefetchData(
            @RequestParam("product") String product,
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId,
            @RequestParam("sql") String sql,
            @RequestParam("tableName") String tableName,
            @RequestParam("fetchSize") String fetchSize,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus,
            @RequestParam("clusterId") Integer clusterId) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.prefetchData(product, jobId, sql, tableName, fetchSize, userId);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.prefetchData(product, jobId, sql, tableName, fetchSize, userId, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    @PostMapping("/readHistoryData")
    @ResponseBody
    public String getHistoryData(
            @RequestParam("product") String product,
            @RequestParam("jobId") String jobId,
            @RequestParam("sql") String sql,
            @RequestParam("userId") String userId,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus,
            @RequestParam("clusterId") Integer clusterId) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.getHistoryData(product, jobId, sql, userId);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.getHistoryData(product, jobId, sql, userId, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }

        return gson.toJson(msg);
    }

    @PostMapping("/debug")
    @ResponseBody
    public String debug(
            @RequestParam("product") String product,
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId,
            @RequestParam("sql") String sql,
            @RequestParam("uniqueId") String uniqueId,
            @RequestParam("operation") String operation,
            @RequestParam("source") String sourceKind,
            @RequestParam("sink") String sinkKind,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus,
            @RequestParam("clusterId") Integer clusterId) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.debug(product, jobId, userId, sql, uniqueId, operation, sourceKind, sinkKind);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.debug(product, jobId, userId, sql, uniqueId, operation, sourceKind, sinkKind, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    @PostMapping("/debugInfo")
    @ResponseBody
    public String getDebugInfo(
            @RequestParam("product") String product,
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId) {
        ResultMessage message = jobService09.getDebugStatus(jobId);
        return gson.toJson(message);
    }

    @PostMapping("/preDebug")
    @ResponseBody
    public String preDebug(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("sql") String sql,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus,
            @RequestParam("clusterId") Integer clusterId) {
        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.preDebug(product, jobId, userId, sql);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.preDebug(product, jobId, userId, sql, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    @PostMapping("/saveData")
    @ResponseBody
    public String saveData(
            @RequestParam("product") String product,
            @RequestParam("jobId") String jobId,
            @RequestParam("userId") String userId,
            @RequestParam("tableName") String tableName,
            @RequestParam("data") String data,
            @RequestParam("uniqueId") String uniqueId,
            @RequestParam("serverId") Integer serverId,
            @RequestParam("versionStatus") Integer versionStatus,
            @RequestParam("clusterId") Integer clusterId) {

        ResultMessage msg;
        if (JOB_VERSION_OLD.equals(versionStatus)) {
            msg = jobService.saveData(product, jobId, tableName, data, uniqueId);
        } else if (JOB_VERSION_NEW.equals(versionStatus)) {
            msg = jobService09.saveData(product, jobId, tableName, data, uniqueId, serverId, clusterId);
        } else {
            msg = ResultMessageUtils.cookFailedMessage("job versionStatus error :" + versionStatus);
        }
        return gson.toJson(msg);
    }

    @PostMapping("/getMetricInfo")
    @ResponseBody
    public String getMetricInfo(
            @RequestParam("product") String product,
            @RequestParam("userId") String userId,
            @RequestParam("jobId") String jobId,
            @RequestParam("metricKey") String metricKey,
            @RequestParam("startTS") long startTS,
            @RequestParam("endTS") long endTS,
            @RequestParam("totalPoints") long totalPoints) {
        ResultMessage msg = jobService09.getMetricInfo(jobId, metricKey, startTS, endTS, totalPoints);
        return gson.toJson(msg);
    }

    @PostMapping("/getAvailableCluster")
    @ResponseBody
    public String getAvailableCluster(@RequestParam("product") String product) {
        ResultMessage msg = jobService09.getAvailableCluster(product);
        return gson.toJson(msg);
    }

    @PostMapping("/getAvailableServer")
    @ResponseBody
    public String getAvailableServer() {
        ResultMessage msg = jobService09.getAvailableServer();
        return gson.toJson(msg);
    }
}

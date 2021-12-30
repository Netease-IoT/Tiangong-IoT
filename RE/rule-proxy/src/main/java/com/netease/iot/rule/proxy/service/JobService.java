package com.netease.iot.rule.proxy.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.netease.iot.rule.proxy.ProxyConfig;
import com.netease.iot.rule.proxy.StreamServerClient;
import com.netease.iot.rule.proxy.StreamServerManager;
import com.netease.iot.rule.proxy.domain.*;
import com.netease.iot.rule.proxy.mapper.*;
import com.netease.iot.rule.proxy.metadata.*;
import com.netease.iot.rule.proxy.model.*;
import com.netease.iot.rule.proxy.util.CommonUtil;
import com.netease.iot.rule.proxy.util.Constants;
import com.netease.iot.rule.proxy.util.HttpUtil;
import com.netease.iot.rule.proxy.util.ResultMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.netease.iot.rule.proxy.metadata.JobStatusUtil.STATE_TRANSLATE_MAP;


@Component
public class JobService {
    public static final int ONLINE_SQL_CHANGED_CODE = -37;
    public static final String ALL_JAR_ID = "-1";
    public static final String DONE = "DONE";
    public static final String REPEAT = "REPEAT";
    private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
    private static final Map<String, Lock> JOB_LOCK_MAP = Maps.newConcurrentMap();
    private static final Gson GSON = new Gson();
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 40, 1, TimeUnit.HOURS,
            new ArrayBlockingQueue<Runnable>(100));
    @Autowired
    private JarFileMapper jarFileMapper;
    @Autowired
    private OfflineJobMapper offlineJobMapper;
    @Autowired
    private OnlineJobMapper onlineJobMapper;
    @Autowired
    private JarReferMapper jarReferMapper;
    @Autowired
    private ClusterMapper clusterMapper;
    @Autowired
    private SqlTemplateMapper sqlTemplate;
    @Autowired
    private MonitorMetaMapper monitorMetaMapper;
    @Autowired
    private DebugInfoMapper debugInfoMapper;
    @Autowired
    private DebugDataInfoMapper debugDataInfoMapper;
    @Autowired
    private SyntaxCheckMapper syntaxCheckMapper;

    /**
     *  save a job info
     *
     * @param product
     * @param userId
     * @param sql
     * @param jobId
     * @param jobName
     * @return
     */
    public ResultMessage saveJob(String product, String userId, String sql, String jobId, String jobName) {
        final String currentTime = CommonUtil.getCurrentDateStr();
        OfflineJob job = null;
        if (jobId == null || jobId.trim().length() == 0) {
            return ResultMessageUtils.cookFailedMessage("create failed: job " + jobName + " already exists in product " + product);
        } else {
            // 如果sql 相同时，不执行保存操作
            OfflineJob oldJob = offlineJobMapper.getJobDetail(jobId);
            if (Objects.equals(oldJob.getSql(), sql)) {
                job = oldJob;
            } else {
                int ret = offlineJobMapper.saveOldJob(userId, sql, jobId, jobName, currentTime);
                if (ret == 1) {
                    job = offlineJobMapper.getJobDetail(jobId);
                }
            }
        }

        Job result = OfflineJob.getJobByOfflineJob(job);
        if (result != null) {
            setJobJarList(result);
            setJobHasModify(result);
            return ResultMessageUtils.cookSuccessMessage(result);
        } else {
            return ResultMessageUtils.cookFailedMessage("create or save job failed.");
        }
    }

    /**
     * do syntax check on sql, result the error pos info and the source list
     *
     * @param product
     * @param userId
     * @param sql
     * @return
     */
    public ResultMessage syntaxCheck(String product, String userId, String jobId, String sql) {
        try {
            if (Strings.isNullOrEmpty(sql)) {
                return new ResultMessage(2, null, "sql is null or empty");
            }

            final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            if (offlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }

            final StreamServerClient streamServerClient = new StreamServerManager().getStreamServerFromDB(
                     StreamServerManager.ClientType.Web,
                    product,
                    offlineJob.getClusterId(),
                    clusterMapper);
            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + product);
            }

            LinkedList<byte[]> jarFiles = new LinkedList<>();
            List<JarRefer> jarList = jarReferMapper.getJobJarList(jobId, Constants.OFFLINE);
            for (JarRefer jarRefer : jarList) {
                JarFile jarFile = jarFileMapper.getJar(jarRefer.getJarId());
                if (jarFile != null) {
                    jarFiles.add(Files.readAllBytes(Paths.get(jarFile.getFilePath())));
                }
            }
            final BaseMessage message = new JobSyntaxCheckMessage(product, offlineJob.getJobName(), sql, jarFiles);
            return requestServer(streamServerClient, message, false);
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }


    /**
     * set job's status (start: 1, delete: 2, stop: 3)
     *
     * @param userId
     * @param jobId
     * @param status
     * @return
     */
    public ResultMessage setJobStatus(String product, String userId, String jobId, String status, String keepState) {

        Lock jobLock = JOB_LOCK_MAP.computeIfAbsent(jobId, k -> new ReentrantLock());

        boolean getLock = jobLock.tryLock();
        if (!getLock) {
            return ResultMessageUtils.cookFailedMessage("Job is operated, please try again later.");
        }
        try {
            if (!jobLock.equals(JOB_LOCK_MAP.get(jobId))) {
                return ResultMessageUtils.cookFailedMessage("Job is operated, please try again later.");
            }

            final OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (onlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }
            product = onlineJob.getProduct();
            Integer clusterId = onlineJob.getClusterId();
            if (clusterId == null) {
                return ResultMessageUtils.cookFailedMessage("no cluster found for job:" + jobId);
            }

            final StreamServerClient streamServerClient = new  StreamServerManager().getStreamServerFromDB(
                     StreamServerManager.ClientType.Web,
                    product,
                    clusterId,
                    clusterMapper);

            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + product);
            }

            if (streamServerClient.getCluster() == null) {
                return ResultMessageUtils.cookFailedMessage(
                        "Sorry, can't find a cluster for you, Please contact {hzdaidang@corp.netease.com} to apply resource");
            }

            Job job = OnlineJob.getJobByOnlineJob(onlineJob, SqlTypeEnum.DEPLOYED);
            switch (Integer.valueOf(status)) {
            case Constants.RUN:

                Cluster cluster = clusterMapper.getClusterById(clusterId);
                if (cluster == null) {
                    return ResultMessageUtils.cookFailedMessage("Can't find any cluster for product:" + product);
                } else {
                    int jobExistsFlag = isJobExistsOnCluster(cluster, job.getProduct(), job.getJobName());
                    if (jobExistsFlag != 0) {
                        if (jobExistsFlag == 1) {
                            return ResultMessageUtils.cookFailedMessage("Job [" + job.getJobName() + "] is already exists on target cluster.");
                        } else {
                            return ResultMessageUtils.cookFailedMessage("Target cluster is un-reachable currently.");
                        }
                    }

                    if (job.getStatus() == TaskStateEnum.SUSPENDED.ordinal()) { //suspend
                        LOG.info("resuming old job:" + job.getId());
                        // todo better way to detect sql change
                        if ("".equals(keepState) && !onlineJob.getDeployedSql().equals(onlineJob.getExecutedSql())) {
                            return new ResultMessage(ONLINE_SQL_CHANGED_CODE, null,
                                    "sql has changed, the state may not be compatible, do you still want to use the formal state?");
                        }
                        if ("".equalsIgnoreCase(keepState)) {
                            keepState = "true";
                        }
                    } else {
                        LOG.info("starting new job:" + jobId);
                        onlineJobMapper.updateStartTime(job.getId(), CommonUtil.getCurrentDateStr(), CommonUtil.getCurrentDateStr());
                        if ("".equalsIgnoreCase(keepState)) {
                            keepState = "false";
                        }
                    }

                    BaseMessage message = null;
                    try {
                        message = buildJobCreateOrResumeMessage(job, job.getStatus(), Boolean.valueOf(keepState));
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResultMessageUtils.cookFailedMessage(e.getMessage());
                    }

                    ResultMessage runResultMessage = startJob(streamServerClient, jobId, message);

                    return runResultMessage;
                }
            case Constants.DELETE:
                return deleteJob(streamServerClient, job);
            case Constants.STOP:
                return stopJob(streamServerClient, job);
            }

            return ResultMessageUtils.cookFailedMessage("unknown job status:" + status);
        } finally {
            jobLock.unlock();
        }
    }

    /**
     * @param cluster cluster object
     * @param product product name
     * @param jobName job name
     * @return 1 means exists, 0 means not exists, -1 means cluster un-reachable
     */
    int isJobExistsOnCluster(Cluster cluster, String product, String jobName) {
        String webUI = cluster.getClusterWebUi();
        try {
            String webResult = HttpUtil.get(webUI + "/joboverview");
            JsonParser jsonParser = new JsonParser();
            JsonElement jobsInfoEle = jsonParser.parse(webResult);
            JsonArray runningJobs = jobsInfoEle.getAsJsonObject().getAsJsonArray("running");

            int flag = 0;
            for (int i = 0; i < runningJobs.size(); ++i) {
                JsonObject jobObject = runningJobs.get(i).getAsJsonObject();
                String runningJobName = jobObject.get("name").getAsString();
                // This way is for backwards compatible
                String[] jobNameSegs = runningJobName.split(Constants.JOB_NAME_SPLITER, 2);
                if (jobNameSegs.length == 2) {
                    if (Objects.equals(product, jobNameSegs[0])
                            && Objects.equals(jobName, jobNameSegs[1].replaceFirst(Constants.JOB_NAME_SPLITER, ""))) {
                        flag = 1;
                        break;
                    }
                }
            }
            return flag;
        } catch (Exception ignored) {
            LOG.warn("Query cluster ui {} failed. {}", webUI, ignored);
            return -1;
        }
    }

    private BaseMessage buildJobCreateOrResumeMessage(Job job, int taskEnum, boolean keepState) throws Exception {
        final String sqlFile = job.getSql();
        if (sqlFile == null || sqlFile.isEmpty()) {
            LOG.error("Can't build a empty job.");
            return null;
        }

        final String jobConf = job.getConf();
        JsonObject jsonObj;
        try {
            jsonObj = new JsonParser().parse(jobConf).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Parse JobConf failed.");
        }

        final String jobName = job.getJobName();
        final String product = job.getProduct();

        Properties properties = new Properties();
        for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                properties.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        List<JarRefer> jarIdList = jarReferMapper.getJobJarList(job.getId(), Constants.ONLINE);
        LinkedList<byte[]> jarFiles = new LinkedList<>();
        for (JarRefer jarRefer : jarIdList) {
            JarFile jarFile = jarFileMapper.getJar(jarRefer.getJarId());
            if (jarFile != null) {
                jarFiles.add(Files.readAllBytes(Paths.get(jarFile.getFilePath())));
            }
        }

        BaseMessage message = null;
        if (taskEnum == TaskStateEnum.SUSPENDED.ordinal()) {
            JobResumeMessage jobResumeMessage = new JobResumeMessage(product, jobName, sqlFile, properties);
            jobResumeMessage.jobCreateMessage.setJarFiles(jarFiles);

            String savepointPath = job.getSavePointPath();

            jobResumeMessage.jobCreateMessage.setJob(new FlinkJob(job.getId(), job.getFlinkJobId(),
                    savepointPath, job.getJarPath()));

            jobResumeMessage.jobCreateMessage.setStartWithSavePoint(keepState);
            message = jobResumeMessage;
        } else {
            JobCreateMessage jobCreateMessage = new JobCreateMessage(product, jobName, sqlFile, properties);
            jobCreateMessage.setJob(new FlinkJob(job.getId(), null, null, job.getJarPath()));
            jobCreateMessage.setJarFiles(jarFiles);
            jobCreateMessage.setStartWithSavePoint(keepState);
            message = jobCreateMessage;
        }

        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultMessage startJob(StreamServerClient streamServerClient, String jobId, BaseMessage msg) {
        try {
            onlineJobMapper.setJobStatus(jobId, TaskStateEnum.READY.ordinal(), CommonUtil.getCurrentDateStr());
            return requestServer(streamServerClient, msg, false);
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse job conf failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultMessage deleteJob(StreamServerClient streamServerClient, Job job) {
        try {
            final String jobName = job.getJobName();
            //streamServerClient.getProudct() may get the product of common cluster.
            final String project = job.getProduct();
            final ResultMessage result;
            if (job.getStartTime() != null
                    && isJobExistsOnCluster(clusterMapper.getClusterById(job.getClusterId()), project, jobName) != 0) {

                final BaseMessage message = new JobCancelMessage(project, jobName);
                ResultMessage cancelResult = requestServer(streamServerClient, message, false);
                if (!cancelResult.isSuccess()) {
                    return cancelResult;
                }
            }

            jarReferMapper.delReference(job.getId(), "-1", Constants.ONLINE);
            monitorMetaMapper.deleteMeta(job.getId());
            //for now, save savepoint to offline job, once let it online, we can directly use it;
            if (job.getSavePointPath() != null) {
                offlineJobMapper.saveSavePointToOffLine(job.getId(), job.getSavePointPath());
            }
            if (!onlineJobMapper.removeJob(job.getId())) {
                return ResultMessageUtils.cookFailedMessage("delete job failed or delete meta info failed.");
            }

            return ResultMessageUtils.cookSuccessMessage(new Object());
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse job conf failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    public ResultMessage stopJob(StreamServerClient streamServerClient, Job job) {
        try {
            final String jobName = job.getJobName();
            final String project = job.getProduct();

            final BaseMessage message = new JobSuspendMessage(project, jobName);
            final ResultMessage result = requestServer(streamServerClient, message, false);
            return result;
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse result failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    private ResultMessage requestServer(StreamServerClient streamServerClient, BaseMessage message, boolean async) {
        try {
            ResultMessage rs = handlerBeforeSubmit(message);
            // change state failed ,so direct return fail
            if (!rs.isSuccess()) {
                return rs;
            }

            // do not need to go to server
            if (DONE.equals(rs.getMsg()) || REPEAT.equals(rs.getMsg())) {
                return rs;
            }

            final Future<Object> future = streamServerClient.submit(message, ProxyConfig.REQUEST_TIME_OUT);
            if (async) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final ResultMessage serverResult = (ResultMessage) Await.result(
                                    future,
                                    new FiniteDuration(ProxyConfig.GET_RESULT_TIMEOUT, TimeUnit.SECONDS));
                            handlerAfterSubmit(message, serverResult);
                            LOG.info("Get server result success: {}", serverResult);
                        } catch (Throwable e) {
                            LOG.warn("Get server result error.", e);
                        }
                    }
                });
                return ResultMessageUtils.cookSuccessMessage("request submit successfully.");
            } else {
                ResultMessage res = (ResultMessage) Await.result(future, new FiniteDuration(ProxyConfig.GET_RESULT_TIMEOUT, TimeUnit.SECONDS));
                handlerAfterSubmit(message, res);
                return res;
            }
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    private ResultMessage handlerBeforeSubmit(BaseMessage message) {
        OnlineJob job;
        if (message instanceof JobResumeMessage) {
            JobResumeMessage jobResumeMessage = (JobResumeMessage) message;
            try {
                job = onlineJobMapper.getJobDetailByName(jobResumeMessage.product,
                        jobResumeMessage.jobName);
            } catch (Exception e) {
                return new ResultMessage(-1,
                        "Get job information error",
                        e.getMessage());
            }

            if (job == null) {
                return new ResultMessage(false, "job '" + jobResumeMessage.jobName
                        + "' does't exist");
            }

            if (jobResumeMessage.jobCreateMessage.getJob().getSavePointPath() == null) {
                return new ResultMessage(false, "Can't find any savepoint for job " + job.getId());
            }

            if (TaskStateEnum.RESUMING.isSameState(job.getStatus())) {
                return new ResultMessage(true, REPEAT);
            }

            if (transferState(job, TaskStateEnum.RESUMING)) {
                return new ResultMessage(true, "Transfer state success");
            } else {
                return new ResultMessage(false, "Transfer job '" + job.getId() + "' from "
                        + TaskStateEnum.getByValue(job.getStatus()).name() + " to "
                        + TaskStateEnum.RESUMING.name() + " get error");
            }
        } else if (message instanceof JobCreateMessage) {
            JobCreateMessage jobCreateMessage = (JobCreateMessage) message;
            try {
                job = onlineJobMapper.getJobDetailByName(jobCreateMessage.product,
                        jobCreateMessage.jobName);
            } catch (Exception e) {
                return new ResultMessage(-1,
                        "Get job information error",
                        e.getMessage());
            }

            if (TaskStateEnum.RUNNING.isSameState(job.getStatus())) {
                return new ResultMessage(false, "job is already running");
            }

            if (transferState(job, TaskStateEnum.STARTING)) {
                return new ResultMessage(true, "Transfer state success");
            } else {
                return new ResultMessage(false, "Can't transfer state from "
                        + TaskStateEnum.getByValue(job.getStatus()).name() + " to "
                        + TaskStateEnum.STARTING.name());
            }
        } else if (message instanceof JobCancelMessage) {
            JobCancelMessage cancelMessage = (JobCancelMessage) message;
            try {
                job = onlineJobMapper.getJobDetailByName(cancelMessage.product, cancelMessage.jobName);
            } catch (Exception e) {
                return new ResultMessage(-1,
                        "Get job information error",
                        e.getMessage());
            }

            if (job == null) {
                return new ResultMessage(false, "job '" + cancelMessage.jobName + "' does't exist");
            }

            if (TaskStateEnum.DELETING.isSameState(job.getStatus())) {
                return new ResultMessage(true, REPEAT);
            }

            Integer oldStatus = job.getStatus();

            if (transferState(job, TaskStateEnum.DELETING)) {
                if (TaskStateEnum.SUSPENDED.isSameState(oldStatus)) {
                    transferState(job, TaskStateEnum.DELETED);
                    LOG.info("delete job {} successfully", job.getId());
                    //this means we do need to send to server to delete job
                    return new ResultMessage(true, DONE);
                }
            }

            cancelMessage.setJob(new FlinkJob(job.getId(), job.getFlinkJobId(),
                    job.getSavePointPath(), job.getJarPath()));

            return new ResultMessage(true, "Transfer state success");
        } else if (message instanceof JobSuspendMessage) {
            JobSuspendMessage jobSuspendMessage = (JobSuspendMessage) message;
            try {
                job = onlineJobMapper.getJobDetailByName(jobSuspendMessage.product, jobSuspendMessage.jobName);
            } catch (Exception e) {
                return new ResultMessage(-1,
                        "Get job information error",
                        e.getMessage());
            }

            if (job == null) {
                return new ResultMessage(false, "job '" + jobSuspendMessage.jobName
                        + "' does't exist");
            }

            if (TaskStateEnum.SUSPENDING.isSameState(job.getStatus())) {
                return new ResultMessage(true, REPEAT);
            }

            if (transferState(job, TaskStateEnum.SUSPENDING)) {
                jobSuspendMessage.setJob(new FlinkJob(job.getId(), job.getFlinkJobId(),
                        job.getSavePointPath(), job.getJarPath()));
                return new ResultMessage(true, "Transfer state success");
            } else {
                return new ResultMessage(false, "Transfer job '" + job.getId() + "' from "
                        + TaskStateEnum.getByValue(job.getStatus()).name() + " to "
                        + TaskStateEnum.SUSPENDING.name() + " get error");
            }
        } else {
            return new ResultMessage(true, "success");
        }
    }

    private void handlerAfterSubmit(BaseMessage message, ResultMessage res) {
        if (message instanceof JobResumeMessage) {
            JobResumeMessage jobResumeMessage = (JobResumeMessage) message;

            if (res.isSuccess()) {
                String[] jobIdAndJarPath = (String[]) res.getData();
                Preconditions.checkArgument(jobIdAndJarPath != null && jobIdAndJarPath.length == 2);
                onlineJobMapper.updateJobAfterStartSuccessfully(jobResumeMessage.jobCreateMessage.getJob().getJobId(),
                        TaskStateEnum.RUNNING.ordinal(), jobIdAndJarPath[0], null, null, CommonUtil.getCurrentDateStr(), null);
            } else {
                onlineJobMapper.setJobStatus(jobResumeMessage.jobCreateMessage.getJob().getJobId(), TaskStateEnum.OPERATION_FAILED.ordinal(),
                        CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobCreateMessage) {
            JobCreateMessage jobCreateMessage = (JobCreateMessage) message;
            FlinkJob job = jobCreateMessage.getJob();
            if (res.isSuccess()) {
                // jarIdAndJarPath contains jobId and jarPath from server
                String[] jobIdAndJarPath = (String[]) res.getData();
                Preconditions.checkArgument(jobIdAndJarPath != null && jobIdAndJarPath.length == 2);
                onlineJobMapper.updateJobAfterStartSuccessfully(job.getJobId(), TaskStateEnum.RUNNING.ordinal(), jobIdAndJarPath[0],
                        jobIdAndJarPath[1], null, CommonUtil.getCurrentDateStr(), null);
            } else {
                onlineJobMapper.setJobStatus(job.getJobId(), TaskStateEnum.OPERATION_FAILED.ordinal(), CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobCancelMessage) {
            JobCancelMessage jobCancelMessage = (JobCancelMessage) message;
            if (res.isSuccess()) {
                onlineJobMapper.setJobStatus(jobCancelMessage.getJob().getJobId(),
                        TaskStateEnum.DELETED.ordinal(), CommonUtil.getCurrentDateStr());
            } else {
                if (QueryUtil.isJobAlive(jobCancelMessage.getClusterWebUi(),
                        jobCancelMessage.getJob().getFlinkJobId())) {
                    throw new RuntimeException("delete job failed:" + res.getMsg());
                } else {
                    onlineJobMapper.setJobStatus(jobCancelMessage.getJob().getJobId(),
                            TaskStateEnum.DELETED.ordinal(), CommonUtil.getCurrentDateStr());
                }
            }
        } else if (message instanceof JobSuspendMessage) {
            JobSuspendMessage jobSuspendMessage = (JobSuspendMessage) message;

            if (res.isSuccess()) {
                //res.getMsg() will get savePointPath from server
                onlineJobMapper.updateJobAfterStartSuccessfully(jobSuspendMessage.getJob().getJobId(), TaskStateEnum.SUSPENDED.ordinal(),
                        null, null, (String) res.getData(), CommonUtil.getCurrentDateStr(), null);
            } else {
                onlineJobMapper.setJobStatus(jobSuspendMessage.getJob().getJobId(),
                        TaskStateEnum.OPERATION_FAILED.ordinal(), CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobDebugMessage) {
            JobDebugMessage jobDebugMessage = (JobDebugMessage) message;
            if (res.isSuccess()) {
                Map<String, String> map = GSON.fromJson((String) res.getData(), Map.class);
                if (map != null) {
                    String jobId = jobDebugMessage.getSourceData().get(Constants.JOB_ID);
                    String unique = jobDebugMessage.getSourceData().get(Constants.UNIQUE_ID);
                    List<DebugDataInfo> list = Lists.newArrayList();
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        String tableName = entry.getKey();
                        list.add(new DebugDataInfo(jobId, tableName, getTopicName(jobDebugMessage, unique, tableName)));
                    }
                    if (!list.isEmpty()) {
                        debugDataInfoMapper.batchRepalceIntoDebugDataInfo(list);
                    }
                }
            }
        }
    }

    private String getTopicName(BaseMessage baseMessage, String uniqueId, String tableName) {
        return new StringBuilder().append(baseMessage.product).append("_").append(baseMessage.jobName)
                .append("_").append(uniqueId).append("_").append(tableName).toString();
    }

    protected boolean transferState(OnlineJob job, TaskStateEnum state) {
        if (job == null) {
            return false;
        }
        final Set<TaskStateEnum> stateSet = STATE_TRANSLATE_MAP.get(TaskStateEnum.getByValue(job.getStatus()));
        if (stateSet != null && stateSet.contains(state)) {
            return onlineJobMapper.setJobStatus(job.getId(), state.ordinal(), CommonUtil.getCurrentDateStr());
        }
        return false;
    }

    /**
     * Delete offline job
     *
     * @param jobId
     * @param userId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage deleteOfflineJob(String jobId, String userId) {
        OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
        if (onlineJob != null) {
            return ResultMessageUtils.cookFailedMessage("job " + jobId + " is online and can't be deleted");
        }
        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        if (debugInfo != null && debugInfo.getStatus() != DebugStatusEnum.STOP.getIndex()) {
            return ResultMessageUtils.cookFailedMessage("job " + jobId + " is being debugged by " + debugInfo.getUserId());
        }
        OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob != null) {
            offlineJobMapper.deleteJob(jobId);
            monitorMetaMapper.deleteMeta(jobId);
            jarReferMapper.delReference(jobId, ALL_JAR_ID, Constants.OFFLINE);
        }
        return ResultMessageUtils.cookSuccessMessage(new Object());
    }


    public ResultMessage debug(String product, String jobId, String userId, String sql,
                                String uniqueId, String operation, String sourceKind, String sinkKind) {

        final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        Map<String, String> map = Maps.newHashMap();
        map.put(Constants.UNIQUE_ID, uniqueId);
        map.put(Constants.OPERATION, operation);
        map.put(Constants.SOURCE, sourceKind);
        map.put(Constants.SINK, sinkKind);
        int op = Integer.valueOf(operation);

        String newTableMessage = "";
        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        SyntaxCheck syntaxCheck = syntaxCheckMapper.getSyntaxCheckMessage(jobId, userId);
        Preconditions.checkNotNull(syntaxCheck, "Can't find SyntaxCheck message for jobId '" + jobId + "' in DB");
        if (op == DebugOperationEnum.RUN.getCode()) {
            ResultMessage newMessage = syntaxCheck(product, userId, jobId, sql);
            if (!newMessage.isSuccess()) {
                return new ResultMessage(false, "sync check failed");
            }
            ResultMessage resultMessage = sqlChangeCheck((String) newMessage.getData(), syntaxCheck.getTableMessage());

            if (!resultMessage.isSuccess()) {
                resultMessage.setMsg(resultMessage.getMsg() + " , please press debug button again");
                return resultMessage;
            }
        }

        DebugStatusEnum debugStatusEnum;
        if (DebugOperationEnum.RUN.getCode() == op) {
            debugStatusEnum = DebugStatusEnum.STARTING;
        } else {
            debugStatusEnum = DebugStatusEnum.STOPING;
        }

        if (org.apache.commons.lang.StringUtils.isEmpty(newTableMessage)) {
            newTableMessage = syntaxCheck.getTableMessage();
        }
        try {
            if (debugInfo == null) {
                debugInfoMapper.insertDebugInfo(jobId, uniqueId, debugStatusEnum.getIndex(), userId,
                        newTableMessage, syntaxCheck.getRawSql(), sourceKind, sinkKind);
            } else {
                debugInfoMapper.updateDebugStatus(jobId, uniqueId, debugStatusEnum.getIndex(), userId,
                        newTableMessage, syntaxCheck.getRawSql(), sourceKind, sinkKind);
            }
        } catch (Exception e) {
            LOG.error("Two people are dubug at the same time");
            return ResultMessageUtils.cookFailedMessage("Two people are dubug at the same time, please wait...");
        }

        ResultMessage m = handlerDebug(product, jobId, sql, map, String.valueOf(offlineJob.getServerId()));
        if (!m.isSuccess()) {
            debugInfoMapper.updateDebugStatus(jobId, uniqueId, DebugStatusEnum.STOP.getIndex(),
                    userId, newTableMessage, sql, sourceKind, sinkKind);
            return m;
        }
        if (DebugOperationEnum.RUN.getCode() == op) {
            debugInfoMapper.updateDebugStatus(jobId, uniqueId, DebugStatusEnum.RUNNING.getIndex(),
                    userId, newTableMessage, syntaxCheck.getRawSql(), sourceKind, sinkKind);
        } else {
            debugInfoMapper.updateDebugStatus(jobId, uniqueId, DebugStatusEnum.STOP.getIndex(), userId,
                    newTableMessage, syntaxCheck.getRawSql(), sourceKind, sinkKind);
        }
        return m;
    }

    private ResultMessage sqlChangeCheck(String newTableMessage, String oldTableMessage) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> oldMap = GSON.fromJson(oldTableMessage, type);
        Map<String, Object> newMap = GSON.fromJson(newTableMessage, type);

        Object oldSourceTableMap = oldMap.get("sourceList");
        Object oldSinkTableMap = oldMap.get("sinkList");
        Object oldSourceDef = oldMap.get("sourceTable");
        Object oldSinkDef = oldMap.get("sinkTable");
        Object oldSourceType = oldMap.get("sourceTableType");
        Object oldSinkType = oldMap.get("sinkTableType");

        Object newSourceTableMap = newMap.get("sourceList");
        Object newSinkTableMap = newMap.get("sinkList");
        Object newSourceDef = newMap.get("sourceTable");
        Object newSinkDef = newMap.get("sinkTable");
        Object newSourceType = newMap.get("sourceTableType");
        Object newSinkType = newMap.get("sinkTableType");

        boolean b1 = Objects.equals(oldSourceTableMap, newSourceTableMap)
                && Objects.equals(oldSourceDef, newSourceDef)
                && Objects.equals(oldSourceType, newSourceType);
        boolean b2 = Objects.equals(oldSinkTableMap, newSinkTableMap)
                && Objects.equals(oldSinkDef, newSinkDef)
                && Objects.equals(oldSinkType, newSinkType);

        List<String> list = Lists.newArrayList();
        Map<String, List<String>> oldSourceNames = (Map<String, List<String>>) oldSourceDef;
        Map<String, List<String>> newSourceNames = (Map<String, List<String>>) newSourceDef;

        Map<String, List<Object>> oldSourceTypes = (Map<String, List<Object>>) oldSourceType;
        Map<String, List<Object>> newSourceTypes = (Map<String, List<Object>>) newSourceType;

        for (String tableNames : oldSourceNames.keySet()) {
            if (Objects.deepEquals(oldSourceNames.get(tableNames), newSourceNames.get(tableNames))
                    && Objects.deepEquals(oldSourceTypes.get(tableNames), newSourceTypes.get(tableNames))) {
                // name and type are equal
                list.add(tableNames);
            }
        }
        if (!b1 && !b2) {
            return new ResultMessage(Constants.DEBUG_CODE_BOTH_CHANGE, list, "source and sink have changed");
        } else if (b1 && !b2) {
            return new ResultMessage(Constants.DEBUG_CODE_SINK_CHANGE, list, "sink have change");
        } else if (!b1) {
            return new ResultMessage(Constants.DEBUG_CODE_SOURCE_CHANGE, list, "source have change");
        }

        return new ResultMessage(0, list, "success");
    }

    /**
     * Summit debug request to server
     *
     * @param product
     * @param jobId
     * @param sql
     * @param map
     * @return
     */
    private ResultMessage handlerDebug(String product, String jobId, String sql,
                                       Map<String, String> map, String clusterId) {
        try {
            final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            if (offlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }

            final StreamServerClient streamServerClient = new  StreamServerManager().getStreamServerFromDB(
                     StreamServerManager.ClientType.Web,
                    product,
                    Integer.valueOf(clusterId),
                    clusterMapper);

            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + product);
            }

            LinkedList<byte[]> jarFiles = new LinkedList<>();
            List<JarRefer> jarList = jarReferMapper.getJobJarList(jobId, Constants.OFFLINE);
            for (JarRefer jarRefer : jarList) {
                JarFile jarFile = jarFileMapper.getJar(jarRefer.getJarId());
                if (jarFile != null) {
                    jarFiles.add(Files.readAllBytes(Paths.get(jarFile.getFilePath())));
                }
            }

            map.put(Constants.JOB_ID, jobId);
            final BaseMessage message = new JobDebugMessage(offlineJob.getProduct(),
                    offlineJob.getJobName(), sql, jarFiles, map);
            return requestServer(streamServerClient, message, false);
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    /**
     * get the flink web url
     *
     */
    public ResultMessage getFlinkWebUrl(String jobId, String userId) {

        Integer clusterId = onlineJobMapper.getJobClusterId(jobId);
        if (clusterId != null) {
            Cluster cluster = clusterMapper.getClusterById(clusterId);
            return ResultMessageUtils.cookSuccessMessage(cluster.getClusterWebUi());
        } else {
            return ResultMessageUtils.cookFailedMessage("Can not get web url.");
        }
    }

    @VisibleForTesting
    public boolean setJobMapper(OfflineJobMapper mapper) {
        offlineJobMapper = mapper;
        return false;
    }

    @VisibleForTesting
    public boolean setOnlineJobMapper(OnlineJobMapper onlineJobMapper) {
        this.onlineJobMapper = onlineJobMapper;
        return false;
    }

    @VisibleForTesting
    public boolean setClusterMapper(ClusterMapper mapper) {
        this.clusterMapper = mapper;
        return false;
    }

    @VisibleForTesting
    public boolean setSqlTemplateMapper(SqlTemplateMapper mapper) {
        this.sqlTemplate = mapper;
        return true;
    }

    /**
     * Set a job jar reference list
     *
     * @param job
     * @return job which has set his jar reference
     */
    private Job setJobJarList(Job job) {
        List<JarRefer> jarRefers = jarReferMapper.getJobJarList(job.getId(), job.getType());
        List<String> jarLists = Lists.newArrayList();
        for (JarRefer jarRefer : jarRefers) {
            jarLists.add(jarRefer.getJarId());
        }

        job.setJarList(jarLists);
        return job;
    }

    /**
     * Set a job sql, jar refer, or conf has change or not
     *
     * @param job xxxx
     * @return
     */
    private Job setJobHasModify(Job job) {

        Job anotherJob = null;
        // if job is online, we find offline job
        if (job.getType() == Constants.ONLINE) {
            anotherJob = OfflineJob.getJobByOfflineJob(
                    offlineJobMapper.getJobDetail(job.getId()));
            // if job is offline, we find online job
        } else if (job.getType() == Constants.OFFLINE) {
            anotherJob = OnlineJob.getJobByOnlineJob(
                    onlineJobMapper.getJobDetail(job.getId()), SqlTypeEnum.EXECUTED);
        }

        if (anotherJob == null) {
            job.setHasModify(false);
            return job;
        }

        job.setHasModify(anotherJob.getVersion() != job.getVersion());
        return job;
    }

    public ResultMessage saveData(String product, String jobId, String tableName, String data, String uniqueId) {
        final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        StreamServerClient streamServerClient = getStreamServerClient(offlineJob.getServerId(), product);
        if (streamServerClient == null) {
            return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + offlineJob.getProduct());
        }

        final BaseMessage jobSaveDataMessage = new JobSaveDataMessage(offlineJob.getProduct(), offlineJob.getJobName(),
                null, null, tableName, data, uniqueId);
        return requestServer(streamServerClient, jobSaveDataMessage, true);
    }

    public ResultMessage prefetchData(String product, String jobId, String sql, String tableName,
                                      String fetchSize, String userId) {
        try {
            //Other parameter not null check have been done by spring, we only need to
            // check fetchSize is a valid number or not
            Preconditions.checkArgument(Integer.valueOf(fetchSize) >= 0);
        } catch (Exception e) {
            return ResultMessageUtils.cookFailedMessage("Invalid value of fetchSize, should be a number, please check...");
        }

        OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        SyntaxCheck jobSyntaxCheckMessage = syntaxCheckMapper.getSyntaxCheckMessage(jobId, userId);
        Preconditions.checkNotNull(jobSyntaxCheckMessage, "Can't find SyntaxCheck message for jobId '" + jobId + "' in DB");

        ResultMessage message = syntaxCheck(product, userId, jobId, sql);
        if (!message.isSuccess()) {
            return ResultMessageUtils.cookFailedMessage("syntax check failed, you may have change sql");
        }

        message = sqlChangeCheck((String) message.getData(), jobSyntaxCheckMessage.getTableMessage());
        if (!message.isSuccess()) {
            return ResultMessageUtils.cookFailedMessage("sql may have change after syntax check: " + message.getMsg());
        }

        Type mapTypeToken = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> option = GSON.fromJson(jobSyntaxCheckMessage.getTableMessage(), mapTypeToken);
        Map<String, Map<String, String>> optionList = (Map<String, Map<String, String>>) option.get("sourceList");
        Map<String, String> op = optionList.get(tableName);
        op.put(Constants.FETCH_SIZE, fetchSize);
        op.put(Constants.TABLE_NAME, tableName);

        StreamServerClient streamServerClient = getStreamServerClient(offlineJob.getServerId(), product);
        if (streamServerClient == null) {
            return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + offlineJob.getProduct());
        }

        LinkedList<byte[]> jarFiles = new LinkedList<>();
        List<JarRefer> jarList = jarReferMapper.getJobJarList(jobId, Constants.OFFLINE);
        try {
            for (JarRefer jarRefer : jarList) {
                JarFile jarFile = jarFileMapper.getJar(jarRefer.getJarId());
                if (jarFile != null) {
                    jarFiles.add(Files.readAllBytes(Paths.get(jarFile.getFilePath())));
                }
            }
        } catch (Exception e) {
            LOG.error("can't get jar refer for job: {}" + jobId);
            return ResultMessageUtils.cookFailedMessage("can't get jar refer for job: " + jobId);
        }

        Preconditions.checkNotNull(offlineJob, "No offline job found for jobId '" + jobId + "'");
        final BaseMessage fetchDataMessage = new FetchDataMessage(product, offlineJob.getJobName(),
                jobSyntaxCheckMessage.getRawSql(), jarFiles, GSON.toJson(op));
        return requestServer(streamServerClient, fetchDataMessage, false);
    }

    public ResultMessage getHistoryData(String product, String jobId, String sql, String userId) {

        OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        SyntaxCheck jobSyntaxCheckMessage = syntaxCheckMapper.getSyntaxCheckMessage(jobId, userId);
        Preconditions.checkNotNull(jobSyntaxCheckMessage, "Can't find SyntaxCheck message for jobId '" + jobId + "' in DB");

        ResultMessage message = syntaxCheck(product, userId, jobId, sql);
        if (!message.isSuccess()) {
            return ResultMessageUtils.cookFailedMessage("syntax check failed, you may have changed sql");
        }

        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        String optionMap;
        List<String> tableNames;
        if (debugInfo != null) {
            optionMap = debugInfo.getSqlString();
            ResultMessage rs = sqlChangeCheck((String) message.getData(), optionMap);
            tableNames = (List<String>) rs.getData();
        } else {
            Type mapTypeToken = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> option = GSON.fromJson((String) message.getData(), mapTypeToken);
            Map<String, Map<String, String>> optionList = (Map<String, Map<String, String>>) option.get("sourceList");
            tableNames = Lists.newArrayList(optionList.keySet());
        }

        if (tableNames.isEmpty()) {
            LOG.info("No history data");
            return new ResultMessage(0, null, "success, but have no history data");
        }

        List<DebugDataInfo> debugDataInfos = debugDataInfoMapper.batchGetDebugDataInfo(jobId, tableNames);
        if (debugDataInfos.size() == 0) {
            return new ResultMessage(0, null, "No history data");
        }

        Map<String, String> tableTopic = Maps.newHashMap();
        for (DebugDataInfo debugDataInfo : debugDataInfos) {
            if (debugDataInfo.getLastTopic() == null) {
                continue;
            }
            tableTopic.put(debugDataInfo.getTableName(), debugDataInfo.getLastTopic());
        }

        StreamServerClient streamServerClient = getStreamServerClient(offlineJob.getServerId(), product);
        if (streamServerClient == null) {
            return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + offlineJob.getProduct());
        }
        final FetchDataMessage fetchHbase = new FetchDataMessage(product, offlineJob.getJobName(), null, null, GSON.toJson(tableTopic));
        fetchHbase.setHistory(true);
        return requestServer(streamServerClient, fetchHbase, false);
    }

    private StreamServerClient getStreamServerClient(Integer clusterId, String product) {
        final StreamServerClient streamServerClient = new  StreamServerManager().getStreamServerFromDB(
                 StreamServerManager.ClientType.Web,
                product,
                clusterId,
                clusterMapper);

        if (streamServerClient == null) {
            return null;
        }
        return streamServerClient;
    }

    public ResultMessage preDebug(String product, String jobId, String userId, String sql) {
        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        ResultMessage resultMessage;
        boolean DebugInfoIsNull = debugInfo == null;
        if (!DebugInfoIsNull && debugInfo.getStatus() != DebugStatusEnum.STOP.getIndex()) {
            // 当前用户不是运行用户，返回码1
            if (!userId.equalsIgnoreCase(debugInfo.getUserId())) {
                return new ResultMessage(1, debugInfo.getUserId(), debugInfo.getUserId() + " is debuging");
            } else {
                //当前用户是运行用启
                resultMessage = syntaxCheck(product, userId, jobId, sql);
                if (resultMessage.isSuccess()) {
                    ResultMessage sqlChangeResult = sqlChangeCheck((String) resultMessage.getData(), debugInfo.getSqlString());
                    //sql 有改变
                    if (!sqlChangeResult.isSuccess()) {
                        return new ResultMessage(3, debugInfo.getUserId(), sqlChangeResult.getMsg()
                                + "\nyou may do one of:\n"
                                + "1. Stop the debug job first and click the debug button again\n"
                                + "2. Still debuging with the current running debug job\n");
                    } else {
                        //sql无改变
                        return new ResultMessage(1, debugInfo.getUserId(), debugInfo.getUserId() + " is debuging");
                    }
                } else {
                    return new ResultMessage(2, debugInfo.getUserId(), "syntax check failed");
                }
            }
        }
        //首次运行或没有人正在调试
        resultMessage = syntaxCheck(product, userId, jobId, sql);
        if (resultMessage.isSuccess()) {
            if (syntaxCheckMapper.getSyntaxCheckMessage(jobId, userId) == null) {
                syntaxCheckMapper.insertSyntaxMessage(jobId, userId, sql, (String) resultMessage.getData());
            } else {
                syntaxCheckMapper.updateSyntaxMessage(jobId, userId, sql, (String) resultMessage.getData());
            }
            return resultMessage;
        } else {
            return new ResultMessage(2, null, "syntax check failed");
        }
    }

    private String jsonArrayString2CommaDelimitedString(String jsonString) {

        StringBuilder stringBuilder = new StringBuilder();
        JsonArray jsonArray = new JsonParser().parse(jsonString).getAsJsonArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            stringBuilder.append(jsonArray.get(i).getAsString()).append(",");
        }
        return stringBuilder.toString();
    }
}

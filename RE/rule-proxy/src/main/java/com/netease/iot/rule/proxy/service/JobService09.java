package com.netease.iot.rule.proxy.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.netease.iot.rule.proxy.ProxyConfig;
import com.netease.iot.rule.proxy.StreamServerClient09;
import com.netease.iot.rule.proxy.StreamServerManager09;
import com.netease.iot.rule.proxy.domain.*;
import com.netease.iot.rule.proxy.mapper.*;
import com.netease.iot.rule.proxy.metadata.DebugOperationEnum;
import com.netease.iot.rule.proxy.metadata.DebugStatusEnum;
import com.netease.iot.rule.proxy.metadata.FlinkJob09;
import com.netease.iot.rule.proxy.metadata.TaskStateEnum;
import com.netease.iot.rule.proxy.metric.MetricUtil;
import com.netease.iot.rule.proxy.model.*;
import com.netease.iot.rule.proxy.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.netease.iot.rule.proxy.metadata.JobStatusUtil.STATE_TRANSLATE_MAP;


/**
 * Service class to handle all requests (Web & Proxy)
 *
 * @author daidan
 */
@Component
public class JobService09 {
    public static final String BASE_PATH = "/tmp";
    public static final int JAR_INUSE_CODE = -47;
    public static final int ONLINE_SQL_CHANGED_CODE = -37;
    public static final int JOB_RUNNING_CODE = -27;
    public static final String ALL_JAR_ID = "-1";
    public static final int TEMPLATE_ROOT_ID = 0;
    public static final String DONE = "DONE";
    public static final String REPEAT = "REPEAT";
    public static final long DEFAULT_POINTS = 60;
    private static final Logger LOG = LoggerFactory.getLogger(JobService09.class);
    //    private static final Map<String, Lock> JOB_LOCK_MAP = Maps.newConcurrentMap();
    private static final Gson GSON = new Gson();
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(20, 40, 1, TimeUnit.HOURS,
            new ArrayBlockingQueue<Runnable>(100));
    //    public MetricBackend metricBackend = new MetricHBaseBackend();
    private Type flinkWebResultType = new TypeToken<HashMap<String, ArrayList<HashMap<String, String>>>>() {
    }.getType();
    @Autowired
    private JarFileMapper jarFileMapper;
    @Autowired
    private OfflineJobMapper offlineJobMapper;
    @Autowired
    private OnlineJobMapper onlineJobMapper;
    @Autowired
    private JobParameterMapper jobParameter;
    @Autowired
    private JarReferMapper jarReferMapper;
    @Autowired
    private DebugDataMapper debugDataMapper;
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
    @Autowired
    private ServerMapper serverMapper;
    @Autowired
    private ClusterConfigMapper clusterConfigMapper;
    @Autowired
    private ProductResourceMapper resourceMapper;

    private static String getMD5(String message) {
        return DigestUtils.md5Hex(message);
    }


    /**
     * upload jar api
     *
     * @param product
     * @param userId
     * @param jarData
     * @return
     */
    public ResultMessage uploadJar(String product, String userId, String explanation, MultipartFile jarData) {
        try {
            //1. save on server
            final String originalUploadFilename = jarData.getOriginalFilename();
            if (!originalUploadFilename.endsWith(".jar")) {
                return ResultMessageUtils.cookFailedMessage("only accept jar file.");
            }
            byte[] jarBytes = jarData.getBytes();
            int jarSize = jarBytes.length;
            final String jarId = getMD5(CommonUtil.getRepresentString(product, originalUploadFilename)); // generate jarId from jar name and product

            final String uploadedPath = ProxyConfig.UPLOAD_JAR_PATH + File.separator + jarId + ".jar";

            final File jarFile = new File(uploadedPath);
            if (jarFile.exists()) {
                return ResultMessageUtils.cookFailedMessage("jar is already exists.");
            }

            if (jarFileMapper.getJar(jarId) != null) {
                return ResultMessageUtils.cookFailedMessage("jar is already exists.");
            }

            FileUtils.writeByteArrayToFile(jarFile, jarBytes);

            //2. record into db
            boolean ret = jarFileMapper.insertJar(product, jarId, originalUploadFilename, uploadedPath, jarSize, explanation, CommonUtil.getCurrentDateStr());
            if (ret) {
                return ResultMessageUtils.cookSuccessMessage(new JarReturn(jarId, uploadedPath));
            } else {
                return ResultMessageUtils.cookFailedMessage("failed");
            }
        } catch (Exception e) {
            return ResultMessageUtils.cookFailedMessage("failed:" + e.getMessage());
        }
    }

    /**
     * remove jar api
     *
     * @param product
     * @param userId
     * @param jarId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage removeJar(String product, String userId, String jarId) {
        try {
            List<JarRefer> jobIdListOffline = jarReferMapper.getJobIdList(jarId, Constants.OFFLINE);
            List<JarRefer> jobIdListOnline = jarReferMapper.getJobIdList(jarId, Constants.ONLINE);
            if (jobIdListOffline.isEmpty() && jobIdListOnline.isEmpty()) {
                final JarFile jarFile = jarFileMapper.getJar(jarId);
                if (jarFile != null) {
                    jarFileMapper.removeJar(jarId);
                    if (deleteFile(jarFile.getFilePath())) {
                        return ResultMessageUtils.cookSuccessMessage("success");
                    } else {
                        return ResultMessageUtils.cookFailedMessage("failed: can't delete file on disk.");
                    }
                } else {
                    return ResultMessageUtils.cookFailedMessage("failed: no such fail in db");
                }
            } else {
                List<Job> jobs = new LinkedList<>();
                for (JarRefer jarRefer : jobIdListOffline) {
                    OfflineJob offlineJob = offlineJobMapper.getJobDetail(jarRefer.getJobId());
                    OnlineJob onLineJob = onlineJobMapper.getJobDetail(jarRefer.getJobId());

                    if (offlineJob != null) {
                        jobs.add(OfflineJob.getJobByOfflineJob(offlineJob));
                    } else {
                        LOG.error("The jarRefer info: {} exists in ob_refer_jar table while doesn't exist in job table", jarRefer);
                    }

                    if (onLineJob != null) {
                        jobs.add(OnlineJob.getJobByOnlineJob(onLineJob, SqlTypeEnum.EXECUTED));
                    } else {
                        LOG.error("The jarRefer info: {} exists in job_refer_jar table while doesn't exist in job table", jarRefer);
                    }
                }
                return new ResultMessage(JAR_INUSE_CODE, jobs, "fail");
            }
        } catch (Exception e) {
            return ResultMessageUtils.cookFailedMessage("delete jar failed:" + e.getMessage());
        }
    }

    private boolean deleteFile(String fileName) {
        if (fileName.startsWith(ProxyConfig.UPLOAD_JAR_PATH)) {
            File file = new File(fileName);
            if (file.exists() && file.isFile()) {
                return file.delete();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * get jar list api
     *
     * @param product
     * @param userId
     * @return
     */
    public ResultMessage getJarList(String product, String userId) {
        final List<JarFile> jarList = jarFileMapper.listByProduct(product);
        return ResultMessageUtils.cookSuccessMessage(jarList);
    }

    /**
     * get job list by product and type
     *
     * @param product
     * @param userId
     * @param type
     * @return
     */
    public ResultMessage getJobStatusList(String product, String userId, Integer type) {
        List<Job> jobList = null;
        if (type != null) {
            if (Constants.ONLINE == type) {
                List<OnlineJob> onlineJobList = onlineJobMapper.getJobList(product);
                jobList = OnlineJob.getJobListByOnlineJobList(onlineJobList);
            } else if (Constants.OFFLINE == type) {
                List<OfflineJob> offlineJobList = offlineJobMapper.getJobList(product);
                jobList = OfflineJob.getJobListByOfflineJobList(offlineJobList);
            }
            return ResultMessageUtils.cookSuccessMessage(jobList);
        }
        //come here, we should know type is null , we need to find in offline and online job
        List<OfflineJob> offLineJob = offlineJobMapper.getJobList(product);
        List<OnlineJob> onLineJob = onlineJobMapper.getJobList(product);

        List<JobStatusInfo> jobStatus = Lists.newArrayList();

        Map<String, OnlineJob> stringJobMap = Maps.newHashMap();
        for (OnlineJob job : onLineJob) {
            stringJobMap.put(job.getId(), job);
        }

        for (OfflineJob job : offLineJob) {
            OnlineJob online = stringJobMap.get(job.getId());
            if (online == null) {
                jobStatus.add(new JobStatusInfo(job.getId(), false, false, job.getVersionStatus()));
            } else {
                if (job.getVersion() == online.getVersion()) {
                    jobStatus.add(new JobStatusInfo(job.getId(), true, false, job.getVersionStatus()));
                } else {
                    jobStatus.add(new JobStatusInfo(job.getId(), true, true, job.getVersionStatus()));
                }
            }
        }

        return ResultMessageUtils.cookSuccessMessage(jobStatus);
    }

    /**
     * Get job by page
     *
     * @param product
     * @param userId
     * @param type
     * @param pageSize
     * @param pageNumber
     * @return
     */
    public ResultMessage getJobListByPage(String product, String userId, String type,
                                          Integer pageSize, Integer pageNumber) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("currPage", pageNumber);
        map.put("pageSize", pageSize);
        map.put("product", product);
        map.put("userId", userId);

        List<Job> jobList = null;
        int totalPage = 0;
        if (Constants.OFFLINE == Integer.valueOf(type)) {
            jobList = OfflineJob.getJobListByOfflineJobList(offlineJobMapper.getJobListByPage(map));
            totalPage = offlineJobMapper.getJobListCount(map);
        } else if (Constants.ONLINE == Integer.valueOf(type)) {
            jobList = OnlineJob.getJobListByOnlineJobList(onlineJobMapper.getJobListByPage(map));
            totalPage = onlineJobMapper.getJobListCount(map);
        }

        for (Job job : jobList) {
            setJobJarList(job);
            setJobHasModify(job);
        }

        return ResultMessageUtils.cookSuccessMessage(new PageJobs(getJobClusterServerName(jobList), totalPage));
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
    public ResultMessage createOrSaveJob(String product,
                                         String userId,
                                         String sql,
                                         String jobId,
                                         String jobName,
                                         Integer clusterId,
                                         Integer serverId,
                                         Integer versionStatus) {
        OfflineJob job = null;
        final String currentTime = CommonUtil.getCurrentDateStr();

        if (jobId == null || jobId.trim().length() == 0) {
            jobId = getMD5(CommonUtil.getRepresentString(product, jobName));
            job = offlineJobMapper.getJobDetail(jobId);
            if (job != null) {
                return ResultMessageUtils.cookFailedMessage("create failed: job " + jobName + " already exists in product " + product);
            }
            int jobRet = offlineJobMapper.createJob(product, userId, jobId, jobName, sql, currentTime, clusterId, serverId, versionStatus);
            if (jobRet == 1) {
                job = offlineJobMapper.getJobDetail(jobId);
            }
        } else {
            OfflineJob oldJob = offlineJobMapper.getJobDetail(jobId);
            if (!Objects.equals(oldJob.getClusterId(), clusterId)) {
                OnlineJob oldJobOnline = onlineJobMapper.getJobDetail(jobId);
                if (null != oldJobOnline) {
                    return ResultMessageUtils.cookFailedMessage(" online job exist , you can't change cluster ");
                }
            }
            if (Objects.equals(oldJob.getSql(), sql) && Objects.equals(oldJob.getClusterId(), clusterId)
                    && Objects.equals(oldJob.getServerId(), serverId)
                    && Objects.equals(oldJob.getVersionStatus(), versionStatus)) {
                job = oldJob;
            } else {
                int ret = offlineJobMapper.saveJob(userId, sql, jobId, jobName, currentTime, clusterId, serverId, versionStatus);
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
     * get job's detail info by jobId
     *
     * @param userId
     * @param jobId
     * @return
     */
    public ResultMessage getJobDetail(String userId, String jobId, int type) {

        Job job = null;
        if (Constants.OFFLINE == type) {
            OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            job = OfflineJob.getJobByOfflineJob(offlineJob);
        } else if (Constants.ONLINE == type) {
            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            job = OnlineJob.getJobByOnlineJob(onlineJob, SqlTypeEnum.EXECUTED);
        }

        try {
            if (job != null) {
                getJobClusterServerName(Lists.newArrayList(job));
                setJobJarList(job);
                setJobHasModify(job);
                return ResultMessageUtils.cookSuccessMessage(job);
            } else {
                return ResultMessageUtils.cookSuccessMessage(new JsonObject());
            }
        } catch (Exception e) {
            return ResultMessageUtils.cookFailedMessage("get job detail failed.");
        }
    }

    /**
     * deploy the job from offline to online
     *
     * @param userId
     * @param jobId
     * @return
     */
    public ResultMessage deployJob(String userId, String jobId, String note) {
        boolean finalUpdateOperatrStatus = true;
        try {
            int count = offlineJobMapper.updateOperatingStatus(jobId, Constants.JOB_OPERATING_STATUS_UNLOCK, Constants.JOB_OPERATING_STATUS_LOCK);
            if (count == 0) {
                finalUpdateOperatrStatus = false;
                return ResultMessageUtils.cookFailedMessage("There is somebody else concurrent deploying " + jobId);
            }
            final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            if (offlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("Please create and save job first before deploy a job");
            }

            OnlineJob deployedJob = null;
            final String currentTime = CommonUtil.getCurrentDateStr();

            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (onlineJob == null) {
                try {
                    onlineJobMapper.deployJob(
                            offlineJob.getId(),
                            offlineJob.getJobName(),
                            offlineJob.getSql(),
                            offlineJob.getConf(),
                            offlineJob.getVersion(),
                            offlineJob.getUserId(),
                            userId,
                            offlineJob.getProduct(),
                            offlineJob.getCreateTime(),
                            offlineJob.getEditTime(),
                            note,
                            currentTime,
                            offlineJob.getClusterId(),
                            offlineJob.getServerId(),
                            offlineJob.getVersionStatus(),
                            offlineJob.getSavePointPath()
                    );
                } catch (Exception e) {
                    return ResultMessageUtils.cookFailedMessage("Deploy job failed for job has exist.");
                }
            } else if (TaskStateEnum.RUNNING.equals(TaskStateEnum.getByValue(onlineJob.getStatus()))) {
                return ResultMessageUtils.cookFailedMessage("Deploy job failed for job is running.");
            } else {
                int ret = onlineJobMapper.reDeployJob(
                        offlineJob.getId(),
                        offlineJob.getJobName(),
                        offlineJob.getSql(),
                        offlineJob.getConf(),
                        offlineJob.getVersion(),
                        offlineJob.getUserId(),
                        userId,
                        offlineJob.getProduct(),
                        offlineJob.getCreateTime(),
                        offlineJob.getEditTime(),
                        note,
                        currentTime,
                        offlineJob.getClusterId(),
                        offlineJob.getServerId(),
                        offlineJob.getVersionStatus(),
                        Lists.newArrayList(TaskStateEnum.RUNNING.ordinal(), TaskStateEnum.STARTING.ordinal(), TaskStateEnum.RESUMING.ordinal())
                );
                if (ret != 1) {
                    return ResultMessageUtils.cookFailedMessage("Deploy job failed for job has exist.");
                }
            }

            final List<JarRefer> offlineJarRefers = jarReferMapper.getJobJarList(jobId, Constants.OFFLINE);
            final List<JarRefer> oldOnlineJarRefers = jarReferMapper.getJobJarList(jobId, Constants.ONLINE);

            //delete old online jars
            for (JarRefer jarRefer : oldOnlineJarRefers) {
                jarReferMapper.delReference(jobId, jarRefer.getJarId(), Constants.ONLINE);
            }

            //add new online jars
            for (JarRefer jarRefer : offlineJarRefers) {
                jarReferMapper.addReference(jobId, jarRefer.getJarId(), Constants.ONLINE);
            }

            deployedJob = onlineJobMapper.getJobDetail(jobId);

            Job result = OnlineJob.getJobByOnlineJob(deployedJob, SqlTypeEnum.DEPLOYED);

            if (deployedJob != null) {
                setJobHasModify(result);
                setJobJarList(result);
                monitorMetaMapper.createDefaultMeta(jobId, userId);
                return ResultMessageUtils.cookSuccessMessage(result);
            } else {
                return ResultMessageUtils.cookFailedMessage("Deploy job failed.");
            }
        } catch (Exception e) {
            LOG.error("deployJob failed.", e);
            return ResultMessageUtils.cookFailedMessage("Set job conf failed.");
        } finally {
            if (finalUpdateOperatrStatus) {
                offlineJobMapper.updateOperatingStatus(jobId, Constants.JOB_OPERATING_STATUS_LOCK, Constants.JOB_OPERATING_STATUS_UNLOCK);
            }
        }
    }

    /**
     * set job's conf that will be used when running the job
     *
     * @param userId
     * @param jobId
     * @param conf
     * @return
     */
    public ResultMessage setJobConf(String userId, String jobId, String conf, String product) {

        boolean finalUpdateOperatrStatus = true;
        try {
            int count = offlineJobMapper.updateOperatingStatus(jobId, Constants.JOB_OPERATING_STATUS_UNLOCK, Constants.JOB_OPERATING_STATUS_LOCK);
            if (count == 0) {
                finalUpdateOperatrStatus = false;
                return ResultMessageUtils.cookFailedMessage("There is somebody else concurrent conf " + jobId);
            }

            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (onlineJob != null) {
                int status = onlineJob.getStatus();
                if (TaskStateEnum.RUNNING.ordinal() == status
                        || TaskStateEnum.RESUMING.ordinal() == status
                        || TaskStateEnum.STARTING.ordinal() == status) {
                    return new ResultMessage(JOB_RUNNING_CODE, null, "Job is " + TaskStateEnum.getByValue(status).name());
                }
            }

            OfflineJob job = null;
            OfflineJob oldJob = offlineJobMapper.getJobDetail(jobId);
            String checkResult = checkResource(oldJob.getConf(), conf, product);
            if (null != checkResult) {
                return ResultMessageUtils.cookFailedMessage(checkResult);
            }
            if (Objects.equals(oldJob.getConf(), conf)) {
                return ResultMessageUtils.cookSuccessMessage(oldJob);
            }
            job = offlineJobMapper.getJobDetail(jobId);
            final String currentTime = CommonUtil.getCurrentDateStr();
            final boolean ret = offlineJobMapper.setJobConf(userId, jobId, conf, currentTime);

            Job result = OfflineJob.getJobByOfflineJob(job);
            if (result != null) {
                setJobJarList(result);
                job.setHasModify(true);
                return ResultMessageUtils.cookSuccessMessage(result);
            } else {
                return ResultMessageUtils.cookFailedMessage("Set job conf failed.");
            }
        } catch (Exception e) {
            LOG.error("Set job conf failed.", e);
            return ResultMessageUtils.cookFailedMessage("Set job conf failed.");
        } finally {
            if (finalUpdateOperatrStatus) {
                offlineJobMapper.updateOperatingStatus(jobId, Constants.JOB_OPERATING_STATUS_LOCK, Constants.JOB_OPERATING_STATUS_UNLOCK);
            }
        }
    }

    public String checkResource(String oldConfString, String newConfString, String product) {
        JsonObject oldJsonConf = new JsonParser().parse(oldConfString).getAsJsonObject();
        JsonObject newJsonConf = new JsonParser().parse(newConfString).getAsJsonObject();
        JobResourceUtil jobResourceUtil = new JobResourceUtil(newJsonConf, oldJsonConf);
        ProductResource productResource = resourceMapper.getProductInfo(product);
        if (null != productResource) {
            String message = jobResourceUtil.check(productResource.getTotalSlot(),
                    productResource.getTotalMem(),
                    productResource.getUsedSlot(),
                    productResource.getUsedMem());
            if (null != message) {
                return message;
            }
            resourceMapper.updateResource(productResource.getProductId(), jobResourceUtil.getUpdateSlot(), jobResourceUtil.getUpdateMem());
        }
        return null;
    }

    /**
     * set job's cluster
     *
     * @param userId
     * @param jobId
     * @param clusterId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage setJobClusterId(String userId, String jobId, Integer clusterId, Integer serverId) {

        OfflineJob job = null;
        final String currentTime = CommonUtil.getCurrentDateStr();

        //then,we need to update db, if clusterId is null, we get clusterId from conf or directly
        // use clusterId
        if (null == clusterId || null == serverId) {
            return ResultMessageUtils.cookFailedMessage("Please specify clusterId.");
        }

        OfflineJob oldjob = offlineJobMapper.getJobDetail(jobId);
        // if clusterId is equal, there is no need to update
        if (clusterId.equals(oldjob.getClusterId()) && serverId.equals(oldjob.getServerId())) {
            return ResultMessageUtils.cookSuccessMessage(oldjob);
        }

        final boolean ret = offlineJobMapper.setJobClusterId(userId, jobId, clusterId, serverId, currentTime);
        if (ret) {
            job = offlineJobMapper.getJobDetail(jobId);
        }

        Job result = OfflineJob.getJobByOfflineJob(job);
        if (result != null) {
            setJobJarList(result);
            job.setHasModify(true);
            return ResultMessageUtils.cookSuccessMessage(result);
        } else {
            return ResultMessageUtils.cookFailedMessage("Failing in set job's clusterId.");
        }
    }

    /**
     * set job's monitor that will be used when monitoring the job
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage setJobMonitor(String userId, String jobId, String group, String emails,
                                       String alarmTypes, String interval, String threshold) {

        emails = jsonArrayString2CommaDelimitedString(emails);
        if ("".equals(emails)) {
            emails = userId;
        }

        if ("".equals(alarmTypes)) {
            alarmTypes = Constants.RECEIVE_TYPE_ARRAY;
        }
        alarmTypes = jsonArrayString2CommaDelimitedString(alarmTypes);

        Integer groupNum = Integer.valueOf(group);

        Integer intervalNum = Constants.INTERVAL;
        if (!"".equals(interval)) {
            intervalNum = Integer.valueOf(interval);
        }

        Integer thresholdNum = Constants.THRESHOLD;
        if (!"".equals(threshold)) {
            thresholdNum = Integer.valueOf(threshold);
        }

        monitorMetaMapper.setMonitorMeta(jobId, groupNum, emails,
                alarmTypes, intervalNum, thresholdNum);

        return ResultMessageUtils.cookSuccessMessage("Successfully set cluster.");
    }

    /**
     * get job's monitor that will be used when monitoring the job
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage getJobMonitor(String userId, String jobId) {

        MonitorMeta monitorMeta = monitorMetaMapper.getMeta(jobId);

        return ResultMessageUtils.cookSuccessMessage(monitorMeta);
    }


    /**
     * update monitor group
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage updateMonitorGroup(String group, String emails) {

        if (group == null || emails == null) {
            return ResultMessageUtils.cookFailedMessage("The group or emails can not be null.");
        }

        emails = jsonArrayString2CommaDelimitedString(emails);
        if ("".equals(emails)) {
            return ResultMessageUtils.cookFailedMessage("The group or emails can not be empty.");
        }

        monitorMetaMapper.updateMonitorGroup(Integer.valueOf(group), emails);

        return ResultMessageUtils.cookSuccessMessage("Successfully update monitor group.");
    }

    /**
     * delete monitor group
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage deleteMonitorGroup(String group) {

        List<MonitorMeta> monitorMetas = monitorMetaMapper.selectByGroup(Integer.valueOf(group));

        if (monitorMetas == null || monitorMetas.isEmpty()) {
            return ResultMessageUtils.cookSuccessMessage("Successfully update monitor group.");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (MonitorMeta monitorMeta : monitorMetas) {
                String jobId = monitorMeta.getJobId();
                OfflineJob jobDetail = offlineJobMapper.getJobDetail(jobId);
                stringBuilder.append(jobDetail.getProduct()).append("::").append(jobDetail.getJobName()).append("  ");
            }
            return ResultMessageUtils.cookFailedMessage(
                    "Can not delete monitor group because the following jobs are using it: " + stringBuilder.toString());
        }
    }

    /**
     * get  conf key list
     *
     * @param userId
     * @param jobId
     * @return
     */
    public ResultMessage getJobConfParameterList(String userId, String jobId) {

        List<JobParameter> parameterList = jobParameter.getJobConfParameterList(userId);

        OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
        OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (onlineJob != null) {
            setJobParameter(onlineJob.getConf(), parameterList);
        } else if (offlineJob != null) {
            setJobParameter(offlineJob.getConf(), parameterList);
        }

        if (parameterList.size() > 0) {
            return ResultMessageUtils.cookSuccessMessage(parameterList);
        } else {
            return ResultMessageUtils.cookFailedMessage("Get job parameter list failed.");
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
    public ResultMessage syntaxCheck(String product, String userId, String jobId, String sql, Integer serverId, Integer clusterId) {
        try {

            if (Strings.isNullOrEmpty(sql)) {
                return new ResultMessage(2, null, "sql is null or empty");
            }

            final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            if (offlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }

            final StreamServerClient09 streamServerClient = getStreamServerClient(jobId, serverId, clusterId);
            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("get server client error . check params");
            }

            LinkedList<byte[]> jarFiles = new LinkedList<>();
            List<JarRefer> jarList = jarReferMapper.getJobJarList(jobId, Constants.OFFLINE);
            for (JarRefer jarRefer : jarList) {
                JarFile jarFile = jarFileMapper.getJar(jarRefer.getJarId());
                if (jarFile != null) {
                    jarFiles.add(Files.readAllBytes(Paths.get(jarFile.getFilePath())));
                }
            }
            final BaseMessage09 message = new JobSyntaxCheckMessage09(product, offlineJob.getJobName(), sql, jarFiles,
                    streamServerClient.getResourcePath(),
                    null);
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
        try {

            LOG.info("set job status");
            final OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (onlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }

            ClusterConfig clusterConfig = clusterConfigMapper.getCluster(onlineJob.getClusterId());

            if (null == clusterConfig) {
                return ResultMessageUtils.cookFailedMessage("cluster not found ,clusterId=" + onlineJob.getClusterId());
            }

            product = onlineJob.getProduct();

            final StreamServerClient09 streamServerClient = getStreamServerClient(jobId, onlineJob.getServerId(), onlineJob.getClusterId());

            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + product);
            }

            Job job = OnlineJob.getJobByOnlineJob(onlineJob, SqlTypeEnum.DEPLOYED);
            String webUI = null;
            String applicationId = onlineJob.getApplicationId();
            if (!Strings.isNullOrEmpty(applicationId)) {
                webUI = clusterConfig.getYarnPath() + applicationId;
            }
            switch (Integer.valueOf(status)) {
            case Constants.RUN:
                LOG.info(" do run job webUI is {}", webUI);
                if (null != webUI) {
                    int jobExistsFlag = getJobRunningStatus(webUI);
                    if (jobExistsFlag == 1) {
                        return ResultMessageUtils.cookFailedMessage("Job [" + job.getJobName() + "] is already running.");
                    } else if (jobExistsFlag == 0) {
                        return ResultMessageUtils.cookFailedMessage("Job [" + job.getJobName() + "] is already exist.");
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

                BaseMessage09 message = null;
                try {
                    message = buildJobCreateOrResumeMessage(job, job.getStatus(), Boolean.valueOf(keepState),
                            clusterConfig.getResourcePath(),
                            clusterConfig.getFlinkPath(), clusterConfig.getCpkPath());
                } catch (Exception e) {
                    return ResultMessageUtils.cookFailedMessage(e.getMessage());
                }

                ResultMessage runResultMessage = startJob(streamServerClient, jobId, message);

                return runResultMessage;
            case Constants.DELETE:
                return deleteJob(streamServerClient, job, clusterConfig.getResourcePath(), clusterConfig.getFlinkPath(), webUI);
            case Constants.STOP:
                return stopJob(streamServerClient, job, clusterConfig.getResourcePath(), clusterConfig.getFlinkPath());
            }

            return ResultMessageUtils.cookFailedMessage("unknown job status:" + status);
        } catch (Exception e) {
            LOG.error("Set job status failed.", e);
            return ResultMessageUtils.cookFailedMessage("Set job status failed.");
        }
    }

    /**
     * @param webUI
     * @return 1: job running
     * 0: job exist ,but not running
     * -1: job not exist
     */
    int getJobRunningStatus(String webUI) {
        String webResult = null;
        try {
            webResult = HttpUtil.get(webUI + "/jobs");
        } catch (IOException se) {
            LOG.warn(" flink web UI not found , job is not running . webUI=" + webUI);
            return -1;
        }
        try {
            HashMap<String, ArrayList<HashMap<String, String>>> flinkJobStatusMap = GSON.fromJson(webResult, flinkWebResultType);
            HashMap<String, String> jobInfoMap = flinkJobStatusMap.get("jobs").get(0);

            if (Constants.JOB_RUNNING_NEW.equals(jobInfoMap.get("status"))) {
                return 1;
            } else {
                return 0;
            }
        } catch (JsonSyntaxException e) {
//            LOG.error(" get job status error . webUI=" + webUI);
            return -1;
        }
    }

    /**
     * @return 1 means exists, 0 means not exists, -1 means cluster un-reachable
     */
//    int isJobExistsOnCluster(String webUI, String product, String jobName) {
//        String webResult = null;
//        try {
//            webResult = HttpUtil.get(webUI + "/joboverview");
//        } catch (IOException e) {
//            LOG.warn("Query cluster ui {} failed. {}", webUI, e);
//            return -1;
//        }
//        JsonParser jsonParser = new JsonParser();
//        JsonElement jobsInfoEle;
//        try {
//            jobsInfoEle = jsonParser.parse(webResult);
//        } catch (JsonSyntaxException e) {
//            LOG.info("url exist,but job has been canceled");
//            return 0;
//        }
//        JsonArray runningJobs = jobsInfoEle.getAsJsonObject().getAsJsonArray("running");
//        int flag = 0;
//        for (int i = 0; i < runningJobs.size(); ++i) {
//            JsonObject jobObject = runningJobs.get(i).getAsJsonObject();
//            String runningJobName = jobObject.get("name").getAsString();
//            // This way is for backwards compatible
//            String[] jobNameSegs = runningJobName.split(JOB_NAME_SPLITER, 2);
//            if (jobNameSegs.length == 2) {
//                if (Objects.equals(product, jobNameSegs[0])
//                        && Objects.equals(jobName, jobNameSegs[1].replaceFirst(JOB_NAME_SPLITER, ""))) {
//                    flag = 1;
//                    break;
//                }
//            }
//        }
//        return flag;
//    }
    public BaseMessage09 buildJobCreateOrResumeMessage(Job job, int taskEnum, boolean keepState, String resourcePath,
                                                       String flinkPath,
                                                       String cpkPath) throws Exception {
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

        BaseMessage09 message = null;
        if (taskEnum == TaskStateEnum.SUSPENDED.ordinal()) {
            JobResumeMessage09 jobResumeMessage = new JobResumeMessage09(product, jobName, sqlFile, properties, resourcePath, flinkPath, cpkPath);
            jobResumeMessage.jobCreateMessage.setJarFiles(jarFiles);

            String savepointPath = job.getSavePointPath();

            jobResumeMessage.jobCreateMessage.setJob(new FlinkJob09(job.getId(), job.getFlinkJobId(),
                    savepointPath, job.getJarPath(), job.getApplicationId()));

            jobResumeMessage.jobCreateMessage.setStartWithSavePoint(keepState);
            message = jobResumeMessage;
        } else {

            JobCreateMessage09 jobCreateMessage09 = new JobCreateMessage09(product, jobName, sqlFile, properties, resourcePath, flinkPath, cpkPath);
            jobCreateMessage09.setJob(new FlinkJob09(job.getId(), null, null, job.getJarPath(), job.getApplicationId()));
            jobCreateMessage09.setJarFiles(jarFiles);
            jobCreateMessage09.setStartWithSavePoint(keepState);
            message = jobCreateMessage09;
        }

        return message;
    }

    //    @Transactional(rollbackFor = Exception.class)
    public ResultMessage startJob(StreamServerClient09 streamServerClient, String jobId, BaseMessage09 msg) {
        try {
//            onlineJobMapper.setJobStatus(jobId, TaskStateEnum.READY.ordinal(), CommonUtil.getCurrentDateStr());
            return requestServer(streamServerClient, msg, true);
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse job conf failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    //    @Transactional(rollbackFor = Exception.class)
    public ResultMessage deleteJob(StreamServerClient09 streamServerClient, Job job, String resourcePath, String flinkPath, String webUI) {
        try {

            final String jobName = job.getJobName();
            //streamServerClient.getProudct() may get the product of common cluster.
            final String project = job.getProduct();
            final ResultMessage result;

            if (job.getStartTime() != null && null != webUI
                    && getJobRunningStatus(webUI) == 1) {
                LOG.info("job running now. need to cancel job first");
                final JobCancelMessage09 message = new JobCancelMessage09(project, jobName, resourcePath, webUI, flinkPath);
                message.setJob(new FlinkJob09(job.getId(), job.getFlinkJobId(),
                        job.getSavePointPath(), job.getJarPath(), job.getApplicationId()));
                ResultMessage cancelResult = requestServer(streamServerClient, message, true);
                if (!cancelResult.isSuccess()) {
                    return cancelResult;
                }
            }

            //delete resource
//            try {
//                LOG.info(" delete job ,do delete resource for product :" + job.getProduct());
//                JsonObject jsonObj = new JsonParser().parse(job.getConf()).getAsJsonObject();
//                JobResourceUtil jobResourceUtil = new JobResourceUtil(jsonObj);
//                ProductResource productResource = resourceMapper.getProductInfo(job.getProduct());
//                if (null != productResource) {
//                    if (jobResourceUtil.getNowSlot() > productResource.getUsedSlot() || jobResourceUtil.getNowMem() > productResource.getUsedMem()) {
//                        return cookFailedMessage(
//                                String.format(
//                                        " produce resource error,now slot %s , now mem %s , release  slot %s  , release mem %s at job %s ",
//                                        jobResourceUtil.getNowSlot(),
//                                        jobResourceUtil.getNowMem(),
//                                        productResource.getUsedSlot(),
//                                        productResource.getUsedMem(),
//                                        job.getId()));
//                    } else {
//                        resourceMapper.updateResource(productResource.getProductId(), -jobResourceUtil.getNowSlot(), -jobResourceUtil.getNowMem());
//                    }
//                }
//            } catch (Exception e) {
//                return ResultMessageUtils.cookFailedMessage(" update product resource error ");
//            }

            jarReferMapper.delReference(job.getId(), "-1", Constants.ONLINE);
            monitorMetaMapper.deleteMeta(job.getId());
            //for now, save savepoint to offline job, once let it online, we can directly use it;
            if (job.getSavePointPath() != null) {
                offlineJobMapper.saveSavePointToOffLine(job.getId(), job.getSavePointPath());
            }
            if (!onlineJobMapper.removeJob(job.getId())) {
                return ResultMessageUtils.cookFailedMessage("delete job failed or delete meta info failed.");
            }
            offlineJobMapper.cleanConf(job.getId());

            return ResultMessageUtils.cookSuccessMessage(new Object());
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse job conf failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    public ResultMessage stopJob(StreamServerClient09 streamServerClient, Job job, String resourcePath, String flinkPath) {
        try {
            final String jobName = job.getJobName();
            final String project = job.getProduct();

            final JobSuspendMessage09 message = new JobSuspendMessage09(project, jobName, resourcePath, flinkPath);
            message.setJob(new FlinkJob09(job.getId(), job.getFlinkJobId(),
                    job.getSavePointPath(), job.getJarPath(), job.getApplicationId()));
            final ResultMessage result = requestServer(streamServerClient, message, true);
            return result;
        } catch (JsonParseException e) {
            return ResultMessageUtils.cookFailedMessage("Parse result failed:" + e.getMessage());
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    private ResultMessage requestServer(StreamServerClient09 streamServerClient, BaseMessage09 message, boolean async) {
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
                executor.execute(() -> {
                    try {
                        final ResultMessage serverResult = (ResultMessage) Await.result(
                                future,
                                new FiniteDuration(ProxyConfig.GET_RESULT_TIMEOUT, TimeUnit.SECONDS));
                        handlerAfterSubmit(message, serverResult);
                        LOG.info("Get server result success: {}", serverResult);
                    } catch (Throwable e) {
                        LOG.warn("Get server result error.", e);
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


    private ResultMessage handlerBeforeSubmit(BaseMessage09 message) {
        OnlineJob job;
        if (message instanceof JobResumeMessage09) {
            JobResumeMessage09 jobResumeMessage = (JobResumeMessage09) message;
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
        } else if (message instanceof JobCreateMessage09) {
            JobCreateMessage09 jobCreateMessage09 = (JobCreateMessage09) message;
            try {
                job = onlineJobMapper.getJobDetailByName(jobCreateMessage09.product,
                        jobCreateMessage09.jobName);
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
        } else if (message instanceof JobCancelMessage09) {
            JobCancelMessage09 cancelMessage = (JobCancelMessage09) message;
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

            cancelMessage.setJob(new FlinkJob09(job.getId(), job.getFlinkJobId(),
                    job.getSavePointPath(), job.getJarPath(), job.getApplicationId()));

            return new ResultMessage(true, "Transfer state success");
        } else if (message instanceof JobSuspendMessage09) {
            JobSuspendMessage09 jobSuspendMessage = (JobSuspendMessage09) message;
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
                jobSuspendMessage.setJob(new FlinkJob09(job.getId(), job.getFlinkJobId(),
                        job.getSavePointPath(), job.getJarPath(), job.getApplicationId()));
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

    private void handlerAfterSubmit(BaseMessage09 message, ResultMessage res) {

        offlineJobMapper.updateOperatingStatus(message.jobId, Constants.JOB_OPERATING_STATUS_LOCK, Constants.JOB_OPERATING_STATUS_UNLOCK);

        if (message instanceof JobResumeMessage09) {
            JobResumeMessage09 jobResumeMessage = (JobResumeMessage09) message;

            if (res.isSuccess()) {
                String[] jobIdAndJarPath = (String[]) res.getData();
                Preconditions.checkArgument(jobIdAndJarPath != null && jobIdAndJarPath.length == 3);
                onlineJobMapper.updateJobAfterStartSuccessfully(jobResumeMessage.jobCreateMessage.getJob().getJobId(),
                        TaskStateEnum.RUNNING.ordinal(), jobIdAndJarPath[0],
                        jobIdAndJarPath[1], null, CommonUtil.getCurrentDateStr(), jobIdAndJarPath[2]);
            } else {
                onlineJobMapper.setJobStatus(jobResumeMessage.jobCreateMessage.getJob().getJobId(), TaskStateEnum.FAILED.ordinal(),
                        CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobCreateMessage09) {
            JobCreateMessage09 jobCreateMessage09 = (JobCreateMessage09) message;
            FlinkJob09 job = jobCreateMessage09.getJob();
            if (res.isSuccess()) {
                // jarIdAndJarPath contains jobId and jarPath from server
                String[] jobIdAndJarPath = (String[]) res.getData();
                Preconditions.checkArgument(jobIdAndJarPath != null && jobIdAndJarPath.length == 3);
                onlineJobMapper.updateJobAfterStartSuccessfully(job.getJobId(), TaskStateEnum.RUNNING.ordinal(), jobIdAndJarPath[0],
                        jobIdAndJarPath[1], null, CommonUtil.getCurrentDateStr(), jobIdAndJarPath[2]);
            } else {
                onlineJobMapper.setJobStatus(job.getJobId(), TaskStateEnum.READY.ordinal(), CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobCancelMessage09) {
            JobCancelMessage09 jobCancelMessage09 = (JobCancelMessage09) message;
            if (!res.isSuccess() && (getJobRunningStatus(jobCancelMessage09.getWebUI()) != -1)) {
                onlineJobMapper.setJobStatus(jobCancelMessage09.getJob().getJobId(), TaskStateEnum.FAILED.ordinal(), CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobSuspendMessage09) {
            JobSuspendMessage09 jobSuspendMessage = (JobSuspendMessage09) message;

            if (res.isSuccess()) {
                //res.getMsg() will get savePointPath from server
                onlineJobMapper.updateJobAfterStartSuccessfully(jobSuspendMessage.getJob().getJobId(), TaskStateEnum.SUSPENDED.ordinal(),
                        null, null, null, CommonUtil.getCurrentDateStr(), null);
            } else {
//                onlineJobMapper.setJobStatus(jobSuspendMessage.getJob().getJobId(),
//                        TaskStateEnum.OPERATION_FAILED.ordinal(), CommonUtil.getCurrentDateStr());
            }
        } else if (message instanceof JobDebugMessage09) {
            JobDebugMessage09 jobDebugMessage = (JobDebugMessage09) message;
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

    private String getTopicName(BaseMessage09 baseMessage09, String uniqueId, String tableName) {
        return new StringBuilder().append(baseMessage09.product).append("_").append(baseMessage09.jobName)
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
     * Run the job with some test data.
     *
     * @param userId
     * @param jobId
     * @param sourceList
     * @param jarList
     * @return
     */
    public ResultMessage testRun(String userId, String jobId, String sourceList, String jarList) {
        return null;
    }

    /**
     * Save jar conf of a job
     *
     * @param userId
     * @param jobId
     * @param type
     * @param jar    JarFileList
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage saveJarList(String userId, String jobId, String type, String jar) {
        List<JarRefer> jarList = jarReferMapper.getJobJarList(jobId, Integer.valueOf(type));
        Type setType = new TypeToken<Set<String>>() {
        }.getType();

        Set<String> newJarSet = new Gson().fromJson(jar, setType);
        Set<String> oldJarSet = Sets.newHashSet();

        for (JarRefer jarReference : jarList) {
            oldJarSet.add(jarReference.getJarId());
        }

        if (Objects.equals(oldJarSet, newJarSet)) {
            return ResultMessageUtils.cookSuccessMessage(new Object());
        }

        // come here, should change the database;
        for (String s : oldJarSet) {
            jarReferMapper.delReference(jobId, s, Integer.valueOf(type));
        }

        for (String s : newJarSet) {
            jarReferMapper.addReference(jobId, s, Integer.valueOf(type));
        }

        offlineJobMapper.updateVersion(jobId);
        return ResultMessageUtils.cookSuccessMessage(new Object());
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

    /**
     * Batch get Job
     *
     * @param jobIdList
     * @param product
     * @param userId
     * @param type
     * @return
     */
    public ResultMessage batchGetJob(String jobIdList, String product, String userId, String type) {

        JsonArray jsonObject = new JsonParser().parse(jobIdList).getAsJsonArray();
        List<String> idList = Lists.newArrayList();

        for (int i = 0; i < jsonObject.size(); i++) {
            idList.add(jsonObject.get(i).getAsString());
        }

        if (idList.isEmpty()) {
            return ResultMessageUtils.cookSuccessMessage(new JsonArray());
        }

        List<Job> jobs = null;
        if (Integer.valueOf(type) == Constants.OFFLINE) {
            jobs = OfflineJob.getJobListByOfflineJobList(offlineJobMapper.batchGetJob(idList, product));
        } else if (Integer.valueOf(type) == Constants.ONLINE) {
            jobs = OnlineJob.getJobListByOnlineJobList(onlineJobMapper.batchGetJob(idList, product));
        }
        for (Job job : jobs) {
            setJobJarList(job);
            setJobHasModify(job);
        }

        return ResultMessageUtils.cookSuccessMessage(getJobClusterServerName(jobs));
    }

    public List<Job> getJobClusterServerName(List<Job> jobs) {
        ClusterConfig clusterConfig = null;
        Server server = null;
        for (Job job : jobs) {
            if (null == job) {
                continue;
            }
            clusterConfig = clusterConfigMapper.getCluster(job.getClusterId());
            if (null != clusterConfig) {
                job.setClusterName(clusterConfig.getClusterName());
            }
            List<Server> serverList = serverMapper.getServer(job.getServerId());
            if (null != serverList && serverList.size() > 0) {
                server = serverList.get(0);
            }
            if (null != server) {
                job.setServerName(server.getServerName());
            }
        }
        return jobs;
    }


    public ResultMessage debug(String product, String jobId, String userId, String sql,
                               String uniqueId, String operation, String sourceKind, String sinkKind, Integer serverId, Integer clusterId) {

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
            ResultMessage newMessage = syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
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

        ResultMessage m = handlerDebug(product, jobId, sql, map, clusterId, serverId);
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
                                       Map<String, String> map, Integer clusterId, Integer serverId) {
        try {
            final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
            if (offlineJob == null) {
                return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
            }

            final StreamServerClient09 streamServerClient = getStreamServerClient(jobId, serverId, clusterId);
            if (streamServerClient == null) {
                return ResultMessageUtils.cookFailedMessage("get server client error . check params");
            }

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
            final BaseMessage09 message = new JobDebugMessage09(offlineJob.getProduct(),
                    offlineJob.getJobName(), sql, jarFiles, map, streamServerClient.getResourcePath(), null);
            return requestServer(streamServerClient, message, false);
        } catch (Throwable e) {
            return ResultMessageUtils.cookFailedMessage(e.getMessage());
        }
    }

    /**
     * Get debug data of a job
     *
     * @param product
     * @param jobId
     * @param userId
     * @param tableNames
     * @return
     */
    public ResultMessage getDebugData(String product, String jobId, String userId, String tableNames) {

        JsonArray jsonObject = new JsonParser().parse(tableNames).getAsJsonArray();
        List<String> list = Lists.newArrayList();

        for (int i = 0; i < jsonObject.size(); i++) {
            list.add(jsonObject.get(i).getAsString());
        }
        List<DebugData> DebugDatas = debugDataMapper.getDebugDataList(list, jobId);
        List<DebugData> returnData = Lists.newArrayList();

        Map<String, DebugData> debugDataMap = Maps.newHashMap();
        for (DebugData debugData : DebugDatas) {
            debugDataMap.put(debugData.getTableName(), debugData);
        }

        for (String s : list) {
            if (!debugDataMap.containsKey(s)) {
                debugDataMap.put(s, new DebugData(jobId, s, ""));
            }
        }

        for (Map.Entry<String, DebugData> entry : debugDataMap.entrySet()) {
            returnData.add(entry.getValue());
        }

        return ResultMessageUtils.cookSuccessMessage(returnData);
    }

    /**
     * get the flink web url
     */
    public ResultMessage getFlinkWebUrl(String jobId) {
        try {
            OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
            if (null == onlineJob) {
                return ResultMessageUtils.cookFailedMessage("OnlineJob is null jobId=" + jobId);
            }
            ClusterConfig clusterConfig = clusterConfigMapper.getCluster(onlineJob.getClusterId());
            if (null == clusterConfig) {
                return ResultMessageUtils.cookFailedMessage("ClusterConfig is null clusterId=" + onlineJob.getClusterId());
            }
            String yarnPath = clusterConfig.getYarnPath();
            if (Strings.isNullOrEmpty(yarnPath)) {
                return ResultMessageUtils.cookFailedMessage("yarnPath is null  clusterId=" + onlineJob.getClusterId());
            }
            String applicationId = onlineJob.getApplicationId();
            if (Strings.isNullOrEmpty(applicationId)) {
                return ResultMessageUtils.cookFailedMessage("applicationId is null . task has not been submitted");
            }
            String webUI = yarnPath + applicationId + Constants.FLINK_URL_SUFFIX;
            return ResultMessageUtils.cookSuccessMessage(webUI);
        } catch (Exception e) {
            return ResultMessageUtils.cookFailedMessage("Can not get web url.");
        }
    }

    /**
     * Get a specific page of job by job_name
     *
     * @param product
     * @param userId
     * @param type
     * @param pageSize
     * @param pageNumber
     * @param searchKey
     * @return
     */
    public ResultMessage searchJobListByPage(String product, String userId, String type,
                                             Integer pageSize, Integer pageNumber, String searchKey) {
        Map<String, Object> stringObjectMap = Maps.newHashMap();
        stringObjectMap.put("product", product);
        stringObjectMap.put("pageSize", pageSize);
        stringObjectMap.put("currPage", pageNumber);
        stringObjectMap.put("jobName", searchKey);

        List<Job> jobs = null;
        Integer count = 0;

        if (Constants.OFFLINE == Integer.valueOf(type)) {
            jobs = OfflineJob.getJobListByOfflineJobList(
                    offlineJobMapper.searchJobByJobNameByPage(stringObjectMap));
            count = offlineJobMapper.searchJobByJobNameCount(stringObjectMap);
        } else if (Constants.ONLINE == Integer.valueOf(type)) {
            jobs = OnlineJob.getJobListByOnlineJobList(
                    onlineJobMapper.searchJobByJobNameByPage(stringObjectMap));
            count = onlineJobMapper.searchJobByJobNameCount(stringObjectMap);
        }

        for (Job job : jobs) {
            setJobJarList(job);
            setJobHasModify(job);
        }

        return ResultMessageUtils.cookSuccessMessage(new PageJobs(getJobClusterServerName(jobs), count));
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
    public boolean setClusterConfigMapper(ClusterConfigMapper clusterConfigMapper) {
        this.clusterConfigMapper = clusterConfigMapper;
        return false;
    }

    /**
     * get the (DDL|DML) sql template
     *
     * @return
     */
    public ResultMessage getSqlModel(String version) {
        List<SqlTemplate> tmpTemplateList = getSqlTemplates(version, TEMPLATE_ROOT_ID);
        return ResultMessageUtils.cookSuccessMessage(tmpTemplateList);
    }

    private List<SqlTemplate> getSqlTemplates(String version, int parentId) {
        List<SqlTemplate> templateList = sqlTemplate.getSqlTemplateByParentId(version, parentId);
        if (templateList == null) {
            return null;
        }
        if (templateList.size() > 0) {
            for (SqlTemplate item : templateList) {
                item.setChildren(getSqlTemplates(version, item.getId()));
            }
            return templateList;
        } else {
            return null;
        }
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

    public ResultMessage getDebugStatus(String jobId) {
        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        return new ResultMessage(0, debugInfo, "success");
    }

    public ResultMessage saveData(String product, String jobId, String tableName, String data, String uniqueId, Integer serverId, Integer clusterId) {
        final OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        StreamServerClient09 streamServerClient = getStreamServerClient(jobId, serverId, clusterId);
        if (streamServerClient == null) {
            return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + offlineJob.getProduct());
        }

        final BaseMessage09 jobSaveDataMessage = new JobSaveDataMessage09(offlineJob.getProduct(), offlineJob.getJobName(),
                null, null, tableName, data, uniqueId, streamServerClient.getResourcePath(), null);
        return requestServer(streamServerClient, jobSaveDataMessage, true);
    }

    public ResultMessage prefetchData(String product, String jobId, String sql, String tableName,
                                      String fetchSize, String userId, Integer serverId, Integer clusterId) {
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

        ResultMessage message = syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
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

        StreamServerClient09 streamServerClient = getStreamServerClient(jobId, serverId, clusterId);
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
        final BaseMessage09 fetchDataMessage09 = new FetchDataMessage09(product, offlineJob.getJobName(),
                jobSyntaxCheckMessage.getRawSql(), jarFiles, GSON.toJson(op), streamServerClient.getResourcePath(), null);
        return requestServer(streamServerClient, fetchDataMessage09, false);
    }

    public ResultMessage getHistoryData(String product, String jobId, String sql, String userId, Integer serverId, Integer clusterId) {

        OfflineJob offlineJob = offlineJobMapper.getJobDetail(jobId);
        if (offlineJob == null) {
            return ResultMessageUtils.cookFailedMessage("No such job:" + jobId);
        }

        SyntaxCheck jobSyntaxCheckMessage = syntaxCheckMapper.getSyntaxCheckMessage(jobId, userId);
        Preconditions.checkNotNull(jobSyntaxCheckMessage, "Can't find SyntaxCheck message for jobId '" + jobId + "' in DB");

        ResultMessage message = syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
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

        StreamServerClient09 streamServerClient = getStreamServerClient(jobId, serverId, clusterId);
        if (streamServerClient == null) {
            return ResultMessageUtils.cookFailedMessage("Can't find any server for product:" + offlineJob.getProduct());
        }
        final FetchDataMessage09 fetchHbase = new FetchDataMessage09(product, offlineJob.getJobName(),
                null,
                null,
                GSON.toJson(tableTopic),
                streamServerClient.getResourcePath(),
                null);
        fetchHbase.setHistory(true);
        return requestServer(streamServerClient, fetchHbase, false);
    }


    // ----- metric interface api

    public StreamServerClient09 getStreamServerClient(String jobId, Integer serverId, Integer clusterId) {

        StreamServerClient09 streamServerClient = null;
        List<Server> serverlist = serverMapper.getServer(serverId);

        if (null == serverlist || serverlist.size() != 1) {
            return streamServerClient;
        }
        Server server = serverlist.get(0);

        if (null == clusterId) {
            return streamServerClient;
        }
        ClusterConfig clusterConfig = clusterConfigMapper.getCluster(clusterId);

        streamServerClient = new StreamServerManager09().getStreamServerFromDB(
                StreamServerManager09.ClientType.Web,
                server.getAkkaPath(),
                jobId,
                clusterConfig.getResourcePath());
        return streamServerClient;
    }

    public ResultMessage preDebug(String product, String jobId, String userId, String sql, Integer serverId, Integer clusterId) {
        DebugInfo debugInfo = debugInfoMapper.getDebugInfo(jobId);
        ResultMessage resultMessage;
        boolean DebugInfoIsNull = debugInfo == null;
        if (!DebugInfoIsNull && debugInfo.getStatus() != DebugStatusEnum.STOP.getIndex()) {
            // 当前用户不是运行用户，返回码1
            if (!userId.equalsIgnoreCase(debugInfo.getUserId())) {
                return new ResultMessage(1, debugInfo.getUserId(), debugInfo.getUserId() + " is debuging");
            } else {
                //当前用户是运行用启
                resultMessage = syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
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
        resultMessage = syntaxCheck(product, userId, jobId, sql, serverId, clusterId);
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

    /**
     * Api to fetch metric info list
     *
     * @param jobId
     * @param metricKey
     * @param startTime
     * @param endTime
     * @param point
     * @return metric info, it format looks like below:
     * ---------------------------------------------------------------
     * {
     * "code":0,
     * "data":[
     * {
     * "metricKey":"latency-source3",
     * "metricInfoList":[
     * {"ts":60000000,"value":10000},
     * {"ts":60001000,"value":10000},
     * {"ts":60002000,"value":10000},
     * {"ts":60003000,"value":10000}
     * ]
     * },
     * {"metricKey":"latency-source1","metricInfoList":[]},
     * {"metricKey":"latency-source2","metricInfoList":[]}
     * ],
     * "msg":"success"
     * }
     * ---------------------------------------------------------------
     */
    public ResultMessage getMetricInfo(String jobId, String metricKey, long startTime, long endTime, long point) {
        OnlineJob onlineJob = onlineJobMapper.getJobDetail(jobId);
        if (onlineJob != null) {
            return getMetricInfoByFlinkJobId(onlineJob.getFlinkJobId(), metricKey, startTime, endTime, point);
        } else {
            return ResultMessageUtils.cookFailedMessage("no such job: " + jobId);
        }
    }

    ResultMessage getMetricInfoByFlinkJobId(String flinkJobId, String metricKey, long startTime, long endTime, long point) {
        if (flinkJobId == null || metricKey == null) {
            return ResultMessageUtils.cookFailedMessage("flink job id is null or metric key is null");
        }

        if (startTime > endTime) {
            return ResultMessageUtils.cookFailedMessage("endTime must bigger than startTime");
        }

        if (point <= 0) {
            point = DEFAULT_POINTS;
        }
        // 1. compute sample time points
        List<Map.Entry<Long, List<Long>>> samplePoints = MetricUtil.getSampleMetricTS(startTime, endTime, point);

        // 2. flat all time points into a list (this is for implementing batch query)
        List<String> rowKeys = samplePoints.stream()
                .flatMap(a -> a.getValue().stream())
                .map(ts -> MetricUtil.getRowKey(flinkJobId, ts, metricKey))
                .collect(Collectors.toList());

        // 3. query hbase to fetch metric result
//        List<List<Map.Entry<String, String>>> rsList = metricBackend.query(rowKeys, metricKey);
        List<List<Map.Entry<String, String>>> rsList = Lists.newArrayList();

        // 4. reorganize metric result into final result
        Iterator<Map.Entry<Long, List<Long>>> iterator = samplePoints.iterator();
        Map.Entry<Long, List<Long>> currentPoint = iterator.next();
        List<List<Map.Entry<String, String>>> tmpList = new ArrayList<>();

        // this loop is a bit complicate cause we want to
        // do every thing in a single loop, this is for a good performance.
        if (rowKeys.size() != rsList.size()) {
            throw new RuntimeException("Batch query failed, this indicates a bug for hbase client.");
        }

        Map<String, List<MetricInfo.MetricInfoUnit>> taskMetricMap = new HashMap<>();

        // all sub tasks
        final Set<String> allTaskSet = new HashSet<>();
        rsList.forEach(rs -> {
            rs.forEach(rsItem -> allTaskSet.add(rsItem.getKey()));
        });

        for (List<Map.Entry<String, String>> rs : rsList) {
            tmpList.add(rs);
            if (tmpList.size() == currentPoint.getValue().size()) {
                long ts = currentPoint.getKey();

                // averaging result
                Map<String, Double> tmpMap = tmpList
                        .stream()
                        .flatMap(a -> a.stream())
                        .collect(Collectors.toMap(a -> a.getKey(),
                                b -> Double.valueOf(b.getValue()) / (double) tmpList.size(),
                                (c, d) -> c + d));

                // record metric info by task
                allTaskSet.forEach(task -> {
                    List<MetricInfo.MetricInfoUnit> taskMetricInfos = taskMetricMap.get(task);
                    if (taskMetricInfos == null) {
                        taskMetricInfos = new ArrayList<>();
                        taskMetricMap.put(task, taskMetricInfos);
                    }
                    Double value = tmpMap.get(task);
                    if (value != null) {
                        taskMetricInfos.add(new MetricInfo.MetricInfoUnit(ts, (long) Math.ceil(value)));
                    } else {
                        taskMetricInfos.add(new MetricInfo.MetricInfoUnit(ts, -1));
                    }
                });
                if (iterator.hasNext()) {
                    currentPoint = iterator.next();
                    tmpList.clear();
                } else {
                    // this break indicates that rsList has also been traversed completely.
                    break;
                }
            }
        }

        List<MetricInfo> MetricInfos = taskMetricMap.entrySet().stream()
                .map(a -> new MetricInfo(CommonUtil.getNodeName(":", a.getKey()), a.getValue()))
                .collect(Collectors.toList());

        return ResultMessageUtils.cookSuccessMessage(MetricInfos);
    }

    private void setJobParameter(String conf, List<JobParameter> defaultJobParameterList) {

        if (conf == null) {
            return;
        }

        JsonObject jsonObj;
        try {
            jsonObj = new JsonParser().parse(conf).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Parse JobConf failed.");
        }

        for (JobParameter jobParameter : defaultJobParameterList) {
            JsonElement jsonElement = jsonObj.get(jobParameter.getKeyName());
            if (jsonElement != null) {
                jobParameter.setDefaultVal(jsonElement.getAsString());
            }
        }
    }

    public ResultMessage getAvailableCluster(String product) {
        ResultMessage resultMessage = null;
        try {
            if (Strings.isNullOrEmpty(product)) {
                resultMessage = ResultMessageUtils.cookFailedMessage("product can't be null");
            } else {
                List<ClusterConfig> clusterConfigList = clusterConfigMapper.getClusters(product);
                resultMessage = ResultMessageUtils.cookSuccessMessage(clusterConfigList);
            }
        } catch (Exception e) {
            LOG.error("getAvailableCluster error", e);
            resultMessage = ResultMessageUtils.cookFailedMessage("getAvailableCluster error");
        }
        return resultMessage;
    }

    public ResultMessage getAvailableServer() {
        ResultMessage resultMessage = null;
        try {
            List<Server> serverList = serverMapper.getServer(null);
            resultMessage = ResultMessageUtils.cookSuccessMessage(serverList);
        } catch (Exception e) {
            LOG.error("getAvailableServer error", e);
            resultMessage = ResultMessageUtils.cookFailedMessage("getAvailableServer error");
        }
        return resultMessage;
    }

    public String getResourcePath(Integer clusterId) {
        ClusterConfig clusterConfig = clusterConfigMapper.getCluster(clusterId);
        if (null == clusterConfig) {
            return null;
        } else {
            return clusterConfig.getResourcePath();
        }
    }

    /**
     * Jar return object
     */
    private static class JarReturn {
        private String jarId;
        private String filePath;

        JarReturn(String jarId, String filePath) {
            this.jarId = jarId;
            this.filePath = filePath;
        }
    }

    /**
     * metric
     */
    public static class MetricInfo implements Serializable {
        public String metricKey;
        public List<MetricInfoUnit> metricInfoList;

        public MetricInfo(String metricKey, List<MetricInfoUnit> metricInfoList) {
            this.metricKey = metricKey;
            this.metricInfoList = metricInfoList;
        }

        /**
         * smallest metric unit
         */
        public static class MetricInfoUnit {
            public long ts;
            public long value;

            public MetricInfoUnit(long ts, long value) {
                this.ts = ts;
                this.value = value;
            }
        }
    }
}

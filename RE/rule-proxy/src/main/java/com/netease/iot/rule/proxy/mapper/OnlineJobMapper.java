package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.OnlineJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


@Mapper
public interface OnlineJobMapper {
    List<OnlineJob> getJobListByType();

    List<OnlineJob> getJobList(@Param("product") String product);

    List<OnlineJob> getAllOldJobs();

    List<OnlineJob> getJobListByPage(
            Map<String, Object> data);

    Integer getJobListCount(Map<String, Object> data);

    List<OnlineJob> searchJobByJobNameByPage(Map<String, Object> data);

    Integer searchJobByJobNameCount(Map<String, Object> data);

    OnlineJob getJobDetail(@Param("jobId") String jobId);

    OnlineJob getJobDetailByFlinkJobId(String userId,
                                       @Param("flinkJobId") String flinkJobId);

    OnlineJob getJobDetailByName(@Param("product") String product,
                                 @Param("jobName") String jobName);

    Integer getJobClusterId(@Param("jobId") String jobId);

    int deployJob(
            @Param("jobId") String jobId,
            @Param("jobName") String jobName,
            @Param("deployedSql") String deployedSql,
            @Param("conf") String conf,
            @Param("version") int version,
            @Param("creator") String creator,
            @Param("editor") String editor,
            @Param("product") String product,
            @Param("createTime") String createTime,
            @Param("editTime") String editTime,
            @Param("deployNote") String deployNote,
            @Param("deployTime") String deployTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("versionStatus") Integer versionStatus,
            @Param("savePointPath") String savePointPath
    );

    int reDeployJob(
            @Param("jobId") String jobId,
            @Param("jobName") String jobName,
            @Param("deployedSql") String deployedSql,
            @Param("conf") String conf,
            @Param("version") int version,
            @Param("creator") String creator,
            @Param("editor") String editor,
            @Param("product") String product,
            @Param("createTime") String createTime,
            @Param("editTime") String editTime,
            @Param("deployNote") String deployNote,
            @Param("deployTime") String deployTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("versionStatus") Integer versionStatus,
            @Param("statusList") List<Integer> statusList
    );

    boolean setJobStatus(
            @Param("jobId") String jobId,
            @Param("status") int status,
            @Param("stateEditTime") String stateEditTime
    );

    boolean setJobStatusByFlinkJobId(
            @Param("flinkJobId") String flinkJobId,
            @Param("status") int status,
            @Param("stateEditTime") String stateEditTime
    );

    boolean removeJob(
            @Param("jobId") String jobId);

    boolean updateStartTime(
            @Param("jobId") String jobId,
            @Param("startTime") String startTime,
            @Param("stateEditTime") String stateEditTime
    );

    List<OnlineJob> batchGetJob(@Param("jobList") List<String> jobList,
                                @Param("product") String product);

    boolean updateJobAfterStartSuccessfully(@Param("jobId") String jobId,
                                            @Param("status") Integer status,
                                            @Param("flinkJobId") String flinkJobId,
                                            @Param("jarPath") String jarPath,
                                            @Param("savePointPath") String savePointPath,
                                            @Param("stateEditTime") String stateEditTime,
                                            @Param("applicationId") String applicationId);

    boolean saveSavePointToOffLine(@Param("jobId") String jobId, @Param("savePointPath")
            String savePointPath, @Param("type") Integer type);

    List<Map<String, Object>> getNewVersionWebUi(String product);

    int updateSql(@Param("jobId") String jobId,
                  @Param("sql") String sql);

    int iotDeployJob(
            @Param("jobId") String jobId,
            @Param("jobName") String jobName,
            @Param("deployedSql") String deployedSql,
            @Param("conf") String conf,
            @Param("version") int version,
            @Param("creator") String creator,
            @Param("editor") String editor,
            @Param("product") String product,
            @Param("createTime") String createTime,
            @Param("editTime") String editTime,
            @Param("deployNote") String deployNote,
            @Param("deployTime") String deployTime,
            @Param("clusterId") Integer clusterId,
            @Param("serverId") Integer serverId,
            @Param("versionStatus") Integer versionStatus,
            @Param("savePointPath") String savePointPath
    );

    int iotReDeployJob(
            @Param("jobId") String jobId,
            @Param("jobName") String jobName,
            @Param("editor") String editor,
            @Param("product") String product,
            @Param("editTime") String editTime,
            @Param("deployTime") String deployTime,
            @Param("deployedSql") String deployedSql,
            @Param("executedSql") String executedSql,
            @Param("statusList") List<Integer> statusList
    );

    int iotOnlineJobCount(@Param("product") String product);
}
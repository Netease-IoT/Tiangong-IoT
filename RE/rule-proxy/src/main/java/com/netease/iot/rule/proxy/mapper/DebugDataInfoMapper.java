package com.netease.iot.rule.proxy.mapper;


import com.netease.iot.rule.proxy.domain.DebugDataInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DebugDataInfoMapper {

    /**
     * Get debug data topic info
     *
     * @param jobId
     * @return
     */
    DebugDataInfo getDebugDataInfo(@Param("jobId") String jobId,
                                   @Param("tableName") String tableName);

    List<DebugDataInfo> batchGetDebugDataInfo(@Param("jobId") String jobId,
                                              @Param("tableNames") List<String> tableNames);

    /**
     * @param jobId
     * @param tableName
     * @param lastTopic
     */
    void updateDebugDataInfo(
            @Param("jobId") String jobId,
            @Param("tableName") String tableName,
            @Param("lastTopic") String lastTopic);

    /**
     * @param jobId
     * @param tableName
     * @param lastTopic
     */
    void insertDebugDataInfo(
            @Param("jobId") String jobId,
            @Param("tableName") String tableName,
            @Param("lastTopic") String lastTopic);

    void batchRepalceIntoDebugDataInfo(@Param("dataList") List<DebugDataInfo> dataList);
}

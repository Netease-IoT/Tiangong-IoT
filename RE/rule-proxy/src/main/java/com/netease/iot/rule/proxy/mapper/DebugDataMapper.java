package com.netease.iot.rule.proxy.mapper;


import com.netease.iot.rule.proxy.domain.DebugData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DebugDataMapper {
    /**
     * Get debug Data list
     *
     * @param tableNames
     * @param jobid
     * @return
     */
    List<DebugData> getDebugDataList(
            @Param("tableNames") List<String> tableNames,
            @Param("jobId") String jobid);

    /**
     * Save Debug Data
     *
     * @param debugDataList
     * @return
     */
    boolean saveDebugDataList(
            @Param("list") List<DebugData> debugDataList);

    /**
     * Delete by list
     *
     * @param jobIdList
     * @return
     */
    boolean deleteDebugDataList(@Param("jobIdList") List<DebugData> jobIdList);
}

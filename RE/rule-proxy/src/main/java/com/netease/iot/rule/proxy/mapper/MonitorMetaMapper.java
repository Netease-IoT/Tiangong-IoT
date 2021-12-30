package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.MonitorMeta;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MonitorMetaMapper {

    List<MonitorMeta> getAllMeta();

    MonitorMeta getMeta(
            @Param("jobId") String jobId);

    boolean setMonitorMeta(
            @Param("jobId") String jobId,
            @Param("group") Integer group,
            @Param("mailList") String mailList,
            @Param("alarmTypes") String alarmTypes,
            @Param("interval") Integer interval,
            @Param("threshold") Integer threshold);

    boolean updateMonitorGroup(
            @Param("group") Integer group,
            @Param("mailList") String mailList);

    List<MonitorMeta> selectByGroup(
            @Param("group") Integer group);

    int createDefaultMeta(
            @Param("jobId") String jobId,
            @Param("mailList") String mailList);

    boolean deleteMeta(@Param("jobId") String jobId);
}

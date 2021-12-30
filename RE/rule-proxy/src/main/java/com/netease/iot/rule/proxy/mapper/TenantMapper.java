package com.netease.iot.rule.proxy.mapper;

import org.apache.ibatis.annotations.Param;

public interface TenantMapper {

    int getServerId(@Param("tenantId") String tenantId);
}

package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.ClusterConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClusterConfigMapper {

    List<ClusterConfig> getClusters(@Param("product") String product);

    ClusterConfig getCluster(@Param("clusterId")Integer clusterId);
}

package com.netease.iot.rule.proxy.mapper;

import com.netease.iot.rule.proxy.domain.Cluster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClusterMapper {
    List<Cluster> getCluster(@Param("product") String product);

    Cluster getClusterById(@Param("clusterId") Integer clusterId);

    List<String> getDistinctClusterWebUrls();

    List<Integer> getClusterIdByWebUrl(@Param("clusterWebUi") String clusterWebUi);
}

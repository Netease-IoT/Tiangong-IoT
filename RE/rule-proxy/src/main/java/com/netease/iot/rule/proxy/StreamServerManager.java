package com.netease.iot.rule.proxy;

import com.netease.iot.rule.proxy.domain.Cluster;
import com.netease.iot.rule.proxy.mapper.ClusterMapper;


public class StreamServerManager {

    /**
     * rule client type, e.g: web and proxy
     */
    public enum ClientType {
        Web
    }

    public StreamServerClient getStreamServerFromDB(ClientType client, String product, Integer clusterId, ClusterMapper mapper) {

        Cluster cluster = mapper.getClusterById(clusterId);
        return new StreamServerClient(client.name(), cluster);
    }
}

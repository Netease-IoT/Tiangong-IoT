package com.netease.iot.rule.proxy;


public class StreamServerManager09 {

    /**
     *  client type, e.g: web and proxy
     */
    public enum ClientType {
        Web
    }

    public StreamServerClient09 getStreamServerFromDB(ClientType client, String akkaPath, String jobId, String resourcePath) {
        return new StreamServerClient09(client.name(), akkaPath, jobId, resourcePath);
    }
}

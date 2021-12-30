package com.netease.iot.rule.proxy.util;

import org.apache.http.Consts;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;


public class HttpClientFactoryBean {

    private int maxTotal;

    private int maxPerRoute;

    private int socketTimeout;

    private int connectTimeout;

    private int connectionRequestTimeout;

    public HttpClient getInstance() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(Consts.UTF_8).build());

        RequestConfig defaultRequestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout).build();

        return HttpClients.custom().setConnectionManager(connectionManager)
                .setDefaultRequestConfig(defaultRequestConfig).build();
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public void setMaxPerRoute(int maxPerRoute) {
        this.maxPerRoute = maxPerRoute;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }
}

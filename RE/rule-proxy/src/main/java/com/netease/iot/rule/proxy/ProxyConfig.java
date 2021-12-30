package com.netease.iot.rule.proxy;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class ProxyConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfig.class);

    private static final String CONF_FILENAME = "conf.properties";

    private static volatile Configuration config = null;

    static {
        Configurations configs = new Configurations();
        try {
            config = configs.properties(new File(CONF_FILENAME));
            LOGGER.info("Loaded configuration file '{}', properties: {}", CONF_FILENAME, config);
        } catch (Throwable t) {
            LOGGER.error("Load configuration file '{}' failed.", CONF_FILENAME, t);
            System.exit(-1);
        }
    }

    public static final String PROXY_ACTOR_SYSTEM_CONFIG =
            config.getString("proxyActorSystemConfig", "RuleProxy");
    public static final int PROXY_CONNECT_TIMEOUT =
            config.getInt("proxyConnectTimeout", 60);
    public static final String UPLOAD_JAR_PATH =
            config.getString("uploadJarPath", "");
    public static final Integer REQUEST_TIME_OUT =
            config.getInt("requestTimeout", 60000);
    public static final Integer GET_RESULT_TIMEOUT =
            config.getInt("getResultTimeout", 60000);
    public static final Integer IOT_LIMIT = config.getInt("iot.limit", 10);
    public static final String DEFAULT_SERVER_REMOTE_AKKA = config.getString("defaultServerRemoteActorPath", "");
    public static final Integer DEFAULT_SERVER_REMOTE_AKKA_TIMEOUT = config.getInt("defaultServerRemoteTimeOut", 30);
    public static final String IOT_AUTO_SQL = config.getString("iot.auto.sql", "");
    public static final String IOT_CONF = config.getString("iot.conf", "");

    public static String getServerRemoteActorPath(String targetStreamServer) {
        return config.getString(targetStreamServer + "_RemoteActorPath");
    }
}

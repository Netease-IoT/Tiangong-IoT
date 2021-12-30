package com.netease.iot.rule.proxy.metric;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationUtils;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MetricConfig {
    private static final Logger LOG = LoggerFactory.getLogger(MetricConfig.class);

    public static final String CONF_FILENAME = "metric.properties";
    public static Configuration metricConfig;

    static {
        try {
            Configurations configs = new Configurations();
            metricConfig = configs.properties(new File(CONF_FILENAME));
            LOG.info("Loaded configuration file '{}', properties: {}", CONF_FILENAME, ConfigurationUtils.toString(metricConfig));
        } catch (Throwable t) {
            LOG.error("Load configuration file '{}' failed: {}", CONF_FILENAME, t);
            System.exit(-1);
        }
    }
}

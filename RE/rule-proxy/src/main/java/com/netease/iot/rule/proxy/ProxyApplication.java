package com.netease.iot.rule.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;


@SpringBootApplication
@ImportResource("classpath:spring/spring-context.xml")
public class ProxyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyApplication.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("starting the rule proxy");
        SpringApplication.run(ProxyApplication.class, args);
    }
}

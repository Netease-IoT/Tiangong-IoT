package com.netease.iot.rule.proxy.akka;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActorSystemFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemFactory.class);

    private static ActorSystem actorSystem;

    private static String actorSystemConfig;

    private static final Object LOCK = new Object();

    private static volatile boolean isLoaded = false;

    // There should be only one actorSystem in current process，try to create more than one actorSystem
    // with different config will throw RuntimeException
    public static ActorSystem getSendActorSystem(String actorSystemConfig) {
        synchronized (LOCK) {
            //TODO this should be a register table to record multi acotrSystem, current only support one.
            if (!isLoaded) {
                LOG.info("Starting actorSystem with config {}.", actorSystemConfig);
                actorSystem = ActorSystem.create(actorSystemConfig, ConfigFactory.load("akka")
                        .getConfig(actorSystemConfig));
                isLoaded = true;
                ActorSystemFactory.actorSystemConfig = actorSystemConfig;
            } else {
                if (!ActorSystemFactory.actorSystemConfig.equals(actorSystemConfig)) {
                    LOG.error("Try to start actorSystem with config {} while there is a exist actorSystem {}.",
                            actorSystemConfig, ActorSystemFactory.actorSystemConfig);
                    throw new RuntimeException();
                }
            }
            return actorSystem;
        }
    }

    public static void shutdown() {
        synchronized (LOCK) {
            //TODO this should be a register table to record multi acotrSystem, current only support one.
            if (isLoaded) {
                actorSystem.shutdown();
                actorSystem.awaitTermination();
                actorSystem = null;
                isLoaded = false;
                LOG.info("Shutdown actor system.");
            }
        }
    }
}

package com.netease.iot.rule.proxy;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.netease.iot.rule.proxy.akka.ActorSystemFactory;
import com.netease.iot.rule.proxy.akka.Submit;
import com.netease.iot.rule.proxy.model.BaseMessage09;
import com.netease.iot.rule.proxy.model.JobResumeMessage09;
import scala.concurrent.Future;

import java.util.concurrent.TimeUnit;


public class StreamServerClient09 implements Submit {

    private ActorSystem actorSystem;
    private ActorSelection actor;
    private String jobId;
    private String resourcePath;

    public String getResourcePath() {
        return resourcePath;
    }

    public StreamServerClient09(String product, String client) {
        actorSystem = ActorSystemFactory.getSendActorSystem(client);
        actor = actorSystem.actorSelection(ProxyConfig.getServerRemoteActorPath(product));
    }

    public StreamServerClient09(String client, String akkaPath, String jobId, String resourcePath) {
        this.resourcePath = resourcePath;
        this.jobId = jobId;
        actorSystem = ActorSystemFactory.getSendActorSystem(client);
        actor = actorSystem.actorSelection(akkaPath);
    }

    public StreamServerClient09(String system, String akkaPath, String test) {
        actorSystem = ActorSystemFactory.getSendActorSystem(system);
        actor = actorSystem.actorSelection(akkaPath);
    }

    /**
     * Submit message to server.
     */
    public Future<Object> submit(Object message, int timeout) {
        if (BaseMessage09.class.isAssignableFrom(message.getClass())) {
            BaseMessage09 msg = (BaseMessage09) message;
            if (JobResumeMessage09.class.isAssignableFrom(message.getClass())) {
                msg = ((JobResumeMessage09) message).jobCreateMessage;
            }
            msg.setJobId(jobId);
            msg.setResourcePath(resourcePath);
            return Patterns.ask(actor, message, new Timeout(timeout, TimeUnit.SECONDS));
        } else {
            return Patterns.ask(actor, message, new Timeout(timeout, TimeUnit.SECONDS));
        }
    }
}

package com.netease.iot.rule.proxy.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class JobStatusUtil {
    /**
     * Job state transfer graph
     */
    public static final Map<TaskStateEnum, Set<TaskStateEnum>> STATE_TRANSLATE_MAP = new HashMap<TaskStateEnum, Set<TaskStateEnum>>() {
        {
            Set<TaskStateEnum> readyTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.STARTING);
                    this.add(TaskStateEnum.DELETING);
                    this.add(TaskStateEnum.READY);
                    this.add(TaskStateEnum.RESUMING);
                }
            };
            put(TaskStateEnum.READY, readyTranslateSet);

            Set<TaskStateEnum> startingTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.RUNNING);
                    this.add(TaskStateEnum.FAILED);
                }
            };
            put(TaskStateEnum.STARTING, startingTranslateSet);

            Set<TaskStateEnum> runningTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.SUSPENDING);
                    this.add(TaskStateEnum.FAILED);
                }
            };
            put(TaskStateEnum.RUNNING, runningTranslateSet);

            Set<TaskStateEnum> suspendingTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.SUSPENDING);
                    this.add(TaskStateEnum.SUSPENDED);
                    this.add(TaskStateEnum.FAILED);
                }
            };
            put(TaskStateEnum.SUSPENDING, suspendingTranslateSet);

            Set<TaskStateEnum> suspendedTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.DELETING);
                    this.add(TaskStateEnum.RESUMING);
                }
            };
            put(TaskStateEnum.SUSPENDED, suspendedTranslateSet);

            Set<TaskStateEnum> deletingTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.DELETED);
                    this.add(TaskStateEnum.FAILED);
                }
            };
            put(TaskStateEnum.DELETING, deletingTranslateSet);

            Set<TaskStateEnum> deletedTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.READY);
                }
            };
            put(TaskStateEnum.DELETED, deletedTranslateSet);

            Set<TaskStateEnum> resumingTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.RESUMING);
                    this.add(TaskStateEnum.RUNNING);
                    this.add(TaskStateEnum.FAILED);
                }
            };
            put(TaskStateEnum.RESUMING, resumingTranslateSet);

            Set<TaskStateEnum> failedTranslateSet = new HashSet<TaskStateEnum>() {
                {
                    this.add(TaskStateEnum.READY);
                    this.add(TaskStateEnum.RESUMING);
                    this.add(TaskStateEnum.DELETING);
                    this.add(TaskStateEnum.STARTING);
                }
            };
            put(TaskStateEnum.FAILED, failedTranslateSet);
        }
    };
}

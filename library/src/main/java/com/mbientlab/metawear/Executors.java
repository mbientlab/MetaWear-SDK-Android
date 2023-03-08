package com.mbientlab.metawear;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class Executors {
    public final static Executor IMMEDIATE_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor();
    public final static ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
}

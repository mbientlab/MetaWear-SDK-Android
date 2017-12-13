package com.mbientlab.metawear.impl.platform;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import bolts.CancellationTokenSource;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * Created by eric on 12/8/17.
 */
public class TimedTask<T> {
    private TaskCompletionSource<T> taskSource;
    private CancellationTokenSource cts;

    public TimedTask() { }

    public Task<T> execute(String msgFormat, long timeout, Runnable action) {
        if (taskSource != null && !taskSource.getTask().isCompleted()) {
            return taskSource.getTask();
        }

        cts = new CancellationTokenSource();
        taskSource = new TaskCompletionSource<>();
        action.run();

        if (timeout != 0) {
            final ArrayList<Task<?>> tasks = new ArrayList<>();
            tasks.add(taskSource.getTask());
            tasks.add(Task.delay(timeout, cts.getToken()));

            Task.whenAny(tasks).continueWith(task -> {
                if (task.getResult() != tasks.get(0)) {
                    setError(new TimeoutException(String.format(msgFormat, timeout)));
                } else {
                    cts.cancel();
                }
                return null;
            });
        }
        return taskSource.getTask();
    }

    public boolean isCompleted() {
        return taskSource != null && taskSource.getTask().isCompleted();
    }

    public void cancel() {
        taskSource.trySetCancelled();
    }

    public void setResult(T result) {
        taskSource.trySetResult(result);
    }

    public void setError(Exception error) {
        taskSource.trySetError(error);
    }
}
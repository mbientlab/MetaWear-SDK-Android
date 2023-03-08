package com.mbientlab.metawear.impl.platform;

import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.concurrent.TimeoutException;

/**
 * Created by eric on 12/8/17.
 */
public class TimedTask<T> {
    private TaskCompletionSource<T> taskSource;
    private CancellationTokenSource cts;

    public TimedTask() { }

    public Task<T> execute(String msgFormat, long timeout, Runnable action) {
        if (taskSource != null && !taskSource.getTask().isComplete()) {
            return taskSource.getTask();
        }

        cts = new CancellationTokenSource();
        taskSource = new TaskCompletionSource<>();
        action.run();

        if (timeout != 0) {
//            final ArrayList<Task<?>> tasks = new ArrayList<>();
//            tasks.add(taskSource.getTask());
//            tasks.add(Tasks.withTimeout(Tasks.forResult(cts.getToken()), timeout, TimeUnit.MILLISECONDS));

            if (taskSource.getTask().isCanceled()) {
                setError(new TimeoutException(String.format(msgFormat, timeout)));
            } else {
                return taskSource.getTask();
            }

//            Tasks.whenAll(tasks).continueWithTask(IMMEDIATE_EXECUTOR, task -> {
//                if (task != tasks.get(0)) {
//                    setError(new TimeoutException(String.format(msgFormat, timeout)));
//                } else {
//                    cts.cancel();
//                }
//                return null;
//            });
        }
        return taskSource.getTask();
    }

    public boolean isCompleted() {
        return taskSource != null && taskSource.getTask().isComplete();
    }

    public void cancel() {
        cts.cancel();
    }

    public void setResult(T result) {
        taskSource.trySetResult(result);
    }

    public void setError(Exception error) {
        taskSource.setException(error);
    }
}